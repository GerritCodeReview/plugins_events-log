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

import com.google.gerrit.server.events.ProjectEvent;
import java.util.List;

/** A store for change events to query. */
public interface EventStore {
  /**
   * Stores the given event.
   *
   * @param event the event to store
   */
  void storeEvent(ProjectEvent event);

  /**
   * Returns events from the store based on the given query.
   *
   * @param query the query used to get events
   * @return a list of events in String format.
   * @throws EventsLogException if the given query can't be processed
   */
  List<String> queryChangeEvents(String query) throws EventsLogException;
}
