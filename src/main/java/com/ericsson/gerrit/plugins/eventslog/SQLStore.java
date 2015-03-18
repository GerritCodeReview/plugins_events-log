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

import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.CurrentUser;
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
import java.net.ConnectException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class SQLStore implements EventStore, LifecycleListener {
  private static final Logger log = LoggerFactory.getLogger(SQLStore.class);

  private final ProjectControl.GenericFactory projectControlFactory;
  private final Provider<CurrentUser> userProvider;
  private final String path;
  private final String username;
  private final String password;
  private final String driver;
  private final int maxTries;
  private final int waitTime;
  private final Gson gson = new Gson();

  private SQLHandler sql;
  private BasicDataSource ds;

  @Inject
  SQLStore(ProjectControl.GenericFactory projectControlFactory,
      Provider<CurrentUser> userProvider, EventsLogConfig cfg, SQLHandler sql) {
    this.path = cfg.getStoreUrl() + TABLE_NAME + ";"
      + cfg.getUrlOptions();
    this.driver = cfg.getStoreDriver();
    this.maxTries = cfg.getMaxTries();
    this.waitTime = cfg.getWaitTime();
    this.username = cfg.getStoreUsername();
    this.password = cfg.getStorePassword();
    this.projectControlFactory = projectControlFactory;
    this.userProvider = userProvider;
    this.sql = sql;
  }

  @Override
  public void start() {
    try {
      ds = new BasicDataSource();
      ds.setDriverClassName(driver);
      ds.setUrl(path);
      ds.setUsername(username);
      ds.setPassword(password);
      Connection conn = sql.getConnection(ds);

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

      Statement stat = sql.createStatement(conn);
      try {
        sql.execute(stat, query.toString());
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
      sql.stopDs(ds);
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
      conn = sql.getConnection(ds);
      stat = sql.createStatement(conn);
      try {
        Project.NameKey project = null;
        rs = sql.executeQuery(stat, query);
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
                + " from cache", e);
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
    int failedConnections = 0;
    boolean done = false;
    while (!done) {
      done = true;
      try {
        Connection conn = sql.getConnection(ds);
        Statement stat = sql.createStatement(conn);
        try {
          sql.executeStore(stat, event, json);
        } finally {
          closeStatement(stat);
          closeConnection(conn);
        }
      } catch (SQLException e) {
        log.warn("Cannot store ChangeEvent for: " + projectName.get() + "\n"
            + e.toString());
        if (e.getCause() instanceof ConnectException
            || e.getMessage().contains("terminating connection")) {
          if (maxTries == 0) {
          } else if (failedConnections < maxTries-1) {
            failedConnections++;
            done = false;
            log.info("Retrying store event");
            try {
              Thread.sleep(waitTime);
            } catch (InterruptedException e1) {
              continue;
            }
          } else {
            log.error("Failed to store event " + maxTries + " times");
          }
        }
      }
    }
  }

  private void removeOldEntries() {
    try {
      Connection conn = ds.getConnection();
      Statement stat = conn.createStatement();
      try {
        sql.executeRemoveOld(stat);
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
        sql.executeRemoveProjectEntries(stat, project);
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
        sql.closeResultSet(resultSet);
      } catch (SQLException e) {
        log.warn("Cannot close result set", e);
      }
    }
  }

  private void closeStatement(Statement stat) {
    if (stat != null) {
      try {
        sql.closeStatement(stat);
      } catch (SQLException e) {
        log.warn("Cannot close statement", e);
      }
    }
  }

  private void closeConnection(Connection conn) {
    if (conn != null) {
      try {
        sql.closeConnection(conn);
      } catch (SQLException e) {
        log.warn("Cannot close connection", e);
      }
    }
  }

  public BasicDataSource getDs() {
    return ds;
  }
}
