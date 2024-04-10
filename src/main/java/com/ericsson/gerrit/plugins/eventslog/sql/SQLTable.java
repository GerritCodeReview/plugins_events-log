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

package com.ericsson.gerrit.plugins.eventslog.sql;

import static java.lang.String.format;

final class SQLTable {
  static final String TABLE_NAME = "ChangeEvents";
  static final String PRIMARY_ENTRY = "id";
  static final String PROJECT_ENTRY = "project";
  static final String DATE_ENTRY = "date_created";
  static final String EVENT_ENTRY = "event_info";

  /** This is the name of the index that tracks the created date. */
  private static final String CREATED_INDEX = "created_idx";
  /** This is the name of the index that tracks the project. */
  private static final String PROJECT_INDEX = "project_idx";
  /**
   * This is the H2 idempotent index-creation query format. Inputs, in order: index-name,
   * table-name, index-column
   */
  private static final String H2_INDEX_CREATION_FORMAT = "CREATE INDEX IF NOT EXISTS %s ON %s (%s)";
  /**
   * This is the MySQL idempotent index-creation query format. Inputs, in order: table-name,
   * index-name, table-name, index-name, index-column
   */
  private static final String MYSQL_INDEX_CREATION_FORMAT =
      "SET @x := (SELECT COUNT(*) FROM information_schema.statistics WHERE table_name = '%s' AND index_name = '%s' AND table_schema = DATABASE());\n"
          + "SET @sql := IF( @x > 0, 'SELECT ''Index exists.''', 'ALTER TABLE %s ADD INDEX %s (%s);');\n"
          + "PREPARE stmt FROM @sql;\n"
          + "EXECUTE stmt";
  /**
   * This is the Postgres idempotent index-creation query format. Inputs, in order: index-name,
   * index-name, table-name, index-column
   */
  private static final String POSTGRESQL_INDEX_CREATION_FORMAT =
      "DO $$\n"
          + "BEGIN\n"
          + "IF NOT EXISTS (\n"
          + "    SELECT 1\n"
          + "    FROM   pg_class c\n"
          + "    JOIN   pg_namespace n ON n.oid = c.relnamespace\n"
          + "    WHERE  c.relname = '%s'\n"
          + "    AND    n.nspname = 'public'\n"
          + "    ) THEN\n"
          + "    CREATE INDEX %s ON %s (%s);\n"
          + "END IF;\n"
          + "END$$;";

  /**
   * This is the Spanner idempotent index-creation query format. Inputs, in order: index-name,
   * table-name, index-column
   */
  private static final String SPANNER_INDEX_CREATION_FORMAT =
      "CREATE INDEX IF NOT EXISTS %s ON %s (%s)";

  private SQLTable() {}

  static String createTableQuery(SQLDialect databaseDialect) {
    StringBuilder query = new StringBuilder(140);
    query.append(format("CREATE TABLE IF NOT EXISTS %s(", TABLE_NAME));
    switch (databaseDialect) {
      case POSTGRESQL:
        query.append(format("%s SERIAL PRIMARY KEY,", PRIMARY_ENTRY));
        break;
      case SPANNER:
        query.append(format("%s STRING(36) DEFAULT (GENERATE_UUID()), ", PRIMARY_ENTRY));
        break;
      case MYSQL:
      case H2:
      default:
        query.append(format("%s INT AUTO_INCREMENT PRIMARY KEY,", PRIMARY_ENTRY));
    }
    switch (databaseDialect) {
      case SPANNER:
        query.append(format("%s STRING(255),", PROJECT_ENTRY));
        query.append(format("%s TIMESTAMP DEFAULT (CURRENT_TIMESTAMP()),", DATE_ENTRY));
        query.append(format("%s STRING(MAX))", EVENT_ENTRY));
        query.append(format(" PRIMARY KEY (%s)", PRIMARY_ENTRY));
        break;
      default:
        query.append(format("%s VARCHAR(255),", PROJECT_ENTRY));
        query.append(format("%s TIMESTAMP DEFAULT NOW(),", DATE_ENTRY));
        query.append(format("%s TEXT)", EVENT_ENTRY));
    }
    return query.toString();
  }

  static String createIndexes(SQLDialect databaseDialect) {
    switch (databaseDialect) {
      case POSTGRESQL:
        return getPostgresqlIndexQuery();
      case MYSQL:
        return getMysqlIndexQuery();
      case H2:
      default:
        return getH2IndexQuery();
    }
  }

  private static String getPostgresqlIndexQuery() {
    StringBuilder query = new StringBuilder(540);
    query.append(
        format(
            POSTGRESQL_INDEX_CREATION_FORMAT,
            CREATED_INDEX,
            CREATED_INDEX,
            TABLE_NAME,
            DATE_ENTRY));
    query.append("\n;\n");
    query.append(
        format(
            POSTGRESQL_INDEX_CREATION_FORMAT,
            PROJECT_INDEX,
            PROJECT_INDEX,
            TABLE_NAME,
            PROJECT_ENTRY));
    return query.toString();
  }

  private static String getMysqlIndexQuery() {
    StringBuilder query = new StringBuilder();
    query.append(
        format(
            MYSQL_INDEX_CREATION_FORMAT,
            TABLE_NAME,
            CREATED_INDEX,
            TABLE_NAME,
            CREATED_INDEX,
            DATE_ENTRY));
    query.append(";");
    query.append(
        format(
            MYSQL_INDEX_CREATION_FORMAT,
            TABLE_NAME,
            PROJECT_INDEX,
            TABLE_NAME,
            PROJECT_INDEX,
            PROJECT_ENTRY));
    return query.toString();
  }

  private static String getH2IndexQuery() {
    StringBuilder query = new StringBuilder();
    query.append(format(H2_INDEX_CREATION_FORMAT, CREATED_INDEX, TABLE_NAME, DATE_ENTRY));
    query.append(";");
    query.append(format(H2_INDEX_CREATION_FORMAT, PROJECT_INDEX, TABLE_NAME, PROJECT_ENTRY));
    return query.toString();
  }

  static String createSpannerDateIndex() {
    StringBuilder query = new StringBuilder();
    query.append(format(SPANNER_INDEX_CREATION_FORMAT, CREATED_INDEX, TABLE_NAME, DATE_ENTRY));
    return query.toString();
  }

  static String createSpannerProjectIndex() {
    StringBuilder query = new StringBuilder();
    query.append(format(SPANNER_INDEX_CREATION_FORMAT, PROJECT_INDEX, TABLE_NAME, PROJECT_ENTRY));
    return query.toString();
  }
}
