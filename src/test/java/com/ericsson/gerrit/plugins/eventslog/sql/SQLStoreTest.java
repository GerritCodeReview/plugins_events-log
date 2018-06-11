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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.reviewdb.client.Project.NameKey;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.events.ProjectEvent;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.gerrit.server.project.ProjectControl;
import com.google.gson.Gson;
import com.google.inject.Provider;

import com.ericsson.gerrit.plugins.eventslog.EventsLogConfig;
import com.ericsson.gerrit.plugins.eventslog.MalformedQueryException;
import com.ericsson.gerrit.plugins.eventslog.ServiceUnavailableException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@RunWith(MockitoJUnitRunner.class)
public class SQLStoreTest {
  private static final Logger log = LoggerFactory.getLogger(SQLStoreTest.class);
  private static final String TEST_URL = "jdbc:h2:mem:" + TABLE_NAME;
  private static final String TEST_LOCAL_URL = "jdbc:h2:mem:test";
  private static final String TEST_DRIVER = "org.h2.Driver";
  private static final String TEST_OPTIONS =
      "DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false";
  private static final String TERM_CONN_MSG = "terminating connection";
  private static final String MSG = "message";
  private static final String GENERIC_QUERY = "SELECT * FROM " + TABLE_NAME;
  private static final boolean PROJECT_VISIBLE_TO_USER = true;
  private static final boolean PROJECT_NOT_VISIBLE_TO_USER = false;

  @Mock
  private ProjectControl.GenericFactory pcFactoryMock;
  @Mock
  private Provider<CurrentUser> userProviderMock;
  @Mock
  private EventsLogConfig cfgMock;
  private SQLClient eventsDb;
  private SQLClient localEventsDb;
  private SQLStore store;
  private ScheduledThreadPoolExecutor poolMock;

  private Statement stat;

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  @Before
  public void setUp() throws SQLException {
    Connection conn =
        DriverManager.getConnection(TEST_URL + ";" + TEST_OPTIONS);
    stat = conn.createStatement();
    poolMock = new PoolMock();
    when(cfgMock.getMaxAge()).thenReturn(5);
    when(cfgMock.getLocalStorePath()).thenReturn(testFolder.getRoot().toPath());
  }

  public void tearDown() throws Exception {
    stat.execute("DROP TABLE " + TABLE_NAME);
    store.stop();
  }

  private void setUpClient() {
    eventsDb = new SQLClient(TEST_DRIVER, TEST_URL, TEST_OPTIONS);
    localEventsDb = new SQLClient(TEST_DRIVER, TEST_LOCAL_URL, TEST_OPTIONS);
    store = new SQLStore(pcFactoryMock, userProviderMock, cfgMock, eventsDb,
        localEventsDb, poolMock);
    store.start();
  }

  private void setUpClientMock() throws SQLException {
    eventsDb = mock(SQLClient.class);
    localEventsDb = mock(SQLClient.class);
    when(localEventsDb.dbExists()).thenReturn(true);
  }

  @Test
  public void storeThenQueryVisible() throws Exception {
    MockEvent mockEvent = setUpMocks(PROJECT_VISIBLE_TO_USER);
    store.storeEvent(mockEvent);
    List<String> events = store.queryChangeEvents(GENERIC_QUERY);
    String json = new Gson().toJson(mockEvent);
    assertThat(events).containsExactly(json);
    tearDown();
  }

  @Test
  public void storeThenQueryNotVisible() throws Exception {
    MockEvent mockEvent = setUpMocks(PROJECT_NOT_VISIBLE_TO_USER);
    store.storeEvent(mockEvent);
    List<String> events = store.queryChangeEvents(GENERIC_QUERY);
    assertThat(events).isEmpty();
    tearDown();
  }

  private MockEvent setUpMocks(boolean isVisible)
      throws NoSuchProjectException, IOException {
    MockEvent mockEvent = new MockEvent();
    ProjectControl pcMock = mock(ProjectControl.class);
    CurrentUser userMock = mock(CurrentUser.class);
    when(userProviderMock.get()).thenReturn(userMock);
    when(pcFactoryMock.controlFor(mockEvent.getProjectNameKey(), userMock))
        .thenReturn(pcMock);
    when(pcMock.isVisible()).thenReturn(isVisible);
    setUpClient();
    return mockEvent;
  }

  @Test(expected = MalformedQueryException.class)
  public void throwBadRequestTriggerOnBadQuery() throws Exception {
    setUpClient();
    String badQuery = "bad query";
    store.queryChangeEvents(badQuery);
  }

  @Test
  public void notReturnEventOfNonExistingProject() throws Exception {
    MockEvent mockEvent = new MockEvent();
    Project.NameKey projectMock = mock(Project.NameKey.class);
    CurrentUser userMock = mock(CurrentUser.class);
    when(userProviderMock.get()).thenReturn(userMock);
    NameKey projectNameKey = mockEvent.getProjectNameKey();
    doThrow(new NoSuchProjectException(projectMock)).when(pcFactoryMock)
        .controlFor(projectNameKey, userMock);
    setUpClient();
    store.storeEvent(mockEvent);
    List<String> events = store.queryChangeEvents(GENERIC_QUERY);
    assertThat(events).isEmpty();
    tearDown();
  }

