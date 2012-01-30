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
/* MonitorController.java */

package aject.monitor;

import utils.Utils;
import aject.communication.PeerToPeer;
import aject.exceptions.PeerToPeerException;

public class InternalMonitor extends PeerToPeer implements MonitorInterface {

  // msg spec + resources + 500 signals
  protected static final int BUFFER_SIZE = (2 + 4) + (64) + (2 * 500);
  protected int _current_attack;
  protected byte[] _monitoring_data;

  // /////////////////////////////////////////////////////////////////////////
  // CLIENT (Injector)
  // /////////////////////////////////////////////////////////////////////////
  /**
   * Client monitor (Injector).
   */
  public InternalMonitor(String host, int port) throws PeerToPeerException {
    super(PeerToPeer.UDP, host, port, BUFFER_SIZE);
    _current_attack = 0;
    // setTimeout(1000); // 1000 milliseconds
    super.open();
  }

  public boolean resetTarget() {
    _current_attack++;
    return send(new MonitorMessage(MonitorMessage.MC_SYNC_RESET, _current_attack));
  }

  public MonitoringData getData() {
    if (send(new MonitorMessage(MonitorMessage.MC_SYNC_DATA, _current_attack)))
      return new MonitoringData(_monitoring_data);
    else
      return null;
  }

  public void exit() {
    _current_attack = 0;
    send(new MonitorMessage(MonitorMessage.MC_SYNC_RESET, _current_attack));
    try {
      this.terminate();
    } catch (PeerToPeerException e) {
    }
  }

  /**
   * Server monitor (RemoteMonitor).
   */
  public InternalMonitor(int local_port) throws PeerToPeerException {
    super(PeerToPeer.UDP, local_port, BUFFER_SIZE);
    _current_attack = 0;
    open();
  }

  public MonitorMessage waitForRequest() throws PeerToPeerException {
    MonitorMessage m = new MonitorMessage(super.receive());
    _current_attack = m._attack_id;
    return m;
  }

  public boolean sendData(byte[] data) {
    MonitorMessage m = new MonitorMessage(MonitorMessage.MC_SYNC_DATA, _current_attack);
    m.setMonitoringData(data);
    return send(m);
  }

  public void sendAck() {
    send(new MonitorMessage(MonitorMessage.MC_SYNC_ACK, _current_attack));
  }

  // /////////////////////////////////////////////////////////////////////////
  // COMMON
  // /////////////////////////////////////////////////////////////////////////

  /**
   * Sends a synchronization message to remote monitor.
   * 
   * @return true if the other end has replied with a correct message, false
   *         otherwise.
   */
  private boolean send(MonitorMessage message) {
    boolean correct_msg = false;

    /* try to send the packet for a maximum of _retries */
    // do {
    /* send message */
    try {
      super.send(message.getBytes());
    } catch (PeerToPeerException e) {
      Utils.pause("> ATTENTION REQUIRED:  Monitor unreachable\n> RESET Monitor and ");
    }

    /* If this is already a replying message, return true */
    if (MonitorMessage.isACK(message))
      return true;

    /* if we got here, then message as been sent, so we get the reply */
    int max_tries = 1;
    while (max_tries-- > 0) {
      try {
        MonitorMessage message_rcv = new MonitorMessage(receive());
        if (MonitorMessage.isACK(message, message_rcv)) {
          _monitoring_data = message_rcv.getMonitoringData();
          correct_msg = true;
          break;
        }
      } catch (PeerToPeerException e) {
        /* repeat */
        if (max_tries > 0)
          System.err.println("repeating");
      }
    }
    // } while (correct_msg == false);
    return correct_msg;
  }

}
