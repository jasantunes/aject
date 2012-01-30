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
import aject.protocol.MessageSpecification;

/**
 * This class implements a state, with its messages and transitional messages.
 */
public class State implements Serializable {
  public static final long serialVersionUID = ProtocolSpecification.serialVersionUID;

  private ArrayList<MessageSpecification> _messages = new ArrayList<MessageSpecification>();
  private String _name;

  public State(String name) {
    _name = name;
  }

  /**
   * SETS and GETS
   */
  public void add(MessageSpecification message) {
    _messages.add(message);
  }

  public ArrayList<MessageSpecification> getMessages() {
    return _messages;
  }

  public ArrayList<TransitionMessage> getTransitions() {
    ArrayList<TransitionMessage> transitions = new ArrayList<TransitionMessage>();
    for (MessageSpecification m : _messages)
      transitions.addAll(m.getTransitions());
    return transitions;
  }

  public String getName() {
    return _name;
  }

  public void setName(String name) {
    _name = name;
  }

  @Override
  public String toString() {
    return _name;
  }

}
