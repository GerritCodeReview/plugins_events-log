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

import java.sql.Timestamp;

class SQLEntry implements Comparable<SQLEntry> {
  private static final int SEED = 31;
  private String name;
  private Timestamp timestamp;
  private String event;
  private int id;

  SQLEntry(String name, Timestamp timestamp, String event, int id) {
    this.name = name;
    this.timestamp = timestamp;
    this.event = event;
    this.id = id;
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
    return Integer.compare(this.id, o.id);
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }
    if (this.getClass() != o.getClass()) {
      return false;
    }
    return this.id == ((SQLEntry) o).id;
  }

  @Override
  public int hashCode() {
    return SEED + id;
  }
}
