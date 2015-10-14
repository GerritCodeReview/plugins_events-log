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

import static com.google.common.truth.Truth.assertThat;

import com.google.gerrit.acceptance.PluginDaemonTest;

import org.eclipse.jgit.errors.ConfigInvalidException;
import org.junit.Test;

import java.io.IOException;

public class EventsLogIT extends PluginDaemonTest {
  private static final String EVENTS =
      "/plugins/events-log/events/?t1=1970-01-01;t2=2999-01-01";

  @Override
  protected void beforeTestServerStarts() throws IOException,
      ConfigInvalidException {
    // otherwise default ~/db not deleted after => corrupted next test/run
    setPluginConfigString("storeUrl", "jdbc:h2:" + testSite + "/db/");
  }

  @Test
  public void getEventsShallBeConsistent() throws Exception {
    createChange();
    String response = adminSession.get(EVENTS).getEntityContent();
    assertThat(response).contains("refs/changes/01/1/1");

    createChange();
    response = adminSession.get(EVENTS).getEntityContent();
    assertThat(response).contains("refs/changes/01/1/1");
    assertThat(response).contains("refs/changes/02/2/1");
  }
}
