// Copyright (C) 2015 Ericsson
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

import org.junit.Before;
import org.junit.Test;

import java.sql.Timestamp;
import java.util.Calendar;

public class SQLEntryTest {

  private static final long NOW = Calendar.getInstance().getTimeInMillis();
  private SQLEntry entry1;
  private SQLEntry entry2;
  private SQLEntry entry3;
  private SQLEntry entry4;

  @Before
  public void setUp() {
    Timestamp timestamp = new Timestamp(NOW);
    entry1 = new SQLEntry("name1", timestamp, "event1", Integer.MAX_VALUE);
    entry2 = new SQLEntry("name2", timestamp, "event2", Integer.MIN_VALUE);
    entry3 = new SQLEntry("name3", timestamp, "event3", 0);
    entry4 = new SQLEntry("name4", timestamp, "event4", Integer.MAX_VALUE);
  }

  @Test
  public void testHashCode() throws Exception {
    assertThat(entry1.hashCode()).isEqualTo(-2147483618);
    assertThat(entry2.hashCode()).isEqualTo(-2147483617);
    assertThat(entry3.hashCode()).isEqualTo(31);
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

  @Test
  public void testEquals() throws Exception {
    assertThat(entry1.equals(null)).isFalse();
    assertThat(entry1.equals("String object")).isFalse();
    assertThat(entry1.equals(entry2)).isFalse();
    assertThat(entry1.equals(entry4)).isTrue();
  }
}
