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
package aject.communication;

import java.util.ArrayList;
import aject.attacks.Attack;
import aject.exceptions.PeerToPeerException;
import aject.protocol.Bits;
import utils.Timer;
import utils.Utils;

public class Target extends PeerToPeer {
  public static int TIMEOUT = 10000; // 10 seconds
  public static int MAX_LENGTH = 100000;

  private Bits.Type _datatype;
  private byte[] _ping = null;
  private int _delay = 0;
  private boolean _banner = false;

  public Target(int protocol, String host_addr, int port) throws PeerToPeerException {
    super(protocol, host_addr, port, MAX_LENGTH);
    _datatype = Bits.Type.HEX;
    setTimeout(TIMEOUT);
  }

  /***************************************************************************
   * GET, SETS
   **************************************************************************/

  public void setDatatype(Bits.Type datatype) {
    _datatype = datatype;
  }

  public Bits.Type getDatatype() {
    return _datatype;
  }

  public void setPing(byte[] ping) {
    _ping = ping;
  }

  public void setDelay(int milisseconds) {
    _delay = milisseconds;
  }

  public int getDelay() {
    return _delay;
  }

  @Override
  public byte[] receive() throws PeerToPeerException {
    int MAX_RETRIES = 1;

    do {
      try {
        return super.receive();
      } catch (PeerToPeerException e) {
        if (e.Type() == PeerToPeerException.Types.SOCKET_TIMEOUT)
          throw e;
        else if (MAX_RETRIES > 0)
          Utils.sleep(500);
      }
    } while (MAX_RETRIES-- > 0);
    throw new PeerToPeerException(PeerToPeerException.Types.SOCKET_RECEIVE);
  }

  /**
   * @param timeout_ms
   * @return
   * @throws aject.exceptions.PeerToPeerException
   */
  public byte[] receive(int timeout_ms) throws PeerToPeerException {
    int old_timeout = getTimeout();
    setTimeout(timeout_ms);
    byte[] data = null;

    try {
      data = this.receive();
    } catch (PeerToPeerException e) {
      if (e.Type() != PeerToPeerException.Types.SOCKET_TIMEOUT) {
        setTimeout(old_timeout);
        throw e;
      }
    }
    setTimeout(old_timeout);
    return data;
  }

  /**
   * Sends a valid byte-array (message) to this peer (as specified by the
   * TargetProtocol.getPingData() method). Usefull for testing and for waiting
   * before the target application goes online befora an attack. the other peer.
   * 
   * @param ping_message ping message
   * @param initial_timeout initial time to wait (in milliseconds) for the first
   *          retry (if no data has arrived and retries > 0). Subsequent retries
   *          will duplicate this timeout.
   * @param tries total number of sends if no data has arrived.
   */
  // public void ping(byte[] ping_message, int tries)
  // throws PeerToPeerException {
  // int timeout = _timeout;
  // boolean success = false;
  //
  // while (tries-- > 0 && success == false) {
  // /* ping */
  // try {
  // ping(ping_message);
  // success = true;
  // } catch (PeerToPeerException e) {
  // if (tries>0) {
  // Utils.sleep(timeout);
  // timeout = 2 * timeout; // subsequent retries duplicates waiting
  // }
  // }
  // }
  // if (!success)
  // throw new PeerToPeerException(PeerToPeerException.Types.HOST_UNREACHABLE,
  // "all pings failled");
  // }

  public void ping(byte[] ping_message) throws PeerToPeerException {
    super.send(ping_message);
    System.out.println(toString(receive()));
  }

  public String toString(byte[] data) {
    return (data == null) ? "N/A" : Bits.byteToString(data, _datatype);
  }

  public void learnParameters() throws PeerToPeerException {
    int TEMP_TIMEOUT = 5000;

    /* open connection and measure round-trip time */
    Timer timer = new Timer();
    super.open();
    int time_open = timer.getElapsedTime();

    /* banner */
    for (byte[] data = null; (data = receive(TEMP_TIMEOUT)) != null;) {
      System.out.println("< banner: " + toString(data));
      _banner = true;
    }

    int time_banner = timer.getElapsedTime() - TEMP_TIMEOUT;

    /* banner */
    if (_banner == true) {
      _ping = null; // ignore ping message
      _delay = time_banner - time_open;
    }

    /* ping (no banner) */
    else if (_ping != null) {
      System.out.println("> ping: " + toString(_ping));
      super.send(_ping);
      byte[] data = this.receive();
      System.out.println("< ping: " + toString(data));
      _delay = timer.getElapsedTime() - time_banner;
    }
    /* no banner, no ping */
    else
      _delay = 500; // set delay to 0.5 secs

    /* round up delay (at least 50ms) */
    _delay = Math.max(50, (int)Math.ceil(_delay / 100) * 100);
    setTimeout(Utils.inBetween(1000, _delay * 10, 10000));

    terminate();
  }

  @Override
  public void open() throws PeerToPeerException {

    /* open connection */
    super.open();

    /* get banner */
    if (_banner) {
      Utils.sleep(_delay);
      this.receive();
    }
    /* ping target */
    if (_ping != null) {
      super.send(_ping);
      Utils.sleep(_delay);
      this.receive();
    }
  }

  /**
   * (Syncs with MonitorController), opens the connection with target, and sends
   * the necessary messages to the intended protocol state of the attack.
   */
  public void transition(ArrayList<byte[]> transition_messages) throws PeerToPeerException {
    /* change state */
    for (byte[] msg_state : transition_messages) {
      System.out.println("> transition:\t" + toString(msg_state));
      super.send(msg_state);
      Utils.sleep(_delay);
      System.out.println("< transition:\t" + toString(receive()));
    }
  }

  /**
   * Method that performs the inject of a particular attack. It also gathers
   * monitoring data.
   */
  public byte[] inject(Attack attack) throws PeerToPeerException {
    /* inject attack */
    send(attack.getMessage());
    Utils.sleep(_delay);
    try {
      return this.receive();
    } catch (PeerToPeerException pe) {
      return null;
    }
  }

}
