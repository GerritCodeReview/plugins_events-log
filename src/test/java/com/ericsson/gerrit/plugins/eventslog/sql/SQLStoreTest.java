// Copyright (C) 2014 The Android Open Source Project
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

import static com.ericsson.gerrit.plugins.eventslog.sql.SQLTable.TABLE_NAME;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ericsson.gerrit.plugins.eventslog.EventsLogConfig;
import com.ericsson.gerrit.plugins.eventslog.MalformedQueryException;
import com.ericsson.gerrit.plugins.eventslog.ServiceUnavailableException;
import com.google.common.collect.ImmutableList;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.events.ProjectEvent;
import com.google.gerrit.server.permissions.PermissionBackend;
import com.google.gerrit.server.permissions.PermissionBackendException;
import com.google.gerrit.server.permissions.ProjectPermission;
import com.google.gson.Gson;
import com.google.inject.Provider;
import com.zaxxer.hikari.HikariConfig;
import java.net.ConnectException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(MockitoJUnitRunner.class)
public class SQLStoreTest {
  private static final Logger log = LoggerFactory.getLogger(SQLStoreTest.class);
  private static final String TEST_URL = "jdbc:h2:mem:" + TABLE_NAME;
  private static final String TEST_LOCAL_URL = "jdbc:h2:mem:test";
  private static final String TEST_OPTIONS = "DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";
  private static final String TERM_CONN_MSG = "terminating connection";
  private static final String MSG = "message";
  private static final String GENERIC_QUERY = "SELECT * FROM " + TABLE_NAME;
  private static final String PLUGIN_NAME = "events-log";

  @Mock private Provider<CurrentUser> userProviderMock;
  @Mock private EventsLogConfig cfgMock;
  @Mock private PermissionBackend permissionBackendMock;
  @Mock private PermissionBackend.ForProject forProjectMock;
  @Mock private PermissionBackend.WithUser withUserMock;
  @Mock private EventsLogCleaner logCleanerMock;

  private SQLClient eventsDb;
  private SQLClient localEventsDb;
  private SQLStore store;
  private ScheduledExecutorService poolMock;
  private HikariConfig config;

  private Statement stat;
  private MockEvent mockEvent;

  @Rule public TemporaryFolder testFolder = new TemporaryFolder();

  @Before
  public void setUp() throws SQLException {
    config = new HikariConfig();
    config.setJdbcUrl(TEST_URL);
    config.addDataSourceProperty("DB_CLOSE_DELAY", "-1");
    config.addDataSourceProperty("DATABASE_TO_UPPER", "false");
    Connection conn = DriverManager.getConnection(TEST_URL + ";" + TEST_OPTIONS);
    mockEvent = new MockEvent();
    stat = conn.createStatement();
    poolMock = new PoolMock();
    when(cfgMock.getMaxAge()).thenReturn(5);
    when(cfgMock.getLocalStorePath()).thenReturn(testFolder.getRoot().toPath());
  }

  @After
  public void tearDown() throws Exception {
    stat.execute("DROP TABLE IF EXISTS " + TABLE_NAME);
    store.stop();
  }

  @Test
  public void storeThenQueryVisible() throws Exception {
    when(permissionBackendMock.user(userProviderMock)).thenReturn(withUserMock);
    when(withUserMock.project(any(Project.NameKey.class))).thenReturn(forProjectMock);
    doNothing().when(forProjectMock).check(ProjectPermission.ACCESS);
    setUpClient();
    store.storeEvent(mockEvent);
    List<String> events = store.queryChangeEvents(GENERIC_QUERY);
    String json = new Gson().toJson(mockEvent);
    assertThat(events).containsExactly(json).inOrder();
  }

