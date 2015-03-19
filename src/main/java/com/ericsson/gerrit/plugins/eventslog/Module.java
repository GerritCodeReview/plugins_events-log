// Copyright (C) 2014 Ericsson
//
// Licensed under the Apache License, Version 2.0 (the "License"),
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
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.internal.UniqueAnnotations;

import java.util.concurrent.ScheduledThreadPoolExecutor;

class Module extends AbstractModule {

  @Override
  protected void configure() {
    bind(EventQueue.class).in(Scopes.SINGLETON);
    bind(EventHandler.class).in(Scopes.SINGLETON);
    bind(LifecycleListener.class)
        .annotatedWith(UniqueAnnotations.create())
        .to(EventQueue.class);
    bind(EventStore.class).to(SQLStore.class);
    bind(LifecycleListener.class).annotatedWith(UniqueAnnotations.create()).to(
        SQLStore.class);
    bind(QueryMaker.class).to(SQLQueryMaker.class);

    DynamicSet.bind(binder(), EventListener.class).to(EventHandler.class);
  }

  @Provides
  @EventPool
  ScheduledThreadPoolExecutor provideEventPool(EventQueue queue) {
    return queue.getPool();
  }

  @Provides
  @Singleton
  @EventsDb
  SQLClient provideSqlClient(EventsLogConfig cfg) {
    SQLClient sqlClient =
        new SQLClient(cfg.getStoreDriver(), cfg.getStoreUrl(),
            cfg.getUrlOptions());
    sqlClient.setUsername(cfg.getStoreUsername());
    sqlClient.setPassword(cfg.getStorePassword());
    return sqlClient;
  }

  @Provides
  @Singleton
  @LocalEventsDb
  SQLClient provideLocalSqlClient(EventsLogConfig cfg) {
    return new SQLClient(cfg.getLocalStoreDriver(), cfg.getLocalStoreUrl(),
        cfg.getUrlOptions());
  }
}
