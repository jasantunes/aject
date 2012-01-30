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
import aject.protocol.ProtocolSpecification;
import utils.Utils;

public class NumberInterval extends Number implements Serializable {
  public static final long serialVersionUID = ProtocolSpecification.serialVersionUID;
  protected long _value_min;
  protected long _value_max;

  public long getMin() {
    return _value_min;
  }

  public long getMax() {
    return _value_max;
  }

  public NumberInterval(long value, int nbits, boolean big_endian, boolean signed)
      throws DataTypeException {
    super(value, nbits, big_endian, signed);
    _value_min = _value_max = _value;
  }

  public NumberInterval(long min, long max, int nbits, boolean big_endian, boolean signed)
      throws DataTypeException {
    super(min, nbits, big_endian, signed);
    _value_min = min;
    _value_max = max;
    if (!_check_value(_value_max))
      throw new DataTypeException(DataTypeException.OVERFLOW);
  }

  public NumberInterval(long value, int nbits) throws DataTypeException {
    this(value, value, nbits, true, true);
  }

  public NumberInterval(long min, long max, int nbits) throws DataTypeException {
    this(min, max, nbits, true, true);
  }

  public NumberInterval(BitSet bitset, int nbits, boolean big_endian, boolean signed)
      throws DataTypeException {
    super(bitset, nbits, big_endian, signed);
    throw new DataTypeException(DataTypeException.INEXISTANT);
  }

  public NumberInterval(BitSet bitset, int nbits) throws DataTypeException {
    this(bitset, nbits, true, true);
  }

  public int getTotalElements() {
    return (int)(_value_max - _value_min) + 1;
  }

  public NumberInterval get(int index) throws DataTypeException {
    if (_value_min + index > _value_max)
      throw new DataTypeException(DataTypeException.OVERFLOW, Integer.toString(index));
    _value = _value_min + index;
    /* update _data */
    _data = toBytes();
    return this;
  }

  public NumberInterval[] remove_index(int index) throws DataTypeException {
    NumberInterval[] new_intervals = new NumberInterval[2];
    if (_value_min + index > _value_max)
      throw new DataTypeException(DataTypeException.OVERFLOW, Integer.toString(index));

    /* new LEFT interval */
    if (index >= 1)
      new_intervals[0] = new NumberInterval(_value_min, _value_min + index - 1, _nbits,
          _big_endian, _signed);

    /* new RIGHT interval */
    if (index < _value_max - _value_min)
      new_intervals[1] = new NumberInterval(_value_min + index + 1, _value_max, _nbits,
          _big_endian, _signed);

    return new_intervals;
  }

  @Override
  public String toString() {
    if (_value_min != _value || _value_min == _value_max)
      return super.toString();
    else
      return "[" + _value_min + "-" + _value_max + "]";
  }

  public static void main(String[] args) {
    try {

      NumberInterval interval = new NumberInterval(3, 8, 5);
      NumberInterval[] intervals = interval.remove_index(1);
      System.out.println(intervals[0]);
      System.out.println(intervals[1]);

    } catch (DataTypeException e) {
      Utils.handleException(e);
    }

  }

}
