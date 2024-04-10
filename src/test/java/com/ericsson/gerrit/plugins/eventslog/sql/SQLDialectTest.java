// Copyright (C) 2018 The Android Open Source Project
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

import org.junit.Test;

public class SQLDialectTest {
  @Test
  public void defaultIsH2() throws Exception {
    assertThat(SQLDialect.fromJdbcUrl("")).isEqualTo(SQLDialect.H2);
    assertThat(SQLDialect.fromJdbcUrl("jdbc:")).isEqualTo(SQLDialect.H2);
    assertThat(SQLDialect.fromJdbcUrl("jdbc:whatever://")).isEqualTo(SQLDialect.H2);
  }

  @Test
  public void mysqlIsParsed() throws Exception {
    assertThat(SQLDialect.fromJdbcUrl("jdbc:mysql://")).isEqualTo(SQLDialect.MYSQL);
  }

  @Test
  public void postgresqlIsParsed() throws Exception {
    assertThat(SQLDialect.fromJdbcUrl("jdbc:postgresql://")).isEqualTo(SQLDialect.POSTGRESQL);
  }

  @Test
  public void spannerIsParsed() throws Exception {
    assertThat(SQLDialect.fromJdbcUrl("jdbc:cloudspanner://")).isEqualTo(SQLDialect.SPANNER);
  }
}
