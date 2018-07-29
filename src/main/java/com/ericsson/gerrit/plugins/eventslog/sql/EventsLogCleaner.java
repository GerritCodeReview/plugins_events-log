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

import com.ericsson.gerrit.plugins.eventslog.EventCleanerPool;
import com.google.common.collect.ImmutableList;
import com.google.gerrit.extensions.events.ProjectDeletedListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Singleton
public class EventsLogCleaner implements ProjectDeletedListener {
  private static final int HOUR = 23;
  private static final long INTERVAL = TimeUnit.DAYS.toSeconds(1);

  private final ImmutableList<SQLClient> clients;

  private ScheduledThreadPoolExecutor pool;

  @Inject
  EventsLogCleaner(
      @EventsDb SQLClient eventsDb,
      @LocalEventsDb SQLClient localEventsDb,
      @EventCleanerPool ScheduledThreadPoolExecutor pool) {
    this.clients = ImmutableList.of(eventsDb, localEventsDb);
    this.pool = pool;
  }

  @Override
  public void onProjectDeleted(Event event) {
    removeProjectEventsAsync(event.getProjectName());
  }

  public void removeProjectEventsAsync(String projectName) {
    for (SQLClient client : clients) {
      pool.submit(() -> client.removeProjectEvents(projectName));
    }
  }

  public void scheduleCleaningWith(int maxAge) {
    long initialDelay = getInitialDelay();
    for (SQLClient client : clients) {
      pool.scheduleAtFixedRate(
          () -> client.removeOldEvents(maxAge), initialDelay, INTERVAL, TimeUnit.SECONDS);
    }
  }

  private long getInitialDelay() {
    ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
    ZonedDateTime next = now.withHour(HOUR).truncatedTo(ChronoUnit.HOURS);
    if (now.isAfter(next)) {
      next = next.plusDays(1);
    }
    return Duration.between(now, next).getSeconds();
  }
}
