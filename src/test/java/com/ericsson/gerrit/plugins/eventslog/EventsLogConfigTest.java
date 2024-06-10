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
import static com.ericsson.gerrit.plugins.eventslog.EventsLogConfig.CONFIG_LOCAL_PATH;
import static com.ericsson.gerrit.plugins.eventslog.EventsLogConfig.CONFIG_MAX_AGE;
import static com.ericsson.gerrit.plugins.eventslog.EventsLogConfig.CONFIG_MAX_CONNECTIONS;
import static com.ericsson.gerrit.plugins.eventslog.EventsLogConfig.CONFIG_MAX_TRIES;
import static com.ericsson.gerrit.plugins.eventslog.EventsLogConfig.CONFIG_PASSWORD;
import static com.ericsson.gerrit.plugins.eventslog.EventsLogConfig.CONFIG_RETURN_LIMIT;
import static com.ericsson.gerrit.plugins.eventslog.EventsLogConfig.CONFIG_URL;
import static com.ericsson.gerrit.plugins.eventslog.EventsLogConfig.CONFIG_URL_OPTIONS;
import static com.ericsson.gerrit.plugins.eventslog.EventsLogConfig.CONFIG_USERNAME;
import static com.ericsson.gerrit.plugins.eventslog.EventsLogConfig.CONFIG_WAIT_TIME;
import static com.ericsson.gerrit.plugins.eventslog.EventsLogConfig.DEFAULT_CONN_TIME;
import static com.ericsson.gerrit.plugins.eventslog.EventsLogConfig.DEFAULT_MAX_AGE;
import static com.ericsson.gerrit.plugins.eventslog.EventsLogConfig.DEFAULT_MAX_CONNECTIONS;
import static com.ericsson.gerrit.plugins.eventslog.EventsLogConfig.DEFAULT_MAX_TRIES;
import static com.ericsson.gerrit.plugins.eventslog.EventsLogConfig.DEFAULT_RETURN_LIMIT;
import static com.ericsson.gerrit.plugins.eventslog.EventsLogConfig.DEFAULT_WAIT_TIME;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.gerrit.server.config.SitePaths;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import org.eclipse.jgit.lib.Config;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EventsLogConfigTest {
  private static final String LOCAL_STORE_PATH = "~/gerrit/events-db/";
  private static final String PLUGIN_NAME = "eventsLog";
  private static final int CUSTOM_MAX_CONNECTIONS = 32;
  private static final List<String> urlOptions = ImmutableList.of("DB_CLOSE_DELAY=10");

  private SitePaths site;

  @Mock private PluginConfigFactory cfgFactoryMock;

  @Rule public TemporaryFolder gerrit_site = new TemporaryFolder();

  @Before
  public void setUp() throws IOException {
    site = new SitePaths(gerrit_site.getRoot().toPath());
    Files.createDirectories(site.etc_dir);
  }

  @Test
  public void shouldReturnDefaultsWhenMissingConfig() {
    Config pluginConfig = new Config();
    when(cfgFactoryMock.getGlobalPluginConfig(PLUGIN_NAME)).thenReturn(pluginConfig);
    EventsLogConfig eventsLogConfig = new EventsLogConfig(cfgFactoryMock, site, PLUGIN_NAME);
    assertThat(eventsLogConfig.getCopyLocal()).isFalse();
    assertThat(eventsLogConfig.getMaxAge()).isEqualTo(DEFAULT_MAX_AGE);
    assertThat(eventsLogConfig.getMaxTries()).isEqualTo(DEFAULT_MAX_TRIES);
    assertThat(eventsLogConfig.getReturnLimit()).isEqualTo(DEFAULT_RETURN_LIMIT);
    assertThat(eventsLogConfig.getConnectTime()).isEqualTo(DEFAULT_CONN_TIME);
    assertThat(eventsLogConfig.getWaitTime()).isEqualTo(DEFAULT_WAIT_TIME);
    assertThat(eventsLogConfig.getLocalStorePath().toString() + "/")
        .isEqualTo(site.site_path.toString() + "/events-db/");
    assertThat(eventsLogConfig.getStoreUrl())
        .isEqualTo("jdbc:h2:" + site.data_dir.toString() + "/db");
    assertThat(eventsLogConfig.getUrlOptions()).isEmpty();
    assertThat(eventsLogConfig.getStoreUsername()).isNull();
    assertThat(eventsLogConfig.getStorePassword()).isNull();
    assertThat(eventsLogConfig.getMaxConnections()).isEqualTo(DEFAULT_MAX_CONNECTIONS);
  }

  @Test
  public void shouldReturnConfigValues() {
    when(cfgFactoryMock.getGlobalPluginConfig(PLUGIN_NAME)).thenReturn(customConfig());
    EventsLogConfig eventsLogConfig = new EventsLogConfig(cfgFactoryMock, site, PLUGIN_NAME);
    assertThat(eventsLogConfig.getCopyLocal()).isTrue();
    assertThat(eventsLogConfig.getMaxAge()).isEqualTo(20);
    assertThat(eventsLogConfig.getMaxTries()).isEqualTo(5);
    assertThat(eventsLogConfig.getReturnLimit()).isEqualTo(10000);
    assertThat(eventsLogConfig.getConnectTime()).isEqualTo(5000);
    assertThat(eventsLogConfig.getWaitTime()).isEqualTo(5000);
    assertThat(eventsLogConfig.getLocalStorePath().toString() + "/").isEqualTo(LOCAL_STORE_PATH);
    assertThat(eventsLogConfig.getStoreUrl()).isEqualTo("jdbc:h2:~/gerrit/db");
    assertThat(eventsLogConfig.getUrlOptions()).asList().isEqualTo(urlOptions);
    assertThat(eventsLogConfig.getStoreUsername()).isEqualTo("testUsername");
    assertThat(eventsLogConfig.getStorePassword()).isEqualTo("testPassword");
    assertThat(eventsLogConfig.getMaxConnections()).isEqualTo(CUSTOM_MAX_CONNECTIONS);
  }

  private Config customConfig() {
    Config config = new Config();
    config.setBoolean(PLUGIN_NAME, null, CONFIG_COPY_LOCAL, true);
    config.setInt(PLUGIN_NAME, null, CONFIG_MAX_AGE, 20);
    config.setInt(PLUGIN_NAME, null, CONFIG_MAX_TRIES, 5);
    config.setInt(PLUGIN_NAME, null, CONFIG_RETURN_LIMIT, 10000);
    config.setInt(PLUGIN_NAME, null, CONFIG_CONN_TIME, 5000);
    config.setInt(PLUGIN_NAME, null, CONFIG_WAIT_TIME, 5000);
    config.setString(PLUGIN_NAME, null, CONFIG_URL, "jdbc:h2:~/gerrit/db");
    config.setString(PLUGIN_NAME, null, CONFIG_LOCAL_PATH, LOCAL_STORE_PATH);
    config.setStringList(PLUGIN_NAME, null, CONFIG_URL_OPTIONS, urlOptions);
    config.setString(PLUGIN_NAME, null, CONFIG_USERNAME, "testUsername");
    config.setString(PLUGIN_NAME, null, CONFIG_PASSWORD, "testPassword");
    config.setInt(PLUGIN_NAME, null, CONFIG_MAX_CONNECTIONS, CUSTOM_MAX_CONNECTIONS);
    return config;
  }
}
