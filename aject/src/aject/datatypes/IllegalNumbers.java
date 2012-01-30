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

import aject.exceptions.DataTypeException;
import aject.protocol.Bits;
import aject.protocol.ProtocolSpecification;
import utils.Utils;

public class IllegalNumbers extends Numbers {
  public static final long serialVersionUID = ProtocolSpecification.serialVersionUID;

  /**
   * Creates a new IllegalNumbers with all the numbers not in valid_numbers.
   * 
   * @pre valid_numbers must be non-empty.
   */
  public IllegalNumbers(Numbers valid_numbers) throws DataTypeException {
    this(valid_numbers, (float)valid_numbers.getTotalElements()
        / (float)valid_numbers.getTotalElementsMax());
  }

  /**
   * Creates a new IllegalNumbers with border numbers from the valid_numbers and
   * with some additional random numbers.
   * 
   * @pre valid_numbers must be non-empty.
   * @param valid_numbers
   * @param ratio Ratio of illegal values to be selected, between 0 and 1. A
   *          value of 1 means that all illegal values are selected.
   * @throws DataTypeException
   */
  public IllegalNumbers(Numbers valid_numbers, float ratio) throws DataTypeException {
    super(valid_numbers.getBitLength(), valid_numbers.isBigEndian(), valid_numbers.isSigned());
    long min = Number.minValue(_nbits, _signed);
    long max = min;

    /**
     * Add all illegal values (faster than the other way)
     */
    if (ratio == 1) {
      for (NumberInterval i : valid_numbers.getIntervals()) {
        max = i._value_min - 1; // min.getPrevious();
        if (min <= max)
          this.add(min, max);
        min = i._value_max + 1; // max.getNext();
      }

      try {
        this.add(min, Number.maxValue(_nbits, _signed));
      } catch (DataTypeException e) {
      }
    }

    /**
     * Add all border values and some (ratio) illegal values
     */
    else {
      IllegalNumbers all = new IllegalNumbers(valid_numbers, 1);

      for (NumberInterval i : valid_numbers.getIntervals()) {
        max = i._value_min - 1; // min.getPrevious();
        if (min <= max) {
          this.add(min);
          this.add(max);
          try {
            all.remove(all.getIndex(min));
          } catch (DataTypeException e) {
          }
          try {
            all.remove(all.getIndex(max));
          } catch (DataTypeException e) {
          }
        }
        min = i._value_max + 1; // max.getNext();
      }

      try {
        max = Number.maxValue(_nbits, _signed);
        this.add(min);
        try {
          all.remove(all.getIndex(min));
        } catch (DataTypeException e) {
        }
        if (min != max) {
          this.add(max);
          try {
            all.remove(all.getIndex(max));
          } catch (DataTypeException e) {
          }
        }
      } catch (DataTypeException e) {
      }

      /* get random elements */
      int random_elems = Math.round(all.getTotalElements() * ratio);

      for (int i = 0; i < random_elems; i++) {
        int index = Utils.random(0, all.getTotalElements());
        long value = all.get(index).getValue();
        this.add(value);
        try {
          all.remove(index);
        } catch (DataTypeException e) {
        }
      }
    }
  }

  public static Bits t(Bits b) {
    return b;
  }

  public static void main(String[] args) {
    try {

      int nbits = 1;

      Numbers numbers = new Numbers(nbits, true, false);
      numbers.add(0);
      // numbers.add(new NumberInterval(11,12);
      // numbers.add(new NumberInterval(31);
      System.out.println("      LEGAL = " + numbers + " (" + numbers.getTotalElements() + "/"
          + numbers.getTotalElementsMax() + ")");

      IllegalNumbers illegal = new IllegalNumbers(numbers);
      System.out.println("RATIO = " + ((float)illegal.getTotalElements())
          / ((float)illegal.getTotalElementsMax()));
      System.out.println("    ILLEGAL = " + illegal);

      Bits b = t(illegal.get(0));
      System.out.println("Bytes: " + b);
      if (b instanceof Number)
        System.out.println("Number: " + b);
      if (b instanceof Number)
        System.out.println("NumberInterval: " + b);

    } catch (DataTypeException e) {
      Utils.handleException(e);
    }
  }

}
