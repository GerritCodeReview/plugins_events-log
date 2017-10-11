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

package com.ericsson.gerrit.plugins.eventslog.sql;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;

import com.ericsson.gerrit.plugins.eventslog.EventsLogConfig;
import com.ericsson.gerrit.plugins.eventslog.MalformedQueryException;
import com.ericsson.gerrit.plugins.eventslog.QueryMaker;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class QueryMakerTest {
  private static final String T2 = "t2";
  private static final String T1 = "t1";
  private static final String OLD_DATE = "2013-10-10 10:00:00";
  private static final String NEW_DATE = "2014-10-10 10:00:00";

  private QueryMaker queryMaker;
  private String defaultQuery;

  @Mock
  private EventsLogConfig cfgMock;

  private String query;

  @Before
  public void setUp() throws Exception {
    when(cfgMock.getReturnLimit()).thenReturn(10);
    queryMaker = new SQLQueryMaker(cfgMock);
    defaultQuery = queryMaker.getDefaultQuery();
  }

  @Test
  public void returnDefaultQueryforNullMap() throws Exception {
    assertThat(queryMaker.formQueryFromRequestParameters(null))
        .isEqualTo(defaultQuery);
  }

  @Test(expected = MalformedQueryException.class)
  public void badParameters() throws Exception {
    queryMaker.formQueryFromRequestParameters(
        ImmutableMap.of(T1, "13/13/32", T2, "14/10/10"));
  }

  @Test
  public void dateOneOnly() throws Exception {
    query = queryMaker
        .formQueryFromRequestParameters(ImmutableMap.of(T1, OLD_DATE));
    assertThat(query).contains(String.format("'%s' and ", OLD_DATE));
  }

  @Test
  public void dateTwoOnly() throws Exception {
    query = queryMaker
        .formQueryFromRequestParameters(ImmutableMap.of(T2, OLD_DATE));
    assertThat(query).contains(String.format("'%s' and ", OLD_DATE));
  }

  @Test(expected = MalformedQueryException.class)
  public void noDate() throws Exception {
    queryMaker
        .formQueryFromRequestParameters(ImmutableMap.of());
  }

  @Test
  public void dateOrdering() throws Exception {
    query = queryMaker.formQueryFromRequestParameters(
        ImmutableMap.of(T1, OLD_DATE, T2, NEW_DATE));
    assertThat(query)
        .contains(String.format("'%s' and '%s'", OLD_DATE, NEW_DATE));

    query = queryMaker.formQueryFromRequestParameters(
        ImmutableMap.of(T1, NEW_DATE, T2, OLD_DATE));
    assertThat(query)
        .contains(String.format("'%s' and '%s'", OLD_DATE, NEW_DATE));
  }

  @Test
  public void bothDateTime() throws Exception {
    query = queryMaker.formQueryFromRequestParameters(
        ImmutableMap.of(T1, OLD_DATE, T2, NEW_DATE));
    assertThat(query).isNotEqualTo(defaultQuery);
  }

  @Test
  public void onlyDateNoTime() throws Exception {
    query = queryMaker.formQueryFromRequestParameters(
        ImmutableMap.of(T1, "2013-10-10", T2, "2014-10-10"));
    assertThat(query).isNotEqualTo(defaultQuery);
  }
}