  @Test
  public void storeThenQueryNotVisible() throws Exception {
    when(permissionBackendMock.user(userProviderMock)).thenReturn(withUserMock);
    when(withUserMock.project(any(Project.NameKey.class))).thenReturn(forProjectMock);
    doThrow(new PermissionBackendException(""))
        .when(forProjectMock)
        .check(ProjectPermission.ACCESS);
    setUpClient();
    store.storeEvent(mockEvent);
    List<String> events = store.queryChangeEvents(GENERIC_QUERY);
    assertThat(events).isEmpty();
  }

  @Test(expected = MalformedQueryException.class)
  public void throwBadRequestTriggerOnBadQuery() throws Exception {
    setUpClient();
    String badQuery = "bad query";
    store.queryChangeEvents(badQuery);
  }

  @Test
  public void notReturnEventWithNoVisibilityInfo() throws Exception {
    when(permissionBackendMock.user(userProviderMock)).thenReturn(withUserMock);
    when(withUserMock.project(any(Project.NameKey.class))).thenReturn(forProjectMock);
    doThrow(new PermissionBackendException(""))
        .when(forProjectMock)
        .check(ProjectPermission.ACCESS);
    setUpClient();
    store.storeEvent(mockEvent);
    List<String> events = store.queryChangeEvents(GENERIC_QUERY);
    assertThat(events).isEmpty();
  }

  @Test
  public void retryOnConnectException() throws Exception {
    when(cfgMock.getMaxTries()).thenReturn(3);
    Throwable[] exceptions = new Throwable[3];
    Arrays.fill(exceptions, new SQLException(new ConnectException()));
    setUpClientMock();
    doThrow(exceptions).doNothing().when(eventsDb).storeEvent(mockEvent);
    doThrow(exceptions).doNothing().when(eventsDb).queryOne();
    store =
        new SQLStore(
            userProviderMock,
            cfgMock,
            eventsDb,
            localEventsDb,
            poolMock,
            permissionBackendMock,
            logCleanerMock,
            PLUGIN_NAME);

    store.start();
    store.storeEvent(mockEvent);
    verify(eventsDb, times(3)).storeEvent(mockEvent);
    verify(localEventsDb).storeEvent(mockEvent);
  }

  @Test
  public void retryOnMessage() throws Exception {
    when(cfgMock.getMaxTries()).thenReturn(3);
    Throwable[] exceptions = new Throwable[3];
    Arrays.fill(exceptions, new SQLException(TERM_CONN_MSG));
    setUpClientMock();
    doThrow(exceptions).doNothing().when(eventsDb).storeEvent(mockEvent);
    doThrow(exceptions).doNothing().when(eventsDb).queryOne();
    store =
        new SQLStore(
            userProviderMock,
            cfgMock,
            eventsDb,
            localEventsDb,
            poolMock,
            permissionBackendMock,
            logCleanerMock,
            PLUGIN_NAME);

    store.start();
    store.storeEvent(mockEvent);
    verify(eventsDb, times(3)).storeEvent(mockEvent);
    verify(localEventsDb).storeEvent(mockEvent);
  }

  @Test
  public void noRetryOnMessage() throws Exception {
    when(cfgMock.getMaxTries()).thenReturn(3);
    setUpClientMock();
    doThrow(new SQLException(MSG)).when(eventsDb).storeEvent(mockEvent);
    store =
        new SQLStore(
            userProviderMock,
            cfgMock,
            eventsDb,
            localEventsDb,
            poolMock,
            permissionBackendMock,
            logCleanerMock,
            PLUGIN_NAME);

    store.start();
    store.storeEvent(mockEvent);
    verify(eventsDb, times(1)).storeEvent(mockEvent);
  }

