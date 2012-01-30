#ifndef _PEERTOPEER_H_
#define _PEERTOPEER_H_

#include "Basic.h"
#include <sys/types.h>
#include <sys/socket.h>
#include <arpa/inet.h>
#include <sys/time.h>
#include <netdb.h>
#include <string.h>
//#include <string>
#include <errno.h>
#include <iostream>

using namespace std;


#define cout cout << "<monitor> "


namespace communication {



    class PeerToPeer
    {
    public:
        enum Protocol {TCP = 0, UDP = 1};
    
        PeerToPeer(int protocol, string IP, int port, int max_len);
        PeerToPeer(int protocol, int local_port, int max_len);
        virtual             ~PeerToPeer();
         
    private:
        bool                _active;
        int                 _socket, _socket_tcp_listen;
        int                 _protocol;
        byte*				_buffer;
        struct sockaddr_in  _remote_addr;
        struct sockaddr_in  _local_addr;
        fd_set              _read_fds;  // File descriptors de leitura
        int                 _MAX_LEN;   // tamanho maximo de mensagens
        string              _hostname;
    

    public:
        //Bytes*				Buffer();
        //int               Length();
        string              RemoteName();
        string              RemoteAddress();
        string              Address();
        unsigned short      LocalPort();
        unsigned short      RemotePort();
        int                 MaxLength();
        int                 Protocol();
        bool                isActive();
        int                 getSocket();

    public:
        bool                open();
        void                terminate();
        Bytes 				recFrom();
        Bytes				recWait(int millisecs);
        //bool                sendTo(byte *buffer, int length);
        bool                sendTo(Bytes &buffer);
        
        
    private:
        bool                setRemoteAddress(string IP, int port);
        string              resolveRemoteName();
    };

};

#endif //_PEERTOPEER_H_

