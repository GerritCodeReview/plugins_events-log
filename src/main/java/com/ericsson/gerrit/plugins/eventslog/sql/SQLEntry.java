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

import java.time.Instant;
import java.util.Objects;

class SQLEntry implements Comparable<SQLEntry> {
  private String name;
  private Instant timestamp;
  private String event;
  private Object id;

  SQLEntry(String name, Instant timestamp, String event, Object id) {
    this.name = name;
    this.timestamp = timestamp;
    this.event = event;
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public String getEvent() {
    return event;
  }

  @Override
  public int compareTo(SQLEntry o) {
    return timestamp.compareTo(o.timestamp);
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof SQLEntry) {
      SQLEntry other = (SQLEntry) o;
      return Objects.equals(id, other.id);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
