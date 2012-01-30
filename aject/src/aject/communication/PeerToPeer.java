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
/* PeerToPeer.java */

package aject.communication;

import java.io.*;
import java.net.*;
import aject.exceptions.PeerToPeerException;
import utils.Utils;

public class PeerToPeer {
  public static final int TCP = 0;
  public static final int UDP = 1;

  private int _protocol;
  private int _length;
  private int _MAX_LENGTH;
  private int _timeout;
  private byte[] _buffer;
  private boolean _active;

  private int _local_port;
  private Socket _tcp_socket;
  private ServerSocket _tcp_socket_listen;
  private DatagramSocket _udp_socket;
  private InetSocketAddress _endpoint;

  // TCP
  private DataInputStream _input;
  private DataOutputStream _output;
  // UDP
  private DatagramPacket _udp_datagram;

  // //////////////////////////////////////////////////////////
  // CONSTRUCTORS //
  // //////////////////////////////////////////////////////////

  /**
   * Creates a PeerToPeer object for TCP/UDP communication. For <b>active</b>
   * sockets (clients initiating communication).
   * 
   * @param protocol protocol used: possible values are the static fields TCP
   *          and UDP.
   * @param local_port local port where to receive data.
   * @param host_addr IP or hostname of the host.
   * @param port remote port number.
   */
  public PeerToPeer(int protocol, String host_addr, int port, int max_len)
      throws PeerToPeerException {
    _udp_socket = null;
    _tcp_socket = null;
    _protocol = protocol;
    _active = true;
    _timeout = 0; // infinite timeout

    /* get and save endpoint address */
    try {
      InetAddress host = InetAddress.getByName(host_addr);
      _endpoint = new InetSocketAddress(host, port);
    } catch (UnknownHostException e) {
      throw new PeerToPeerException(PeerToPeerException.Types.UNKNOWN_HOST);
    }

    /* Allocate buffer */
    _MAX_LENGTH = max_len;
    _length = 0;
    _buffer = new byte[max_len];
  }

  /**
   * Creates a PeerToPeer object for TCP/UDP communication. For <b>passive</b>
   * sockets (servers waiting for clients to initiating communication).
   * 
   * @param protocol protocol used: possible values are the static fields TCP
   *          and UDP.
   * @param local_port local port where to receive data.
   */
  public PeerToPeer(int protocol, int local_port, int max_len) {
    _udp_socket = null;
    _tcp_socket = null;
    _protocol = protocol;
    _active = false;
    _timeout = 0; // infinite timeout

    /* get and save local address (port) */
    _local_port = local_port;

    /* Allocate buffer */
    _MAX_LENGTH = max_len;
    _length = 0;
    _buffer = new byte[max_len];
  }

  // //////////////////////////////////////////////////////////
  // GETS / SETS //
  // //////////////////////////////////////////////////////////

  // public byte[] Buffer() { return _buffer; }

  public String getRemoteAddress() {
    return _endpoint.getAddress().getHostAddress();
  }

  public String getRemoteName() {
    return _endpoint.getHostName();
  }

  public int getMaxLength() {
    return _MAX_LENGTH;
  }

  public int getLocalPort() {
    return _local_port;
  }

  public int getRemotePort() {
    return _endpoint.getPort();
  }

  @Override
  public String toString() {
    return getRemoteAddress() + ":" + getRemotePort() + " (" + getRemoteName() + ")";
  }

  // public int Length() { return _length; }

  public int getProtocol() {
    return _protocol;
  }

  public int getTimeout() {
    return _timeout;
  }

  public void setTimeout(int milliseconds) {
    _timeout = milliseconds;
    try {
      if (_protocol == TCP && _tcp_socket != null)
        _tcp_socket.setSoTimeout(_timeout);
      else if (_protocol == UDP && _udp_socket != null)
        _udp_socket.setSoTimeout(_timeout);
    } catch (Exception e) {
    }
  }

  private int _get_timeout() {
    int timeout = 0;
    try {
      timeout = (_protocol == TCP) ? _tcp_socket.getSoTimeout() : _udp_socket.getSoTimeout();
    } catch (SocketException e) {
    }

    return timeout;
  }

  public boolean isActive() {
    return _active;
  }

  public boolean isConnected() {
    return (_protocol == TCP) ? _tcp_socket.isConnected() : true;
  }

  // //////////////////////////////////////////////////////////
  // METHODS //
  // //////////////////////////////////////////////////////////

  /**
   * Opens the stream for communication within a given number of tries, and
   * timeouts.
   * 
   * @param initial_timeout initial time to wait (in milliseconds) for the first
   *          retry. Subsequent retries will duplicate this timeout.
   * @param tries maximum number of opens.
   */
  public void open(int tries) throws PeerToPeerException {
    boolean success = false;
    int timeout = _timeout;
    int i = 0;

    while (tries-- > 0 && success == false) {
      /* open */
      try {
        open();
        success = true; // only upon not raised exception
      } catch (PeerToPeerException e) {
        if (tries > 0) {
          Utils.handleException(e);
          System.err.println("> open error (" + (++i) + ")");
          Utils.sleep(timeout);
          timeout = 2 * timeout; // subsequent retries duplicates waiting
        }
      }
    }

    /* if not success raise exception */
    if (success == false)
      throw new PeerToPeerException(PeerToPeerException.Types.SOCKET_OPEN);

  }

