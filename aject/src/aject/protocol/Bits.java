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
import java.math.BigInteger;
import java.util.BitSet;
import java.util.Collection;
import utils.Utils;

public class Bits implements Serializable {

  public static final long serialVersionUID = ProtocolSpecification.serialVersionUID;

  public enum Type {
    TXT, DEC, HEX, BIN
  };

  protected byte[] _data;
  protected int _nbits;

  public Bits(byte[] data, int nbits) {
    _data = data;
    _nbits = nbits;
  }

  public Bits(BitSet bitset, int nbits) {
    _nbits = nbits;
    _data = toByteArray(bitset, nbits);
  }

  public Bits(byte[] data) {
    this(data, 8 * data.length);
    _data = data;
  }

  public Bits() {
    this(new byte[0], 0);
  }

  public byte[] getBytes() {
    return _data;
  }

  public int getBitLength() {
    return _nbits;
  }

  /**
   * Returns a bitset containing the values in bytes. The byte-ordering of bytes
   * must be big-endian which means the most significant bit is in element 0.
   */
  protected BitSet toBitSet() { /* must be little endian */
    BitSet bits = new BitSet(_nbits);
    for (int i = 0; i < _nbits; i++) {
      if ((_data[i / 8] & (1 << (8 - (i % 8) - 1))) != 0)
        bits.set(i);
    }
    return bits;
  }

  public void append(Bits bytes) {
    if (bytes != null && bytes.getBitLength() != 0) {
      Bits concatenated = concat(this, bytes);
      _data = concatenated.getBytes();
      _nbits = concatenated.getBitLength();
    }
  }

  public static Bits concat(Collection<Bits> set) {
    Bits bytes = new Bits();
    for (Bits b : set) {
      bytes.append(b);
    }
    return bytes;
  }

  public boolean equals(Bits other) {
    if (_nbits != other._nbits)
      return false;

    int total = Bits._min_bytes(_nbits);
    for (int i = 0; i < total; i++) {
      if (_data[i] != other._data[i])
        return false;
    }
    return true;

  }

  /***********************************************************************
   * PRIVATE
   ***********************************************************************/
  // byte-ordering independent
  private static BitSet concatBitSets(BitSet setA, int nbitsA, BitSet setB, int nbitsB) {
    BitSet merged = new BitSet(nbitsA + nbitsB);

    /* copy setA */
    for (int i = 0; i < nbitsA; i++) {
      if (setA.get(i))
        merged.set(i);
    }

    /* copy setA */
    for (int i = 0; i < nbitsB; i++) {
      if (setB.get(i))
        merged.set(nbitsA + i);
    }

    return merged;
  }

  /**
   * Returns the number of bytes needed to accomodate nbits.
   */
  private static int _min_bytes(int nbits) {
    int size = nbits / 8;
    if (nbits % 8 > 0)
      size++;
    return size;
  }

  // byte-ordering: big-endian (toByteArray)
  public static Bits concat(Bits A, Bits B) {
    byte[] merged = null;
    int total_bits;

    /*
     * perform regular byte array concatenation if there are not partial filled
     * bytes
     */
    if (A.getBitLength() % 8 == 0 && B.getBitLength() % 8 == 0) {

      total_bits = A.getBitLength() + B.getBitLength();
      merged = new byte[A.getBytes().length + B.getBytes().length];
      byte[] bytes;
      int last_byte = 0;

      /* copy A */
      bytes = A.getBytes();
      for (int i = 0; i < bytes.length; i++) {
        merged[last_byte++] = bytes[i];
      }

      /* copy B */
      bytes = B.getBytes();
      for (int i = 0; i < bytes.length; i++) {
        merged[last_byte++] = bytes[i];
      }

    } /* bytes are "incomplete" */else {
      /* convert byte arrays to BitSets */
      BitSet bit_set_A = A.toBitSet();
      BitSet bit_set_B = B.toBitSet();

      /* concat BitSets and convert it to a byte array */
      total_bits = A.getBitLength() + B.getBitLength();
      BitSet merges_bit_sets = concatBitSets(bit_set_A, A.getBitLength(), bit_set_B,
          B.getBitLength());
      merged = toByteArray(merges_bit_sets, total_bits);
    }

    return new Bits(merged, total_bits);
  }

  /***********************************************************************
   * STRING (print functions)
   ***********************************************************************/
  @Override
  public String toString() {
    return byteToString(_data, Type.TXT);
  }

