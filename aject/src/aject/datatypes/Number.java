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
package aject.datatypes;

import java.io.Serializable;
import java.util.BitSet;
import aject.exceptions.DataTypeException;
import aject.protocol.*;

public class Number extends Bits implements Serializable {
  protected boolean _big_endian = true;
  protected boolean _signed = true;
  protected long _value;

  public static final long serialVersionUID = ProtocolSpecification.serialVersionUID;

  /**
   * Creates a number of n_bits.
   * 
   * @param value decimal representation of the number.
   * @param nbits bits for representing the number.
   * @param big_endian bit ordering.
   * @param signed number with signed bit.
   * @throws DataTypeException
   */
  public Number(long value, int nbits, boolean big_endian, boolean signed) throws DataTypeException {
    _nbits = nbits;
    _big_endian = big_endian;
    _signed = signed;

    if (!_check_value(value))
      throw new DataTypeException(DataTypeException.OVERFLOW);
    else
      _value = value;

    /* update _data */
    _data = toBytes();
  }

  public Number(long value, int nbits) throws DataTypeException {
    this(value, nbits, true, true);
  }

  public Number(BitSet bitset, int nbits, boolean big_endian, boolean signed) {
    _nbits = nbits;
    _big_endian = big_endian;
    _signed = signed;
    _value = 0;
    int first_bit = (_signed) ? 1 : 0;

    for (int i = bitset.nextSetBit(first_bit); i >= 0 && i < _nbits; i = bitset.nextSetBit(i + 1))
      _value += Math.pow(2, (_big_endian) ? _nbits - i - 1 : i - 1);

    if (_signed && bitset.get(0))
      _value *= -1;

    /* update _data */
    _data = toBytes();
  }

  public Number(BitSet bitset, int nbits) {
    this(bitset, nbits, true, true);
  }

  protected boolean _check_value(long value) {
    if (value > maxValue(_nbits, _signed) || value < minValue(_nbits, _signed)
        || (!_signed && value < 0))
      return false;
    else
      return true;
  }

  private BitSet _to_BitSet() {
    BitSet b = new BitSet(_nbits);
    long value = _value;
    // check for negative number
    boolean is_neg = (value < 0);
    if (is_neg) {
      b.set(0);
      value = -value;
    }

    /* set the bits */
    for (int i = _nbits - 1; i >= 0 && value > 0; i--) {
      long bit_value = (long)Math.pow(2, i);
      int bit_pos = (_big_endian) ? _nbits - 1 - i : i;

      if (value >= bit_value) {
        value -= bit_value;
        b.set(bit_pos);
      }
    }

    return b;
  }

  public boolean isBigEndian() {
    return _big_endian;
  }

  public boolean isSigned() {
    return _signed;
  }

  public long getValue() {
    return _value;
  }

  protected byte[] toBytes() {
    return toByteArray(_to_BitSet(), _nbits);
  }

  @Override
  public String toString() {
    return Long.toString(_value);
  }

  // unsigned
  public static long maxValue(int nbits, boolean is_signed) {
    if (is_signed)
      return (long)(Math.pow(2, nbits - 1) - 1);
    else
      return (long)(Math.pow(2, nbits) - 1);
  }

  public static long minValue(int nbits, boolean is_signed) {
    if (is_signed)
      return -(long)(Math.pow(2, nbits - 1) - 1);
    else
      return 0;
  }

}
