// Copyright (C) 2014 Ericsson
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

import java.util.Map;

public interface QueryMaker {

  /**
   * Forms a String query based on the given parameters.
   *
   * @param params parameters which are used to form the query
   * @return a query based on the given parameters. The query should conform to
   *         the format required by the database.
   * @throws MalformedQueryException if the given parameters do conform to
   *         requirements
   */
  String formQueryFromRequestParameters(Map<String, String> params)
      throws MalformedQueryException;

  /**
   * Get the query designated as the default when no parameters are given.
   *
   * @return the query which is formed when no parameters are given.
   */
  String getDefaultQuery();
}
