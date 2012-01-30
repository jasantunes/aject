#include "Basic.h"

#include <vector>
#include "Monitor.h"

using namespace monitor;
using namespace std;

#define MONITOR_PORT       4001

/* signaling threads: MC_* message codes + */
#define SIGNAL_TARGET_STOP   10

MonitorController *mc;
Monitor *m;
Signalize _main_signal, _mon_signal;
Thread thread_monitoring, thread_sync;
bool _monitoring;

//////////////////////////////////////////////////////////////////////
// THREAD FUNCIONS                                                  //
//////////////////////////////////////////////////////////////////////

void thread_func_monitor() {
	while (true) {
		dout << "Waiting for MC_SYNC_RESET" << endl;
		_mon_signal.wait(MC_SYNC_RESET);
		dout << "Received MC_SYNC_RESET" << endl;
		m->start(&_mon_signal);
		//_main_signal.signal(SIGNAL_TARGET_STOP);
	}
	cout << "! ! ! ! !  quitting thread_monitor  ! ! ! ! !" << endl;
	pthread_exit(NULL);
}

void thread_func_sync() {
	int message_code;
	while (true) {
		cout << "______________________" << endl;
		message_code = mc->synchronizeAttack();
		cout << "SYNC: received msg_code: " << message_code << endl;
		_main_signal.signal(message_code);
	}
	//} while (message_code != MC_EXIT_MONITOR);
	cout << "! ! ! ! !  quitting thread_func_sync  ! ! ! ! !" << endl;
	pthread_exit(NULL);
}

//////////////////////////////////////////////////////////////////////
// MAIN                                                             //
//////////////////////////////////////////////////////////////////////
int main(int argc, char**argv) {
	bool exit_monitor = false;

	// 2 params
	if (argc < 2) {
		cout
				<< "usage: Monitor [MON_PORT] [--inetd <PORT>] <TARGET_PATH> [args...]"
				<< endl;
		cout << "  MON_PORT     \t Monitor's local port (sync with Injector)."
				<< endl;
		cout
				<< "               \t If not present, monitor launches target and keeps monitoring it."
				<< endl;
		cout << "  --inetd PORT \t Target is an inetd server" << endl;
		cout << "  TARGET_PATH  \t Target's executable" << endl;
		exit(1);
	}

	/* Initialize Monitor:
	 * MonitorController - communicates with AJECT
	 *           Monitor - monitors target
	 */
	int port = atoi(argv[1]);
	if (port == 0) {

		// standalone version
		m = new Monitor(&argv[1]);
		cout << "#####              STANDALONE VERSION              #####"
				<< endl;
		cout << "########################################################"
				<< endl;
		m->setStandalone();
		m->start(NULL);
		cout << "MONITOR REPORT:\n" << m->toString() << endl;

		exit_monitor = true;

	} else {
		// normal version (with injector)
		mc = new MonitorController(port);

		// --inetd
		if (strcmp(argv[2], "--inetd") == 0) {
			m = new Monitor(&argv[4]);
			m->setInetd(atoi(argv[3]));
		} else {
			m = new Monitor(&argv[2]);
		}
		cout << "listening on port " << port << endl;
		thread_monitoring.run(thread_func_monitor);
		thread_sync.run(thread_func_sync);
	}

	/* LOOP */
	while (!exit_monitor) {

		printf(" > idle <\n");
		switch (_main_signal.wait()) {

		case MC_SYNC_RESET:
			dout << "> MC_SYNC_RESET" << endl;
			if (m->isExecuting()) {
				m->stop();
				cout << "\t      Target stopped: OK" << endl;
			}

			// Send signal to monitoring thread to restart target process.
			_mon_signal.signal(MC_SYNC_RESET, Signalize::DONE);
			cout << "\t      Target started: OK" << endl;
			if (!mc->sendACK())
				cout << "\t            ACK sent: ERROR" << endl;
			dout << "< MC_SYNC_RESET" << endl;
			break;

		case MC_SYNC_DATA:
			dout << "> MC_SYNC_DATA" << endl;
			if (m->monitor_process()) {
				cout << m->toString() << endl;
				if (mc->sendMonitoringData(m->getData())) {
					m->clearMonitoring(); // Clearing monitoring data at each sync.
					cout << "\tMonitoring data sent: OK" << endl;
				} else
					cout
							<< "\tMonitoring data sent: ERROR (sendMonitoringData)"
							<< endl;

			} else
				cout << "\tMonitoring data sent: ERROR (forceUpdate)" << endl;

			dout << "< MC_SYNC_DATA" << endl;
			break;

		case SIGNAL_TARGET_STOP:
			dout << "> SIGNAL_TARGET_STOP" << endl;
			if (m->isExecuting()) {
				m->stop();
				cout << "\t      Target stopped: OK" << endl;
			} else
				cout << "\t       Target exited: OK" << endl;
			dout << "< SIGNAL_TARGET_STOP" << endl;
			break;

		case MC_EXIT_MONITOR:
			cout << "\t   Injector finished: OK" << endl;
			if (m->isExecuting()) {
				m->stop();
				cout << "\t      Target stopped: OK" << endl;
			}
			if (!mc->sendACK())
				cout << "\t            ACK sent: ERROR" << endl;
			//thread_sync.terminate();
			exit_monitor = true;
			break;

		}
	}

	/* exit */
	//thread_sync.terminate();
	delete mc;
	delete m;

	return EXIT_SUCCESS;
}

