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

import com.google.common.flogger.FluentLogger;
import com.google.gerrit.extensions.restapi.Url;
import com.google.gerrit.server.CurrentUser;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Singleton
class EventsRestApiServlet extends HttpServlet {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();
  private static final long serialVersionUID = 1L;

  private final EventStore store;
  private final QueryMaker queryMaker;
  private final Provider<CurrentUser> userProvider;

  @Inject
  EventsRestApiServlet(
      EventStore store, QueryMaker queryMaker, Provider<CurrentUser> userProvider) {
    this.store = store;
    this.queryMaker = queryMaker;
    this.userProvider = userProvider;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse rsp)
      throws IOException, ServletException {
    if (!userProvider.get().isIdentifiedUser()) {
      rsp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }
    rsp.setContentType("text/html");
    Map<String, String> params = req.getQueryString() != null ? getParameters(req) : null;

    try (Writer out = rsp.getWriter()) {
      String query = queryMaker.formQueryFromRequestParameters(params);
      for (String event : store.queryChangeEvents(query)) {
        out.write(event + "\n");
      }
    } catch (MalformedQueryException e) {
      log.atSevere().withCause(e).log("Bad Request");
      rsp.sendError(HttpServletResponse.SC_BAD_REQUEST);
    } catch (ServiceUnavailableException e) {
      log.atSevere().withCause(e).log("Service Unavailable");
      rsp.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    } catch (EventsLogException e) {
      log.atSevere().withCause(e).log("Could not query from request parameters");
    }
  }

  private static Map<String, String> getParameters(HttpServletRequest req) {
    Map<String, String> params = new HashMap<>();
    for (final String pair : req.getQueryString().split("[&;]")) {
      int eq = pair.indexOf('=');
      if (0 < eq) {
        String name = pair.substring(0, eq);
        String value = pair.substring(eq + 1);

        name = Url.decode(name);
        value = Url.decode(value);
        params.put(name, value);
      }
    }
    return params;
  }
}
