#include "PeerToPeer.h"

namespace communication {

    //////////////////////////////////////////////////////////////////////
    // CONSTRUTORES                                                     //
    //////////////////////////////////////////////////////////////////////

    /**
     * Construtor da classe. Inicializa uma socket activa (cliente).
     * @param protocol    Protocolo de comunicacao: TCP ou UDP.
     * @param locat_port  Porto local.
     * @param IP          Endereco IP (XX.XX.XX.XX ou nome da maquina).
     * @param port        Porto remoto.
     */
    PeerToPeer::PeerToPeer(int protocol, string IP, int port, int max_len)
    {
        _active = true;
        _hostname = string();
        _protocol = protocol;

        /* set socket */
        _socket = socket(AF_INET, (protocol == TCP)?SOCK_STREAM:SOCK_DGRAM, 0);
        int reuse = 1;
        setsockopt(_socket, SOL_SOCKET, SO_REUSEADDR, (const char*)&reuse, sizeof(reuse));

        /* set buffer */
        _MAX_LEN = max_len;
        _buffer = new byte[_MAX_LEN]();

        /* local peer */
        bzero(&_local_addr, sizeof(_local_addr));
        _local_addr.sin_family = AF_INET;
        _local_addr.sin_addr.s_addr = INADDR_ANY;
        _local_addr.sin_port = htons(0);

        /* remote peer */
        bzero(&_remote_addr, sizeof(_remote_addr));
        _remote_addr.sin_family = AF_INET;

        /* set remote IP address */
        setRemoteAddress(IP, port); //TODO: Levantar excepcao se da falso
    }


    /**
     * Construtor da classe. Inicializa uma socket passive
     * (de escuta ou servidor).
     * @param protocol    Protocolo de comunicacao: TCP ou UDP.
     * @param locat_port  Porto local.
     */
    PeerToPeer::PeerToPeer(int protocol, int local_port, int max_len)
    {
        _active = false;
        _hostname = string();
        _protocol = protocol;

        /* set socket */
        int reuse = 1;
        if (protocol == TCP) {
            _socket_tcp_listen = socket(AF_INET, SOCK_STREAM, 0);
            setsockopt(_socket_tcp_listen, SOL_SOCKET, SO_REUSEADDR, (const char*)&reuse, sizeof(reuse));
        } else {
            _socket = socket(AF_INET, SOCK_DGRAM, 0);
            setsockopt(_socket, SOL_SOCKET, SO_REUSEADDR, (const char*)&reuse, sizeof(reuse));
        }

        /* set buffer */
        _MAX_LEN = max_len;
        _buffer = new byte[_MAX_LEN]();

        /* local peer */
        bzero(&_local_addr, sizeof(_local_addr));
        _local_addr.sin_family = AF_INET;
        _local_addr.sin_addr.s_addr = htonl(INADDR_ANY);
        _local_addr.sin_port = htons(local_port);

        /* remote peer */
        bzero(&_remote_addr, sizeof(_remote_addr));
        _remote_addr.sin_family = AF_INET;

    }

    PeerToPeer::~PeerToPeer() { delete [] _buffer; }



    //////////////////////////////////////////////////////////////////////
    // GETS e SETS                                                      //
    //////////////////////////////////////////////////////////////////////

    string PeerToPeer::RemoteName() {
        if (_hostname.empty() ) _hostname = resolveRemoteName();
        return _hostname;
    }
    string PeerToPeer::RemoteAddress() { return string(inet_ntoa(_remote_addr.sin_addr)); }

    unsigned short PeerToPeer::LocalPort() { return ntohs(_local_addr.sin_port); }

    unsigned short PeerToPeer::RemotePort() { return ntohs(_remote_addr.sin_port); }

    int PeerToPeer::MaxLength() { return _MAX_LEN; }

    int PeerToPeer::Protocol() { return _protocol; }

    bool PeerToPeer::isActive() { return _active; }

    int PeerToPeer::getSocket() {
        return (_protocol == TCP && !_active) ? _socket_tcp_listen : _socket;
    }



    //////////////////////////////////////////////////////////////////////
    // METODOS                                                          //
    //////////////////////////////////////////////////////////////////////

    /**
     * Funcao que obtem o endereco do servidor atraves do argumento (string)
     * @param IP        string sob a forma de IP ou de nome da maquina
     */
    bool PeerToPeer::setRemoteAddress(string IP, int port) {
        struct hostent *servidor;
        bool error = false;

        /* IP = hostname */
		servidor = gethostbyname(IP.c_str());

		/* IP = AA.BB.CC.DD */
        if (servidor == NULL) {
			if (inet_aton(IP.c_str(), &_remote_addr.sin_addr) > 0) {

				servidor = gethostbyaddr ((char *) &_remote_addr.sin_addr.s_addr,
					sizeof(_remote_addr.sin_addr.s_addr), AF_INET);
				if (servidor == NULL) error = true;
			} else error = true;
        }

        if (error) {
	        cerr << "IP em formato errado!: " + IP << endl;
            switch (h_errno) {
            	case HOST_NOT_FOUND:
            	cerr << "The specified host is unknown." << endl;
            	break;
            	case NO_ADDRESS: // or case NO_DATA:
            	cerr << "The requested name is valid but does not have an IP address." << endl;
            	break;
            	case NO_RECOVERY:
            	cerr << "A non-recoverable name server error occurred." << endl;
            	break;
            	case TRY_AGAIN:
            	cerr << "A temporary error occurred on an authoritative name server.  Try again later." << endl;
            	break;
            }
            return false;
        }
        bcopy(servidor->h_addr, &_remote_addr.sin_addr.s_addr, servidor->h_length);

        _hostname = string(servidor->h_name);
        _remote_addr.sin_port = htons(port);

        return true;
    }

