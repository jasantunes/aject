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

public class MonitoringData implements aject.monitor.MonitoringDataInterface, java.io.Serializable {
  private static final long serialVersionUID = 1L;

  public static final int CTIME = 0;
  public static final int CTIME_GLIB = 1;
  public static final int WTIME = 2;
  public static final int PROCS = 3;
  public static final int PROCS_MAX = 4;
  public static final int PROCS_TOTAL = 5;
  public static final int MEM = 6;
  public static final int MEM_MAX = 7;
  public static final int MEM_TOTAL = 8;
  public static final int OFILES = 9;
  public static final int OFILES_MAX = 10;
  public static final int OFILES_TOTAL = 11;
  public static final int DISK = 12;
  public static final int DISK_MAX = 13;
  public static final int DISK_TOTAL = 14;
  public static final int MCYC_PER_SEC = 15;
  public static final int TOTAL_RESOURCES = 16;

  public static final String[] resource_name = {
      "CPU (M cycles)", "CPU (libgtop) ", "Wall time     ", "Procs         ", "Procs (max)   ",
      "Procs (total) ", "Mem           ", "Mem (max)     ", "Mem (total)   ", "Files         ",
      "Files (max)   ", "Files (total) ", "Disk          ", "Disk (max)    ", "Disk (total)  ",
      "MCyc/Sec      "
  };

  private int[] _signals; // *nix signals
  private long[] _resources;

  public MonitoringData(byte[] monitor_data) {
    ByteBuffer data = ByteBuffer.wrap(monitor_data);

    /* signals */
    int offset = 0;
    _signals = new int[data.getInt(offset)];
    offset += 4;
    for (int i = 0; i < _signals.length; i++) {
      _signals[i] = data.getShort(offset);
      offset += 2;
    }

    /* resources */
    _resources = new long[TOTAL_RESOURCES];
    _resources[PROCS] = data.getInt(offset);
    offset += 4;
    _resources[PROCS_MAX] = data.getInt(offset);
    offset += 4;
    _resources[PROCS_TOTAL] = data.getInt(offset);
    offset += 4;
    _resources[CTIME] = ((long)data.getLong(offset)) / 1000000;
    offset += 8;
    _resources[CTIME_GLIB] = data.getInt(offset);
    offset += 4;
    _resources[WTIME] = data.getInt(offset);
    offset += 4;
    _resources[MEM] = data.getInt(offset);
    offset += 4;
    _resources[MEM_MAX] = data.getInt(offset);
    offset += 4;
    _resources[MEM_TOTAL] = data.getInt(offset);
    offset += 4;
    _resources[DISK] = data.getInt(offset);
    offset += 4;
    _resources[DISK_MAX] = data.getInt(offset);
    offset += 4;
    _resources[DISK_TOTAL] = data.getInt(offset);
    offset += 4;
    _resources[OFILES] = data.getInt(offset);
    offset += 4;
    _resources[OFILES_MAX] = data.getInt(offset);
    offset += 4;
    _resources[OFILES_TOTAL] = data.getInt(offset);
    offset += 4;
    _resources[MCYC_PER_SEC] = 0;
  }

  public long[] getResources() {
    return _resources;
  }

  public void subtractBase(MonitoringData base) {
    _resources[CTIME] -= base._resources[CTIME];
    _resources[CTIME_GLIB] -= base._resources[CTIME_GLIB];
    _resources[WTIME] -= base._resources[WTIME];

    _resources[MCYC_PER_SEC] = _resources[CTIME] / (_resources[WTIME] / 1000);
  }

  public boolean isSignalError() {
    for (int s : _signals)
      switch (s) {
        case 4: // SIGILL
        case 6: // SIGABRT, SIGIOT
        case 7: // SIGBUS
        case 8: // SIGFPE
        case 11: // SIGSEGV
          return true;
        default:
          continue;
      }
    return false;
  }

  public static String getHeader() {
    StringBuffer s = new StringBuffer("Signals");
    for (String name : resource_name)
      s.append("\t" + name);
    return new String(s);
  }

  @Override
  public String toString() {
    StringBuffer s = new StringBuffer();
    for (int sig : _signals)
      s.append(sig + " ");

    for (int res = 0; res < TOTAL_RESOURCES; res++)
      s.append("\t" + _resources[res]);

    return new String(s);
  }

}
