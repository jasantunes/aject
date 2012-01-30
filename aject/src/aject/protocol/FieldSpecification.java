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
/* FieldSpecification.java */

package aject.protocol;

import java.io.Serializable;
import java.util.ArrayList;
import aject.exceptions.DataTypeException;

public class FieldSpecification<T extends Bits> extends Element implements Serializable {
  public static final long serialVersionUID = ProtocolSpecification.serialVersionUID;
  protected ArrayList<T> _valid_data = new ArrayList<T>();

  public FieldSpecification() {
    super();
  }

  public ArrayList<T> getValidData() {
    return _valid_data;
  }

  public T get(int index) throws DataTypeException {
    return _valid_data.get(index);
  }

  public void add(T element) {
    _valid_data.add(element);
  }

  public int getTotalElements() {
    return _valid_data.size();
  }

  public void remove(int index) throws DataTypeException {
    _valid_data.remove(index);
  }

  public void clear() {
    _valid_data.clear();
  }

  @Override
  public String toString() {
    return _valid_data.toString();
  }

  @Override
  public FieldSpecification<T> clone() {
    FieldSpecification<T> f = new FieldSpecification<T>();
    f._name = this._name;
    f._final_delimiter = this._final_delimiter;
    f._initial_delimiter = this._initial_delimiter;
    f._valid_data = this._valid_data;
    return f;
  }

}
