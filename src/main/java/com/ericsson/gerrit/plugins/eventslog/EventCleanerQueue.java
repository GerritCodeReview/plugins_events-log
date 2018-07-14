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

import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.server.git.WorkQueue;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.concurrent.ScheduledThreadPoolExecutor;

@Singleton
public class EventCleanerQueue implements LifecycleListener {
  private final WorkQueue workQueue;
  private WorkQueue.Executor pool;

  @Inject
  public EventCleanerQueue(WorkQueue workQueue) {
    this.workQueue = workQueue;
  }

  @Override
  public void start() {
    pool = workQueue.createQueue(1, "[events-log] Remove events");
  }

  @Override
  public void stop() {
    if (pool != null) {
      pool.unregisterWorkQueue();
      pool = null;
    }
  }

  public ScheduledThreadPoolExecutor getPool() {
    return pool;
  }
}
