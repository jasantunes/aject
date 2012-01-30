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
package aject.file;

public class RandomAccessFile extends FileInterface {

  private String _mode;
  private java.io.RandomAccessFile _file;

  public RandomAccessFile(String filename) {
    _filename = filename;
  }

  public void open() throws java.io.IOException {
    _mode = "rw";
    _file = new java.io.RandomAccessFile(_filename, _mode);
  }

  public void clear() throws java.io.IOException {
    _file.setLength(0);
  }

  //
  // public void truncate() throws java.io.IOException {
  // _file.setLength(getPos());
  // }
  public void close() throws java.io.IOException {
    if (_file != null)
      _file.close();
    _file = null;
  }

  public long getPos() throws java.io.IOException {
    return _file.getFilePointer();
  }

  public void seek(long pos) throws java.io.IOException {
    _file.seek(pos);
  }

  // protected void seekFromEnd(long pos) throws java.io.IOException {
  // long total = _file.length();
  // _file.seek(total - pos);
  // }

  protected void read(byte[] dest, int offset, int length) throws java.io.IOException {
    _file.read(dest, offset, length);
  }

  protected void write(byte[] src, int offset, int length) throws java.io.IOException {
    _file.write(src, offset, length);
  }

  public void writeInt(int integer) throws java.io.IOException {
    _file.writeInt(integer);
  }

  public int readInt() throws java.io.IOException {
    return _file.readInt();
  }

}
