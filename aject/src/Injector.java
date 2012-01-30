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
import utils.*;
import aject.attacks.Attack;
import aject.attacks.AttacksFile;
import aject.communication.PeerToPeer;
import aject.communication.Target;
import aject.exceptions.PeerToPeerException;
import aject.file.RandomAccessFile;
import aject.monitor.*;
import aject.protocol.Bits;

public class Injector {

  public static void printUsage() {
    System.out.println("Usage: injector [OPTION]... ATTACKS_FILE");
    System.out.println();
    System.out.println("Injects the generated attacks (from file_input) into "
        + "some target and outputs the results to file_output. ");
    System.out.println();
    System.out.println("  Options:");
    System.out.println("    --continue|-c <attack_id>  Continue injection from this attack");
    System.out.println("    --attack|-a <attack_id>    Inject the attack with this id");
    System.out.println("    --delay|-d <millisecs>     Delay before each network read");
    System.out.println("    --tcp|--udp                Transport protocol used by target");
    System.out
        .println("    --target|-t <host:port>    Target's host IP address or hostname (IP:port)");
    System.out.println("    --monitor|-m <port>        Monitor's port");
    System.out.println("    --ascii|--hex|--bin        target output format (default: --ascii)");
    System.out.println();
    System.out.println("Report bugs to <jantunes@di.fc.ul.pt>.");
  }