  @Test
  public void noRetryOnZeroMaxTries() throws Exception {
    when(cfgMock.getMaxTries()).thenReturn(0);
    Throwable[] exceptions = new Throwable[3];
    Arrays.fill(exceptions, new SQLException(new ConnectException()));
    setUpClientMock();
    doThrow(exceptions).doNothing().when(eventsDb).storeEvent(mockEvent);
    doThrow(exceptions).doNothing().when(eventsDb).queryOne();
    store =
        new SQLStore(
            userProviderMock,
            cfgMock,
            eventsDb,
            localEventsDb,
            poolMock,
            permissionBackendMock,
            logCleanerMock,
            PLUGIN_NAME);

    store.start();
    store.storeEvent(mockEvent);
    verify(eventsDb, times(1)).storeEvent(mockEvent);
  }

  @Test(expected = ServiceUnavailableException.class)
  public void throwSQLExceptionIfNotOnline() throws Exception {
    setUpClientMock();
    doThrow(new SQLException(new ConnectException())).when(eventsDb).createDBIfNotCreated();
    doThrow(new SQLException()).when(eventsDb).queryOne();
    store =
        new SQLStore(
            userProviderMock,
            cfgMock,
            eventsDb,
            localEventsDb,
            poolMock,
            permissionBackendMock,
            logCleanerMock,
            PLUGIN_NAME);

    store.start();
    store.storeEvent(mockEvent);
    store.queryChangeEvents(GENERIC_QUERY);
  }

  @Test
  public void restoreEventsFromLocalDb() throws Exception {
    MockEvent mockEvent = new MockEvent();
    MockEvent mockEvent2 = new MockEvent("proj");
    when(permissionBackendMock.user(userProviderMock)).thenReturn(withUserMock);
    when(withUserMock.project(any(Project.NameKey.class))).thenReturn(forProjectMock);
    doNothing().when(forProjectMock).check(ProjectPermission.ACCESS);

    config.setJdbcUrl(TEST_URL);
    eventsDb = new SQLClient(config);
    config.setJdbcUrl(TEST_LOCAL_URL);
    localEventsDb = new SQLClient(config);
    store =
        new SQLStore(
            userProviderMock,
            cfgMock,
            eventsDb,
            localEventsDb,
            poolMock,
            permissionBackendMock,
            logCleanerMock,
            PLUGIN_NAME);

    localEventsDb.createDBIfNotCreated();
    localEventsDb.storeEvent(mockEvent);
    localEventsDb.storeEvent(mockEvent2);
    store.start();

    List<String> events = store.queryChangeEvents(GENERIC_QUERY);
    Gson gson = new Gson();
    String json = gson.toJson(mockEvent);
    String json2 = gson.toJson(mockEvent2);
    assertThat(events).containsExactly(json, json2).inOrder();
  }

  @Test
  public void offlineUponStart() throws Exception {
    setUpClientMock();
    doThrow(new SQLException(new ConnectException())).when(eventsDb).createDBIfNotCreated();
    doThrow(new SQLException()).when(eventsDb).queryOne();
    store =
        new SQLStore(
            userProviderMock,
            cfgMock,
            eventsDb,
            localEventsDb,
            poolMock,
            permissionBackendMock,
            logCleanerMock,
            PLUGIN_NAME);

    store.start();
    verify(localEventsDb).createDBIfNotCreated();
  }

  @Test
  public void storeLocalOffline() throws Exception {
    setUpClientMock();
    doThrow(new SQLException(new ConnectException())).when(eventsDb).createDBIfNotCreated();
    doThrow(new SQLException()).when(eventsDb).queryOne();
    store =
        new SQLStore(
            userProviderMock,
            cfgMock,
            eventsDb,
            localEventsDb,
            poolMock,
            permissionBackendMock,
            logCleanerMock,
            PLUGIN_NAME);

    store.start();
    store.storeEvent(mockEvent);
    verify(localEventsDb).storeEvent(mockEvent);
  }

