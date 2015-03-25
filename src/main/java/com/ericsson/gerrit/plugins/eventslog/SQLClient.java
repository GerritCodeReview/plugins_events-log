// Copyright (C) 2015 Ericsson
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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;

import org.apache.commons.dbcp.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.gerrit.server.events.ProjectEvent;
import com.google.gson.Gson;
import com.google.inject.Inject;

public class SQLClient {

  private static final Logger log = LoggerFactory.getLogger(SQLClient.class);

  private BasicDataSource ds;
  private final int maxAge;
  private final Gson gson = new Gson();

  @Inject
  SQLClient(EventsLogConfig cfg) {
    this.maxAge = cfg.getMaxAge();
    ds = new BasicDataSource();
    ds.setDriverClassName(cfg.getStoreDriver());
    ds.setUrl(cfg.getStoreUrl() + TABLE_NAME + ";" + cfg.getUrlOptions());
    ds.setUsername(cfg.getStoreUsername());
    ds.setPassword(cfg.getStorePassword());
  }

  public void createDBIfNotCreated() throws SQLException {
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
  }

  public void close() throws SQLException {
    ds.close();
  }

  public ListMultimap<String, String> getEvents(String query)
      throws MalformedQueryException {
    Connection conn = null;
    Statement stat = null;
    ResultSet rs = null;
    try {
      conn = ds.getConnection();
      stat = conn.createStatement();
      try {
        rs = stat.executeQuery(query);
        ListMultimap<String, String> result = ArrayListMultimap.create();
        while (rs.next()) {
          result.put(rs.getString(PROJECT_ENTRY), rs.getString(EVENT_ENTRY));
        }
        return result;
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
  }

  public void storeEvent(ProjectEvent event) throws SQLException {
    String json = gson.toJson(event);
    Connection conn = ds.getConnection();
    Statement stat = conn.createStatement();
    try {
      stat.execute(format("INSERT INTO %s(%s, %s, %s) ", TABLE_NAME,
          PROJECT_ENTRY, DATE_ENTRY, EVENT_ENTRY)
          + format("VALUES('%s', '%s', '%s')", event.getProjectNameKey().get(),
              new Timestamp(event.eventCreatedOn * 1000L), json));
    } finally {
      closeStatement(stat);
      closeConnection(conn);
    }
  }

  public void removeOldEvents() throws SQLException {
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
  }

  public void removeProjectEvents(String project) throws SQLException {
    Connection conn = ds.getConnection();
    Statement stat = conn.createStatement();
    try {
      stat.execute(String.format("DELETE FROM %s WHERE project = '%s'",
          TABLE_NAME, project));
    } finally {
      closeStatement(stat);
      closeConnection(conn);
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
