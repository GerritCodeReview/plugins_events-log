// Copyright (C) 2014 Ericsson
//
// Licensed under the Apache License, Version 2.0 (the "License"),
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.ericsson.gerrit.plugins.eventslog;

import static com.ericsson.gerrit.plugins.eventslog.SQLTable.DATE_ENTRY;
import static com.ericsson.gerrit.plugins.eventslog.SQLTable.EVENT_ENTRY;
import static com.ericsson.gerrit.plugins.eventslog.SQLTable.PRIMARY_ENTRY;
import static com.ericsson.gerrit.plugins.eventslog.SQLTable.PROJECT_ENTRY;
import static com.ericsson.gerrit.plugins.eventslog.SQLTable.TABLE_NAME;
import static java.lang.String.format;

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.events.ProjectEvent;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.gerrit.server.project.ProjectControl;
import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import org.apache.commons.dbcp.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Singleton
public class SQLStore implements EventStore, LifecycleListener {
  private static final Logger log = LoggerFactory.getLogger(SQLStore.class);

  private static final String DEFAULT_URL = "jdbc:h2:~/db/";
  private static final String DEFAULT_DRIVER = "org.h2.Driver";
  private static final String CONFIG_URL = "storeUrl";
  private static final String CONFIG_DRIVER = "storeDriver";
  private static final String CONFIG_URL_OPTIONS = "urlOptions";
  private static final String CONFIG_MAX_AGE = "maxAge";
  private static final String CONFIG_USERNAME = "storeUsername";
  private static final String CONFIG_PASSWORD = "storePassword";
  private static final int DEFAULT_MAX_AGE = 30;

  private final ProjectControl.GenericFactory projectControlFactory;
  private final Provider<CurrentUser> userProvider;
  private final String path;
  private final String username;
  private final String password;
  private final String driver;
  private final int maxAge;
  private final Gson gson = new Gson();

  private BasicDataSource ds;

  @Inject
  SQLStore(ProjectControl.GenericFactory projectControlFactory,
      Provider<CurrentUser> userProvider, PluginConfigFactory cfgFactory,
      @PluginName String pluginName) {
    PluginConfig cfg = cfgFactory.getFromGerritConfig(pluginName, true);
    this.path = cfg.getString(CONFIG_URL, DEFAULT_URL) + TABLE_NAME + ";"
      + cfg.getString(CONFIG_URL_OPTIONS, "");
    this.driver = cfg.getString(CONFIG_DRIVER, DEFAULT_DRIVER);
    this.maxAge = cfg.getInt(CONFIG_MAX_AGE, DEFAULT_MAX_AGE);
    this.username = cfg.getString(CONFIG_USERNAME);
    this.password = cfg.getString(CONFIG_PASSWORD);
    this.projectControlFactory = projectControlFactory;
    this.userProvider = userProvider;
  }

  @Override
  public void start() {
    try {
      ds = new BasicDataSource();
      ds.setDriverClassName(driver);
      ds.setUrl(path);
      ds.setUsername(username);
      ds.setPassword(password);
      Connection conn = ds.getConnection();

      StringBuilder query = new StringBuilder();
      query.append(format("CREATE TABLE IF NOT EXISTS %s(", TABLE_NAME));
      if (ds.getDriverClassName().contains("postgresql")) {
        query.append(format("%s SERIAL PRIMARY KEY,", PRIMARY_ENTRY));
      } else {
        query.append(format("%s INT AUTO_INCREMENT PRIMARY KEY,", PRIMARY_ENTRY));
      }
      query.append(format("%s VARCHAR(255),", PROJECT_ENTRY));
      query.append(format("%s TIMESTAMP DEFAULT NOW(),", DATE_ENTRY));
      query.append(format("%s TEXT)", EVENT_ENTRY));

      Statement stat = conn.createStatement();
      try {
        stat.execute(query.toString());
      } finally {
        closeStatement(stat);
        closeConnection(conn);
      }
    } catch (SQLException e) {
      throw new RuntimeException("Cannot start the database", e);
    }
    removeOldEntries();
  }

