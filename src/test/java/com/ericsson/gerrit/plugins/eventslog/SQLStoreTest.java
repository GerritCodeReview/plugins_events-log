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

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static com.ericsson.gerrit.plugins.eventslog.SQLTable.TABLE_NAME;

import java.io.IOException;
import java.net.ConnectException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.apache.commons.dbcp.BasicDataSource;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

import com.google.gerrit.reviewdb.client.Change.Key;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.events.ChangeEvent;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.gerrit.server.project.ProjectControl;
import com.google.gson.Gson;
import com.google.inject.Provider;

import com.ericsson.gerrit.plugins.eventslog.MalformedQueryException;
import com.ericsson.gerrit.plugins.eventslog.SQLStore;

public class SQLStoreTest {
  private static final String TEST_PATH = "jdbc:h2:mem:";
  private static final String TEST_DRIVER = "org.h2.Driver";
  private static final String TEST_OPTIONS = "DB_CLOSE_DELAY=-1";
  private static final String MSG = "terminating connection";
  private static final String MSG2 = "message";

  private EasyMockSupport easyMock;
  private ProjectControl.GenericFactory pcFactoryMock;
  private Provider<CurrentUser> userProviderMock;
  private EventsLogConfig cfgMock;
  private SQLException SQLExceptionMock;
  private ConnectException connException;
  private SQLHandler handler;
  private SQLStore store;

  private String path = TEST_PATH + TABLE_NAME + ";" + TEST_OPTIONS;
  private Connection conn;
  private Statement stat;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() throws SQLException {
    conn = DriverManager.getConnection(path);
    stat = conn.createStatement();
    easyMock = new EasyMockSupport();
    pcFactoryMock = easyMock.createNiceMock(ProjectControl.GenericFactory.class);
    userProviderMock = easyMock.createNiceMock(Provider.class);
    cfgMock = easyMock.createNiceMock(EventsLogConfig.class);
    expect(cfgMock.getStoreUrl()).andReturn(TEST_PATH).once();
    expect(cfgMock.getUrlOptions()).andReturn(TEST_OPTIONS).once();
    expect(cfgMock.getStoreDriver()).andReturn(TEST_DRIVER).once();
    easyMock.replayAll();
  }

  public void tearDown() throws Exception {
    stat.execute("DROP TABLE " + TABLE_NAME);
    store.stop();
  }

  private void setUpHandler() {
    handler = new SQLHandler(cfgMock);
    store = new SQLStore(pcFactoryMock, userProviderMock, cfgMock, handler);
    store.start();
  }

  private void setUpHandlerMock() {
    handler = easyMock.createNiceMock(SQLHandler.class);
    cfgMock = easyMock.createNiceMock(EventsLogConfig.class);
    SQLExceptionMock = easyMock.createNiceMock(SQLException.class);
    connException = easyMock.createNiceMock(ConnectException.class);
    easyMock.resetAll();

    expect(cfgMock.getStoreUrl()).andReturn(TEST_PATH).once();
    expect(cfgMock.getUrlOptions()).andReturn(TEST_OPTIONS).once();
    expect(cfgMock.getStoreDriver()).andReturn(TEST_DRIVER).once();
  }

  @Test
  public void storeThenQueryVisible() throws Exception {
    setUpHandler();
    String genericQuery = "SELECT * FROM " + TABLE_NAME;
    MockEvent mockEvent = new MockEvent();
    ProjectControl pcMock = easyMock.createNiceMock(ProjectControl.class);
    CurrentUser userMock = easyMock.createNiceMock(CurrentUser.class);
    easyMock.resetAll();
    expect(userProviderMock.get()).andStubReturn(userMock);
    expect(pcFactoryMock.controlFor(mockEvent.getProjectNameKey(),
        userMock)).andStubReturn(pcMock);
    expect(pcMock.isVisible()).andStubReturn(true);
    easyMock.replayAll();
    store.storeEvent(mockEvent);
    List<String> events = store.queryChangeEvents(genericQuery);
    Gson gson = new Gson();
    String json = gson.toJson(mockEvent);
    assertEquals(1, events.size());
    assertEquals(json, events.get(0));
    tearDown();
  }

  @Test
  public void storeThenQueryNotVisible() throws Exception {
    setUpHandler();
    String genericQuery = "SELECT * FROM " + TABLE_NAME;
    MockEvent mockEvent = new MockEvent();
    ProjectControl pcMock = easyMock.createNiceMock(ProjectControl.class);
    CurrentUser userMock = easyMock.createNiceMock(CurrentUser.class);
    easyMock.resetAll();
    expect(userProviderMock.get()).andStubReturn(userMock);
    expect(pcFactoryMock.controlFor(mockEvent.getProjectNameKey(),
        userMock)).andStubReturn(pcMock);
    expect(pcMock.isVisible()).andStubReturn(false);
    easyMock.replayAll();
    store.storeEvent(mockEvent);
    List<String> events = store.queryChangeEvents(genericQuery);
    assertEquals(0, events.size());
    tearDown();
  }

