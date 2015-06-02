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

import static com.ericsson.gerrit.plugins.eventslog.SQLTable.TABLE_NAME;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.assertEquals;

import com.google.gerrit.reviewdb.client.Change.Key;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.events.ChangeEvent;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.gerrit.server.project.ProjectControl;
import com.google.gson.Gson;
import com.google.inject.Provider;

import com.ericsson.gerrit.plugins.eventslog.SQLClient.SQLEntry;

import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ConnectException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SQLStoreTest {
  private static final Logger log = LoggerFactory.getLogger(SQLStoreTest.class);
  private static final String TEST_PATH = "jdbc:h2:mem:";
  private static final String TEST_LOCAL_PATH = "jdbc:h2:mem:test:";
  private static final String TEST_DRIVER = "org.h2.Driver";
  private static final String TEST_OPTIONS = "DB_CLOSE_DELAY=-1";
  private static final String TERM_CONN_MSG = "terminating connection";
  private static final String MSG = "message";
  private static final String GENERIC_QUERY = "SELECT * FROM " + TABLE_NAME;

  private EasyMockSupport easyMock;
  private ProjectControl.GenericFactory pcFactoryMock;
  private Provider<CurrentUser> userProviderMock;
  private EventsLogConfig cfgMock;
  private SQLClient eventsDb;
  private SQLClient localEventsDb;
  private SQLStore store;
  private ScheduledThreadPoolExecutor poolMock;

  private String path = TEST_PATH + TABLE_NAME + ";" + TEST_OPTIONS;
  private Connection conn;
  private Statement stat;
  private List<SQLEntry> results;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws SQLException {
    conn = DriverManager.getConnection(path);
    stat = conn.createStatement();
    results = new ArrayList<>();
    poolMock = new PoolMock(1);
    easyMock = new EasyMockSupport();
    pcFactoryMock = easyMock.createNiceMock(ProjectControl.GenericFactory.class);
    userProviderMock = easyMock.createNiceMock(Provider.class);
    cfgMock = easyMock.createNiceMock(EventsLogConfig.class);
    easyMock.replayAll();
  }

  public void tearDown() throws Exception {
    stat.execute("DROP TABLE " + TABLE_NAME);
    store.stop();
  }

  private void setUpClient() {
    eventsDb = new SQLClient(TEST_DRIVER, TEST_PATH, TEST_OPTIONS);
    localEventsDb = new SQLClient(TEST_DRIVER, TEST_LOCAL_PATH, TEST_OPTIONS);
    store =
        new SQLStore(pcFactoryMock, userProviderMock, cfgMock, eventsDb,
            localEventsDb, poolMock);
    store.start();
  }

  private void setUpClientMock() {
    eventsDb = easyMock.createNiceMock(SQLClient.class);
    localEventsDb = easyMock.createNiceMock(SQLClient.class);
    easyMock.resetAll();
  }

  @Test
  public void storeThenQueryVisible() throws Exception {
    setUpClient();
    MockEvent mockEvent = new MockEvent();
    ProjectControl pcMock = easyMock.createNiceMock(ProjectControl.class);
    CurrentUser userMock = easyMock.createNiceMock(CurrentUser.class);
    easyMock.resetAll();
    expect(userProviderMock.get()).andStubReturn(userMock);
    expect(pcFactoryMock.controlFor(mockEvent.getProjectNameKey(), userMock))
        .andStubReturn(pcMock);
    expect(pcMock.isVisible()).andStubReturn(true);
    easyMock.replayAll();
    store.storeEvent(mockEvent);
    List<String> events = store.queryChangeEvents(GENERIC_QUERY);
    Gson gson = new Gson();
    String json = gson.toJson(mockEvent);
    assertEquals(1, events.size());
    assertEquals(json, events.get(0));
    tearDown();
  }

  @Test
  public void storeThenQueryNotVisible() throws Exception {
    setUpClient();
    MockEvent mockEvent = new MockEvent();
    ProjectControl pcMock = easyMock.createNiceMock(ProjectControl.class);
    CurrentUser userMock = easyMock.createNiceMock(CurrentUser.class);
    easyMock.resetAll();
    expect(userProviderMock.get()).andStubReturn(userMock);
    expect(pcFactoryMock.controlFor(mockEvent.getProjectNameKey(), userMock))
        .andStubReturn(pcMock);
    expect(pcMock.isVisible()).andStubReturn(false);
    easyMock.replayAll();
    store.storeEvent(mockEvent);
    List<String> events = store.queryChangeEvents(GENERIC_QUERY);
    assertEquals(0, events.size());
    tearDown();
  }

  @Test(expected = MalformedQueryException.class)
  public void throwBadRequestTriggerOnBadQuery() throws EventsLogException {
    setUpClient();
    String badQuery = "bad query";
    easyMock.resetAll();
    easyMock.replayAll();
    store.queryChangeEvents(badQuery);
    easyMock.verifyAll();
  }

  @Test
  public void notReturnEventOfNonExistingProject() throws Exception {
    setUpClient();
    MockEvent mockEvent = new MockEvent();
    Project.NameKey projectMock = easyMock.createMock(Project.NameKey.class);
    easyMock.resetAll();
    expect(projectMock.get()).andStubReturn(" ");
    expect(
        pcFactoryMock.controlFor(EasyMock.anyObject(Project.NameKey.class),
            EasyMock.anyObject(CurrentUser.class))).andThrow(
        new NoSuchProjectException(projectMock));
    easyMock.replayAll();
    store.storeEvent(mockEvent);
    List<String> events = store.queryChangeEvents(GENERIC_QUERY);
    assertEquals(0, events.size());
    tearDown();
  }

  @Test
  public void notReturnEventWithNoVisibilityInfo() throws Exception {
    setUpClient();
    MockEvent mockEvent = new MockEvent();
    Project.NameKey projectMock = easyMock.createMock(Project.NameKey.class);
    easyMock.resetAll();
    expect(projectMock.get()).andStubReturn(" ");
    expect(
        pcFactoryMock.controlFor(EasyMock.anyObject(Project.NameKey.class),
            EasyMock.anyObject(CurrentUser.class))).andThrow(new IOException());
    easyMock.replayAll();
    store.storeEvent(mockEvent);
    List<String> events = store.queryChangeEvents(GENERIC_QUERY);
    assertEquals(0, events.size());
    tearDown();
  }

  @Test
  public void retryOnConnectException() throws Exception {
    MockEvent mockEvent = new MockEvent();
    setUpClientMock();
    expect(cfgMock.getMaxTries()).andReturn(3).once();
    eventsDb.storeEvent(mockEvent);
    expectLastCall().andThrow(new SQLException(new ConnectException()))
        .times(3);
    expect(localEventsDb.getAll()).andStubReturn(results);
    easyMock.replayAll();
    store =
        new SQLStore(pcFactoryMock, userProviderMock, cfgMock, eventsDb,
            localEventsDb, poolMock);
    store.start();

    store.storeEvent(mockEvent);
    easyMock.verifyAll();
  }

  @Test
  public void retryOnMessage() throws Exception {
    MockEvent mockEvent = new MockEvent();
    setUpClientMock();
    expect(cfgMock.getMaxTries()).andReturn(3).once();
    eventsDb.storeEvent(mockEvent);
    expectLastCall().andThrow(new SQLException(TERM_CONN_MSG)).times(3);
    expect(localEventsDb.getAll()).andStubReturn(results);
    easyMock.replayAll();
    store =
        new SQLStore(pcFactoryMock, userProviderMock, cfgMock, eventsDb,
            localEventsDb, poolMock);
    store.start();

    store.storeEvent(mockEvent);
    easyMock.verifyAll();
  }

  @Test
  public void noRetryOnMessage() throws Exception {
    MockEvent mockEvent = new MockEvent();
    setUpClientMock();
    expect(cfgMock.getMaxTries()).andReturn(3).once();
    eventsDb.storeEvent(mockEvent);
    expectLastCall().andThrow(new SQLException(MSG)).once();
    expect(localEventsDb.getAll()).andReturn(results);
    easyMock.replayAll();
    store =
        new SQLStore(pcFactoryMock, userProviderMock, cfgMock, eventsDb,
            localEventsDb, poolMock);
    store.start();

    store.storeEvent(mockEvent);
    easyMock.verifyAll();
  }

  @Test
  public void noRetryOnZeroMaxTries() throws Exception {
    MockEvent mockEvent = new MockEvent();
    setUpClientMock();
    expect(cfgMock.getMaxTries()).andReturn(0).once();
    eventsDb.storeEvent(mockEvent);
    expectLastCall().andThrow(new SQLException(new ConnectException())).once();
    expect(localEventsDb.getAll()).andStubReturn(results);
    easyMock.replayAll();
    store =
        new SQLStore(pcFactoryMock, userProviderMock, cfgMock, eventsDb,
            localEventsDb, poolMock);
    store.start();

    store.storeEvent(mockEvent);
    easyMock.verifyAll();
  }

  @Test(expected = ServiceUnavailableException.class)
  public void throwSQLExceptionIfNotOnline() throws EventsLogException,
      SQLException {
    MockEvent mockEvent = new MockEvent();
    setUpClientMock();
    eventsDb.createDBIfNotCreated();
    expectLastCall().andThrow(new SQLException(new ConnectException())).once();
    eventsDb.queryOne();
    expectLastCall().andThrow(new SQLException());
    easyMock.replayAll();
    store =
        new SQLStore(pcFactoryMock, userProviderMock, cfgMock, eventsDb,
            localEventsDb, poolMock);
    store.start();
    store.storeEvent(mockEvent);
    store.queryChangeEvents(GENERIC_QUERY);
    easyMock.verifyAll();
  }

  @Test
  public void restoreFromLocalAndRemoveUnfoundProjectEvents() throws Exception {
    MockEvent mockEvent = new MockEvent();
    MockEvent mockEvent2 = new MockEvent("proj");
    MockEvent mockEvent3 = new MockEvent("unfound");

    ProjectControl pc = easyMock.createNiceMock(ProjectControl.class);
    NoSuchProjectException e =
        easyMock.createNiceMock(NoSuchProjectException.class);
    easyMock.resetAll();
    expect(cfgMock.getMaxAge()).andReturn(5);
    expect(
        pcFactoryMock.controlFor(EasyMock.anyObject(Project.NameKey.class),
            EasyMock.anyObject(CurrentUser.class))).andReturn(pc).times(2);
    expect(pc.isVisible()).andReturn(true).times(2);
    expect(
        pcFactoryMock.controlFor(EasyMock.anyObject(Project.NameKey.class),
            EasyMock.anyObject(CurrentUser.class))).andThrow(e);
    easyMock.replayAll();

    eventsDb = new SQLClient(TEST_DRIVER, TEST_PATH, TEST_OPTIONS);
    localEventsDb = new SQLClient(TEST_DRIVER, TEST_LOCAL_PATH, TEST_OPTIONS);
    store =
        new SQLStore(pcFactoryMock, userProviderMock, cfgMock, eventsDb,
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
    assertEquals(2, events.size());
    assertEquals(json, events.get(0));
    assertEquals(json2, events.get(1));
    easyMock.verifyAll();
    tearDown();
  }

  @Test
  public void offlineUponStart() throws Exception {
    setUpClientMock();
    eventsDb.createDBIfNotCreated();
    expectLastCall().andThrow(new SQLException(new ConnectException())).once();
    eventsDb.queryOne();
    expectLastCall().andThrow(new SQLException());
    easyMock.replayAll();
    store =
        new SQLStore(pcFactoryMock, userProviderMock, cfgMock, eventsDb,
            localEventsDb, poolMock);
    store.start();
    easyMock.verifyAll();
  }

  @Test
  public void storeLocalOffline() throws SQLException {
    MockEvent mockEvent = new MockEvent();
    setUpClientMock();
    eventsDb.createDBIfNotCreated();
    expectLastCall().andThrow(new SQLException(new ConnectException())).once();
    localEventsDb.storeEvent(mockEvent);
    expectLastCall().once();
    eventsDb.queryOne();
    expectLastCall().andThrow(new SQLException());
    easyMock.replayAll();
    store =
        new SQLStore(pcFactoryMock, userProviderMock, cfgMock, eventsDb,
            localEventsDb, poolMock);
    store.start();

    store.storeEvent(mockEvent);
    easyMock.verifyAll();
  }

  @Test
  public void storeLocalOfflineAfterNoRetry() throws SQLException {
    MockEvent mockEvent = new MockEvent();
    setUpClientMock();
    expect(cfgMock.getMaxTries()).andReturn(0).once();
    eventsDb.storeEvent(mockEvent);
    expectLastCall().andThrow(new SQLException(new ConnectException())).once();
    expect(localEventsDb.getAll()).andStubReturn(results);
    easyMock.replayAll();
    store =
        new SQLStore(pcFactoryMock, userProviderMock, cfgMock, eventsDb,
            localEventsDb, poolMock);
    store.start();

    store.storeEvent(mockEvent);
    easyMock.verifyAll();
  }

  /**
   * For this test we expect that if we can connect to main database, then we
   * should come back online and try setting up again. This involves trying to
   * restore events from the local database.
   */
  @Test
  public void testConnectionTask() throws SQLException {
    eventsDb = new SQLClient(TEST_DRIVER, TEST_PATH, TEST_OPTIONS);
    localEventsDb = easyMock.createMock(SQLClient.class);
    expect(localEventsDb.getAll()).andReturn(new ArrayList<SQLEntry>());
    EasyMock.replay(localEventsDb);
    store =
        new SQLStore(pcFactoryMock, userProviderMock, cfgMock, eventsDb,
            localEventsDb, poolMock);
    eventsDb.createDBIfNotCreated();
    poolMock.scheduleWithFixedDelay(store.new CheckConnectionTask(), 0, 0,
        TimeUnit.MILLISECONDS);
    easyMock.verifyAll();
  }

  @Test
  public void checkConnectionAndRestoreCopyLocal() throws SQLException {
    checkConnectionAndRestore(true);
  }

  @Test
  public void checkConnectionAndRestoreNoCopyLocal() throws SQLException {
    checkConnectionAndRestore(false);
  }

  private void checkConnectionAndRestore(boolean copy) throws SQLException {
    MockEvent mockEvent = new MockEvent();
    eventsDb = easyMock.createNiceMock(SQLClient.class);
    easyMock.resetAll();
    localEventsDb = new SQLClient(TEST_DRIVER, TEST_LOCAL_PATH, TEST_OPTIONS);
    localEventsDb.createDBIfNotCreated();
    localEventsDb.storeEvent(mockEvent);
    expect(cfgMock.getMaxAge()).andReturn(5);
    eventsDb.createDBIfNotCreated();
    expectLastCall().andThrow(new SQLException(new ConnectException())).once();
    eventsDb.queryOne();
    expectLastCall().once();
    eventsDb.storeEvent(EasyMock.anyString(),
        EasyMock.anyObject(Timestamp.class), EasyMock.anyString());
    expectLastCall().once();

    if (copy) {
      testCopyLocal();
    }
    easyMock.replayAll();

    store =
        new SQLStore(pcFactoryMock, userProviderMock, cfgMock, eventsDb,
            localEventsDb, poolMock);
    store.start();
    List<SQLEntry> entries = localEventsDb.getAll();
    assertEquals(0, entries.size());
    easyMock.verifyAll();
  }

  private void testCopyLocal() {
    expect(cfgMock.getCopyLocal()).andReturn(true).once();
    expect(cfgMock.getLocalStoreUrl()).andReturn(TEST_LOCAL_PATH).once();
  }

  public class MockEvent extends ChangeEvent {
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

    @Override
    public Key getChangeKey() {
      return null;
    }

    @Override
    public String getRefName() {
      return null;
    }
  }

  class PoolMock extends ScheduledThreadPoolExecutor {
    PoolMock(int corePoolSize) {
      super(corePoolSize);
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
