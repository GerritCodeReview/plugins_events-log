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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ericsson.gerrit.plugins.eventslog.EventsLogConfig;
import com.google.gerrit.extensions.events.ProjectDeletedListener;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EventsLogCleanerTest {
  private static final int MAX_AGE = 30;
  private static final String PROJECT = "testProject";

  @Mock private EventsLogConfig cfgMock;
  @Mock private EventsLogCleaner logCleanerMock;
  @Mock private SQLClient eventsDb;
  @Mock private SQLClient localEventsDb;
  @Mock private ProjectDeletedListener.Event event;

  private ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
  private EventsLogCleaner eventsLogCleaner;

  @Before
  public void setUp() throws Exception {
    when(event.getProjectName()).thenReturn(PROJECT);
    eventsLogCleaner = new EventsLogCleaner(eventsDb, localEventsDb, executor);
  }

  @Test
  public void testOnProjectDeleted() throws InterruptedException {
    eventsLogCleaner.onProjectDeleted(event);
    executor.awaitTermination(1, TimeUnit.SECONDS);
    verify(eventsDb, times(1)).removeProjectEvents(PROJECT);
    verify(localEventsDb, times(1)).removeProjectEvents(PROJECT);
  }

  @Test
  public void testScheduleCleaning() throws InterruptedException {
    eventsLogCleaner.scheduleCleaningWith(MAX_AGE, 1, 60_000);
    executor.awaitTermination(250, TimeUnit.MILLISECONDS);
    verify(eventsDb, times(1)).removeOldEvents(MAX_AGE);
    verify(localEventsDb, times(1)).removeOldEvents(MAX_AGE);
  }

  @Test
  public void testRemoveOldEventsAsync() throws InterruptedException {
    eventsLogCleaner.removeOldEventsAsync(MAX_AGE);
    executor.awaitTermination(250, TimeUnit.MILLISECONDS);
    verify(eventsDb, times(1)).removeOldEvents(MAX_AGE);
    verify(localEventsDb, times(1)).removeOldEvents(MAX_AGE);
  }

  @After
  public void tearDown() throws Exception {
    executor.shutdownNow();
  }
}
