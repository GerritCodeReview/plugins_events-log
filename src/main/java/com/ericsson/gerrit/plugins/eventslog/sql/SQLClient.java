// Copyright (C) 2015 Ericsson
//
// Licensed under the Apache License, Version 2.0 (the "License");
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

package com.ericsson.gerrit.plugins.eventslog.sql;

import static com.ericsson.gerrit.plugins.eventslog.sql.SQLTable.DATE_ENTRY;
import static com.ericsson.gerrit.plugins.eventslog.sql.SQLTable.EVENT_ENTRY;
import static com.ericsson.gerrit.plugins.eventslog.sql.SQLTable.PRIMARY_ENTRY;
import static com.ericsson.gerrit.plugins.eventslog.sql.SQLTable.PROJECT_ENTRY;
import static com.ericsson.gerrit.plugins.eventslog.sql.SQLTable.TABLE_NAME;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.ericsson.gerrit.plugins.eventslog.EventsLogException;
import com.ericsson.gerrit.plugins.eventslog.MalformedQueryException;
import com.google.common.base.Supplier;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.gerrit.server.events.ProjectEvent;
import com.google.gerrit.server.events.SupplierSerializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.dbcp.BasicDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SQLClient {
  private static final Logger log = LoggerFactory.getLogger(SQLClient.class);
  private final Gson gson;
  private BasicDataSource ds;

  @Inject
  SQLClient(String storeDriver, String storeUrl, String urlOptions) {
    ds = new BasicDataSource();
    ds.setDriverClassName(storeDriver);
    ds.setUrl(storeUrl);
    ds.setConnectionProperties(urlOptions);
    gson = new GsonBuilder().registerTypeAdapter(Supplier.class, new SupplierSerializer()).create();
  }

  /**
   * Set the username to connect to the database.
   *
   * @param username the username as a string
   */
  void setUsername(String username) {
    ds.setUsername(username);
  }

  /**
   * Set the password to connect to the database.
   *
   * @param password the password as a string
   */
  void setPassword(String password) {
    ds.setPassword(password);
  }

  /**
   * Set the time before an idle connection is evicted as well as the time between eviction runs.
   *
   * @param evictIdleTime the time in milliseconds before eviction
   */
  void setEvictIdleTime(int evictIdleTime) {
    ds.setMinEvictableIdleTimeMillis(evictIdleTime);
    ds.setTimeBetweenEvictionRunsMillis(evictIdleTime / 2);
  }

  /**
   * Create the database if it has not yet been created.
   *
   * @throws SQLException If there was a problem with the database
   */
  void createDBIfNotCreated() throws SQLException {
    boolean postgresql = ds.getDriverClassName().contains("postgresql");
    execute(SQLTable.createTableQuery(postgresql));
  }

  /**
   * Return if the database exists.
   *
   * @return true if it exists, otherwise return false
   * @throws SQLException If there was a problem with the database
   */
  boolean dbExists() throws SQLException {
    try (Connection conn = ds.getConnection();
        ResultSet tables = conn.getMetaData().getTables(null, null, TABLE_NAME, null)) {
      return tables.next();
    }
  }

  void close() {
    try {
      ds.close();
    } catch (SQLException e) {
      log.warn("Cannot close datasource", e);
    }
  }

  /**
   * Get events as a multimap list of Strings and SQLEntries. The String represents the project
   * name, and the SQLEntry is the event information.
   *
   * @param query the query as a string
   * @return Multimap list of Strings (project names) and SQLEntries (events)
   * @throws EventsLogException If there was a problem with the database
   */
  ListMultimap<String, SQLEntry> getEvents(String query) throws EventsLogException {
    try (Connection conn = ds.getConnection();
        Statement stat = conn.createStatement()) {
      return listEvents(stat, query);
    } catch (SQLException e) {
      throw new EventsLogException("Cannot query database", e);
    }
  }

  /**
   * Store the event in the database.
   *
   * @param event The event to store
   * @throws SQLException If there was a problem with the database
   */
  void storeEvent(ProjectEvent event) throws SQLException {
    storeEvent(
        event.getProjectNameKey().get(),
        new Timestamp(SECONDS.toMillis(event.eventCreatedOn)),
        gson.toJson(event));
  }

  /**
   * Store the event in the database.
   *
   * @param projectName The project in which this event happened
   * @param timestamp The time at which this event took place
   * @param event The event as a string
   * @throws SQLException If there was a problem with the database
   */
  void storeEvent(String projectName, Timestamp timestamp, String event) throws SQLException {
    execute(
        format("INSERT INTO %s(%s, %s, %s) ", TABLE_NAME, PROJECT_ENTRY, DATE_ENTRY, EVENT_ENTRY)
            + format("VALUES('%s', '%s', '%s')", projectName, timestamp, event));
  }

  /**
   * Remove all events that are older than maxAge.
   *
   * @param maxAge The maximum age to keep events
   */
  void removeOldEvents(int maxAge) {
    try {
      execute(
          format(
              "DELETE FROM %s WHERE %s < '%s'",
              TABLE_NAME,
              DATE_ENTRY,
              new Timestamp(System.currentTimeMillis() - MILLISECONDS.convert(maxAge, DAYS))));
    } catch (SQLException e) {
      log.warn("Cannot remove old event entries from database", e);
    }
  }

  /**
   * Remove all events corresponding to this project.
   *
   * @param project Events attributed to this project should be removed
   */
  void removeProjectEvents(String project) {
    try {
      execute(format("DELETE FROM %s WHERE project = '%s'", TABLE_NAME, project));
    } catch (SQLException e) {
      log.warn("Cannot remove project " + project + " events from database", e);
    }
  }

  /**
   * Do a simple query on the database. This is used to determine whether or not the main database
   * is online.
   *
   * @throws SQLException If there was a problem with the database
   */
  void queryOne() throws SQLException {
    execute("SELECT * FROM " + TABLE_NAME + " LIMIT 1");
  }

  /**
   * Get all events from the database as a list of database entries.
   *
   * @return List of all events retrieved from the database
   * @throws SQLException If there was a problem with the database
   */
  List<SQLEntry> getAll() throws SQLException {
    List<SQLEntry> entries = new ArrayList<>();
    try (Connection conn = ds.getConnection();
        Statement stat = conn.createStatement();
        ResultSet rs = stat.executeQuery("SELECT * FROM " + TABLE_NAME)) {
      while (rs.next()) {
        entries.add(
            new SQLEntry(
                rs.getString(PROJECT_ENTRY),
                rs.getTimestamp(DATE_ENTRY),
                rs.getString(EVENT_ENTRY),
                rs.getInt(PRIMARY_ENTRY)));
      }
      return entries;
    }
  }

  private ListMultimap<String, SQLEntry> listEvents(Statement stat, String query)
      throws MalformedQueryException {
    try (ResultSet rs = stat.executeQuery(query)) {
      ListMultimap<String, SQLEntry> result = ArrayListMultimap.create();
      while (rs.next()) {
        SQLEntry entry =
            new SQLEntry(
                rs.getString(PROJECT_ENTRY),
                rs.getTimestamp(DATE_ENTRY),
                rs.getString(EVENT_ENTRY),
                rs.getInt(PRIMARY_ENTRY));
        result.put(rs.getString(PROJECT_ENTRY), entry);
      }
      return result;
    } catch (SQLException e) {
      throw new MalformedQueryException(e);
    }
  }

  private void execute(String query) throws SQLException {
    try (Connection conn = ds.getConnection();
        Statement stat = conn.createStatement()) {
      stat.execute(query);
    }
  }
}
