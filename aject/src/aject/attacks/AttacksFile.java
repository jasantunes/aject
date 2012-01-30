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

import java.io.IOException;
import aject.file.FileInterface;

public class AttacksFile {

  private FileInterface _file;
  private String _proto_name;
  private String _test_name;
  private byte[] _ping;

  private int _total_attacks;

  /**
   * Load from file
   */
  public AttacksFile(FileInterface file) throws IOException {
    _file = file;
    _file.open();
    _total_attacks = _file.readInt();
    _proto_name = _file.readString();
    _test_name = _file.readString();
    _ping = _file.readBytes();
  }

  public Attack getNextAttack() {
    try {
      return _file.readAttack();
    } catch (Exception e) {
      return null;
    }
  }

  public String getProtocolName() {
    return _proto_name;
  }

  public String getTestName() {
    return _test_name;
  }

  public int getTotalAttacks() {
    return _total_attacks;
  }

  /***************************************************************************
   * MERGE
   **************************************************************************/

  private boolean _is_in(int value, int[] set) {
    for (int i : set)
      if (value == i)
        return true;
    return false;
  }

  private static String _merge_strings(String A, String B) {
    if (A != null && B != null) {
      if (A.equals(B))
        return A;
      else {
        return A + "|" + B;
      }
    }

    else if (A != null)
      return A;
    else
      return B;
  }

  public void merge(AttacksFile other) throws IOException {
    int[] id_set = new int[this._total_attacks];
    int i = 0;
    _file.seek(4); // total attacks
    _file.writeString(_merge_strings(this._proto_name, other._proto_name));
    _file.writeString(_merge_strings(this._test_name, other._test_name));
    _file.writeBytes((this._ping != null) ? this._ping : other._ping);

    for (Attack a = this.getNextAttack(); a != null; a = getNextAttack())
      id_set[i++] = a.getAttackId();

    for (Attack a = other.getNextAttack(); a != null; a = other.getNextAttack()) {
      if (!this._test_name.equals(other._test_name) && !_is_in(a.getAttackId(), id_set)) {
        _file.writeAttack(a);
        _total_attacks++;
      }
    }
    long pos = _file.getPos();
    _file.seek(0);
    _file.writeInt(_total_attacks);
    _file.seek(pos);
  }

}