  @Test(expected = MalformedQueryException.class)
  public void throwBadRequestTriggerOnBadQuery()
      throws MalformedQueryException {
    setUpHandler();
    String badQuery = "bad query";
    easyMock.resetAll();
    easyMock.replayAll();
    store.queryChangeEvents(badQuery);
    easyMock.verifyAll();
  }

  @Test
  public void notReturnEventOfNonExistingProject() throws Exception {
    setUpHandler();
    String genericQuery = "SELECT * FROM " + TABLE_NAME;
    MockEvent mockEvent = new MockEvent();
    Project.NameKey projectMock = easyMock.createMock(Project.NameKey.class);
    easyMock.resetAll();
    expect(projectMock.get()).andStubReturn(" ");
    expect(pcFactoryMock.controlFor(EasyMock.anyObject(Project.NameKey.class),
        EasyMock.anyObject(CurrentUser.class))).andThrow(
        new NoSuchProjectException(projectMock));
    easyMock.replayAll();
    store.storeEvent(mockEvent);
    List<String> events = store.queryChangeEvents(genericQuery);
    assertEquals(0, events.size());
    tearDown();
  }

  @Test
  public void notReturnEventWithNoVisibilityInfo() throws Exception {
    setUpHandler();
    String genericQuery = "SELECT * FROM " + TABLE_NAME;
    MockEvent mockEvent = new MockEvent();
    Project.NameKey projectMock = easyMock.createMock(Project.NameKey.class);
    easyMock.resetAll();
    expect(projectMock.get()).andStubReturn(" ");
    expect(pcFactoryMock.controlFor(EasyMock.anyObject(Project.NameKey.class),
        EasyMock.anyObject(CurrentUser.class))).andThrow(
        new IOException());
    easyMock.replayAll();
    store.storeEvent(mockEvent);
    List<String> events = store.queryChangeEvents(genericQuery);
    assertEquals(0, events.size());
    tearDown();
  }

  @Test
  public void retryOnConnectException() throws Exception {
    setUpHandlerMock();
    expect(cfgMock.getMaxTries()).andReturn(3).once();
    expect(handler.getConnection(EasyMock.anyObject(BasicDataSource.class)))
    .andReturn(conn).once();
    expect(handler.getConnection(EasyMock.anyObject(BasicDataSource.class)))
    .andThrow(SQLExceptionMock).times(3);
    expect(SQLExceptionMock.getCause()).andReturn(connException).times(3);
    easyMock.replayAll();
    store = new SQLStore(pcFactoryMock, userProviderMock, cfgMock, handler);
    store.start();

    MockEvent mockEvent = new MockEvent();
    store.storeEvent(mockEvent);
    easyMock.verifyAll();
  }

  @Test
  public void retryOnMessage() throws Exception {
    setUpHandlerMock();
    expect(cfgMock.getMaxTries()).andReturn(3).once();
    expect(handler.getConnection(EasyMock.anyObject(BasicDataSource.class)))
    .andReturn(conn).once();
    expect(handler.getConnection(EasyMock.anyObject(BasicDataSource.class)))
    .andThrow(SQLExceptionMock).times(3);
    expect(SQLExceptionMock.getCause()).andReturn(null).times(3);
    expect(SQLExceptionMock.getMessage()).andReturn(MSG).times(3);
    easyMock.replayAll();
    store = new SQLStore(pcFactoryMock, userProviderMock, cfgMock, handler);
    store.start();

    MockEvent mockEvent = new MockEvent();
    store.storeEvent(mockEvent);
    easyMock.verifyAll();
  }

  @Test
  public void noRetryOnMessage() throws Exception {
    setUpHandlerMock();
    expect(cfgMock.getMaxTries()).andReturn(3).once();
    expect(handler.getConnection(EasyMock.anyObject(BasicDataSource.class)))
    .andReturn(conn).once();
    expect(handler.getConnection(EasyMock.anyObject(BasicDataSource.class)))
    .andThrow(SQLExceptionMock).once();
    expect(SQLExceptionMock.getCause()).andReturn(null).once();
    expect(SQLExceptionMock.getMessage()).andReturn(MSG2).once();
    easyMock.replayAll();
    store = new SQLStore(pcFactoryMock, userProviderMock, cfgMock, handler);
    store.start();

    MockEvent mockEvent = new MockEvent();
    store.storeEvent(mockEvent);
    easyMock.verifyAll();
  }

  @Test
  public void noRetryOnZeroMaxTries() throws Exception {
    setUpHandlerMock();
    expect(cfgMock.getMaxTries()).andReturn(0).once();
    expect(handler.getConnection(EasyMock.anyObject(BasicDataSource.class)))
    .andReturn(conn).once();
    expect(handler.getConnection(EasyMock.anyObject(BasicDataSource.class)))
    .andThrow(SQLExceptionMock).once();
    expect(SQLExceptionMock.getCause()).andReturn(connException).once();
    easyMock.replayAll();
    store = new SQLStore(pcFactoryMock, userProviderMock, cfgMock, handler);
    store.start();

    MockEvent mockEvent = new MockEvent();
    store.storeEvent(mockEvent);
    easyMock.verifyAll();
  }

  public class MockEvent extends ChangeEvent {
    public String project = "mock project";

    MockEvent() {
      super("mock event");
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
}
