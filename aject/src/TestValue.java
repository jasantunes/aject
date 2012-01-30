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
import java.io.IOException;
import java.util.ArrayList;
import aject.attacks.Test;
import aject.datatypes.*;
import aject.exceptions.DataTypeException;
import aject.protocol.*;
import utils.Utils;

public class TestValue implements Test {

  private IllegalWords _illegal_words; // check Class constructor
  // private static final float _random_ratio = 0.0f;
  private int[] _indexes;
  private ArrayList<MessageSpecification> _master_messages;
  private int _current_message;
  private boolean _more;
  private ArrayList<FieldSpecification> _invalid_fields;

  private Bits _initial_protocol_delimiter, _final_protocol_delimiter;

  /**
   * Enhanced test value. Values for Word type are generated and combined from a
   * set of chosen malicious tokens and payload.
   */
  public TestValue(String filename_tokens, String filename_payload, int max_combination)
      throws IOException {
    _illegal_words = new IllegalWords(filename_tokens, filename_payload, max_combination);
  }

  /**
   * Simple test value. Values for Word type are plain random.
   */
  public TestValue() {
    _illegal_words = new IllegalWords();
  }

  public String getName() {
    return "TestValue";
  }

  /*
   * (non-Javadoc)
   * @see attack.Packet#initialize()
   */
  // @Override
  public void initialize(ProtocolSpecification protocol, MessageSpecification m) {
    _invalid_fields = getIllegalData(m);
    _master_messages = generateMasterMessages(m);
    _current_message = 0;
    initialize_vars(_master_messages.get(_current_message));
    _initial_protocol_delimiter = protocol.getInitialDelimiter();
    _final_protocol_delimiter = protocol.getFinalDelimiter();
  }

  private void initialize_vars(MessageSpecification m) {
    _indexes = new int[m.getFields().size()];
    _more = true;
  }

  /*
   * (non-Javadoc)
   * @see attack.Packet#generate()
   */
  // @Override
  public byte[] generate() {
    byte[] a = null;
    try {

      while (a == null && _more && _current_message < _master_messages.size()) {
        a = generate(_master_messages.get(_current_message));

        // propagate indexes if possible
        if (!propagate_indexes(_master_messages.get(_current_message), _indexes.length - 1))
          // process next master message
          if (_current_message + 1 < _master_messages.size())
            initialize_vars(_master_messages.get(++_current_message));
          // last attack
          else
            _more = false;
      }
    } catch (DataTypeException e) {
      Utils.handleException(e);
    }

    return a;
  }

  private boolean propagate_indexes(MessageSpecification m, int i) {

    // finish, no more propagation
    if (i < 0)
      return false;
    // can propagate current field
    else if (_indexes[i] + 1 < m.getFields().get(i).getTotalElements()) {
      if (_indexes[i] == Integer.MAX_VALUE)
        System.err.println("OVERFLOW _indexes[" + i + "] =" + _indexes[i]);
      _indexes[i]++;
      return true;
    } else {
      _indexes[i] = 0;
      return propagate_indexes(m, i - 1);
    }
  }

  private byte[] generate(MessageSpecification message) throws DataTypeException {
    Bits data = new Bits();

    data.append(_initial_protocol_delimiter);

    int i = 0;
    /* message */
    data.append(message.getInitialDelimiter());
    for (FieldSpecification f : message.getFields()) {
      data.append(f.getInitialDelimiter());
      data.append(f.get(_indexes[i++]));
      data.append(f.getFinalDelimiter());
    }
    data.append(message.getFinalDelimiter());
    data.append(_final_protocol_delimiter);

    return data.getBytes();
  }

  private ArrayList<FieldSpecification> getIllegalData(MessageSpecification message) {
    ArrayList<FieldSpecification> messages = new ArrayList<FieldSpecification>();

    /* create a new message for each invalid field to be tested */
    for (FieldSpecification f : message.getFields()) {
      try {
        FieldSpecification illegal_field = null;
        if (f instanceof Numbers)
          illegal_field = new IllegalNumbers((Numbers)f);
        else if (f instanceof Words)
          illegal_field = _illegal_words.clone();
        else
          illegal_field = new IllegalWords(); // random data

        illegal_field.setInitialDelimiter(f.getInitialDelimiter());
        illegal_field.setFinalDelimiter(f.getFinalDelimiter());
        messages.add(illegal_field);

      } catch (DataTypeException e) {
        Utils.handleException(e);
      }

    }
    return messages;
  }

  private ArrayList<MessageSpecification> generateMasterMessages(MessageSpecification message) {
    int total_fields = message.getFields().size();
    ArrayList<MessageSpecification> expanded = new ArrayList<MessageSpecification>(total_fields);

    /* create a new message for each invalid field to be tested */
    for (int i = 0; i < total_fields; i++) {
      MessageSpecification m = new MessageSpecification(message.getName(),
          message.getInitialDelimiter(), message.getFinalDelimiter());
      ArrayList<FieldSpecification> fields = m.getFields();

      /* copy valid fields */
      for (int j = 0; j < i; j++)
        fields.add(message.getFields().get(j));

      /*
       * create field with illegal FieldSpecification from the original
       * FieldSpecification
       */
      fields.add(_invalid_fields.get(i));

      /* copy valid fields */
      for (int j = i + 1; j < total_fields; j++)
        fields.add(message.getFields().get(j));

      expanded.add(m);
    }

    return expanded;

  }
}
