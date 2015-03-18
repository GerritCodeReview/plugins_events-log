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

package com.ericsson.gerrit.plugins.eventslog;

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class EventsLogConfig {
  static final String CONFIG_POOL_SIZE = "threadPoolSize";
  static final String CONFIG_MAX_AGE = "maxAge";
  static final String CONFIG_MAX_TRIES = "maxTries";
  static final String CONFIG_RETURN_LIMIT = "returnLimit";
  static final String CONFIG_DRIVER = "storeDriver";
  static final String CONFIG_URL = "storeUrl";
  static final String CONFIG_URL_OPTIONS = "urlOptions";
  static final String CONFIG_USERNAME = "storeUsername";
  static final String CONFIG_PASSWORD = "storePassword";
  static final String CONFIG_WAIT_TIME = "waitForRetry";

  static final int DEFAULT_POOL_SIZE = 1;
  static final int DEFAULT_MAX_AGE = 30;
  static final int DEFAULT_MAX_TRIES = 3;
  static final int DEFAULT_RETURN_LIMIT = 5000;
  static final int DEFAULT_WAIT_TIME = 1000;
  static final String DEFAULT_DRIVER = "org.h2.Driver";
  static final String DEFAULT_URL = "jdbc:h2:~/db/";

  private int threadPoolSize = DEFAULT_POOL_SIZE;
  private int maxAge = DEFAULT_MAX_AGE;
  private int maxTries = DEFAULT_MAX_TRIES;
  private int returnLimit = DEFAULT_RETURN_LIMIT;
  private int waitTime = DEFAULT_WAIT_TIME;
  private String storeDriver;
  private String storeUrl;
  private String urlOptions;
  private String storeUsername;
  private String storePassword;

  @Inject
  public EventsLogConfig(PluginConfigFactory cfgFactory,
      @PluginName String pluginName) {
    PluginConfig cfg = cfgFactory.getFromGerritConfig(pluginName, true);
    threadPoolSize = cfg.getInt(CONFIG_POOL_SIZE, DEFAULT_POOL_SIZE);
    maxAge = cfg.getInt(CONFIG_MAX_AGE, DEFAULT_MAX_AGE);
    maxTries = cfg.getInt(CONFIG_MAX_TRIES, DEFAULT_MAX_TRIES);
    returnLimit = cfg.getInt(CONFIG_RETURN_LIMIT, DEFAULT_RETURN_LIMIT);
    waitTime = cfg.getInt(CONFIG_WAIT_TIME, DEFAULT_WAIT_TIME);
    storeDriver = cfg.getString(CONFIG_DRIVER, DEFAULT_DRIVER);
    storeUrl = cfg.getString(CONFIG_URL, DEFAULT_URL);
    urlOptions = cfg.getString(CONFIG_URL_OPTIONS, "");
    storeUsername = cfg.getString(CONFIG_USERNAME);
    storePassword = cfg.getString(CONFIG_PASSWORD);
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

  public int getPoolSize() {
    return threadPoolSize;
  }

  public int getMaxTries() {
    return maxTries;
  }
}
