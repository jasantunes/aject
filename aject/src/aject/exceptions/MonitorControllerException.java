/*******************************************************************************
 * Copyright 2011 João Antunes
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
/* PeerToPeerException.java */

// NÃO ESTÁ A SER UTILIZADA

package aject.exceptions;

public class MonitorControllerException extends Exception {
  private static final long serialVersionUID = 1;

  public enum Types {
    UNKNOWN_MESSAGE, CONTROLLER_UNREACHABLE
  };

  private Types _type;

  public MonitorControllerException(String verbose) {
    this(Types.CONTROLLER_UNREACHABLE, verbose);
  }

  public MonitorControllerException(Types type, String verbose) {
    super(getMessage(type) + ": " + verbose);
    _type = type;
  }

  public MonitorControllerException(Types type) {
    super(getMessage(type));
    _type = type;
  }

  private static String getMessage(Types type) {
    switch (type) {
      case UNKNOWN_MESSAGE:
        return "Unknown or wrong message received";
      case CONTROLLER_UNREACHABLE:
        return "Monitor controller unreachable";
      default:
        return "UNKNOWN ERROR!!";
    }
  }

  public Types Type() {
    return _type;
  }

}
