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

import static com.ericsson.gerrit.plugins.eventslog.sql.SQLTable.DATE_ENTRY;
import static com.ericsson.gerrit.plugins.eventslog.sql.SQLTable.PRIMARY_ENTRY;
import static com.ericsson.gerrit.plugins.eventslog.sql.SQLTable.TABLE_NAME;

import com.ericsson.gerrit.plugins.eventslog.EventsLogConfig;
import com.ericsson.gerrit.plugins.eventslog.MalformedQueryException;
import com.ericsson.gerrit.plugins.eventslog.QueryMaker;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Map;

@Singleton
class SQLQueryMaker implements QueryMaker {
  private static final int TWO = 2;
  private static final String TIME_ONE = "t1";
  private static final String TIME_TWO = "t2";
  private static final String UTC = "Z";
  private static final DateTimeFormatter DATE_TIME_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  private static final DateTimeFormatter DATE_ONLY_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd");

  private final int returnLimit;

  @Inject
  SQLQueryMaker(EventsLogConfig config) {
    this.returnLimit = config.getReturnLimit();
  }

  @Override
  public String formQueryFromRequestParameters(Map<String, String> params)
      throws MalformedQueryException {
    if (params == null) {
      return getDefaultQuery();
    }
    String[] dates;
    try {
      dates = parseDates(params.get(TIME_ONE), params.get(TIME_TWO));
    } catch (DateTimeParseException e) {
      throw new MalformedQueryException(e);
    }
    return String.format(
        "SELECT * FROM %s WHERE %s BETWEEN '%s%s' and '%s%s' ORDER BY date_created LIMIT %d",
        TABLE_NAME, DATE_ENTRY, dates[0], UTC, dates[1], UTC, returnLimit);
  }

  @Override
  public String getDefaultQuery() {
    return String.format(
        "SELECT * FROM (SELECT * FROM %s ORDER BY %s DESC LIMIT %s) a ORDER BY %s ASC",
        TABLE_NAME, PRIMARY_ENTRY, returnLimit, PRIMARY_ENTRY);
  }

  private String[] parseDates(String dateOne, String dateTwo)
      throws MalformedQueryException, DateTimeParseException {
    if (dateOne == null && dateTwo == null) {
      throw new MalformedQueryException();
    }
    LocalDateTime dOne = dateOne == null ? LocalDateTime.now() : parseDate(dateOne);
    LocalDateTime dTwo = dateTwo == null ? LocalDateTime.now() : parseDate(dateTwo);
    LocalDateTime[] dates = new LocalDateTime[TWO];

    dates[0] = dOne.isBefore(dTwo) ? dOne : dTwo;
    dates[1] = dOne.isBefore(dTwo) ? dTwo : dOne;
    return new String[] {DATE_TIME_FORMAT.format(dates[0]), DATE_TIME_FORMAT.format(dates[1])};
  }

  private LocalDateTime parseDate(String date) throws DateTimeParseException {
    LocalDateTime parsedDate;
    try {
      parsedDate = LocalDateTime.parse(date, DATE_TIME_FORMAT);
    } catch (DateTimeParseException e) {
      LocalDate localDate = LocalDate.parse(date, DATE_ONLY_FORMAT);
      parsedDate = localDate.atStartOfDay();
    }
    return parsedDate;
  }
}
