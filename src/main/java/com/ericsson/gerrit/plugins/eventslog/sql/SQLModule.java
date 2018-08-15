// Copyright (C) 2015 The Android Open Source Project
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

import static com.ericsson.gerrit.plugins.eventslog.EventsLogConfig.H2_DB_PREFIX;

import com.ericsson.gerrit.plugins.eventslog.EventModule;
import com.ericsson.gerrit.plugins.eventslog.EventStore;
import com.ericsson.gerrit.plugins.eventslog.EventsLogConfig;
import com.ericsson.gerrit.plugins.eventslog.QueryMaker;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.internal.UniqueAnnotations;
import com.zaxxer.hikari.HikariConfig;

class SQLModule extends AbstractModule {

  @Override
  protected void configure() {
    install(new EventModule());
    bind(EventStore.class).to(SQLStore.class);
    bind(LifecycleListener.class).annotatedWith(UniqueAnnotations.create()).to(SQLStore.class);
    bind(QueryMaker.class).to(SQLQueryMaker.class);
  }

  @Provides
  @Singleton
  @EventsDb
  SQLClient provideSqlClient(EventsLogConfig cfg, @PluginName String pluginName) {
    HikariConfig dsConfig = new HikariConfig();
    dsConfig.setJdbcUrl(cfg.getStoreUrl());
    dsConfig.setUsername(cfg.getStoreUsername());
    dsConfig.setPassword(cfg.getStorePassword());
    dsConfig.setPoolName("[" + pluginName + "] EventsDb");
    dsConfig.setMaximumPoolSize(cfg.getMaxConnections());
    setDataSourceOptions(cfg, dsConfig);
    return new SQLClient(dsConfig);
  }

  @Provides
  @Singleton
  @LocalEventsDb
  SQLClient provideLocalSqlClient(EventsLogConfig cfg, @PluginName String pluginName) {
    HikariConfig dsConfig = new HikariConfig();
    dsConfig.setJdbcUrl(H2_DB_PREFIX + cfg.getLocalStorePath().resolve(SQLTable.TABLE_NAME));
    dsConfig.setPoolName("[" + pluginName + "] LocalEventsDb");
    setDataSourceOptions(cfg, dsConfig);
    return new SQLClient(dsConfig);
  }

  private void setDataSourceOptions(EventsLogConfig cfg, HikariConfig dsConfig) {
    for (String option : cfg.getUrlOptions()) {
      int equalsPos = option.indexOf('=');
      String key = option.substring(0, equalsPos);
      String value = option.substring(equalsPos + 1);
      dsConfig.addDataSourceProperty(key, value);
    }
  }
}
