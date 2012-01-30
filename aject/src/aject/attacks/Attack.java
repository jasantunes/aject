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
package aject.attacks;

import java.util.ArrayList;
import aject.protocol.Bits;

public class Attack implements Comparable<Attack> {
  private final static long serialVersionUID = aject.protocol.ProtocolSpecification.serialVersionUID;

  protected int _attack_id;
  protected byte[] _attack;
  protected ArrayList<byte[]> _transitions; // protocol's state

  public int compareTo(Attack attack) {
    return _attack_id - attack.getAttackId();
  }

  public Attack(int attack_id, byte[] data, ArrayList<byte[]> transitions) {
    _attack_id = attack_id;
    _attack = data;
    _transitions = transitions;
  }

  @Override
  public Attack clone() {
    return new Attack(_attack_id, _attack, _transitions);
  }

  public int getAttackId() {
    return _attack_id;
  }

  public byte[] getMessage() {
    return _attack;
  }

  public ArrayList<byte[]> getTransitionMessages() {
    return _transitions;
  }

  // //////////////////////////////////////////////////////////////////////////
  // SELECTED RESOURCES
  // //////////////////////////////////////////////////////////////////////////
  // public boolean getSelectedResource(int resource) { return
  // _selected_resources[resource]; }
  // public void setSelectedResource(int resource, boolean is_selected) {
  // _selected_resources[resource] = is_selected;
  // }
  //
  // public boolean[] getSelectedResources() {
  // return _selected_resources;
  // }
  //
  // public void setSelectedResources(int total_resources) {
  // _selected_resources = new boolean[total_resources];
  // for (int i = 0; i < _selected_resources.length; i++) {
  // _selected_resources[i] = false;
  // }
  //
  // }
  //
  // public void setSelectedResources(boolean[] selected_resources) {
  // _selected_resources = selected_resources;
  // }
  //
  // public void appendBytes(Bits bytes) {
  // _attack.getFields().add(bytes);
  // }
  //
  // public void prependBytes(Bits bytes) {
  // _attack.getFields().add(0, bytes);
  // }

  public String toString(Bits.Type type) {
    return "[" + _attack_id + "]\t{" + Bits.byteToString(_attack, type);
  }
}
