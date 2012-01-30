#include "Monitor.h"

namespace monitor {

//////////////////////////////////////////////////////////////////////
// CONSTRUTORES                                                     //
//////////////////////////////////////////////////////////////////////
Monitor::Monitor(char** full_command) :
	_pid(0), _target_command(full_command), _inetd(false), _inetd_setup(false) {
	cout << "########################################################" << endl;
	cout << "#####               MONITOR STARTED                #####" << endl;
	dout << "#####                DEBUG VERSION                 #####" << endl;
	cout << "########################################################" << endl;
}

Monitor::~Monitor() {
}

//////////////////////////////////////////////////////////////////////
// INETD                                                            //
//////////////////////////////////////////////////////////////////////

bool Monitor::setInetd(int inetd_port) {
	struct sockaddr_in local_addr;

	_inetd = true;
	_sock_inetd = socket(AF_INET, SOCK_STREAM, 0);

	/* local peer */
	bzero(&local_addr, sizeof(local_addr));
	local_addr.sin_family = AF_INET;
	local_addr.sin_addr.s_addr = INADDR_ANY;
	local_addr.sin_port = htons(inetd_port);
	if (bind(_sock_inetd, (struct sockaddr*)&local_addr, sizeof(local_addr))!=0) {
		perror("setInetd");
		return false;
	} else {
		cout << "inetd server port = " << inetd_port << endl;
		return true;
	}
}

int Monitor::_listenInetd() {
	if (listen(_sock_inetd, 1) == 0) {
		return accept(_sock_inetd, 0, 0);
	} else {
		perror("listenInetd");
		return 0;
	}
}

//////////////////////////////////////////////////////////////////////
// FIELDS
//////////////////////////////////////////////////////////////////////
pid_t Monitor::PID() {
	return _pid;
}
bool Monitor::isExecuting() {
	return (_pid != 0);
}

//////////////////////////////////////////////////////////////////////
// METHODS                                                          //
//////////////////////////////////////////////////////////////////////
bool Monitor::start(Signalize *signalize) {
	int socket_inetd = 0;

	// TODO: Nao deve ser bloqueante e deve suportar TCP/UDP.
	if (_inetd)
		socket_inetd = _listenInetd();

	switch (_pid = fork()) {
	case -1:
		/* fork error */
		perror("fork");
		if (_inetd)
			close(socket_inetd);
		return false;

	case 0:
		/* child */
		if (_inetd) {
			dup2(socket_inetd, 0);
			dup2(socket_inetd, 1);
			dup2(socket_inetd, 2);
			close(socket_inetd);
		}
		cout << "      launched " << _target_command[0] << " (pid = " << getpid() << ")" << endl;

		_exec_monitor.exec(_target_command); // exec + ptrace
		// execvp(_target_command[0], _target_command);
		exit(0);
		break;

	default:
		/* parent */
		if (_inetd)
			close(socket_inetd);
		break;
	}

	return _exec_monitor.start(_pid, signalize);
}

bool Monitor::stop() {
	_pid = 0;
	return _exec_monitor.stop();
}
string Monitor::toString() {
	return _exec_monitor.toString();
}
Bytes Monitor::getData() {
	return _exec_monitor.getData();
}
bool Monitor::monitor_process() {
	return _exec_monitor.wait_for_SIGCONT();
}
void Monitor::clearMonitoring() {
	return _exec_monitor.clearMonitoring();
}
void Monitor::cleanup() {
	return _exec_monitor.cleanup();
}
void Monitor::setStandalone(bool standalone) {
	_exec_monitor.setStandalone(standalone);
}

}
;
