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
import aject.attacks.Attack;
import aject.attacks.AttacksFile;
import aject.file.RandomAccessFile;
import aject.protocol.Bits;
import utils.*;

public class Display {

  public static void printUsage() {
    System.out.println("Usage: aject.Display [OPTION] ATTACKS_FILE");
    System.out.println();
    System.out.println("Displays the generated attacks from file <attacks_file>.");
    System.out.println();
    System.out.println("  Options:");
    System.out.println("    -a, --attack <attack_id>     inject the attack with this id");
    System.out.println("    --ascii, --hex, --bin        target output format (default: --ascii)");
    System.out.println("    -d, --dump                   additional dump lines [dump:]");
    System.out.println();
    System.out.println("Report bugs to <jantunes@di.fc.ul.pt>.");
  }

  public static void main(String[] args) {
    Bits.Type type = null;

    Options opt = new Options();
    opt.setOption("--attack", "-a");
    opt.setOption("--ascii", null);
    opt.setOption("--bin", null);
    opt.setOption("--hex", null);
    opt.setOption("--dump", "-d");

    /***************************************************************************
     * check params
     */
    try {

      /* output filename for the generated attacks */
      if (args.length > 4 || args.length < 1)
        throw new OptionsException(OptionsException.Types.MISSING_PARAMETER,
            "Please specify <attacks_file>: file with the generated attacks");

      opt.setArgs(args);

      /* Target output format (default: ascii) */
      if (opt.getValueBoolean("--hex"))
        type = Bits.Type.HEX;
      else if (opt.getValueBoolean("--bin"))
        type = Bits.Type.BIN;
      else
        type = Bits.Type.TXT;

      /* --dump */
      boolean opt_dump = opt.getValueBoolean("--dump");
      System.out.println("--dump = " + opt_dump);

      /* --attack */
      boolean opt_attack = opt.getValueBoolean("--attack");
      int opt_attack_id = (opt_attack == true) ? opt.getValueInteger("--attack") : 0;

      /* get generated attacks */
      String file_input = args[args.length - 1];
      AttacksFile attacks = new AttacksFile(new RandomAccessFile(file_input));

      System.out.println("Protocol specification: " + attacks.getProtocolName());
      System.out.println("Test case generation algorithm: " + attacks.getTestName());
      System.out.println(file_input + " (" + attacks.getTotalAttacks() + " attacks)");
      System.out
          .println("________________________________________________________________________________");

      for (Attack a = null; (a = attacks.getNextAttack()) != null;) {
        int attack_id = a.getAttackId();

        if (opt_attack == true && opt_attack_id != attack_id)
          continue;

        byte[] attack = a.getMessage();
        for (byte[] t : a.getTransitionMessages())
          System.out.println("[" + attack_id + "]\ttransition:\t" + Bits.byteToString(t, type));
        System.out.println("[" + attack_id + "]\t    attack:\t" + Bits.byteToString(attack, type)
            + "\t (" + attack.length + " bytes)");

        if (opt_dump == true) {
          System.out.print("[" + attack_id + "]\t      dump:\t");
          System.out.write(attack, 0, attack.length);
          System.out.println();
        }

        if (opt_attack == true && opt_attack_id == attack_id)
          break;

      }
      System.out
          .println("________________________________________________________________________________");
      System.out.println("Protocol specification: " + attacks.getProtocolName());
      System.out.println("Test case generation algorithm: " + attacks.getTestName());
      System.out.println(file_input + " (" + attacks.getTotalAttacks() + " attacks)");

    } catch (OptionsException oe) {
      System.out.println(oe.getMessage());
      printUsage();
      System.exit(1);
    } catch (IOException ie) {
      Utils.handleException(ie);
      System.exit(1);
    }

  }
}