  @Test
  public void storeLocalOfflineAfterNoRetry() throws Exception {
    setUpClientMock();
    when(cfgMock.getMaxTries()).thenReturn(0);
    doThrow(new SQLException(new ConnectException())).when(eventsDb).createDBIfNotCreated();
    doThrow(new SQLException()).when(eventsDb).queryOne();
    store =
        new SQLStore(
            userProviderMock,
            cfgMock,
            eventsDb,
            localEventsDb,
            poolMock,
            permissionBackendMock,
            logCleanerMock,
            PLUGIN_NAME);

    store.start();
    store.storeEvent(mockEvent);
    verify(localEventsDb).storeEvent(mockEvent);
  }

  private void setUpClient() {
    eventsDb = new SQLClient(config);
    localEventsDb = new SQLClient(config);
    store =
        new SQLStore(
            userProviderMock,
            cfgMock,
            eventsDb,
            localEventsDb,
            poolMock,
            permissionBackendMock,
            logCleanerMock,
            PLUGIN_NAME);
    store.start();
  }

  private void setUpClientMock() throws SQLException {
    eventsDb = mock(SQLClient.class);
    localEventsDb = mock(SQLClient.class);
    when(localEventsDb.dbExists()).thenReturn(true);
  }

  /**
   * For this test we expect that if we can connect to main database, then we should come back
   * online and try setting up again. We just want to make sure that restoreEventsFromLocal gets
   * called, so verifying that getLocalDBFile is called is sufficient.
   */
  @Test
  public void testConnectionTask() throws Exception {
    config.setJdbcUrl(TEST_URL);
    eventsDb = new SQLClient(config);
    localEventsDb = mock(SQLClient.class);
    when(localEventsDb.dbExists()).thenReturn(true);
    when(localEventsDb.getAll()).thenReturn(ImmutableList.of(mock(SQLEntry.class)));
    store =
        new SQLStore(
            userProviderMock,
            cfgMock,
            eventsDb,
            localEventsDb,
            poolMock,
            permissionBackendMock,
            logCleanerMock,
            PLUGIN_NAME);

    store.start();
    poolMock.scheduleWithFixedDelay(
        store.new CheckConnectionTask(PLUGIN_NAME), 0, 0, TimeUnit.MILLISECONDS);
    verify(localEventsDb, times(2)).removeOldEvents(0);
  }

  @Test
  public void checkConnectionAndRestoreCopyLocal() throws Exception {
    checkConnectionAndRestore(true);
  }

  @Test
  public void checkConnectionAndRestoreNoCopyLocal() throws Exception {
    checkConnectionAndRestore(false);
  }

  private void checkConnectionAndRestore(boolean copy) throws Exception {
    eventsDb = mock(SQLClient.class);
    config.setJdbcUrl(TEST_LOCAL_URL);
    localEventsDb = new SQLClient(config);
    localEventsDb.createDBIfNotCreated();
    localEventsDb.storeEvent(mockEvent);
    doThrow(new SQLException(new ConnectException()))
        .doNothing()
        .when(eventsDb)
        .createDBIfNotCreated();

    if (copy) {
      when(cfgMock.getCopyLocal()).thenReturn(true);
    }

    store =
        new SQLStore(
            userProviderMock,
            cfgMock,
            eventsDb,
            localEventsDb,
            poolMock,
            permissionBackendMock,
            logCleanerMock,
            PLUGIN_NAME);

    store.start();
    verify(eventsDb).queryOne();
    verify(eventsDb).storeEvent(any(String.class), any(Timestamp.class), any(String.class));
    List<SQLEntry> entries = localEventsDb.getAll();
    assertThat(entries).isEmpty();
  }

  public class MockEvent extends ProjectEvent {
    public String project = "mock project";

    MockEvent() {
      super("mock event");
    }

    MockEvent(String project) {
      this();
      this.project = project;
    }

    @Override
    public Project.NameKey getProjectNameKey() {
      return new Project.NameKey(project);
    }
  }

  class PoolMock extends ScheduledThreadPoolExecutor {
    PoolMock() {
      super(1);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(
        Runnable command, long initialDelay, long delay, TimeUnit unit) {
      log.info(command.toString());
      command.run();
      return null;
    }
  }
}
