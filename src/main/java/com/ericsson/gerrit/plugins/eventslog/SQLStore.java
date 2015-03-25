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

import java.io.IOException;
import java.net.ConnectException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gerrit.extensions.events.LifecycleListener;
import com.google.gerrit.reviewdb.client.Project;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.events.ProjectEvent;
import com.google.gerrit.server.project.NoSuchProjectException;
import com.google.gerrit.server.project.ProjectControl;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

@Singleton
public class SQLStore implements EventStore, LifecycleListener {
  private static final Logger log = LoggerFactory.getLogger(SQLStore.class);

  private final ProjectControl.GenericFactory projectControlFactory;
  private final Provider<CurrentUser> userProvider;
  private SQLClient sqlClient;
  private final int maxTries;
  private final int waitTime;

  @Inject
  SQLStore(ProjectControl.GenericFactory projectControlFactory,
      Provider<CurrentUser> userProvider,
      EventsLogConfig cfg,
      SQLClient sqlClient) {
    this.maxTries = cfg.getMaxTries();
    this.waitTime = cfg.getWaitTime();
    this.projectControlFactory = projectControlFactory;
    this.userProvider = userProvider;
    this.sqlClient = sqlClient;
  }

  @Override
  public void start() {
    try {
      sqlClient.createDBIfNotCreated();
    } catch (SQLException e) {
      throw new RuntimeException("Cannot start the database", e);
    }
    removeOldEvents();
  }

  @Override
  public void stop() {
    try {
      sqlClient.close();
    } catch (SQLException e) {
      throw new RuntimeException("Cannot close datasource ", e);
    }
  }

  @Override
  public List<String> queryChangeEvents(String query)
      throws MalformedQueryException {
    List<String> events = new ArrayList<>();
    Project.NameKey project = null;
    for (Entry<String, Collection<String>> entry : sqlClient.getEvents(query)
        .asMap().entrySet()) {
      try {
        project = new Project.NameKey(entry.getKey());
        if (projectControlFactory.controlFor(project, userProvider.get())
            .isVisible()) {
          events.addAll(entry.getValue());
        }
      } catch (NoSuchProjectException e) {
        log.warn("Database contains a non-existing project, " + project.get()
            + ", removing project from database", e);
        removeProjectEvents(project.get());
      } catch (IOException e) {
        log.warn("Cannot get project visibility info for " + project.get()
            + " from cache", e);
      }
    }
    return events;
  }

  @Override
  public void storeEvent(ProjectEvent event) {
    Project.NameKey projectName = event.getProjectNameKey();
    if (projectName == null) {
      return;
    }
    int failedConnections = 0;
    boolean done = false;
    while (!done) {
      done = true;
      try {
        sqlClient.storeEvent(event);
      } catch (SQLException e) {
        log.warn("Cannot store ChangeEvent for: " + projectName.get(), e);
        if (e.getCause() instanceof ConnectException
            || e.getMessage().contains("terminating connection")) {
          if (maxTries == 0) {
          } else if (failedConnections < maxTries - 1) {
            failedConnections++;
            done = false;
            log.info("Retrying store event");
            try {
              Thread.sleep(waitTime);
            } catch (InterruptedException e1) {
              continue;
            }
          } else {
            log.error("Failed to store event " + maxTries + " times");
          }
        }
      }
    }
  }

  private void removeOldEvents() {
    try {
      sqlClient.removeOldEvents();
    } catch (SQLException e) {
      log.warn("Cannot remove old entries from database", e);
    }
  }

  private void removeProjectEvents(String project) {
    try {
      sqlClient.removeProjectEvents(project);
    } catch (SQLException e) {
      log.warn("Cannot remove project " + project + " from database", e);
    }
  }
}
