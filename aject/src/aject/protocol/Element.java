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

public abstract class Element implements java.io.Serializable {
  public final static long serialVersionUID = ProtocolSpecification.serialVersionUID;
  protected String _name;

  /* delimiters */
  protected Bits _final_delimiter; // space for word fields
  protected Bits _initial_delimiter; // eg: IMAP's initial tag

  // //////////////////////////////////////////////////////////
  // CONSTRUCTORS //
  // //////////////////////////////////////////////////////////
  public Element(String name, Bits initial_delimiter, Bits final_delimiter) {
    _name = name;
    _initial_delimiter = initial_delimiter;
    _final_delimiter = final_delimiter;
  }

  public Element() {
    this(null, new Bits(), new Bits());
  }

  // //////////////////////////////////////////////////////////
  // GETS / SETS //
  // //////////////////////////////////////////////////////////
  public String getName() {
    return _name;
  }

  public Bits getInitialDelimiter() {
    return _initial_delimiter;
  }

  public Bits getFinalDelimiter() {
    return _final_delimiter;
  }

  public void setName(String name) {
    _name = name;
  }

  public void setInitialDelimiter(Bits delimiter) {
    _initial_delimiter = delimiter;
  }

  public void setFinalDelimiter(Bits delimiter) {
    _final_delimiter = delimiter;
  }

}
