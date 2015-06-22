// Copyright (C) 2015 Ericsson
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

package com.ericsson.gerrit.plugins.eventslog.sql;

import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.internal.UniqueAnnotations;

import com.ericsson.gerrit.plugins.eventslog.EventModule;
import com.ericsson.gerrit.plugins.eventslog.EventStore;
import com.ericsson.gerrit.plugins.eventslog.EventsLogConfig;
import com.ericsson.gerrit.plugins.eventslog.QueryMaker;

class SqlModule extends AbstractModule {

  private static final String H2_DB_PREFIX = "jdbc:h2:";

  @Override
  protected void configure() {
    install(new EventModule());
    bind(EventStore.class).to(SQLStore.class);
    bind(LifecycleListener.class).annotatedWith(UniqueAnnotations.create()).to(
        SQLStore.class);
    bind(QueryMaker.class).to(SQLQueryMaker.class);
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
    return new SQLClient(cfg.getLocalStoreDriver(), H2_DB_PREFIX
        + cfg.getLocalStorePath(), cfg.getUrlOptions());
  }
}