  /**
   * Opens the stream for communication.
   */
  public void open() throws PeerToPeerException {

    /* TCP */
    if (_protocol == TCP) {
      try {
        /* client */
        if (_active) {
          _tcp_socket = new Socket();
          _tcp_socket.connect(_endpoint);
        }

        /* server */
        else {
          _tcp_socket_listen = new ServerSocket(_local_port);
          _tcp_socket = _tcp_socket_listen.accept();
        }
        // _tcp_socket.setPerformancePreferences(1, 2, 0); // connectiontime,
        // latency, bandwidth
        _input = new DataInputStream(new BufferedInputStream(_tcp_socket.getInputStream()));
        _output = new DataOutputStream(new BufferedOutputStream(_tcp_socket.getOutputStream()));

      } catch (Exception e) {
        throw new PeerToPeerException(PeerToPeerException.Types.SOCKET_INIT, e.getMessage());
      }
    }

    /* UDP */
    else {
      try {
        _udp_socket = (_active) ? new DatagramSocket() // client
            : new DatagramSocket(_local_port); // server
      } catch (SocketException e) {
        throw new PeerToPeerException(PeerToPeerException.Types.SOCKET_CREATION, e.getMessage());
      }
    }

    /* set socket options */
    try {
      if (_protocol == TCP) {
        _tcp_socket.setSendBufferSize(_MAX_LENGTH);
        _tcp_socket.setReceiveBufferSize(_MAX_LENGTH);
        _tcp_socket.setSoTimeout(_timeout);
      } else {
        _udp_datagram = (_active) ? new DatagramPacket(_buffer, _MAX_LENGTH, _endpoint)
            : new DatagramPacket(_buffer, _MAX_LENGTH);

        _udp_socket.setSendBufferSize(_MAX_LENGTH);
        _udp_socket.setReceiveBufferSize(_MAX_LENGTH);
        _udp_socket.setSoTimeout(_timeout);

      }
    } catch (SocketException e) {
      throw new PeerToPeerException(PeerToPeerException.Types.SOCKET_OPTION, e.getMessage());
    }

    if (_timeout > 0)
      setTimeout(_timeout);
    _timeout = _get_timeout();
  }

  // public byte[] slowReceive(int timeout_ms) throws PeerToPeerException {
  // ArrayList<byte[]> messages = new ArrayList<byte[]>();
  // int length = 0;
  // byte[] data = null;
  //
  // /* receive data */
  // while (true) {
  // try { data = tryReceive(timeout_ms); }
  // catch (PeerToPeerException e) { break; }
  // if (data == null) break;
  // length += data.length;
  // messages.add(data);
  // }
  //
  // /* merge messages */
  // byte[] merged = new byte[length];
  // int i=0;
  // for (byte[] line : messages) {
  // for (int j=0; j<line.length; j++)
  // merged[i++] = line[j];
  // }
  // return merged;
  // }

  /**
   * Closes the stream for communication (only for TCP).
   */
  public void terminate() throws PeerToPeerException {
    /*
     * We start by closing the input and output streams since we're not writing
     * or reading from it anymore. Finally we close the socket.
     */
    try {
      // only TCP is connected to the other end
      if (_protocol == TCP) {

        if (!_active && !_tcp_socket_listen.isClosed())
          _tcp_socket_listen.close();

        if (_tcp_socket.isConnected()) {
          _tcp_socket.shutdownOutput(); // _output.close();
          _tcp_socket.shutdownInput(); // _input.close();
          _tcp_socket.close();
        }
      }
      // UDP
      else
        _udp_socket.close();

    } catch (Exception e) {
      throw new PeerToPeerException(PeerToPeerException.Types.SOCKET_CLOSE);
    }
  }

  /**
   * Reads data from the socket, and fills the _buffer field, updating _length
   * accordingly.
   */
  public byte[] receive() throws PeerToPeerException {
    try {
      /* TCP */
      if (_protocol == TCP) {
        _length = _input.read(_buffer); // block for the first time
        while (_input.available() > 0)
          _length += _input.read(_buffer, _length, _input.available());
      }

      /* UDP */
      else {
        _udp_socket.receive(_udp_datagram);
        _length = _udp_datagram.getLength();
        _endpoint = (InetSocketAddress)_udp_datagram.getSocketAddress();
      }
    } catch (SocketTimeoutException e1) {
      throw new PeerToPeerException(PeerToPeerException.Types.SOCKET_TIMEOUT);
    } catch (IOException e2) {
      throw new PeerToPeerException(PeerToPeerException.Types.SOCKET_RECEIVE, e2.getMessage());
    }

    if (_length < 0)
      throw new PeerToPeerException(PeerToPeerException.Types.SOCKET_RECEIVE);

    byte[] received = new byte[_length];
    for (int i = 0; i < _length; i++)
      received[i] = _buffer[i];
    return received;
  }

  /**
   * Sends a byte-array (message) to this peer.
   * 
   * @param message byte-array to send.
   */
  public void send(byte[] message) throws PeerToPeerException {
    try {

      /* TCP */
      if (_protocol == TCP) {
        _output.write(message, 0, message.length);
        _output.flush();
      }

      /* UDP */
      else {
        if (_endpoint != null)
          _udp_socket.send(new DatagramPacket(message, message.length, _endpoint));
        else
          _udp_socket.send(new DatagramPacket(message, message.length, _udp_datagram
              .getSocketAddress()));
      }

    } catch (IOException e) {
      // System.err.println("! send failed!");
      // e.printStackTrace();
      throw new PeerToPeerException(PeerToPeerException.Types.SOCKET_SEND, e.getMessage());
    }
  }

}
