// Copyright (C) 2019 The Android Open Source Project
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

/** SQLDialect provides enumerations for the various supported dialects of SQL. */
public enum SQLDialect {
  H2,
  MYSQL,
  POSTGRESQL,
  SPANNER;

  /**
   * This attempts to determine the SQL dialect from the JDBC URL. If the URL does not match one of
   * the supported dialects, then H2 will be returned by default.
   *
   * @param jdbcUrl The JDBC URL.
   * @return The dialect for the JDBC URL.
   */
  public static SQLDialect fromJdbcUrl(String jdbcUrl) {
    if (jdbcUrl.contains("postgresql")) {
      return POSTGRESQL;
    } else if (jdbcUrl.contains("mysql")) {
      return MYSQL;
    } else if (jdbcUrl.contains("cloudspanner")) {
      return SPANNER;
    }
    return H2;
  }
}
