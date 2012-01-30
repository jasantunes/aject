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

import aject.datatypes.Number;
import aject.exceptions.DataTypeException;
import aject.protocol.Bits;
import utils.Convert;

public class MonitorMessage {
  protected static final int MC_SYNC_RESET = 1;
  protected static final int MC_SYNC_DATA = 2;
  protected static final int MC_SYNC_ACK = 3;

  protected int _type;
  protected int _attack_id;
  protected byte[] _monitoring_data;

  public MonitorMessage(int type, int attack_id, byte[] monitoring_data) {
    _type = type;
    _attack_id = attack_id;
    _monitoring_data = monitoring_data;
  }

  public MonitorMessage(int type, int attack_id) {
    this(type, attack_id, new byte[0]);
  }

  public MonitorMessage(byte[] message) {
    _type = Convert.toInteger(message, 0, 2);
    _attack_id = Convert.toInteger(message, 2, 4);
    int length = message.length - 6;
    _monitoring_data = new byte[length];
    System.arraycopy(message, 6, _monitoring_data, 0, length);
  }

  public byte[] getBytes() {
    Bits data = new Bits();
    try {
      data.append(new Number(_type, 2 * 8));
      data.append(new Number(_attack_id, 4 * 8));
      data.append(new Bits(_monitoring_data));
    } catch (DataTypeException e) {
    }
    return data.getBytes();
  }

  public byte[] getMonitoringData() {
    return _monitoring_data;
  }

  public void setMonitoringData(byte[] data) {
    _monitoring_data = data;
  }

  public static boolean isACK(MonitorMessage message) {
    if (message._type == MonitorMessage.MC_SYNC_ACK || message._monitoring_data.length > 0)
      return true;
    else
      return false;
  }

  public static boolean isACK(MonitorMessage message_snt, MonitorMessage message_rcv) {
    switch (message_snt._type) {
      case MC_SYNC_RESET:
        return (message_rcv._type == MC_SYNC_ACK && message_snt._attack_id == message_rcv._attack_id);
      case MC_SYNC_DATA:
        return (message_rcv._type == MC_SYNC_DATA && message_snt._attack_id == message_rcv._attack_id);
      default:
        return false;
    }
  }

  public boolean isReset() {
    return (_type == MC_SYNC_RESET);
  }

  public boolean isDataRequest() {
    return (_type == MC_SYNC_DATA);
  }

}
