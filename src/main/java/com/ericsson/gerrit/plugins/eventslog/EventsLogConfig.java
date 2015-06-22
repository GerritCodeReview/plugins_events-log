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
import com.google.gerrit.server.config.SitePaths;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.nio.file.Path;
import java.nio.file.Paths;

@Singleton
public class EventsLogConfig {
  static final String H2_DB_SUFFIX = ".h2.db";
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

  static final boolean DEFAULT_COPY_LOCAL = false;
  static final int DEFAULT_MAX_AGE = 30;
  static final int DEFAULT_MAX_TRIES = 3;
  static final int DEFAULT_RETURN_LIMIT = 5000;
  static final int DEFAULT_WAIT_TIME = 1000;
  static final int DEFAULT_CONN_TIME = 1000;
  static final String DEFAULT_DRIVER = "org.h2.Driver";
  static final String DEFAULT_URL = "jdbc:h2:~/db/";
  static String DEFAULT_LOCAL_PATH;

  private boolean copyLocal = DEFAULT_COPY_LOCAL;
  private int maxAge = DEFAULT_MAX_AGE;
  private int maxTries = DEFAULT_MAX_TRIES;
  private int returnLimit = DEFAULT_RETURN_LIMIT;
  private int waitTime = DEFAULT_WAIT_TIME;
  private int connectTime = DEFAULT_CONN_TIME;
  private String storeDriver;
  private String storeUrl;
  private Path localStorePath;
  private String urlOptions;
  private String storeUsername;
  private String storePassword;

  @Inject
  public EventsLogConfig(PluginConfigFactory cfgFactory, SitePaths site,
      @PluginName String pluginName) {
    DEFAULT_LOCAL_PATH = site.site_path.toString() + "/events-db/";
    PluginConfig cfg = cfgFactory.getFromGerritConfig(pluginName, true);
    copyLocal = cfg.getBoolean(CONFIG_COPY_LOCAL, DEFAULT_COPY_LOCAL);
    maxAge = cfg.getInt(CONFIG_MAX_AGE, DEFAULT_MAX_AGE);
    maxTries = cfg.getInt(CONFIG_MAX_TRIES, DEFAULT_MAX_TRIES);
    returnLimit = cfg.getInt(CONFIG_RETURN_LIMIT, DEFAULT_RETURN_LIMIT);
    waitTime = cfg.getInt(CONFIG_WAIT_TIME, DEFAULT_WAIT_TIME);
    connectTime = cfg.getInt(CONFIG_CONN_TIME, DEFAULT_CONN_TIME);
    storeDriver = cfg.getString(CONFIG_DRIVER, DEFAULT_DRIVER);
    storeUrl = cfg.getString(CONFIG_URL, DEFAULT_URL);
    localStorePath = Paths.get(cfg.getString(CONFIG_LOCAL_PATH, DEFAULT_LOCAL_PATH));
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

  public String getLocalStoreDriver() {
    return DEFAULT_DRIVER;
  }

  public Path getLocalStorePath() {
    return localStorePath;
  }

  public boolean getCopyLocal() {
    return copyLocal;
  }
}
