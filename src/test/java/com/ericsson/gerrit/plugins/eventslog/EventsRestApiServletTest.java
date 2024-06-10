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

package com.ericsson.gerrit.plugins.eventslog;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ericsson.gerrit.plugins.eventslog.sql.SQLStore;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gerrit.server.CurrentUser;
import com.google.inject.Provider;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EventsRestApiServletTest {
  private static final String RANDOM_QUERY = "random query";

  @Mock private SQLStore storeMock;
  @Mock private Provider<CurrentUser> userProviderMock;
  @Mock private CurrentUser userMock;
  @Mock private HttpServletRequest reqMock;
  @Mock private HttpServletResponse rspMock;
  @Captor private ArgumentCaptor<Map<String, String>> captor;

  private EventsRestApiServlet eventServlet;

  @Before
  public void setUp() {
    eventServlet = new EventsRestApiServlet(storeMock, userProviderMock);

    when(userProviderMock.get()).thenReturn(userMock);
    when(userMock.isIdentifiedUser()).thenReturn(true);
  }

  @Test
  public void queryStringSplitting() throws Exception {
    when(reqMock.getQueryString()).thenReturn("a=1;b=2");
    when(storeMock.queryChangeEvents(captor.capture())).thenReturn(new ArrayList<>());
    eventServlet.doGet(reqMock, rspMock);
    assertThat(ImmutableMap.of("a", "1", "b", "2")).isEqualTo(captor.getValue());
  }

  @Test
  public void badQueryString() throws Exception {
    when(reqMock.getQueryString()).thenReturn("a;b");
    when(storeMock.queryChangeEvents(captor.capture())).thenReturn(new ArrayList<>());
    eventServlet.doGet(reqMock, rspMock);
    assertThat(captor.getValue()).isEmpty();
  }

  @Test
  public void testUnAuthorizedCode() throws Exception {
    when(userMock.isIdentifiedUser()).thenReturn(false);
    eventServlet.doGet(reqMock, rspMock);
    verify(rspMock).sendError(HttpServletResponse.SC_UNAUTHORIZED);
  }

  @Test
  public void testBadRequestCode() throws Exception {
    when(reqMock.getQueryString()).thenReturn("@@");
    Map<String, String> emptyParams = ImmutableMap.of();
    when(storeMock.queryChangeEvents(emptyParams)).thenThrow(new MalformedQueryException());
    eventServlet.doGet(reqMock, rspMock);
    verify(rspMock).sendError(HttpServletResponse.SC_BAD_REQUEST);
  }

  @Test
  public void queryDatabaseAndWrite() throws Exception {
    when(reqMock.getQueryString()).thenReturn("@@");
    PrintWriter outMock = mock(PrintWriter.class);
    List<String> listMock = ImmutableList.of("event one", "event two");
    when(rspMock.getWriter()).thenReturn(outMock);
    when(storeMock.queryChangeEvents(captor.capture())).thenReturn(listMock);
    eventServlet.doGet(reqMock, rspMock);
    verify(outMock).write(listMock.get(0) + "\n");
    verify(outMock).write(listMock.get(1) + "\n");
  }
}
