// Copyright (C) 2015 Ericsson
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

public class EventsLogException extends Exception {
  private static final long serialVersionUID = 1L;

  public static final String MSG = "Error in events-log plugin";

  /**
   * Constructs a <code>EventsLogException</code> object with the default
   * message.
   */
  public EventsLogException() {
    super(MSG);
  }

  /**
   * Constructs an <code>EventsLogException</code> object with the default
   * message and a given <code>cause</code>.
   *
   * @param cause the underlying reason for this
   *        <code>EventsLogException</code>
   */
  public EventsLogException(Throwable cause) {
    super(MSG, cause);
  }

  /**
   * Constructs an <code>EventsLogException</code> object with a given
   * <code>message</code>.
   *
   * @param message a description of the exception
   */
  public EventsLogException(String message) {
    super(message);
  }

  /**
   * Constructs an <code>EventsLogException</code> object with a given
   * <code>message</code> and a given <code>cause</code>.
   *
   * @param message a description of the exception
   * @param cause the underlying reason for this
   *        <code>EventsLogException</code>
   */
  public EventsLogException(String message, Throwable cause) {
    super(message, cause);
  }
}
