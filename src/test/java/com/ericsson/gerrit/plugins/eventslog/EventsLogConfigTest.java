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
import static com.ericsson.gerrit.plugins.eventslog.EventsLogConfig.DEFAULT_URL;
import static com.ericsson.gerrit.plugins.eventslog.EventsLogConfig.DEFAULT_WAIT_TIME;
import static com.google.common.truth.Truth.assertThat;
import static org.easymock.EasyMock.expect;

import com.google.common.base.Joiner;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.config.SitePaths;

import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;

public class EventsLogConfigTest {
  private SitePaths site;
  private EventsLogConfig config;
  private PluginConfigFactory cfgFactoryMock;
  private PluginConfig configMock;
  private EasyMockSupport easyMock;
  private String defaultLocalStorePath;
  private String localStorePath;
  private String [] urlOptions = new String[]{"a=b", "c=d"};

  private static final int CUSTOM_EVICT_IDLE_TIME = 10000;

  @Rule
  public TemporaryFolder gerrit_site = new TemporaryFolder();

  @Before
  public void setUp() throws IOException {
    easyMock = new EasyMockSupport();
    site = new SitePaths(gerrit_site.getRoot().toPath());
    Files.createDirectories(site.etc_dir);
    defaultLocalStorePath = site.site_path.toString() + "/events-db/";
    configMock = easyMock.createMock(PluginConfig.class);
    cfgFactoryMock = easyMock.createMock(PluginConfigFactory.class);
    expect(cfgFactoryMock.getFromGerritConfig(EasyMock.anyString(),
        EasyMock.anyBoolean())).andStubReturn(configMock);
  }

  private void setUpDefaults() {
    expect(configMock.getBoolean(CONFIG_COPY_LOCAL, DEFAULT_COPY_LOCAL)).andReturn(DEFAULT_COPY_LOCAL);
    expect(configMock.getInt(CONFIG_MAX_AGE, DEFAULT_MAX_AGE)).andReturn(DEFAULT_MAX_AGE);
    expect(configMock.getInt(CONFIG_MAX_TRIES, DEFAULT_MAX_TRIES)).andReturn(DEFAULT_MAX_TRIES);
    expect(configMock.getInt(CONFIG_RETURN_LIMIT, DEFAULT_RETURN_LIMIT)).andReturn(DEFAULT_RETURN_LIMIT);
    expect(configMock.getInt(CONFIG_CONN_TIME, DEFAULT_CONN_TIME)).andReturn(DEFAULT_CONN_TIME);
    expect(configMock.getInt(CONFIG_WAIT_TIME, DEFAULT_WAIT_TIME)).andReturn(DEFAULT_WAIT_TIME);
    expect(configMock.getString(CONFIG_DRIVER, DEFAULT_DRIVER)).andReturn(DEFAULT_DRIVER);
    expect(configMock.getString(CONFIG_URL, DEFAULT_URL)).andReturn(DEFAULT_URL);
    expect(configMock.getString(CONFIG_LOCAL_PATH, defaultLocalStorePath)).andReturn(defaultLocalStorePath);
    expect(configMock.getStringList(CONFIG_URL_OPTIONS)).andReturn(new String[0]);
    expect(configMock.getString(CONFIG_USERNAME)).andReturn(null);
    expect(configMock.getString(CONFIG_PASSWORD)).andReturn(null);
    expect(configMock.getInt(CONFIG_EVICT_IDLE_TIME, DEFAULT_EVICT_IDLE_TIME)).andReturn(DEFAULT_EVICT_IDLE_TIME);

    easyMock.replayAll();
  }

  private void setUpCustom() {
    localStorePath = "~/gerrit/events-db/";
    expect(configMock.getBoolean(CONFIG_COPY_LOCAL, DEFAULT_COPY_LOCAL)).andReturn(true);
    expect(configMock.getInt(CONFIG_MAX_AGE, DEFAULT_MAX_AGE)).andReturn(20);
    expect(configMock.getInt(CONFIG_MAX_TRIES, DEFAULT_MAX_TRIES)).andReturn(5);
    expect(configMock.getInt(CONFIG_RETURN_LIMIT, DEFAULT_RETURN_LIMIT)).andReturn(10000);
    expect(configMock.getInt(CONFIG_CONN_TIME, DEFAULT_CONN_TIME)).andReturn(5000);
    expect(configMock.getInt(CONFIG_WAIT_TIME, DEFAULT_WAIT_TIME)).andReturn(5000);
    expect(configMock.getString(CONFIG_DRIVER, DEFAULT_DRIVER)).andReturn("org.h2.Driver2");
    expect(configMock.getString(CONFIG_URL, DEFAULT_URL)).andReturn("jdbc:h2:~/gerrit/db");
    expect(configMock.getString(CONFIG_LOCAL_PATH, defaultLocalStorePath)).andReturn(localStorePath);
    expect(configMock.getStringList(CONFIG_URL_OPTIONS)).andReturn(urlOptions);
    expect(configMock.getString(CONFIG_USERNAME)).andReturn("testUsername");
    expect(configMock.getString(CONFIG_PASSWORD)).andReturn("testPassword");
    expect(configMock.getInt(CONFIG_EVICT_IDLE_TIME, DEFAULT_EVICT_IDLE_TIME)).andReturn(CUSTOM_EVICT_IDLE_TIME);

    easyMock.replayAll();
  }

  @Test
  public void shouldReturnDefaultsWhenMissingConfig() {
    setUpDefaults();
    config = new EventsLogConfig(cfgFactoryMock, site, null);
    assertThat(config.getCopyLocal()).isFalse();
    assertThat(config.getMaxAge()).isEqualTo(30);
    assertThat(config.getMaxTries()).isEqualTo(3);
    assertThat(config.getReturnLimit()).isEqualTo(5000);
    assertThat(config.getConnectTime()).isEqualTo(1000);
    assertThat(config.getWaitTime()).isEqualTo(1000);
    assertThat(config.getLocalStoreDriver()).isEqualTo(DEFAULT_DRIVER);
    assertThat(config.getLocalStorePath().toString() + "/").isEqualTo(defaultLocalStorePath);
    assertThat(config.getStoreDriver()).isEqualTo(DEFAULT_DRIVER);
    assertThat(config.getStoreUrl()).isEqualTo(DEFAULT_URL);
    assertThat(config.getUrlOptions()).isEmpty();
    assertThat(config.getStoreUsername()).isNull();
    assertThat(config.getStorePassword()).isNull();
    assertThat(config.getEvictIdleTime()).isEqualTo(DEFAULT_EVICT_IDLE_TIME);
  }

  @Test
  public void shouldReturnConfigValues() {
    setUpCustom();
    config = new EventsLogConfig(cfgFactoryMock, site, null);
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
