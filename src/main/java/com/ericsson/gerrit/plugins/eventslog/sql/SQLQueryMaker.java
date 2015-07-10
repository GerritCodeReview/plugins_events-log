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

import static com.ericsson.gerrit.plugins.eventslog.sql.SQLTable.DATE_ENTRY;
import static com.ericsson.gerrit.plugins.eventslog.sql.SQLTable.PRIMARY_ENTRY;
import static com.ericsson.gerrit.plugins.eventslog.sql.SQLTable.TABLE_NAME;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.ericsson.gerrit.plugins.eventslog.EventsLogConfig;
import com.ericsson.gerrit.plugins.eventslog.MalformedQueryException;
import com.ericsson.gerrit.plugins.eventslog.QueryMaker;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

@Singleton
class SQLQueryMaker implements QueryMaker {
  private static final String TIME_ONE = "t1";
  private static final String TIME_TWO = "t2";
  private static final DateFormat DATE_TIME_FORMAT = new SimpleDateFormat(
      "yyyy-MM-dd HH:mm:ss");
  private static final DateFormat DATE_ONLY_FORMAT = new SimpleDateFormat(
      "yyyy-MM-dd");

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
    Date[] dates;
    try {
      dates = parseDates(params.get(TIME_ONE), params.get(TIME_TWO));
    } catch (ParseException e) {
      throw new MalformedQueryException(e);
    }
    return String.format(
        "SELECT * FROM %s WHERE %s BETWEEN '%s' and '%s' LIMIT %d", TABLE_NAME,
        DATE_ENTRY, DATE_TIME_FORMAT.format(dates[0]),
        DATE_TIME_FORMAT.format(dates[1]), returnLimit);
  }

  @Override
  public String getDefaultQuery() {
    return "SELECT * FROM(SELECT * FROM " + TABLE_NAME
        + " ORDER BY " + PRIMARY_ENTRY + " DESC LIMIT " + returnLimit + ")"
        + " a ORDER BY " + PRIMARY_ENTRY + " ASC";
  }

  private Date[] parseDates(String dateOne, String dateTwo)
      throws MalformedQueryException, ParseException {
    Calendar cal = Calendar.getInstance();
    if (dateOne == null & dateTwo == null) {
      throw new MalformedQueryException();
    }
    Date[] dates = new Date[2];
    Date dOne =
        dateOne == null && dateTwo != null ? cal.getTime() : parseDate(dateOne);
    Date dTwo =
        dateTwo == null && dateOne != null ? cal.getTime() : parseDate(dateTwo);
    dates[0] = dOne.compareTo(dTwo) < 0 ? dOne : dTwo;
    dates[1] = dOne.compareTo(dTwo) < 0 ? dTwo : dOne;
    return dates;
  }

  private Date parseDate(String date) throws ParseException {
    Date parsedDate;
    try {
      parsedDate = DATE_TIME_FORMAT.parse(date);
    } catch (ParseException e) {
      parsedDate = DATE_ONLY_FORMAT.parse(date);
    }
    return parsedDate;
  }
}
