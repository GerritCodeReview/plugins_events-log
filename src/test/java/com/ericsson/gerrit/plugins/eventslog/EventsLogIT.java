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

import static com.google.common.truth.Truth.assertThat;

import com.google.gerrit.acceptance.config.GerritConfig;
import com.google.gerrit.acceptance.LightweightPluginDaemonTest;
import com.google.gerrit.acceptance.TestPlugin;
import org.junit.Test;

@TestPlugin(
    name = "events-log",
    sysModule = "com.ericsson.gerrit.plugins.eventslog.sql.SQLModule",
    httpModule = "com.ericsson.gerrit.plugins.eventslog.HttpModule")
public class EventsLogIT extends LightweightPluginDaemonTest {

  @Test
  @GerritConfig(name = "plugin.events-log.storeUrl", value = "jdbc:h2:mem:db")
  public void getEventsShallBeConsistent() throws Exception {
    String events = "/plugins/events-log/events/?t1=1970-01-01;t2=2999-01-01";
    String change1 = "refs/changes/01/1/1";

    createChange();
    String response = adminRestSession.get(events).getEntityContent();
    assertThat(response).contains(change1);

    createChange();
    response = adminRestSession.get(events).getEntityContent();
    assertThat(response).contains(change1);
    assertThat(response).contains("refs/changes/02/2/1");
  }
}
