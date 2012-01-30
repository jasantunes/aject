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
package aject.monitor;

import java.nio.ByteBuffer;
import aject.communication.Target;
import aject.exceptions.PeerToPeerException;
import utils.Utils;

/**
 * @author jantunes
 */
public class ExternalMonitor implements MonitorInterface {

  private Target _target;
  private int _delay;

  public ExternalMonitor(Target target) {
    _target = target;
    _delay = target.getDelay();
  }

  public boolean resetTarget() {
    return true;
  }

  public MonitoringData getData() {
    boolean is_alive = false;
    int old_delay = _delay;

    for (int i = 0; i < 5 && is_alive != true; i++) {
      try {
        _target.terminate();
      } catch (PeerToPeerException e) {
      }
      try {
        _target.open();
        is_alive = true;
      } catch (PeerToPeerException e) {
        Utils.sleep(_delay);
        _delay += 100;
      }
    }

    if (is_alive) {
      return new MonitoringData(_create_data(0)); // 0 = OK
    } else {
      _delay = old_delay;
      return new MonitoringData(_create_data(-1)); // -1 = CRASH
    }
  }

  public void exit() {
    /* do nothing */
  }

  private byte[] _create_data(int signal) {
    byte[] data = new byte[4 + 4 + (MonitoringData.TOTAL_RESOURCES * 4)];
    ByteBuffer data_buffer = ByteBuffer.wrap(data);
    data_buffer.putInt(1); // total signals ahead
    data_buffer.putInt(signal); // signal
    return data;
  }

}
