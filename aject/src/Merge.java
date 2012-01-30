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
import aject.attacks.AttacksFile;
import aject.file.RandomAccessFile;
import utils.OptionsException;
import utils.Utils;

public class Merge {

  public static void printUsage() {
    System.out.println("Usage: merge OUTPUT_FILE INPUT_FILE...");
    System.out.println();
    System.out.println("Merges the generated attacks from different files into one bigger file.");
    System.out.println();
    System.out.println("Report bugs to <jantunes@di.fc.ul.pt>.");
  }

  public static void main(String[] args) {
    AttacksFile merged_attacks = null;

    try {

      /* output filename for the generated attacks */
      if (args.length < 1) {
        throw new OptionsException(OptionsException.Types.MISSING_PARAMETER,
            "Please specify file_output: Output file for the generated attacks");
      }
      String file_output = args[0];
      merged_attacks = new AttacksFile(new RandomAccessFile(file_output));

      /* remaining input files */
      for (int i = 0; i < args.length - 1; i++) {
        String filename = args[i + 1];
        RandomAccessFile file = new RandomAccessFile(filename);
        AttacksFile n = new AttacksFile(file);
        System.out.println(filename + " (" + n.getTotalAttacks() + ")");
        merged_attacks.merge(n);
      }
      System.out.println(merged_attacks.getTotalAttacks() + " attacks written to file: "
          + file_output);

    } catch (OptionsException oe) {
      System.out.println(oe.getMessage());
      printUsage();
      System.exit(1);
      // } catch (ClassNotFoundException ce) {
      // System.out.println("Error loading " + file_input +
      // ". Wrong type of file.");
      // System.exit(1);
    } catch (IOException ie) {
      Utils.handleException(ie);
      System.exit(1);
    }

  }
}
