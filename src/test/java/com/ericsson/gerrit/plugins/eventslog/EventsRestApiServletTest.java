// Copyright (C) 2014 Ericsson
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

package com.ericsson.gerrit.plugins.eventslog;

import static com.google.common.truth.Truth.assertThat;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

import com.google.gerrit.server.CurrentUser;
import com.google.inject.Provider;

import com.ericsson.gerrit.plugins.eventslog.EventsRestApiServlet;
import com.ericsson.gerrit.plugins.eventslog.EventStore;
import com.ericsson.gerrit.plugins.eventslog.MalformedQueryException;
import com.ericsson.gerrit.plugins.eventslog.QueryMaker;

public class EventsRestApiServletTest {
  private EasyMockSupport easyMock;
  private EventStore storeMock;
  private QueryMaker queryMakerMock;
  private Provider<CurrentUser> userProviderMock;
  private CurrentUser userMock;
  private HttpServletRequest reqMock;
  private HttpServletResponse rspMock;
  private EventsRestApiServlet eventServlet;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp() {
    easyMock = new EasyMockSupport();
    storeMock = easyMock.createNiceMock(EventStore.class);
    queryMakerMock = easyMock.createNiceMock(QueryMaker.class);
    userProviderMock = easyMock.createNiceMock(Provider.class);
    userMock = easyMock.createNiceMock(CurrentUser.class);
    reqMock = easyMock.createNiceMock(HttpServletRequest.class);
    rspMock = easyMock.createNiceMock(HttpServletResponse.class);
    easyMock.replayAll();
    eventServlet = new EventsRestApiServlet(storeMock, queryMakerMock,
        userProviderMock);
  }

  @Test
  public void queryStringSplitting() throws Exception {
    String queryStringMock = "a=1;b=2";
    Map<String, String> paramMock = new HashMap<>();
    paramMock.put("a", "1");
    paramMock.put("b", "2");
    Capture<Map<String, String>> catcher = newCapture();
    easyMock.resetAll();
    expect(userProviderMock.get()).andStubReturn(userMock);
    expect(userMock.isIdentifiedUser()).andStubReturn(true);
    expect(reqMock.getQueryString()).andStubReturn(queryStringMock);
    expect(queryMakerMock.formQueryFromRequestParameters(capture(catcher)))
      .andStubReturn("random query");
    expect(storeMock.queryChangeEvents("random query")).andStubReturn(
        new ArrayList<String>());
    easyMock.replayAll();
    eventServlet.doGet(reqMock, rspMock);
    Map<String, String> capturedParam = catcher.getValue();
    assertThat(paramMock).isEqualTo(capturedParam);
  }

  @Test
  public void badQueryString() throws Exception {
    String queryStringMock = "a;b";
    Capture<Map<String, String>> catcher = newCapture();
    easyMock.resetAll();
    expect(userProviderMock.get()).andStubReturn(userMock);
    expect(userMock.isIdentifiedUser()).andStubReturn(true);
    expect(reqMock.getQueryString()).andStubReturn(queryStringMock);
    expect(queryMakerMock.formQueryFromRequestParameters(capture(catcher)))
      .andStubReturn("random query");
    expect(storeMock.queryChangeEvents("random query")).andStubReturn(
        new ArrayList<String>());
    easyMock.replayAll();
    eventServlet.doGet(reqMock, rspMock);
    Map<String, String> capturedParam = catcher.getValue();
    assertThat(capturedParam).isEmpty();
  }

  @Test
  public void testUnAuthorizedCode() throws Exception {
    easyMock.resetAll();
    expect(userProviderMock.get()).andStubReturn(userMock);
    expect(userMock.isIdentifiedUser()).andStubReturn(false);
    rspMock.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    expectLastCall().once();
    easyMock.replayAll();
    eventServlet.doGet(reqMock, rspMock);
    easyMock.verifyAll();
  }

  @Test
  public void testBadRequestCode() throws Exception {
    easyMock.resetAll();
    expect(userProviderMock.get()).andStubReturn(userMock);
    expect(userMock.isIdentifiedUser()).andStubReturn(true);
    expect(queryMakerMock.formQueryFromRequestParameters(
        EasyMock.<Map<String, String>> anyObject())).andStubThrow(
            new MalformedQueryException());
    rspMock.sendError(HttpServletResponse.SC_BAD_REQUEST);
    expectLastCall().once();
    easyMock.replayAll();
    eventServlet.doGet(reqMock, rspMock);
    easyMock.verifyAll();
  }

  @Test
  public void queryDatabaseAndWrite() throws Exception {
    PrintWriter outMock = easyMock.createMock(PrintWriter.class);
    List<String> listMock = new ArrayList<>();
    listMock.add("event one");
    listMock.add("event two");
    easyMock.resetAll();
    expect(userProviderMock.get()).andStubReturn(userMock);
    expect(userMock.isIdentifiedUser()).andStubReturn(true);
    expect(rspMock.getWriter()).andStubReturn(outMock);
    expect(storeMock.queryChangeEvents(EasyMock.anyString())).andReturn(
        listMock);
    outMock.write(listMock.get(0) + "\n");
    expectLastCall().once();
    outMock.write(listMock.get(1) + "\n");
    expectLastCall().once();
    easyMock.replayAll();
    eventServlet.doGet(reqMock, rspMock);
    easyMock.verifyAll();
  }
}
