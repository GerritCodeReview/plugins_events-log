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

package com.ericsson.gerrit.plugins.eventslog;

import com.google.gerrit.common.EventListener;
import com.google.gerrit.server.events.Event;
import com.google.gerrit.server.events.ProjectEvent;
import com.google.inject.Inject;
import java.util.concurrent.ScheduledExecutorService;

/** Listen to Events and store them into the EventStore */
class EventHandler implements EventListener {
  private final EventStore store;
  private final ScheduledExecutorService pool;

  @Inject
  EventHandler(EventStore store, @EventPool ScheduledExecutorService pool) {
    this.store = store;
    this.pool = pool;
  }

  @Override
  public void onEvent(Event event) {
    if (event instanceof ProjectEvent) {
      pool.execute(new StoreEventTask((ProjectEvent) event));
    }
  }

  class StoreEventTask implements Runnable {
    private ProjectEvent event;

    StoreEventTask(ProjectEvent event) {
      this.event = event;
    }

    @Override
    public void run() {
      store.storeEvent(event);
    }

    @Override
    public String toString() {
      return "(Events-log) Insert Event";
    }
  }
}
