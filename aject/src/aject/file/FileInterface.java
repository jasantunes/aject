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
package aject.file;

import java.io.IOException;
import java.util.ArrayList;
import aject.attacks.Attack;
import utils.Convert;

public abstract class FileInterface {

  protected String _filename;

  public abstract void open() throws IOException;

  public abstract void close() throws IOException;

  public abstract void clear() throws IOException;

  // public abstract void truncate() throws IOException;

  public abstract long getPos() throws IOException;

  public abstract void seek(long pos) throws IOException;

  // protected abstract void seekFromEnd(long pos) throws IOException;

  protected abstract void write(byte[] src, int offset, int length) throws IOException;

  protected abstract void read(byte[] dest, int offset, int length) throws IOException;

  public int readInt() throws IOException {
    byte[] value = new byte[4];
    read(value, 0, 4);
    return Convert.toInt32(value);
  }

  public void writeInt(int integer) throws IOException {
    write(Convert.toByte(integer, 4), 0, 4);
  }

  private long _get_pos_attacks() throws IOException {
    long last_pos = getPos();
    seek(0 + 4);
    readString(); // Protocol
    readString(); // Test
    readBytes(); // Ping
    long ret = getPos();
    seek(last_pos);
    return ret;
  }

  public Attack readAttack() throws IOException {
    // if current position is BEFORE attacks, go to attacks
    if (getPos() < _get_pos_attacks())
      seek(_get_pos_attacks());
    int attack_id = readInt(); // attack id
    byte[] data = readBytes(); // data
    ArrayList<byte[]> transition_msgs = _read_vector_bytes();// protocol's state
    return new Attack(attack_id, data, transition_msgs);
  }

  public void writeAttack(Attack attack) throws IOException {
    // if current position is BEFORE attacks, go to attacks
    if (getPos() < _get_pos_attacks())
      seek(_get_pos_attacks());
    writeInt(attack.getAttackId()); // attack id
    writeBytes(attack.getMessage()); // data
    _write_vector_bytes(attack.getTransitionMessages()); // protocol's state
  }

  public String readString() throws IOException {
    byte[] bytes = readBytes();
    if (bytes != null)
      return new String(bytes);
    else
      return null;
  }

  public void writeString(String string) throws IOException {
    writeBytes((string != null) ? string.getBytes() : null);
  }

  public byte[] readBytes() throws IOException {
    int size = readInt();
    byte[] bytes = new byte[size];
    if (size > 0)
      read(bytes, 0, size);

    return bytes;
  }

  public void writeBytes(byte[] bytes) throws IOException {
    if (bytes == null)
      writeInt(0);
    else
      writeInt(bytes.length); // size
    write(bytes, 0, bytes.length); // byte[]
  }

  private ArrayList<byte[]> _read_vector_bytes() throws IOException {
    ArrayList<byte[]> vector = null;
    int size = readInt(); // total elements

    if (size > 0) {
      vector = new ArrayList<byte[]>(size);
      for (int i = 0; i < size; i++)
        vector.add(readBytes());
    } else
      vector = new ArrayList<byte[]>(0);

    return vector;
  }

  private void _write_vector_bytes(ArrayList<byte[]> vector) throws IOException {
    if (vector == null)
      writeInt(0); // total elements

    else {
      writeInt(vector.size()); // total elements
      for (byte[] bytes : vector)
        // byte[]
        writeBytes(bytes);
    }
  }
}
