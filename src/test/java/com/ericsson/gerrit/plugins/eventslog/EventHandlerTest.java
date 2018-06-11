// Copyright (C) 2014 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;
import static org.easymock.EasyMock.expectLastCall;

import com.google.gerrit.common.EventListener;
import com.google.gerrit.server.events.ChangeEvent;
import com.google.gerrit.server.events.Event;

import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ScheduledThreadPoolExecutor;

public class EventHandlerTest {
  private EasyMockSupport easyMock = new EasyMockSupport();
  private EventStore storeMock;
  private EventListener listener;
  private ScheduledThreadPoolExecutor poolMock;

  @Before
  public void setUp() {
    storeMock = easyMock.createMock(EventStore.class);
  }

  @Test
  public void passEventToStore() {
    ChangeEvent eventMock = easyMock.createNiceMock(ChangeEvent.class);
    easyMock.resetAll();
    storeMock.storeEvent(eventMock);
    expectLastCall().once();
    easyMock.replayAll();
    poolMock = new PoolMock(1);
    listener = new EventHandler(storeMock, poolMock);
    listener.onEvent(eventMock);
    easyMock.verifyAll();
  }

  @Test
  public void nonProjectEvent() {
    poolMock = easyMock.createMock(ScheduledThreadPoolExecutor.class);
    Event eventMock = easyMock.createMock(Event.class);
    easyMock.replayAll();
    listener = new EventHandler(storeMock, poolMock);
    listener.onEvent(eventMock);
    easyMock.verifyAll();
  }

  class PoolMock extends ScheduledThreadPoolExecutor {
    PoolMock(int corePoolSize) {
      super(corePoolSize);
    }
    @Override
    public void execute(Runnable command) {
      assertThat(command.toString()).isEqualTo("(Events-log) Insert Event");
      command.run();
    }
  }
}
