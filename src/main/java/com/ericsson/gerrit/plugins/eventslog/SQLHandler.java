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
import static com.ericsson.gerrit.plugins.eventslog.SQLTable.PROJECT_ENTRY;
import static com.ericsson.gerrit.plugins.eventslog.SQLTable.TABLE_NAME;
import static java.lang.String.format;

import com.google.gerrit.server.events.ProjectEvent;
import com.google.inject.Inject;

import org.apache.commons.dbcp.BasicDataSource;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.concurrent.TimeUnit;

public class SQLHandler {

  private final int maxAge;

  @Inject
  SQLHandler(EventsLogConfig cfg) {
    this.maxAge = cfg.getMaxAge();
  }

  public void stopDataSource(BasicDataSource ds) throws SQLException {
    ds.close();
  }

  public void closeResultSet(ResultSet resultSet) throws SQLException {
    resultSet.close();
  }

  public void closeStatement(Statement stat) throws SQLException {
    stat.close();
  }

  public void closeConnection(Connection conn) throws SQLException {
    conn.close();
  }

  public Connection getConnection(BasicDataSource ds) throws SQLException {
    return ds.getConnection();
  }

  public Statement createStatement(Connection conn) throws SQLException {
    return conn.createStatement();
  }

  public void execute(Statement stat, String query) throws SQLException {
    stat.execute(query);
  }

  public ResultSet executeQuery(Statement stat, String query) throws SQLException {
    return stat.executeQuery(query);
  }

  public void executeStore(Statement stat, ProjectEvent event, String json) throws SQLException {
    stat.execute(format("INSERT INTO %s(%s, %s, %s) ",
        TABLE_NAME, PROJECT_ENTRY, DATE_ENTRY, EVENT_ENTRY)
        + format("VALUES('%s', '%s', '%s')", event.getProjectNameKey().get(),
            new Timestamp(event.eventCreatedOn * 1000L), json));
  }

  public void executeRemoveOld(Statement stat) throws SQLException {
    stat.execute(format("DELETE FROM %s WHERE %s < '%s'", TABLE_NAME,
        DATE_ENTRY, new Timestamp(System.currentTimeMillis()
            - TimeUnit.MILLISECONDS.convert(maxAge, TimeUnit.DAYS))));
  }

  public void executeRemoveProjectEntries(Statement stat, String project) throws SQLException {
    stat.execute(String.format("DELETE FROM %s WHERE project = '%s'",
        TABLE_NAME, project));
  }
}
