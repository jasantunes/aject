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

public class Message extends Element implements Serializable {

  public static final long serialVersionUID = ProtocolSpecification.serialVersionUID;

  protected ArrayList<Bits> _payload;
  protected MessageSpecification _specification;

  public Message() {
    _payload = new ArrayList<Bits>();
    _specification = null;
  }

  public Message(MessageSpecification spec) {
    _specification = spec;
    _payload = new ArrayList<Bits>(_specification._fields.size());
    // for (FieldSpecification f: _specification._fields)
    for (int i = 0; i < _specification._fields.size(); i++)
      _payload.add(new Bits());
  }

  public MessageSpecification getSpecification() {
    return _specification;
  }

  public ArrayList<Bits> getPayload() {
    return _payload;
  }

  public void setPayload(ArrayList<Bits> data) {
    _payload = data;
  }

  public Bits toBits() {
    Bits data = new Bits();
    data.append(_initial_delimiter);

    for (int i = 0; i < _payload.size(); i++) {
      Bits b = _payload.get(i);
      @SuppressWarnings("rawtypes")
      FieldSpecification f = _specification._fields.get(i);
      data.append(f._initial_delimiter);
      data.append(b);
      data.append(f._final_delimiter);
    }
    data.append(_final_delimiter);
    return data;
  }

}
