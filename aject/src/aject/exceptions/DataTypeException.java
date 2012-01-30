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
package aject.exceptions;

import aject.protocol.ProtocolSpecification;

public class DataTypeException extends AjectException {
  public static final long serialVersionUID = ProtocolSpecification.serialVersionUID;
  public static final int UNKNOWN = 0;
  public static final int OVERFLOW = 1;
  public static final int OVERLAP = 2;
  public static final int INEXISTANT = 3;
  public static final int WRONG_TYPE = 4;

  public DataTypeException(String verbose) {
    this(UNKNOWN, verbose);
  }

  public DataTypeException(int type, String verbose) {
    super(type);
    _type = type;
    setMessage(getMessageType(type) + ": " + verbose);
  }

  public DataTypeException(int type) {
    super(type);
    _type = type;
    setMessage(getMessageType(type));
  }

  private static String getMessageType(int type) {
    switch (type) {
      case OVERFLOW:
        return "datatype overflow";
      case OVERLAP:
        return "datatype overlap";
      case INEXISTANT:
        return "value inexistant";
      case WRONG_TYPE:
        return "wrong type";
      default:
        return "unkwnown error";
    }
  }

}
