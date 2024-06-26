// Copyright (C) 2014 The Android Open Source Project
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.google.gerrit.server.events.ChangeEvent;
import com.google.gerrit.server.events.Event;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EventHandlerTest {
  @Mock private EventStore storeMock;
  private EventHandler eventHandler;

  @Before
  public void setUp() {
    ScheduledExecutorService poolMock = new PoolMock();
    eventHandler = new EventHandler(storeMock, poolMock);
  }

  @Test
  public void passEventToStore() {
    ChangeEvent eventMock = mock(ChangeEvent.class);
    eventHandler.onEvent(eventMock);
    verify(storeMock).storeEvent(eventMock);
  }

  @Test
  public void nonProjectEvent() {
    Event eventMock = mock(Event.class);
    eventHandler.onEvent(eventMock);
    verifyNoInteractions(storeMock);
  }

  class PoolMock extends ScheduledThreadPoolExecutor {
    PoolMock() {
      super(1);
    }

    @Override
    public void execute(Runnable command) {
      assertThat(command.toString()).isEqualTo("(Events-log) Insert Event");
      command.run();
    }
  }
}
