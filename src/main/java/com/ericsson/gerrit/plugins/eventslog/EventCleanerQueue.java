// Copyright (C) 2018 The Android Open Source Project
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

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.server.git.WorkQueue;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.concurrent.ScheduledExecutorService;

@Singleton
public class EventCleanerQueue implements LifecycleListener {
  private final WorkQueue workQueue;
  private final String pluginName;
  private ScheduledExecutorService pool;

  @Inject
  public EventCleanerQueue(WorkQueue workQueue, @PluginName String pluginName) {
    this.workQueue = workQueue;
    this.pluginName = pluginName;
  }

  @Override
  public void start() {
    pool = workQueue.createQueue(1, String.format("[%s] Remove events", pluginName));
  }

  @Override
  public void stop() {
    if (pool != null) {
      pool = null;
    }
  }

  ScheduledExecutorService getPool() {
    return this.pool;
  }
}
