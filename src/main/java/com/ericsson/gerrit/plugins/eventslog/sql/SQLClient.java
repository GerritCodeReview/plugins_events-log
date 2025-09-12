// Copyright (C) 2015 The Android Open Source Project
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

import com.ericsson.gerrit.plugins.eventslog.EventsLogException;
import com.ericsson.gerrit.plugins.eventslog.MalformedQueryException;
import com.google.common.base.Supplier;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.flogger.FluentLogger;
import com.google.gerrit.server.events.ProjectEvent;
import com.google.gerrit.server.events.SupplierSerializer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

class SQLClient {
  static final int MAX_BATCH_SIZE = 100;
  static final int QUEUE_CAPACITY = 10000;

  private static final FluentLogger log = FluentLogger.forEnclosingClass();
  private final Gson gson;
  private final SQLDialect databaseDialect;
  private final BlockingQueue<ProjectEvent> eventQueue;
  private final ScheduledExecutorService scheduler;

  private HikariDataSource ds;

  public SQLClient(HikariConfig config) {
    ds = new HikariDataSource(config);
    eventQueue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    gson = new GsonBuilder().registerTypeAdapter(Supplier.class, new SupplierSerializer()).create();
    databaseDialect = SQLDialect.fromJdbcUrl(config.getJdbcUrl());
    scheduler = Executors.newSingleThreadScheduledExecutor();
    scheduler.scheduleAtFixedRate(this::flush, 2, 2, TimeUnit.SECONDS);
  }

  void flush() {
    List<ProjectEvent> batch = new ArrayList<>();
    getQueue().drainTo(batch, MAX_BATCH_SIZE);

    if (batch.isEmpty()) {
      return;
    }

    try {
      batchInsert(batch);
    } catch (SQLException e) {
      log.atSevere().withCause(e).log("Failed to batch insert events");
    }
  }

  private void batchInsert(List<ProjectEvent> events) throws SQLException {
    String sql =
        "INSERT INTO "
            + TABLE_NAME
            + " ("
            + PROJECT_ENTRY
            + ", "
            + DATE_ENTRY
            + ", "
            + EVENT_ENTRY
            + ") VALUES (?, ?, ?)";

    try (Connection conn = ds.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {

      for (ProjectEvent e : events) {
        String projectName = e.getProjectNameKey().get();
        Instant ts = Instant.ofEpochSecond(e.eventCreatedOn);
        String eventJson = gson.toJson(e);

        if (databaseDialect == SQLDialect.SPANNER && eventJson != null) {
          eventJson = eventJson.replace("\\n", "\\\\n");
        }

        ps.setString(1, projectName);
        ps.setTimestamp(2, Timestamp.from(ts));
        ps.setString(3, eventJson);
        ps.addBatch();
      }

      ps.executeBatch();
    }
  }

  /**
   * Create the database if it has not yet been created.
   *
   * @throws SQLException If there was a problem with the database
   */
  void createDBIfNotCreated() throws SQLException {
    execute(SQLTable.createTableQuery(databaseDialect));
    switch (databaseDialect) {
      case SPANNER:
        execute(SQLTable.createSpannerDateIndex());
        execute(SQLTable.createSpannerProjectIndex());
        break;
      default:
        execute(SQLTable.createIndexes(databaseDialect));
    }
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
    scheduler.shutdownNow();
    ds.close();
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
   * Get the queue of events to store.
   *
   * @return The queue of events
   */
  BlockingQueue<ProjectEvent> getQueue() {
    return eventQueue;
  }

  /**
   * Queue the event in memory for processing.
   *
   * @throws EventsLogException If there was a problem queueing the event
   * @param event the event to store
   */
  void storeEvent(ProjectEvent event) throws EventsLogException {
    if (!eventQueue.offer(event)) {
      throw new EventsLogException(String.format("Cannot offer event %s", gson.toJson(event)));
    }
  }

  void storeEvent(String projectName, Instant timestamp, String eventJson) throws SQLException {
    String sql =
        "INSERT INTO "
            + TABLE_NAME
            + " ("
            + PROJECT_ENTRY
            + ", "
            + DATE_ENTRY
            + ", "
            + EVENT_ENTRY
            + ") VALUES (?, ?, ?)";
    try (Connection conn = ds.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, projectName);
      ps.setTimestamp(2, Timestamp.from(timestamp));
      ps.setString(3, eventJson);
      ps.executeUpdate();
    }
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
      log.atInfo().log(
          "Events older than %d days were removed from database %s", maxAge, ds.getPoolName());
    } catch (SQLException e) {
      log.atWarning().withCause(e).log(
          "Cannot remove old event entries from database %s", ds.getPoolName());
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
      log.atWarning().withCause(e).log("Cannot remove project %s events from database", project);
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
                rs.getTimestamp(DATE_ENTRY).toInstant(),
                rs.getString(EVENT_ENTRY),
                rs.getObject(PRIMARY_ENTRY)));
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
                rs.getTimestamp(DATE_ENTRY).toInstant(),
                rs.getString(EVENT_ENTRY),
                rs.getObject(PRIMARY_ENTRY));
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