  @Test
  public void notReturnEventWithNoVisibilityInfo() throws Exception {
    MockEvent mockEvent = new MockEvent();
    CurrentUser userMock = mock(CurrentUser.class);
    when(userProviderMock.get()).thenReturn(userMock);
    NameKey projectNameKey = mockEvent.getProjectNameKey();
    doThrow(new IOException()).when(pcFactoryMock).controlFor(projectNameKey,
        userMock);
    setUpClient();
    store.storeEvent(mockEvent);
    List<String> events = store.queryChangeEvents(GENERIC_QUERY);
    assertThat(events).isEmpty();
    tearDown();
  }

  @Test
  public void retryOnConnectException() throws Exception {
    MockEvent mockEvent = new MockEvent();
    when(cfgMock.getMaxTries()).thenReturn(3);
    Throwable[] exceptions = new Throwable[3];
    Arrays.fill(exceptions, new SQLException(new ConnectException()));
    setUpClientMock();
    doThrow(exceptions).doNothing().when(eventsDb).storeEvent(mockEvent);
    doThrow(exceptions).doNothing().when(eventsDb).queryOne();
    store = new SQLStore(pcFactoryMock, userProviderMock, cfgMock, eventsDb,
        localEventsDb, poolMock);
    store.start();
    store.storeEvent(mockEvent);
    verify(eventsDb, times(3)).storeEvent(mockEvent);
    verify(localEventsDb).storeEvent(mockEvent);
  }

  @Test
  public void retryOnMessage() throws Exception {
    MockEvent mockEvent = new MockEvent();
    when(cfgMock.getMaxTries()).thenReturn(3);
    Throwable[] exceptions = new Throwable[3];
    Arrays.fill(exceptions, new SQLException(TERM_CONN_MSG));
    setUpClientMock();
    doThrow(exceptions).doNothing().when(eventsDb).storeEvent(mockEvent);
    doThrow(exceptions).doNothing().when(eventsDb).queryOne();
    store = new SQLStore(pcFactoryMock, userProviderMock, cfgMock, eventsDb,
        localEventsDb, poolMock);
    store.start();
    store.storeEvent(mockEvent);
    verify(eventsDb, times(3)).storeEvent(mockEvent);
    verify(localEventsDb).storeEvent(mockEvent);
  }

  @Test
  public void noRetryOnMessage() throws Exception {
    MockEvent mockEvent = new MockEvent();
    when(cfgMock.getMaxTries()).thenReturn(3);
    setUpClientMock();
    doThrow(new SQLException(MSG)).when(eventsDb).storeEvent(mockEvent);
    store = new SQLStore(pcFactoryMock, userProviderMock, cfgMock, eventsDb,
        localEventsDb, poolMock);
    store.start();
    store.storeEvent(mockEvent);
    verify(eventsDb, times(1)).storeEvent(mockEvent);
  }

  @Test
  public void noRetryOnZeroMaxTries() throws Exception {
    MockEvent mockEvent = new MockEvent();
    when(cfgMock.getMaxTries()).thenReturn(0);
    Throwable[] exceptions = new Throwable[3];
    Arrays.fill(exceptions, new SQLException(new ConnectException()));
    setUpClientMock();
    doThrow(exceptions).doNothing().when(eventsDb).storeEvent(mockEvent);
    doThrow(exceptions).doNothing().when(eventsDb).queryOne();
    store = new SQLStore(pcFactoryMock, userProviderMock, cfgMock, eventsDb,
        localEventsDb, poolMock);
    store.start();
    store.storeEvent(mockEvent);
    verify(eventsDb, times(1)).storeEvent(mockEvent);
  }

  @Test(expected = ServiceUnavailableException.class)
  public void throwSQLExceptionIfNotOnline() throws Exception {
    MockEvent mockEvent = new MockEvent();
    setUpClientMock();
    doThrow(new SQLException(new ConnectException())).when(eventsDb)
        .createDBIfNotCreated();
    doThrow(new SQLException()).when(eventsDb).queryOne();
    store = new SQLStore(pcFactoryMock, userProviderMock, cfgMock, eventsDb,
        localEventsDb, poolMock);
    store.start();
    store.storeEvent(mockEvent);
    store.queryChangeEvents(GENERIC_QUERY);
  }

