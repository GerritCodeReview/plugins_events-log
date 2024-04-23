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

import java.sql.Timestamp;
import java.util.Objects;

class SQLEntry implements Comparable<SQLEntry> {
  private SQLDialect dialect;
  private String name;
  private Timestamp timestamp;
  private String event;
  private Object id;

  SQLEntry(SQLDialect dialect, String name, Timestamp timestamp, String event, Object id) {
    this.dialect = dialect;
    this.name = name;
    this.timestamp = timestamp;
    this.event = event;
    this.id = id;
  }

  public SQLDialect getDialect() {
    return dialect;
  }

  public String getName() {
    return name;
  }

  public Timestamp getTimestamp() {
    return timestamp;
  }

  public String getEvent() {
    return event;
  }

  @Override
  public int compareTo(SQLEntry o) {
    switch (dialect) {
      case SPANNER:
        return timestamp.compareTo(o.timestamp);
      default:
        return Integer.compare((int) id, (int) o.id);
    }
  }

  @Override
  public boolean equals(Object o) {
    SQLEntry other = (SQLEntry) o;
    return Objects.equals(id, other.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
