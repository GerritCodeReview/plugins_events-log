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

import static com.google.common.truth.Truth.assertThat;

import java.sql.Timestamp;
import java.util.Calendar;
import org.junit.Before;
import org.junit.Test;

public class SQLEntryTest {

  private static final long NOW = Calendar.getInstance().getTimeInMillis();
  private SQLEntry entry1;
  private SQLEntry entry2;
  private SQLEntry entry3;
  private SQLEntry entry4;
  private SQLEntry entry5;
  private SQLEntry entry6;
  private SQLEntry entry7;

  @Before
  public void setUp() {
    Timestamp timestamp = new Timestamp(NOW);
    entry1 = new SQLEntry(SQLDialect.H2, "name1", timestamp, "event1", Integer.MAX_VALUE);
    entry2 = new SQLEntry(SQLDialect.H2, "name2", timestamp, "event2", Integer.MIN_VALUE);
    entry3 = new SQLEntry(SQLDialect.H2, "name3", timestamp, "event3", 0);
    entry4 = new SQLEntry(SQLDialect.H2, "name4", timestamp, "event4", Integer.MAX_VALUE);
    entry5 =
        new SQLEntry(
            SQLDialect.SPANNER,
            "name5",
            timestamp,
            "event1",
            "b54e6b86-7686-4c3e-a4e4-8edd60ae328e");
    entry6 =
        new SQLEntry(
            SQLDialect.SPANNER,
            "name6",
            timestamp,
            "event2",
            "0ebc1d7f-5888-45d8-8cd1-244eda611a38");
    entry7 =
        new SQLEntry(
            SQLDialect.SPANNER,
            "name7",
            timestamp,
            "event2",
            "0ebc1d7f-5888-45d8-8cd1-244eda611a38");
  }

  @Test
  public void testGetName() throws Exception {
    assertThat(entry1.getName()).isEqualTo("name1");
  }

  @Test
  public void testGetTimestamp() throws Exception {
    assertThat(entry1.getTimestamp()).isEqualTo(new Timestamp(NOW));
  }

  @Test
  public void testGetEvent() throws Exception {
    assertThat(entry1.getEvent()).isEqualTo("event1");
  }

  @Test
  public void testCompareTo() throws Exception {
    assertThat(entry1.compareTo(entry2)).isEqualTo(1);
    assertThat(entry2.compareTo(entry1)).isEqualTo(-1);
  }

  @SuppressWarnings("unlikely-arg-type")
  @Test
  public void testEquals() throws Exception {
    assertThat(entry1.equals(null)).isFalse();
    assertThat(entry1.equals("String object")).isFalse();
    assertThat(entry1.equals(entry2)).isFalse();
    assertThat(entry1.equals(entry4)).isTrue();
    assertThat(entry4.equals(entry5)).isFalse();
    assertThat(entry5.equals(entry6)).isFalse();
    assertThat(entry6.equals(entry7)).isTrue();
  }
}
