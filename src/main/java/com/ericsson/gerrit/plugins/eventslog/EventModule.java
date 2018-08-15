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

package com.ericsson.gerrit.plugins.eventslog;

import com.ericsson.gerrit.plugins.eventslog.sql.EventsLogCleaner;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.extensions.events.ProjectDeletedListener;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.server.events.EventListener;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.internal.UniqueAnnotations;
import java.util.concurrent.ScheduledExecutorService;

/** Configures handling for an event queue while providing its pool. */
public class EventModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(EventQueue.class).in(Scopes.SINGLETON);
    bind(EventHandler.class).in(Scopes.SINGLETON);
    bind(LifecycleListener.class).annotatedWith(UniqueAnnotations.create()).to(EventQueue.class);
    bind(LifecycleListener.class)
        .annotatedWith(UniqueAnnotations.create())
        .to(EventCleanerQueue.class);
    DynamicSet.bind(binder(), EventListener.class).to(EventHandler.class);
    DynamicSet.bind(binder(), ProjectDeletedListener.class).to(EventsLogCleaner.class);
  }

  @Provides
  @EventPool
  ScheduledExecutorService provideEventPool(EventQueue queue) {
    return queue.getPool();
  }

  @Provides
  @EventCleanerPool
  ScheduledExecutorService provideEventCleanerPool(EventCleanerQueue queue) {
    return queue.getPool();
  }
}
