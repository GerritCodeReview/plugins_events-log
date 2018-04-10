// Copyright (C) 2015 Ericsson
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

import com.google.common.base.Joiner;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.config.SitePaths;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Holder of all things related to events-log plugin configuration. */
@Singleton
public class EventsLogConfig {
  static final String CONFIG_COPY_LOCAL = "copyLocal";
  static final String CONFIG_MAX_AGE = "maxAge";
  static final String CONFIG_MAX_TRIES = "maxTries";
  static final String CONFIG_RETURN_LIMIT = "returnLimit";
  static final String CONFIG_DRIVER = "storeDriver";
  static final String CONFIG_URL = "storeUrl";
  static final String CONFIG_LOCAL_PATH = "localStorePath";
  static final String CONFIG_URL_OPTIONS = "urlOptions";
  static final String CONFIG_USERNAME = "storeUsername";
  static final String CONFIG_PASSWORD = "storePassword";
  static final String CONFIG_WAIT_TIME = "retryTimeout";
  static final String CONFIG_CONN_TIME = "connectTimeout";
  static final String CONFIG_EVICT_IDLE_TIME = "evictIdleTime";

  static final boolean DEFAULT_COPY_LOCAL = false;
  static final int DEFAULT_MAX_AGE = 30;
  static final int DEFAULT_MAX_TRIES = 3;
  static final int DEFAULT_RETURN_LIMIT = 5000;
  static final int DEFAULT_WAIT_TIME = 1000;
  static final int DEFAULT_CONN_TIME = 1000;
  static final String DEFAULT_DRIVER = "org.h2.Driver";
  static final int DEFAULT_EVICT_IDLE_TIME = 1000 * 60;

  private boolean copyLocal;
  private int maxAge;
  private int maxTries;
  private int returnLimit;
  private int waitTime;
  private int connectTime;
  private String storeDriver;
  private String storeUrl;
  private Path localStorePath;
  private String urlOptions;
  private String storeUsername;
  private String storePassword;
  private int evictIdleTime;
  private String defaultUrl;

  @Inject
  EventsLogConfig(PluginConfigFactory cfgFactory, SitePaths site, @PluginName String pluginName) {
    String defaultLocalPath = site.site_path.toString() + "/events-db/";
    PluginConfig cfg = cfgFactory.getFromGerritConfig(pluginName, true);
    copyLocal = cfg.getBoolean(CONFIG_COPY_LOCAL, DEFAULT_COPY_LOCAL);
    maxAge = cfg.getInt(CONFIG_MAX_AGE, DEFAULT_MAX_AGE);
    maxTries = cfg.getInt(CONFIG_MAX_TRIES, DEFAULT_MAX_TRIES);
    returnLimit = cfg.getInt(CONFIG_RETURN_LIMIT, DEFAULT_RETURN_LIMIT);
    waitTime = cfg.getInt(CONFIG_WAIT_TIME, DEFAULT_WAIT_TIME);
    connectTime = cfg.getInt(CONFIG_CONN_TIME, DEFAULT_CONN_TIME);
    storeDriver = cfg.getString(CONFIG_DRIVER, DEFAULT_DRIVER);
    defaultUrl = "jdbc:h2:" + site.data_dir.toString() + "/db";
    storeUrl = cfg.getString(CONFIG_URL, defaultUrl);
    localStorePath = Paths.get(cfg.getString(CONFIG_LOCAL_PATH, defaultLocalPath));
    urlOptions = concatenate(cfg.getStringList(CONFIG_URL_OPTIONS));
    storeUsername = cfg.getString(CONFIG_USERNAME);
    storePassword = cfg.getString(CONFIG_PASSWORD);
    evictIdleTime = cfg.getInt(CONFIG_EVICT_IDLE_TIME, DEFAULT_EVICT_IDLE_TIME);
  }

  private String concatenate(String[] stringList) {
    return Joiner.on(";").join(stringList);
  }

  public int getMaxAge() {
    return maxAge;
  }

  public int getReturnLimit() {
    return returnLimit;
  }

  public int getWaitTime() {
    return waitTime;
  }

  public int getConnectTime() {
    return connectTime;
  }

  public String getStoreDriver() {
    return storeDriver;
  }

  public String getStoreUrl() {
    return storeUrl;
  }

  public String getUrlOptions() {
    return urlOptions;
  }

  public String getStoreUsername() {
    return storeUsername;
  }

  public String getStorePassword() {
    return storePassword;
  }

  public int getMaxTries() {
    return maxTries;
  }

  /** @return the local-store (database) driver which happens to be h2 */
  public String getLocalStoreDriver() {
    return DEFAULT_DRIVER;
  }

  public Path getLocalStorePath() {
    return localStorePath;
  }

  public boolean getCopyLocal() {
    return copyLocal;
  }

  public int getEvictIdleTime() {
    return evictIdleTime;
  }
}
