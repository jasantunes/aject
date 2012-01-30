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

import java.io.IOException;
import java.util.ArrayList;
import aject.Combination;
import aject.protocol.*;
import utils.Utils;

public class IllegalWords extends FieldSpecification<Word> {

  public static final long serialVersionUID = ProtocolSpecification.serialVersionUID;
  private static final String PAYLOAD_KEYWORD = "$(PAYLOAD)";
  private static final String RANDOM_KEYWORD = "$(RANDOM)";
  private static final int RANDOM_LENGTH = 500;

  /**
   * Simple set of random Word's.
   */
  public IllegalWords() {
    _valid_data.add(new Word(new String(Bits.crap(10, true))));
    // _valid_data.add(new Word(new String(Bytes.crap(10, false)),
    // Bytes.Type.HEX));
    _valid_data.add(new Word(new String(Bits.crap(100, true))));
    // _valid_data.add(new Word(new String(Bytes.crap(100, false)),
    // Bytes.Type.HEX));
    // _valid_data.add(new Word(new String(Bytes.crap(300, true))));
    // _valid_data.add(new Word(new String(Bytes.crap(300, false)),
    // Bytes.Type.HEX));
    // _valid_data.add(new Word(new String(Bytes.crap(1000000, true))));
    // _valid_data.add(new Word(new String(Bytes.crap(1000000, false)),
    // Bytes.Type.HEX));
  }

  public IllegalWords(String filename_tokens, String filename_payload, int max_combination)
      throws IOException {
    this(Utils.getFileContents(filename_tokens), Utils.getFileContents(filename_payload),
        max_combination);
  }

  public IllegalWords(ArrayList<String> tokens, ArrayList<String> payloads, int max_combination) {
    ArrayList<String> words = new ArrayList<String>();

    /* expand $(PAYLOAD) keyword in malicious tokens */
    words = expandTokens(tokens, payloads);
    System.out.println(words);

    /* generate combinations */
    ArrayList<String> temp = new ArrayList<String>();
    for (int i = 1; i <= max_combination; i++) {
      temp.addAll(generateCombinations(words, i));
    }
    words = temp;

    /*
     * expand $(RANDOM) keywords in resulting expanded tokens. This is done
     * here, after expanding all tokens with the purpose of obtaining different
     * random tokens
     */
    words = expandRandoms(words, RANDOM_LENGTH);

    /* add words to _valid_data */
    for (String s : words) {
      Word w = new Word(s);
      _valid_data.add(w);
    }
  }

  private static ArrayList<String> generateCombinations(ArrayList<String> tokens,
      int max_combination_len) {
    Combination<String> oc = new Combination<String>(tokens);
    ArrayList<String> combinations = new ArrayList<String>();

    for (ArrayList<String> combination : oc.getCombinations(max_combination_len)) {
      StringBuffer line = new StringBuffer();
      for (String c : combination) {
        line.append(c);
      }
      combinations.add(new String(line));
    }
    return combinations;
  }

  private static ArrayList<String> expandTokens(ArrayList<String> tokens, ArrayList<String> payloads) {
    ArrayList<String> expanded_tokens = new ArrayList<String>();
    for (String token : tokens) {
      expanded_tokens.addAll(_expand_token(token, payloads));
    }
    return expanded_tokens;
  }

  private static ArrayList<String> _expand_token(String token, ArrayList<String> payloads) {
    ArrayList<String> expanded = new ArrayList<String>();

    /* search for $(PAYLOAD) */
    int index = token.indexOf(PAYLOAD_KEYWORD);

    // not found, return token unchanged immediately
    if (index < 0) {
      expanded.add(token);
      return expanded;
    }

    String left = token.substring(0, index);
    String right = token.substring(index + PAYLOAD_KEYWORD.length());

    /* recursive right side (remaining $(PAYLOAD)'s) */
    for (String right_expansion : _expand_token(right, payloads)) {
      for (String s : payloads) {
        expanded.add(left + s + right_expansion);
      }
    }

    return expanded;
  }

  private static ArrayList<String> expandRandoms(ArrayList<String> tokens, int length) {
    ArrayList<String> expanded_randoms = new ArrayList<String>();
    for (String token : tokens) {
      expanded_randoms.addAll(_expand_random(token, length));
    }
    return expanded_randoms;
  }

  private static ArrayList<String> _expand_random(String token, int length) {
    ArrayList<String> expanded = new ArrayList<String>();

    /* search for $(RANDOM_KEYWORD) */
    int index = token.indexOf(RANDOM_KEYWORD);

    // not found, return token unchanged immediately
    if (index < 0) {
      expanded.add(token);
      return expanded;
    }

    String left = token.substring(0, index);
    String right = token.substring(index + RANDOM_KEYWORD.length());

    /* recursive right side (remaining $(PAYLOAD)'s) */
    for (String right_expansion : _expand_random(right, length)) {
      // add random string (1) with only letters and (2) with any char code
      expanded.add(left + Bits.crap(length, 'a', 'z') + right_expansion);
      expanded.add(left + Bits.crap(length, 0, 255) + right_expansion);

    }

    return expanded;
  }

  // public static void main(String[] args) {
  // try {
  // String path = "/home/jantunes/src/injector/scripts/";
  // //IllegalWords illegal = new IllegalWords(path+"tokens.txt",
  // path+"payload.txt", 3);
  // IllegalWords illegal = new IllegalWords(args[0], args[1], new
  // Integer(args[2]).intValue());
  // System.out.println(illegal);
  // } catch (IOException e) { Utils.handleException(e); }
  // }
}