  public String toString(Type type) {
    return byteToString(_data, type);
  }

  public static String byteToString(byte[] data, Type type) {
    if (data == null || data.length == 0)
      return "";
    switch (type) {
      case TXT:
        return _byte_to_text(data);
      case DEC:
        return new BigInteger(data).toString(10);
      case HEX:
        return _byte_to_hex(data);
      case BIN:
        return _byte_to_bin(data);
      default:
        return _byte_to_hex(data);
    }
  }

  private static String _byte_to_text(byte[] data) {
    StringBuffer s = new StringBuffer();
    for (byte b : data)
      s.append(byteToString(b));
    return new String(s);
  }

  public static String byteToString(byte b) {
    switch (b) {
      case '\r':
        return "\\r";
      case '\n':
        return "\\n";
      case '\t':
        return "\\t";
      case '\f':
        return "\\f";
      default:
        return (b < 32 || b > 126) ? "#" + b : "" + (char)b;
    }
  }

  private static String _byte_to_bin(byte[] msg) {
    StringBuffer out = new StringBuffer(msg.length * 2);

    for (int i = 0; i < msg.length; i++) {
      for (int bit = 8 - 1; bit >= 0; bit--) {
        if ((msg[i] & (1 << bit)) != 0)
          out.append('1');
        else
          out.append('0');
        if (bit % 8 == 0 && i != msg.length - 1)
          out.append(' ');
      }
    }
    return new String(out);
  }

  /**
   * Convert a byte[] array to readable string format. This makes the "hex"
   * readable!
   * 
   * @return result String buffer in String format
   * @param in byte[] buffer to convert to string format
   */
  private static String _byte_to_hex(byte[] msg) {
    char[] pseudo = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };
    StringBuffer out = new StringBuffer(msg.length * 2);

    for (int i = 0, ch = 0; i < msg.length; i++) {
      ch = (msg[i] & 0xF0) >>> 4 & 0x0F;
      out.append(pseudo[ch]);

      ch = msg[i] & 0x0F;
      out.append(pseudo[ch]);

      if (i < msg.length - 1)
        out.append(' ');
    }
    return new String(out);
  }

  public static boolean isTextual(byte[] data) {
    for (byte b : data) {
      if (b < 32 || b > 126)
        return false;
    }
    return true;
  }

  /***********************************************************************
   * BitSet
   ***********************************************************************/
  /**
   * Converts a BitSet of nbits to a byte[].
   */
  public static byte[] toByteArray(BitSet bits, int nbits) {
    byte[] bytes = new byte[_min_bytes(nbits)];
    /* bitset order (left to right) is implicitly little endian */
    /*
     * Java Vm is big endian, so we must convert our bits to a big endian byte
     * array (most significant bit first)
     */
    for (int i = 0; i < nbits; i++) {
      if (bits.get(i))
        bytes[i / 8] |= 1 << (8 - (i % 8) - 1);
    }

    return bytes;
  }

  // // OK
  // private static String BitSetToString(BitSet bits, int nbits) {
  // StringBuffer s = new StringBuffer(nbits + nbits/8);
  // for (int i=0; i<nbits; i++) {
  // if (bits.get(i)) s.append('1');
  // else s.append('0');
  // if (i%8 == 0 && i!=0) s.append(' ');
  // }
  // return new String(s);
  // }
  public static byte[] crap(int length, int range_min, int range_max) {
    byte[] crap = new byte[length];
    for (int i = 0; i < length; i++) {
      crap[i] = (byte)Utils.random(range_min, range_max + 1);
    }
    return crap;
  }

  public static byte[] crap(int length, boolean printable_only) {
    if (printable_only)
      return crap(length, 32, 126);
    else
      return crap(length, 0, 256);
  }

  public static byte[] crap(int length) {
    return crap(length, false);
  }

  // public void readObject(FileWrapper file) throws IOException {
  // _type = Bits.intToType(file.readInt());
  // _data = file.readBytes();
  // _nbits = file.readInt();
  // }
  //
  // public void writeObject(FileWrapper file) throws IOException {
  // file.writeInt(0);
  // file.writeInt(Bits.typeToInt(_type));
  // file.writeBytes(_data);
  // file.writeInt(_nbits);
  // }
  //
  @Override
  public Bits clone() {
    return new Bits(_data.clone(), _nbits);
  }
}