  @Override
  public void stop() {
    try {
       ds.close();
     } catch (SQLException e) {
       throw new RuntimeException("Cannot close datasource ", e);
     }
  }

  @Override
  public List<String> queryChangeEvents(String query)
      throws MalformedQueryException {
    List<String> events = new ArrayList<>();
    Connection conn = null;
    Statement stat = null;
    ResultSet rs = null;
    try {
      conn = ds.getConnection();
      stat = conn.createStatement();
      try {
        Project.NameKey project = null;
        rs = stat.executeQuery(query);
        while (rs.next()) {
          try {
            project = new Project.NameKey(rs.getString(PROJECT_ENTRY));
            if (projectControlFactory.controlFor(project, userProvider.get())
                .isVisible()) {
              events.add(rs.getString(EVENT_ENTRY));
            }
          } catch (NoSuchProjectException e) {
            log.warn("Database contains a non-existing project, " + project.get()
                + ", removing project from database", e);
            removeProjectEntries(project.get());
          } catch (IOException e) {
            log.warn("Cannot get project visibility info for " + project.get()
                + "from cache", e);
          }
        }
      } catch (SQLException e) {
        throw new MalformedQueryException(e);
      }
    } catch (SQLException e) {
      throw new RuntimeException("Cannot query database", e);
    } finally {
      closeResultSet(rs);
      closeStatement(stat);
      closeConnection(conn);
    }
    return events;
  }

  @Override
  public void storeEvent(ProjectEvent event) {
    Project.NameKey projectName = event.getProjectNameKey();
    if (projectName == null) {
      return;
    }
    String json = gson.toJson(event);
    try {
      Connection conn = ds.getConnection();
      Statement stat = conn.createStatement();
      try {
        stat.execute(format("INSERT INTO %s(%s, %s, %s) ",
          TABLE_NAME, PROJECT_ENTRY, DATE_ENTRY, EVENT_ENTRY)
          + format("VALUES('%s', '%s', '%s')", projectName.get(), new Timestamp(event.eventCreatedOn * 1000L), json));
      } finally {
        closeStatement(stat);
        closeConnection(conn);
      }
    } catch (SQLException e) {
      log.warn("Cannot store ChangeEvent for: " + projectName.get(), e);
    }
  }

  private void removeOldEntries() {
    try {
      Connection conn = ds.getConnection();
      Statement stat = conn.createStatement();
      try {
        stat.execute(format("DELETE FROM %s WHERE %s < '%s'", TABLE_NAME,
            DATE_ENTRY, new Timestamp(System.currentTimeMillis()
                - TimeUnit.MILLISECONDS.convert(maxAge, TimeUnit.DAYS))));
      } finally {
        closeStatement(stat);
        closeConnection(conn);
      }
    } catch (SQLException e) {
      log.warn("Cannot remove old entries from database", e);
    }
  }

  private void removeProjectEntries(String project) {
    try {
      Connection conn = ds.getConnection();
      Statement stat = conn.createStatement();
      try {
        stat.execute(String.format("DELETE FROM %s WHERE project = '%s'",
            TABLE_NAME, project));
      } finally {
        closeStatement(stat);
        closeConnection(conn);
      }
    } catch (SQLException e) {
      log.warn("Cannot remove project " + project + " from database", e);
    }
  }

  private void closeResultSet(ResultSet resultSet) {
    if (resultSet != null) {
      try {
        resultSet.close();
      } catch (SQLException e) {
        log.warn("Cannot close result set", e);
      }
    }
  }

  private void closeStatement(Statement stat) {
    if (stat != null) {
      try {
        stat.close();
      } catch (SQLException e) {
        log.warn("Cannot close statement", e);
      }
    }
  }

  private void closeConnection(Connection conn) {
    if (conn != null) {
      try {
        conn.close();
      } catch (SQLException e) {
        log.warn("Cannot close connection", e);
      }
    }
  }
}
