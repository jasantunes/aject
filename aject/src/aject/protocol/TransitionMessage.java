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
package aject.protocol;

import java.io.Serializable;
import java.util.ArrayList;

public class TransitionMessage extends Message implements Serializable {
  public static final long serialVersionUID = aject.protocol.ProtocolSpecification.serialVersionUID;
  private State _dest;

  public TransitionMessage(MessageSpecification specification, ArrayList<Bits> data, State dest) {
    super(specification);
    _payload = data;
    _dest = dest;
  }

  public TransitionMessage(MessageSpecification specification, State dest) {
    this(specification, new ArrayList<Bits>(specification._fields.size()), dest);
    for (int i = 0; i < specification._fields.size(); i++)
      _payload.add(i, new Bits());
  }

  public State getState() {
    return _dest;
  }

}