  @Test
  public void restoreFromLocalAndRemoveUnfoundProjectEvents() throws Exception {
    MockEvent mockEvent = new MockEvent();
    MockEvent mockEvent2 = new MockEvent("proj");
    MockEvent mockEvent3 = new MockEvent("unfound");

    ProjectControl pc = mock(ProjectControl.class);
    NoSuchProjectException e = mock(NoSuchProjectException.class);
    CurrentUser userMock = mock(CurrentUser.class);
    when(userProviderMock.get()).thenReturn(userMock);
    when(pcFactoryMock.controlFor((mockEvent.getProjectNameKey()), userMock))
        .thenReturn(pc);
    when(pcFactoryMock.controlFor((mockEvent2.getProjectNameKey()), userMock))
        .thenReturn(pc);
    when(pc.isVisible()).thenReturn(true);
    doThrow(e).when(pcFactoryMock).controlFor((mockEvent3.getProjectNameKey()),
        userMock);

    eventsDb = new SQLClient(TEST_DRIVER, TEST_URL, TEST_OPTIONS);
    localEventsDb = new SQLClient(TEST_DRIVER, TEST_LOCAL_URL, TEST_OPTIONS);
    store = new SQLStore(pcFactoryMock, userProviderMock, cfgMock, eventsDb,
        localEventsDb, poolMock);

    localEventsDb.createDBIfNotCreated();
    localEventsDb.storeEvent(mockEvent);
    localEventsDb.storeEvent(mockEvent2);
    localEventsDb.storeEvent(mockEvent3);
    store.start();
    List<String> events = store.queryChangeEvents(GENERIC_QUERY);
    Gson gson = new Gson();
    String json = gson.toJson(mockEvent);
    String json2 = gson.toJson(mockEvent2);
    assertThat(events).containsExactly(json, json2);
    tearDown();
  }

  @Test
  public void offlineUponStart() throws Exception {
    setUpClientMock();
    doThrow(new SQLException(new ConnectException())).when(eventsDb)
        .createDBIfNotCreated();
    doThrow(new SQLException()).when(eventsDb).queryOne();
    store = new SQLStore(pcFactoryMock, userProviderMock, cfgMock, eventsDb,
        localEventsDb, poolMock);
    store.start();
    verify(localEventsDb).createDBIfNotCreated();
  }

  @Test
  public void storeLocalOffline() throws Exception {
    MockEvent mockEvent = new MockEvent();
    setUpClientMock();
    doThrow(new SQLException(new ConnectException())).when(eventsDb)
        .createDBIfNotCreated();
    doThrow(new SQLException()).when(eventsDb).queryOne();
    store = new SQLStore(pcFactoryMock, userProviderMock, cfgMock, eventsDb,
        localEventsDb, poolMock);
    store.start();
    store.storeEvent(mockEvent);
    verify(localEventsDb).storeEvent(mockEvent);
  }

  @Test
  public void storeLocalOfflineAfterNoRetry() throws Exception {
    MockEvent mockEvent = new MockEvent();
    setUpClientMock();
    when(cfgMock.getMaxTries()).thenReturn(0);
    doThrow(new SQLException(new ConnectException())).when(eventsDb)
        .createDBIfNotCreated();
    doThrow(new SQLException()).when(eventsDb).queryOne();
    store = new SQLStore(pcFactoryMock, userProviderMock, cfgMock, eventsDb,
        localEventsDb, poolMock);
    store.start();
    store.storeEvent(mockEvent);
    verify(localEventsDb).storeEvent(mockEvent);
  }

  /**
   * For this test we expect that if we can connect to main database, then we
   * should come back online and try setting up again. We just want to make sure
   * that restoreEventsFromLocal gets called, so verifying that getLocalDBFile
   * is called is sufficient.
   */
  @Test
  public void testConnectionTask() throws Exception {
    eventsDb = new SQLClient(TEST_DRIVER, TEST_URL, TEST_OPTIONS);
    localEventsDb = mock(SQLClient.class);
    when(localEventsDb.dbExists()).thenReturn(true);
    when(localEventsDb.getAll())
        .thenReturn(ImmutableList.of(mock(SQLEntry.class)));
    store = new SQLStore(pcFactoryMock, userProviderMock, cfgMock, eventsDb,
        localEventsDb, poolMock);
    store.start();
    poolMock.scheduleWithFixedDelay(store.new CheckConnectionTask(), 0, 0,
        TimeUnit.MILLISECONDS);
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
    MockEvent mockEvent = new MockEvent();
    eventsDb = mock(SQLClient.class);
    localEventsDb = new SQLClient(TEST_DRIVER, TEST_LOCAL_URL, TEST_OPTIONS);
    localEventsDb.createDBIfNotCreated();
    localEventsDb.storeEvent(mockEvent);
    doThrow(new SQLException(new ConnectException())).doNothing().when(eventsDb)
        .createDBIfNotCreated();

    if (copy) {
      when(cfgMock.getCopyLocal()).thenReturn(true);
    }

    store = new SQLStore(pcFactoryMock, userProviderMock, cfgMock, eventsDb,
        localEventsDb, poolMock);
    store.start();
    verify(eventsDb).queryOne();
    verify(eventsDb).storeEvent(any(String.class), any(Timestamp.class),
        any(String.class));
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
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command,
        long initialDelay, long delay, TimeUnit unit) {
      log.info(command.toString());
      command.run();
      return null;
    }
  }
}
