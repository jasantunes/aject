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

/**
 * Class that implements a Protocol Specification
 */
public class ProtocolSpecification extends Element implements Serializable {

  public static final long serialVersionUID = 4;

  protected ArrayList<State> _states;
  protected Message _ping;

  public ProtocolSpecification(String name) {
    super(name, new Bits(), new Bits());
    _states = new ArrayList<State>();
    _ping = null;
  }

  public ProtocolSpecification() {
    this("");
  }

  public void setPingMessage(Message ping_message) {
    _ping = ping_message;
  }

  public Message getPingMessage() {
    return _ping;
  }

  public ArrayList<State> getStates() {
    return _states;
  }

  public byte[][] getPath(ProtocolSpecification protocol, State dest) {
    if (_states.get(0) == dest) {
      return new byte[0][];
    } // initial state
    ArrayList<TransitionMessage> path = _get_path(_states.get(0), dest, new ArrayList<State>());
    byte[][] msgs = new byte[path.size()][];
    for (int i = 0; i < path.size(); i++) {
      Bits t = new Bits();
      t.append(protocol.getInitialDelimiter());
      t.append(path.get(i).toBits());
      t.append(protocol.getFinalDelimiter());
      msgs[i] = t.getBytes();
    }
    return msgs;
  }

  private ArrayList<TransitionMessage> _get_path(State orig, State dest, ArrayList<State> searched) {
    if (_is_in(orig, searched)) {
      return null;
    } else {
      searched.add(orig);
    }

    ArrayList<TransitionMessage> path = new ArrayList<TransitionMessage>();

    // path is searched in a tree-fashion, expanding every node
    for (TransitionMessage tm : orig.getTransitions()) {
      if (tm.getState() == dest) {
        path.add(tm);
        return path;
      } else {
        // try to get a path through node 'tm',
        // and if there's any, add it and return it
        ArrayList<TransitionMessage> sub_path = _get_path(tm.getState(), dest, searched);
        if (sub_path != null) {
          path.add(tm);
          path.addAll(sub_path);
          return path;
        }
      }
    }
    return null;
  }

  private static boolean _is_in(State state, ArrayList<State> set) {
    for (State s : set) {
      if (s == state) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString() {
    return _name;
  }

  public boolean isValid(TransitionMessage transition) {
    return _is_in(transition.getState(), _states);
  }
}
