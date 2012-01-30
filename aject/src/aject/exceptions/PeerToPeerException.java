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

package aject.exceptions;

public class PeerToPeerException extends Exception {
  private static final long serialVersionUID = 1;

  public enum Types {
    UNKNOWN_HOST, HOST_UNREACHABLE, SOCKET_TIMEOUT, SOCKET_OPEN, SOCKET_CREATION, SOCKET_OPTION, SOCKET_INIT, SOCKET_RECEIVE, SOCKET_SEND, SOCKET_CLOSE
  };

  private Types _type;

  public PeerToPeerException(Types type, String verbose) {
    super(getMessage(type) + ": " + verbose);
    _type = type;
  }

  public PeerToPeerException(Types type) {
    super(getMessage(type));
    _type = type;
  }

  private static String getMessage(Types type) {
    switch (type) {
      case UNKNOWN_HOST:
        return "Unknown hostname or address";
      case HOST_UNREACHABLE:
        return "Host unreachable";
      case SOCKET_TIMEOUT:
        return "Socket timeout";
      case SOCKET_OPEN:
        return "Socket open error";
      case SOCKET_CREATION:
        return "Socket creation error";
      case SOCKET_OPTION:
        return "Socket option error";
      case SOCKET_INIT:
        return "Socket initialization error";
      case SOCKET_RECEIVE:
        return "Socket receive error";
      case SOCKET_SEND:
        return "Socket send error";
      default:
        return "UNKNOWN ERROR!!";
    }
  }

  public Types Type() {
    return _type;
  }
}