    /**
     * Abre o canal para comunicacao.
     * Obrigatorio antes de qualquer comunicacao.
     * O comportamento depende se é uma instância active ou passive.
     */
    bool PeerToPeer::open() {
        int result = 0;

        // UDP
         if (_protocol == UDP)
            result |= bind(_socket, (struct sockaddr*)&_local_addr, sizeof(struct sockaddr_in));

        // TCP
        else {

            // client (active == true)
            if (_active)
                result |= connect(_socket, (struct sockaddr *) &_remote_addr, sizeof(_remote_addr));

            // server (active == false)
            else {
                socklen_t size = sizeof(_remote_addr);
                result |= bind(_socket_tcp_listen, (struct sockaddr*)&_local_addr, sizeof(_local_addr)); // 0 OK | -1 NOK
                if (result == 0) {
                    result |= listen(_socket_tcp_listen, 1);  // 0 OK | -1 NOK
                    if (result == 0) {
                        cout << "waiting for connections..." << endl;
                        _socket = accept(_socket_tcp_listen, (struct sockaddr *) &_remote_addr, &size);
                        if (_socket < 0) {
                            result = -1;
                        }
                    }
                }
            }
        }

        if (result != 0) {
            cerr << "Open: " << strerror(errno) << endl;
            exit(-1);
            return false;
        } else {
            FD_ZERO(&_read_fds);
            FD_SET(_socket, &_read_fds);
            return true;
        }
    }


    /**
     * Fecha o canal para comunicacao. Nao e permitido mais nenhum tipo
     * de comunicacao: envio/recepcao.
     */
    void PeerToPeer::terminate() {
        if (_protocol == TCP && _active == false) {
            //shutdown(_socket_tcp_listen, SHUT_RDWR);
            shutdown(_socket_tcp_listen, 0);    // we have stopped reading data
            shutdown(_socket_tcp_listen, 1);    // we have stopped writing data
            shutdown(_socket_tcp_listen, 2);    // we have stopped using this socket
            close(_socket_tcp_listen);
        }

        //shutdown(_socket, SHUT_RDWR);
        shutdown(_socket, 0);    // we have stopped reading data
        shutdown(_socket, 1);    // we have stopped writing data
        shutdown(_socket, 2);    // we have stopped using this socket
        close(_socket);
    }

    Bytes PeerToPeer::recFrom() {
        socklen_t sock_size = sizeof(_remote_addr);

        int received_len = (_protocol == TCP)
        		? recv(_socket, _buffer, _MAX_LEN, 0)
            : recvfrom(_socket, _buffer, _MAX_LEN, 0, (sockaddr *) &_remote_addr, &sock_size);


        return Bytes(_buffer, received_len);
    }

    Bytes PeerToPeer::recWait(int millisecs) {
        struct timeval *timeout;
        bool received = false;

        if (millisecs > 0) {
            timeout = new struct timeval;
            timeout->tv_sec = millisecs/1000;
            timeout->tv_usec = millisecs%1000;
        }
        else
            timeout = NULL;

        if (select(_socket+1, &_read_fds, NULL, NULL, timeout) > 0)
        	received = FD_ISSET(_socket, &_read_fds);

    	if (timeout != NULL) delete timeout;
        return (received) ? recFrom(): Bytes(NULL, 0);
    }


    bool PeerToPeer::sendTo(Bytes &buffer) {
        u_int total_sent = 0;
        int sent = 0;
        u_int to_send = buffer.length;

        // ciclo de envio ate enviar todo o buffer
        do {
            // discrimicacao do tipo de envio (TCP ou UDP)
            sent = (_protocol == TCP)
                ? send(_socket, &buffer.data[total_sent], to_send, 0)
                : sendto(_socket, &buffer.data[total_sent], to_send, 0,
                    (sockaddr *) &_remote_addr, sizeof(_remote_addr));
            // em caso de erro sair
            if (sent <= 0) return false;

            //actualizacao
            total_sent += sent;
            to_send -= sent;
        } while (total_sent < buffer.length);
        dout << "sent " << total_sent << "/" << buffer.length << " bytes" << endl;
        return true;
    }


    //////////////////////////////////////////////////////////////////////
    // PRIVADOS                                                         //
    //////////////////////////////////////////////////////////////////////

    string PeerToPeer::resolveRemoteName() {
        struct hostent *servidor;

        servidor = gethostbyaddr((char *) &_remote_addr.sin_addr.s_addr,
								  sizeof(_remote_addr.sin_addr.s_addr), AF_INET);

        if ( servidor == NULL)  return string("wrong address");
        else                    return string(servidor->h_name);
    }
};
