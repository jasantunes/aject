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
import aject.protocol.*;

public class Words extends FieldSpecification<Word> implements Serializable {
  public static final long serialVersionUID = ProtocolSpecification.serialVersionUID;

  public Words() {
    _final_delimiter = new Word(" ");
  }

  public void add(String word) {
    super.add(new Word(word));
  }
}
