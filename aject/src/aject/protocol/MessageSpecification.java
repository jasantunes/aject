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
import java.util.*;

public class MessageSpecification extends Element implements Iterable<FieldSpecification>,
    Serializable {
  public static final long serialVersionUID = ProtocolSpecification.serialVersionUID;

  protected ArrayList<FieldSpecification> _fields;
  protected ArrayList<TransitionMessage> _transitions;

  public Iterator<FieldSpecification> iterator() {
    return _fields.iterator();
  }

  // //////////////////////////////////////////////////////////
  // CONSTRUCTORS //
  // //////////////////////////////////////////////////////////
  public MessageSpecification(String name, Bits initial_delimiter, Bits final_delimiter) {
    super(name, initial_delimiter, final_delimiter);
    _fields = new ArrayList<FieldSpecification>();
    _transitions = new ArrayList<TransitionMessage>();
  }

  public MessageSpecification() {
    this("", new Bits(), new Bits());
  }

  @Override
  public MessageSpecification clone() {
    MessageSpecification m = new MessageSpecification(_name, _initial_delimiter, _final_delimiter);
    for (FieldSpecification f : _fields)
      m._fields.add(f);
    // m._transitions.addAll(_transitions);
    for (TransitionMessage t : _transitions)
      m._transitions.add(t);
    return m;
  }

  // //////////////////////////////////////////////////////////
  // GETS / SETS //
  // //////////////////////////////////////////////////////////

  @Override
  public String toString() {
    return _name;
  }

  public ArrayList<FieldSpecification> getFields() {
    return _fields;
  }

  public ArrayList<TransitionMessage> getTransitions() {
    return _transitions;
  }

  // //////////////////////////////////////////////////////////
  // METHODS //
  // //////////////////////////////////////////////////////////

  // public FieldSpecification getField(int index) { return
  // _fields.remove(index); }

  /*
   * Removes the ending delimiter from the last FieldSpec.
   */
  // public void trim() {
  // _fields.get(_fields.size()-1).setFinalDelimiter(null);
  // }

}