  /**
   * @param args [OPTION]... ATTACKS_FILE
   */
  public static void main(String[] args) {
    MonitorInterface monitor = null;
    Target target = null;
    AttacksFile attacks = null;
    String host = null;
    String file_input = null;

    Options opt = new Options();
    boolean opt_continue = false;
    boolean opt_attack = false;
    boolean opt_delay = false;
    int opt_attack_id = 0;
    opt.setOption("--continue", "-c");
    opt.setOption("--attack", "-a");
    opt.setOption("--delay", "-d");
    opt.setOption("--tcp", null);
    opt.setOption("--udp", null);
    opt.setOption("--monitor", "-m");
    opt.setOption("--target", "-t");
    opt.setOption("--ascii", null);
    opt.setOption("--bin", null);
    opt.setOption("--hex", null);

    /* Check command-line parameters. */
    try {

      // Output filename for the generated attacks.
      if (args.length < 2)
        throw new OptionsException(OptionsException.Types.MISSING_PARAMETER,
            "Please specify ATTACKS_FILE");
      opt.setArgs(args);

      // Input filename with generated attacks.
      try {
        file_input = args[args.length - 1];
        RandomAccessFile file = new RandomAccessFile(file_input);
        attacks = new AttacksFile(file);
        System.err.println("[i] Total attacks: " + attacks.getTotalAttacks() + " ("
            + attacks.getProtocolName() + ")");
      } catch (IOException ie) {
        Utils.handleException(ie, "");
        System.err.println("[!] Error loading " + file_input);
        System.exit(1);
      }

      // --target
      if (opt.getValueBoolean("--target")) {
        try {

          int protocol = (opt.getValueBoolean("--udp")) ? PeerToPeer.UDP : PeerToPeer.TCP;
          String address = opt.getValueString("--target");
          int separator = address.indexOf(':');
          host = address.substring(0, separator);
          int port = new Integer(address.substring(separator + 1)).intValue();
          System.err.println("[i] Target: " + host + ":" + port);
          target = new Target(protocol, host, port);

          // --ascii|hex|bin
          if (opt.getValueBoolean("--hex"))
            target.setDatatype(Bits.Type.HEX);
          else if (opt.getValueBoolean("--bin"))
            target.setDatatype(Bits.Type.BIN);
          else
            target.setDatatype(Bits.Type.TXT);

        } catch (Exception e) {
          throw new OptionsException(OptionsException.Types.MISSING_PARAMETER,
              "wrong --target option");
        }
      } else {
        throw new OptionsException(OptionsException.Types.MISSING_PARAMETER,
            "Please specify --target");
      }

      // --continue
      if (opt.getValueBoolean("--continue")) {
        opt_continue = true;
        opt_attack_id = opt.getValueInteger("--continue");
      }

      // --attack
      if (opt.getValueBoolean("--attack")) {
        opt_attack = true;
        opt_attack_id = opt.getValueInteger("--attack");
      }

      // --delay
      if (opt.getValueBoolean("--delay")) {
        opt_delay = true;
        int delay = opt.getValueInteger("--delay");
        target.setDelay(delay);
      }

      // --monitor
      if (opt.getValueBoolean("--monitor")) {
        int port = opt.getValueInteger("--monitor");
        monitor = new InternalMonitor(host, port);
        System.err.println("[i] Monitor: " + host + ":" + port);
      } else {
        monitor = new ExternalMonitor(target);
        System.err.println("[i] Monitor: N/A");
      }

    } catch (OptionsException oe) {
      /* print usage and quit */
      System.err.println("[!] " + oe.getMessage());
      printUsage();
      System.exit(1);
    } catch (PeerToPeerException pe) {
      Utils.handleException(pe, "[!] Monitor error");
    }

    /**
     * ATTACK INJECTION
     */
    try {
      monitor.resetTarget();
      int total = attacks.getTotalAttacks();
      int executed = 0, skipped = 0;

      if (opt_delay == false) {
        System.err.println("[+] tunning...");
        target.learnParameters();
      }
      int delay = target.getDelay();
      System.out.println("Target protocol:\t" + attacks.getProtocolName());
      System.out.println("Generation algorithm:\t" + attacks.getTestName());
      System.out.println("Total attacks:\t" + total);
      System.out.println("Communication delay:\t" + delay + "ms");

      /* Main cycle. */
      Timer timer = new Timer();
      Attack attack = attacks.getNextAttack();
      int retries = 0;
      while (attack != null) {
        if ((opt_attack || opt_continue) && attack.getAttackId() != opt_attack_id) {
          skipped++;
          attack = attacks.getNextAttack();
          continue;
        }

        try {
          /* Estimated time of arrival */
          System.out
              .println("________________________________________________________________________________");
          if (retries > 0)
            System.err.print("[!] ");
          else
            System.err.print("[+] ");
          System.err.printf("Attack: %d/%d (%.1f%%)   |", skipped + executed + 1, total,
              (float)((skipped + executed) * 100) / (float)total); // percentage
          System.err.print("   elapsed: " + Timer.toStringFuzzy(timer.getElapsedTime()));
          System.err
              .print("   remaining: "
                  + Timer.toStringFuzzy(timer.calculateETA((float)executed
                      / (float)(total - skipped))));
          System.err.print("   delay: " + target.getDelay() + "ms");
          System.err.println();
          System.out.println(">> Attack ID:\t" + attack.getAttackId());

          /* Start target, open connection, and go to transition. */
          monitor.resetTarget();
          target.open();
          // TODO: add this sleep to target.open()
          Utils.sleep(target.getDelay());
          target.transition(attack.getTransitionMessages());

          /* Inject attack and get reply. */
          Utils.sleep(target.getDelay());
          System.out.println("> attack:\t" + target.toString(attack.getMessage()));
          timer.mark();
          byte[] reply = target.inject(attack);
          System.out.println("< attack:\t" + // header
              ((reply != null) ? target.toString(reply) : "N/A") + // reply
              "\t(" + Timer.toString(timer.getElapsedTimeFromMark()) + ")"); // time

          /* Get monitoring data from Monitor. */
          System.out.println("> M(" + attack.getAttackId() + "):\t" + MonitoringData.getHeader());
          MonitoringData data = (MonitoringData)monitor.getData();
          if (data != null)
            System.out.println("< M:\t" + data);
          else
            System.out.println("< M:\tN/A");

          /* Terminate connection. */
          try {
            target.terminate();
          } catch (PeerToPeerException e) {
          }

        } catch (PeerToPeerException e) {
          /* Fatal error: repeat last attack. */
          System.out.println("> An error occurred!");
          // Utils.handleException(e);

          try {
            target.terminate();
          } catch (PeerToPeerException ex) {
          }

          /* Increase target's delay on open(). */
          if (retries > 0 && opt_delay == false) {
            int new_delay = target.getDelay() + java.lang.Math.max(delay / 10, 100);
            target.setDelay(new_delay);
            target.setTimeout(Utils.inBetween(2000, new_delay * 10, 5000));
          }
          retries++;
          if (retries < 5) {
            Utils.sleep(target.getDelay());
          } else {
            if (opt_delay == false) {
              target.setDelay(delay);
              target.setTimeout(Utils.inBetween(2000, delay * 10, 5000));
            }
            if (retries == 5) {
              retries = 0;
              timer.pause();
              Utils.pause("[!] ATTENTION REQUIRED: RESET target and/or monitor");
              // System.err.println("> tunning...");
              timer.resume();
            } else {
              System.err.println("[ ] waiting...");
              Utils.sleep(60 * 1000);
              // target.learnParameters();
            }
          }

          System.out.println("> Repeating attack " + attack.getAttackId() + " with  "
              + target.getDelay() + "ms of delay");
          continue;
        }

        /* Next iteration. */
        retries = 0;
        delay = target.getDelay();
        executed++;

        // options: --attack|--continue
        if (opt_attack && attack.getAttackId() == opt_attack_id)
          break;
        else if (opt_continue && attack.getAttackId() == opt_attack_id)
          opt_continue = false;

        attack = attacks.getNextAttack();
      } // Main cycle

      /* Stop monitor remotely. */
      System.out
          .println("________________________________________________________________________________");
      System.out.println("Executed " + executed + " attacks in " + timer.toString() + ".\n");
      System.err.println("[i] Executed " + executed + " attacks in " + timer.toString() + ".\n");
      monitor.exit();
    } catch (PeerToPeerException e) {
      Utils.handleException(e);
      System.err.println("[!] Fatal error!\n");
    }

  }

}
