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
/* CombineObjects.java */

package aject;

import java.util.ArrayList;

public class Combination<T> {
  private final ArrayList<T> _objs;
  private ArrayList<T> _combination;
  private int[] _combined_indexes;

  // public Combination(T[] objs) {
  // _objs = objs;
  // }

  public Combination(ArrayList<T> objs) {
    _objs = objs;
  }

  public void reset(int len) {
    _combination = new ArrayList<T>(len);
    _combined_indexes = new int[len];
    for (int i = 0; i < len; i++) {
      _combined_indexes[i] = 0;
      _combination.add(_objs.get(0));
    }
  }

  public boolean next(int len) {

    for (int i = 0; i < len; i++) {

      /* if we can increment */
      if (_combined_indexes[i] + 1 < _objs.size()) {
        /* increment this last one and return */
        _combined_indexes[i]++;
        _combination.set(i, _objs.get(_combined_indexes[i]));
        return true;
      }

      /* reset iteration and continue */
      _combined_indexes[i] = 0;
      _combination.set(i, _objs.get(0));
    }

    /* no more to increment */
    return false;
  }

  public ArrayList<T> getCombination() {
    return new ArrayList<T>(_combination);
  }

  public ArrayList<ArrayList<T>> getCombinations(int len) {
    reset(len);

    ArrayList<ArrayList<T>> lines = new ArrayList<ArrayList<T>>();
    do {
      lines.add(getCombination());
    } while (next(len));
    return lines;
  }

  public static void main(String[] args) {
    // String[] str = args;
    String[] str = {
        "0", "1"
    };
    ArrayList<String> arr = new ArrayList<String>();
    for (String s : str)
      arr.add(s);

    // Combination<String> oc = new Combination<String>(str);
    Combination<String> oc = new Combination<String>(arr);
    ArrayList<ArrayList<String>> combinations = oc.getCombinations(2);
    for (ArrayList<String> combination : combinations) {
      for (String c : combination)
        System.out.print(c);
      System.out.println();
    }

  }

}
