// Copyright (C) 2014 Ericsson
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

import static org.easymock.EasyMock.expectLastCall;

import com.google.gerrit.common.EventListener;
import com.google.gerrit.server.events.ChangeEvent;

import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

public class EventHandlerTest {
  private EasyMockSupport easyMock;
  private EventStore storeMock;
  private EventListener listener;

  @Before
  public void setUp() {
    easyMock = new EasyMockSupport();
    storeMock = easyMock.createMock(EventStore.class);
    listener = new EventHandler(storeMock);
  }

  @Test
  public void passEventToStore() {
    ChangeEvent eventMock = easyMock.createNiceMock(ChangeEvent.class);
    easyMock.resetAll();
    storeMock.storeEvent(eventMock);
    expectLastCall().once();
    easyMock.replayAll();
    listener.onEvent(eventMock);
    easyMock.verifyAll();
  }
}
