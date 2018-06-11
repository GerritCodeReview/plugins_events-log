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

package com.ericsson.gerrit.plugins.eventslog;

import static com.ericsson.gerrit.plugins.eventslog.EventsLogConfig.CONFIG_CONN_TIME;
import static com.ericsson.gerrit.plugins.eventslog.EventsLogConfig.CONFIG_COPY_LOCAL;
import static com.ericsson.gerrit.plugins.eventslog.EventsLogConfig.CONFIG_DRIVER;
import static com.ericsson.gerrit.plugins.eventslog.EventsLogConfig.CONFIG_EVICT_IDLE_TIME;
import static com.ericsson.gerrit.plugins.eventslog.EventsLogConfig.CONFIG_LOCAL_PATH;
import static com.ericsson.gerrit.plugins.eventslog.EventsLogConfig.CONFIG_MAX_AGE;
import static com.ericsson.gerrit.plugins.eventslog.EventsLogConfig.CONFIG_MAX_TRIES;
import static com.ericsson.gerrit.plugins.eventslog.EventsLogConfig.CONFIG_PASSWORD;
import static com.ericsson.gerrit.plugins.eventslog.EventsLogConfig.CONFIG_RETURN_LIMIT;
import static com.ericsson.gerrit.plugins.eventslog.EventsLogConfig.CONFIG_URL;
import static com.ericsson.gerrit.plugins.eventslog.EventsLogConfig.CONFIG_URL_OPTIONS;
import static com.ericsson.gerrit.plugins.eventslog.EventsLogConfig.CONFIG_USERNAME;
import static com.ericsson.gerrit.plugins.eventslog.EventsLogConfig.CONFIG_WAIT_TIME;
import static com.ericsson.gerrit.plugins.eventslog.EventsLogConfig.DEFAULT_CONN_TIME;
import static com.ericsson.gerrit.plugins.eventslog.EventsLogConfig.DEFAULT_COPY_LOCAL;
import static com.ericsson.gerrit.plugins.eventslog.EventsLogConfig.DEFAULT_DRIVER;
import static com.ericsson.gerrit.plugins.eventslog.EventsLogConfig.DEFAULT_EVICT_IDLE_TIME;
import static com.ericsson.gerrit.plugins.eventslog.EventsLogConfig.DEFAULT_MAX_AGE;
import static com.ericsson.gerrit.plugins.eventslog.EventsLogConfig.DEFAULT_MAX_TRIES;
import static com.ericsson.gerrit.plugins.eventslog.EventsLogConfig.DEFAULT_RETURN_LIMIT;
import static com.ericsson.gerrit.plugins.eventslog.EventsLogConfig.DEFAULT_WAIT_TIME;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import com.google.common.base.Joiner;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.config.SitePaths;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EventsLogConfigTest {
  private static final String PLUGIN_NAME = "eventsLog";
  private static final int CUSTOM_EVICT_IDLE_TIME = 10000;

  private SitePaths site;
  private EventsLogConfig config;
  private String defaultLocalStorePath;
  private String defaultUrl;
  private String localStorePath;
  private String[] urlOptions = new String[] {"DB_CLOSE_DELAY=10"};

  @Mock private PluginConfigFactory cfgFactoryMock;
  @Mock private PluginConfig configMock;

  @Rule public TemporaryFolder gerrit_site = new TemporaryFolder();

  @Before
  public void setUp() throws IOException {
    site = new SitePaths(gerrit_site.getRoot().toPath());
    Files.createDirectories(site.etc_dir);
    defaultLocalStorePath = site.site_path.toString() + "/events-db/";
    defaultUrl = "jdbc:h2:" + site.data_dir.toString() + "/db";
    when(cfgFactoryMock.getFromGerritConfig(PLUGIN_NAME, true)).thenReturn(configMock);
  }

  private void setUpDefaults() {
    when(configMock.getBoolean(CONFIG_COPY_LOCAL, DEFAULT_COPY_LOCAL))
        .thenReturn(DEFAULT_COPY_LOCAL);
    when(configMock.getInt(CONFIG_MAX_AGE, DEFAULT_MAX_AGE)).thenReturn(DEFAULT_MAX_AGE);
    when(configMock.getInt(CONFIG_MAX_TRIES, DEFAULT_MAX_TRIES)).thenReturn(DEFAULT_MAX_TRIES);
    when(configMock.getInt(CONFIG_RETURN_LIMIT, DEFAULT_RETURN_LIMIT))
        .thenReturn(DEFAULT_RETURN_LIMIT);
    when(configMock.getInt(CONFIG_CONN_TIME, DEFAULT_CONN_TIME)).thenReturn(DEFAULT_CONN_TIME);
    when(configMock.getInt(CONFIG_WAIT_TIME, DEFAULT_WAIT_TIME)).thenReturn(DEFAULT_WAIT_TIME);
    when(configMock.getString(CONFIG_DRIVER, DEFAULT_DRIVER)).thenReturn(DEFAULT_DRIVER);
    when(configMock.getString(CONFIG_URL, defaultUrl)).thenReturn(defaultUrl);
    when(configMock.getString(CONFIG_LOCAL_PATH, defaultLocalStorePath))
        .thenReturn(defaultLocalStorePath);
    when(configMock.getStringList(CONFIG_URL_OPTIONS)).thenReturn(new String[] {});
    when(configMock.getString(CONFIG_USERNAME)).thenReturn(null);
    when(configMock.getString(CONFIG_PASSWORD)).thenReturn(null);
    when(configMock.getInt(CONFIG_EVICT_IDLE_TIME, DEFAULT_EVICT_IDLE_TIME))
        .thenReturn(DEFAULT_EVICT_IDLE_TIME);
  }

  private void setUpCustom() {
    localStorePath = "~/gerrit/events-db/";
    when(configMock.getBoolean(CONFIG_COPY_LOCAL, DEFAULT_COPY_LOCAL)).thenReturn(true);
    when(configMock.getInt(CONFIG_MAX_AGE, DEFAULT_MAX_AGE)).thenReturn(20);
    when(configMock.getInt(CONFIG_MAX_TRIES, DEFAULT_MAX_TRIES)).thenReturn(5);
    when(configMock.getInt(CONFIG_RETURN_LIMIT, DEFAULT_RETURN_LIMIT)).thenReturn(10000);
    when(configMock.getInt(CONFIG_CONN_TIME, DEFAULT_CONN_TIME)).thenReturn(5000);
    when(configMock.getInt(CONFIG_WAIT_TIME, DEFAULT_WAIT_TIME)).thenReturn(5000);
    when(configMock.getString(CONFIG_DRIVER, DEFAULT_DRIVER)).thenReturn("org.h2.Driver2");
    when(configMock.getString(CONFIG_URL, defaultUrl)).thenReturn("jdbc:h2:~/gerrit/db");
    when(configMock.getString(CONFIG_LOCAL_PATH, defaultLocalStorePath)).thenReturn(localStorePath);
    when(configMock.getStringList(CONFIG_URL_OPTIONS)).thenReturn(urlOptions);
    when(configMock.getString(CONFIG_USERNAME)).thenReturn("testUsername");
    when(configMock.getString(CONFIG_PASSWORD)).thenReturn("testPassword");
    when(configMock.getInt(CONFIG_EVICT_IDLE_TIME, DEFAULT_EVICT_IDLE_TIME))
        .thenReturn(CUSTOM_EVICT_IDLE_TIME);
  }

  @Test
  public void shouldReturnDefaultsWhenMissingConfig() {
    setUpDefaults();
    config = new EventsLogConfig(cfgFactoryMock, site, PLUGIN_NAME);
    assertThat(config.getCopyLocal()).isFalse();
    assertThat(config.getMaxAge()).isEqualTo(30);
    assertThat(config.getMaxTries()).isEqualTo(3);
    assertThat(config.getReturnLimit()).isEqualTo(5000);
    assertThat(config.getConnectTime()).isEqualTo(1000);
    assertThat(config.getWaitTime()).isEqualTo(1000);
    assertThat(config.getLocalStoreDriver()).isEqualTo(DEFAULT_DRIVER);
    assertThat(config.getLocalStorePath().toString() + "/").isEqualTo(defaultLocalStorePath);
    assertThat(config.getStoreDriver()).isEqualTo(DEFAULT_DRIVER);
    assertThat(config.getStoreUrl()).isEqualTo(defaultUrl);
    assertThat(config.getUrlOptions()).isEmpty();
    assertThat(config.getStoreUsername()).isNull();
    assertThat(config.getStorePassword()).isNull();
    assertThat(config.getEvictIdleTime()).isEqualTo(DEFAULT_EVICT_IDLE_TIME);
  }

  @Test
  public void shouldReturnConfigValues() {
    setUpCustom();
    config = new EventsLogConfig(cfgFactoryMock, site, PLUGIN_NAME);
    assertThat(config.getCopyLocal()).isTrue();
    assertThat(config.getMaxAge()).isEqualTo(20);
    assertThat(config.getMaxTries()).isEqualTo(5);
    assertThat(config.getReturnLimit()).isEqualTo(10000);
    assertThat(config.getConnectTime()).isEqualTo(5000);
    assertThat(config.getWaitTime()).isEqualTo(5000);
    assertThat(config.getLocalStoreDriver()).isEqualTo(DEFAULT_DRIVER);
    assertThat(config.getLocalStorePath().toString() + "/").isEqualTo(localStorePath);
    assertThat(config.getStoreDriver()).isEqualTo("org.h2.Driver2");
    assertThat(config.getStoreUrl()).isEqualTo("jdbc:h2:~/gerrit/db");
    assertThat(config.getUrlOptions()).isEqualTo(Joiner.on(";").join(urlOptions));
    assertThat(config.getStoreUsername()).isEqualTo("testUsername");
    assertThat(config.getStorePassword()).isEqualTo("testPassword");
    assertThat(config.getEvictIdleTime()).isEqualTo(CUSTOM_EVICT_IDLE_TIME);
  }
}
