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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;

import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.After;
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

  private EasyMockSupport easyMock;
  private ProjectControl.GenericFactory pcFactoryMock;
  private Provider<CurrentUser> userProviderMock;
  private EventsLogConfig cfgMock;
  private SQLStore store;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() {
    easyMock = new EasyMockSupport();
    pcFactoryMock = easyMock.createNiceMock(ProjectControl.GenericFactory.class);
    userProviderMock = easyMock.createNiceMock(Provider.class);
    cfgMock = easyMock.createNiceMock(EventsLogConfig.class);
    expect(cfgMock.getStoreUrl()).andReturn(TEST_PATH).once();
    expect(cfgMock.getUrlOptions()).andReturn(TEST_OPTIONS).once();
    expect(cfgMock.getStoreDriver()).andReturn(TEST_DRIVER).once();
    easyMock.replayAll();
    store = new SQLStore(pcFactoryMock, userProviderMock, cfgMock);
    store.start();
  }

  @After
  public void tearDown() throws Exception {
    String path = TEST_PATH + TABLE_NAME + ";" + TEST_OPTIONS;
    Connection conn = DriverManager.getConnection(path);
    Statement stat = conn.createStatement();
    stat.execute("DROP TABLE " + TABLE_NAME);
  }

  @Test
  public void storeThenQueryVisible() throws Exception {
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
  }

  @Test
  public void storeThenQueryNotVisible() throws Exception {
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
  }

  @Test(expected = MalformedQueryException.class)
  public void throwBadRequestTriggerOnBadQuery()
      throws MalformedQueryException {
    String badQuery = "bad query";
    easyMock.resetAll();
    easyMock.replayAll();
    store.queryChangeEvents(badQuery);
    easyMock.verifyAll();
  }

  @Test
  public void notReturnEventOfNonExistingProject() throws Exception {
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
  }

  @Test
  public void notReturnEventWithNoVisibilityInfo() throws Exception {
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
