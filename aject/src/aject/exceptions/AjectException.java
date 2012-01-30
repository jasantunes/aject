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

import aject.protocol.ProtocolSpecification;

public abstract class AjectException extends Exception {
  public static final long serialVersionUID = ProtocolSpecification.serialVersionUID;
  protected int _type;
  protected String _message;

  public AjectException(int type, Exception e) {
    _message = e.getMessage();
    _type = type;

  }

  public AjectException(int type) {
    _message = new String();
    _type = type;
  }

  public void setMessage(String message) {
    _message = message;
  }

  @Override
  public String getMessage() {
    return _message;
  }

  @Override
  public String toString() {
    return _message;
  }

}
