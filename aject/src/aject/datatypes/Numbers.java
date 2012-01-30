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
import java.util.ArrayList;
import aject.exceptions.DataTypeException;
import aject.protocol.*;
import utils.Utils;

public class Numbers extends FieldSpecification<NumberInterval> implements Serializable {
  public static final long serialVersionUID = ProtocolSpecification.serialVersionUID;

  protected int _nbits;
  protected boolean _big_endian = true;
  protected boolean _signed = true;
  protected Bits.Type _type = Bits.Type.DEC;

  /**
   * Creates a field that can hold NumberInterval data.
   * 
   * @param nbits bits for representing the numbers.
   * @param big_endian bit ordering.
   * @param signed number with signed bit.
   * @throws DataTypeException
   */
  public Numbers(int nbits, boolean big_endian, boolean signed) {
    _nbits = nbits;
    _big_endian = big_endian;
    _signed = signed;
  }

  public ArrayList<NumberInterval> getIntervals() {
    return _valid_data;
  }

  public int getBitLength() {
    return _nbits;
  }

  public boolean isBigEndian() {
    return _big_endian;
  }

  public boolean isSigned() {
    return _signed;
  }

  public void add(long min, long max) throws DataTypeException {
    NumberInterval interval = new NumberInterval(min, max, _nbits, _big_endian, _signed);
    super.add(interval);
  }

  public void add(long value) throws DataTypeException {
    NumberInterval interval = new NumberInterval(value, _nbits, _big_endian, _signed);
    super.add(interval);
  }

  @Override
  public NumberInterval get(int index) throws DataTypeException {
    long accum = 0;
    long index_long = index;
    /* get interval */
    for (NumberInterval interval : _valid_data) {
      if (index_long < accum + interval.getTotalElements()) {
        return interval.get((int)(index_long - accum));
      } else
        accum += interval.getTotalElements();
    }
    throw new DataTypeException(DataTypeException.INEXISTANT, Integer.toString(index));
  }

  public int getIndex(long value) throws DataTypeException {
    for (int i = 0; i < this.getTotalElements(); i++) {
      if (value == get(i).getValue())
        return i;
    }
    return -1;
  }

  @Override
  public void remove(int index) throws DataTypeException {
    long accum = 0;
    NumberInterval interval = null;

    /* get interval */
    for (int i = 0; i < _valid_data.size(); i++, accum += interval.getTotalElements()) {
      interval = _valid_data.get(i);
      if (index < accum + interval.getTotalElements()) {
        /* found */
        _valid_data.remove(i);
        NumberInterval[] new_intervals = interval.remove_index((int)(index - accum));
        if (new_intervals[0] != null)
          _valid_data.add(i++, new_intervals[0]);
        if (new_intervals[1] != null)
          _valid_data.add(i++, new_intervals[1]);
        return;
      }
    }
    throw new DataTypeException(DataTypeException.INEXISTANT, Integer.toString(index));

  }

  @Override
  public int getTotalElements() {
    int total = 0;
    for (NumberInterval interval : _valid_data)
      total += interval.getTotalElements();
    return total;
  }

  public long getTotalElementsMax() throws DataTypeException {
    Number n = get(0);
    int nbits = n.getBitLength();
    boolean is_signed = n.isSigned();
    return Number.maxValue(nbits, is_signed) - Number.minValue(nbits, is_signed) + 1;
  }

  public static void main(String[] args) {
    try {
      Numbers numbers = new Numbers(4, true, false);
      numbers.add(new NumberInterval(0, 8));
      System.out.println("TOTAL = " + numbers.getTotalElements() + "\t" + numbers);
      numbers.remove(7); // 0-8 (2) -> 0-1 3-8
      System.out.println("TOTAL = " + numbers.getTotalElements() + "\t" + numbers);
      numbers.remove(0); // 0-1 3-8 (4) -> 0-1 3 5-8
      System.out.println("TOTAL = " + numbers.getTotalElements() + "\t" + numbers);

    } catch (DataTypeException e) {
      Utils.handleException(e);
    }

  }

}
