#include "ExecutionMonitor.h"

namespace monitor {
vector<child_t> ExecutionMonitor::_children_vec;

//////////////////////////////////////////////////////////////////////
// CONSTRUTORES                                                     //
//////////////////////////////////////////////////////////////////////

ExecutionMonitor::ExecutionMonitor() :
	_pid(0), _status_vec(), _max_running_children(0), _last_syscall(0),
			_standalone(false), _PTRACE_RESUME(PTRACE_CONT), _sig() {
	pthread_mutex_init(&_mutex, NULL);
}

ExecutionMonitor::~ExecutionMonitor() {
	cout << "########## ExecutionMonitor destructor called ##########" << endl;
	cleanup();
}

void ExecutionMonitor::clearMonitoring() {
	_status_vec.clear();
	_status_vec.reserve(20);
}

void ExecutionMonitor::cleanup() {
	_PTRACE_RESUME = (_standalone) ? PTRACE_SYSCALL : PTRACE_CONT;

	for (u_int i = 0; i < _children_vec.size(); i++) {
		delete _children_vec.at(i).time_tracing;
		delete _children_vec.at(i).mem_tracing;
		delete _children_vec.at(i).disk_tracing;
	}
	_children_vec.clear();
	_status_vec.clear();
	_max_running_children = 0;

	TimeTrace::cleanup();
	MemTrace::cleanup();
	DiskTrace::cleanup();
}

//////////////////////////////////////////////////////////////////////
// METODOS                                                          //
//////////////////////////////////////////////////////////////////////
void ExecutionMonitor::setStandalone(bool standalone) {
	_standalone = standalone;
}

/**
 * Send SIGCONT to target process, which when the execution monitor intercepts,
 * will pause the target process, get monitoring information, and instruct it
 * to continue.
 */
bool ExecutionMonitor::wait_for_SIGCONT() {
	bool retval = false;
	//_sig.lock();
	if (_pid > 0 && kill(_pid, SIGCONT) == 0) {
		cout << "PAUSED target process " << _pid << endl;
		retval = _sig.wait(DONE);
		cout << "RESUMED target process " << _pid << endl;
	}
	//_sig.unlock();
	return retval;
}

bool ExecutionMonitor::start(pid_t pid, Signalize* signalize) {
	int status;
	child_t *child;
	_pid = pid;

	/* Reserve space for 20 traced signals and 5 children */
	cleanup();
	_children_vec.reserve(5);
	_status_vec.reserve(20);

	/* wait for the target process' SIGSTOP */
#   ifdef HAVE_LIBPAPI
	/* __WALL messes up glibtop_get_proc_time values for threaded apps */
	waitpid(_pid, &status, __WALL);
#   else
	waitpid(_pid, &status, 0);
#   endif

	/* target process is now paused, waiting to be instructed to resume exec */

	// set ptrace options: also trace its children
	if (!_ptrace(_pid, PTRACE_SETOPTIONS, (void*) (
#       ifdef HAVE_LIBPAPI
			//PTRACE_O_TRACECLONE |
			/* requires waitpid(-1, &status, __WALL) but messes up glibtop_get_proc_time
			 * values for threaded apps */
#       endif
			PTRACE_O_TRACECLONE | PTRACE_O_TRACEEXEC | PTRACE_O_TRACEFORK
					| PTRACE_O_TRACEVFORK))) {
		perror("PTRACE_SETOPTIONS");
		return false;
	}

	// create monitoring for the target process and force it to resume
	_create_child(_pid);
	if (!_ptrace(_pid, _PTRACE_RESUME)) {
		perror("PTRACE_RESUME");
		return false;
	}
	if (signalize != NULL)
		signalize->signal(Signalize::DONE);

	/* Continuously trace target process and children. */
	status_t proc_status;
	do {
		proc_status = _status();

		if (proc_status.pid == -1 || proc_status.pid < _pid) {
			dout << "!! Received status from pid " << proc_status.pid << " !!"
					<< endl;
			if (proc_status.pid > 1)
				_ptrace(proc_status.pid, PTRACE_DETACH);
			break;
			//			continue;
		}

		// Probe for monitoring data and continue.
		if (proc_status.type == SIGNAL && proc_status.code == SIGCONT) {
			_update_children(proc_status);
			_sig.signal(DONE);
			_PTRACE_RESUME = PTRACE_SYSCALL; // stop on next syscall
			continue;
		}

		pthread_mutex_lock(&_mutex);
		// Updates internal traced elements.
		if ((child = _get_child(proc_status.pid)) != NULL)
			_update_child(*child, proc_status);

		// Just log entries in syscalls, ignore exits.
		if (!(proc_status.type == SYSCALL && !proc_status.in_syscall)) {
			dout << "Logging " << proc_status << endl;
			_status_vec.push_back(proc_status);
		}
		pthread_mutex_unlock(&_mutex);

		//dout << proc_status << endl;
		//dout << toString() << endl;
	} while (_resume(proc_status));

	dout << ">>> could not resume because: " << proc_status << endl;

	// leave signal DONE for delayed signal_update()
	cout << "ExecutionMonitor.cpp: exiting start();" << endl;
	_sig.signal(DONE);
	cout << "ExecutionMonitor.cpp: exiting start()  [_sig.signal(DONE)];"
			<< endl;

	return true;
}

bool ExecutionMonitor::stop() {
	bool retval = true;

	cout << "Terminating " << _pid << endl;

	/* kill children and their tracing components */
	for (int i = _children_vec.size() - 1; i >= 0; i--) {
		dout << "killing " << _children_vec.size() << " children" << endl;
		if (_children_vec.at(i).is_running) {
			//_stop_child(_children_vec.at(i));
			//cout << _children_vec.at(i).pid << endl;
			retval &= _ptrace(_children_vec.at(i).pid, PTRACE_DETACH);
			retval &= _ptrace(_children_vec.at(i).pid, PTRACE_KILL);
			//retval &= (waitpid(_children_vec.at(i).pid, &status, 0) > 0);
		}
	}

	if (!_ptrace(_pid, PTRACE_DETACH)) {
		//perror("_ptrace(_pid, PTRACE_DETACH)");
		//retval = false;
	}

	if (!kill(_pid, SIGKILL) == 0) {
		perror("kill(_pid, SIGKILL)");
		retval = false;
	}

	/*
	 if (!(waitpid(_pid, NULL, 0) > 0)) {
	 perror("waitpid(_pid, NULL, 0)");
	 retval = false;
	 }
	 */

	/* return true if everything went ok */
	_pid = -1;
	return retval;
}

void ExecutionMonitor::exec(char** full_command) {
	_ptrace(0, PTRACE_TRACEME);
	kill(getpid(), SIGSTOP);
	execvp(full_command[0], full_command);
}

///////////////////////////////////////////////////////////////
// status_t
///////////////////////////////////////////////////////////////
bool ExecutionMonitor::_ptrace(pid_t pid, int request) {
	return _ptrace(pid, request, NULL);
}
bool ExecutionMonitor::_ptrace(pid_t pid, int request, void *param) {
	return (ptrace((__ptrace_request ) request, pid, 0, param) != -1);
}

status_t ExecutionMonitor::_status() {
	int status;
	struct user_regs_struct uregs;
	child_t *child = NULL;
	status_t proc_status;

#   ifdef HAVE_LIBPAPI
	proc_status.pid = waitpid(-1, &status, __WALL);
#   else
	proc_status.pid = wait(&status);
#   endif

	if (proc_status.pid == -1) {
		// TODO: debugging output
		perror("wait()");
		return proc_status;
	}

	/* Process status of process pid */
	if (WIFEXITED(status)) {
		proc_status.type = EXITED;
		proc_status.code = WEXITSTATUS(status);

	} else if (WIFSIGNALED(status)) {
		proc_status.type = TERMINATED;
		proc_status.code = WTERMSIG(status);

	} else if (WIFSTOPPED(status)) {
		proc_status.type = SIGNAL;
		proc_status.code = WSTOPSIG(status);

		/* trace PTRACE_EVENT's
		 if (proc_status.code == SIGTRAP) {
		 switch((status >> 16) & 0xffff) {
		 // just trace threads (clone)
		 case PTRACE_EVENT_FORK:
		 case PTRACE_EVENT_VFORK:
		 case PTRACE_EVENT_CLONE: // threads
		 default:  break;
		 }
		 }
		 */
		/* System call */
		if (_PTRACE_RESUME == PTRACE_SYSCALL && proc_status.code == SIGTRAP) {
			if (_ptrace(proc_status.pid, PTRACE_GETREGS, &uregs)) {
				proc_status.type = SYSCALL;
				proc_status.code = uregs.orig_eax;

				if (proc_status.code != _last_syscall) {
					// entering in syscall
					_last_syscall = proc_status.code;
					proc_status.in_syscall = true;
				} else {
					// exiting from syscall
					_last_syscall = 0;
					proc_status.in_syscall = false;
				}
			}
		}

	} else
		proc_status.type = UNKNOWN;

	if (proc_status.pid < _pid)
		return proc_status;

	/* Get reference to child tracing and update proc_status' id */
	if ((child = _get_child(proc_status.pid)) == NULL)
		child = &(child_t &) _create_child(proc_status.pid);

	proc_status.id = child->id;

	return proc_status;
}

bool ExecutionMonitor::_resume(const status_t &proc_status) {
	child_t *child = NULL;
	unsigned int running_children;

	if (proc_status.pid >= _pid) {

		// TODO: ignoring this for servers that auto-restart themselves
		// process has quitted: stop monitoring child
		if (cannot_proceed(proc_status)) {
			if ((child = _get_child(proc_status.pid)) != NULL)
				_stop_child(*child);
			_ptrace(_pid, PTRACE_KILL);
		}

		// normal signal/sys_call interception
		else
			_ptrace(proc_status.pid, _PTRACE_RESUME);
	}

	/* return false if there are no more children running */
	running_children = _children_running();
	if (running_children > _max_running_children)
		_max_running_children = running_children;
	return (running_children > 0);
}

///////////////////////////////////////////////////////////////
// PRINT FUNCTIONS
///////////////////////////////////////////////////////////////
string ExecutionMonitor::toString() {
	ostringstream o;
	o << " signals: " << "\t";
	for (u_int j = 0; j < _status_vec.size(); j++) {
		// ignore system call output AND SIGSTOP
		if (
#ifdef VERBOSE
				_status_vec.at(j).type == SYSCALL ||
#endif
				!(_status_vec.at(j).type == SIGNAL && _status_vec.at(j).code == SIGSTOP))
			o << status_to_string(_status_vec.at(j)) << " ";
	}
	o << endl;

	o << "resource\tcurrent\tmax\ttotal" << endl;
	o << "    procs: " << "\t" << _children_running() << "\t" // currently  procs running
			<< _max_running_children << "\t" // max procs
			<< _children_vec.size() << endl; // total procs

	o << TimeTrace::toString() << endl; // total time
	o << MemTrace::toString() << endl; // total memory
	o << DiskTrace::toString() << endl; // total disk
	return o.str();
}

Bytes ExecutionMonitor::getData() {
	int elems = 0;
	Bytes data, signals;

	pthread_mutex_lock(&_mutex);

	/* Log signals. */
	for (u_int i = 0; i < _status_vec.size(); i++) {
		// Ignore syscall output and SIGSTOP.
		if (!(_status_vec.at(i).type == SYSCALL || (_status_vec.at(i).type
				== SIGNAL && _status_vec.at(i).code == SIGSTOP))) {
			elems++;
			if (_status_vec.at(i).type == SIGNAL)
				signals.addInteger(_status_vec.at(i).code, 2);
			else
				signals.addInteger(-_status_vec.at(i).code, 2);
		}
	}
	data.addInteger(elems, 4);
	data += signals;

	/* Log processes. */
	data.addInteger(_children_running(), 4);
	data.addInteger(_max_running_children, 4);
	data.addInteger(_children_vec.size(), 4);

	/* Log resources. */
	data += TimeTrace::Data(); // total time
	data += MemTrace::Data(); // total memory
	data += DiskTrace::Data(); // total disk

	pthread_mutex_unlock(&_mutex);

	return data;
}

///////////////////////////////////////////////////////////////
// CHILDREN FUNCTIONS
///////////////////////////////////////////////////////////////
child_t* ExecutionMonitor::_get_child(pid_t pid) {
	child_t *child;
	for (u_int i = 0; i < _children_vec.size(); i++) {
		child = &(_children_vec.at(i));
		if (child->pid == pid)
			return child;
	}
	return NULL;
}

/*
 * Creates a new child_t in vector and returns its reference.
 */
child_t& ExecutionMonitor::_create_child(pid_t pid) {
	child_t new_child;
	new_child.pid = pid;
	new_child.id = _children_vec.size(); // incremented in push_back(new_child)
	new_child.is_running = true;

	/* tracing components */
	new_child.time_tracing = new TimeTrace(pid);
	new_child.mem_tracing = new MemTrace(pid);
	new_child.disk_tracing = new DiskTrace(pid);

	_children_vec.push_back(new_child);
	return _children_vec.back();
}

bool ExecutionMonitor::_stop_child(child_t& child) {
	bool retval = true;
	child.is_running = false;
	// return _ptrace(child.pid, PTRACE_DETACH, (void *)SIGSTOP);
	// retval = _ptrace(child.pid, PTRACE_DETACH,  (void *)SIGCONT);
	// retval &= _ptrace(child.pid, PTRACE_KILL);
	// if (!retval)   perror("PTRACE_KILL");
	retval &= _ptrace(child.pid, PTRACE_DETACH);
	// if (!retval)   perror("PTRACE_DETACH");
	// retval &= (waitpid(child.pid, NULL, 0) > 0);
	// if (!retval)   perror("waitpid()");
	return retval;
}

//bool ExecutionMonitor::_kill_child(child_t& child) {
//	bool retval;
//	child.is_running = false;
//	retval = _ptrace(child.pid, PTRACE_KILL);
//	if (!retval)
//		perror("_ptrace(child.pid, PTRACE_KILL)");
//	return retval;
//}

void ExecutionMonitor::_update_child(child_t& child, const status_t &status) {
	/* updates internal traced elems upon system call: mmap, clone, etc. */
	child.time_tracing->trace(status);
	child.mem_tracing->trace(status);
	child.disk_tracing->trace(status);
}

void ExecutionMonitor::_update_children(const status_t &status) {
	vector<child_t>::iterator iter = _children_vec.begin();
	vector<child_t>::iterator end = _children_vec.end();
	while (iter != end) {
		if ((*iter).is_running) {
			_update_child(*iter, status);
		}
		iter++;
	}
}

unsigned int ExecutionMonitor::_children_running() {
	unsigned int running = 0;
	for (u_int i = 0; i < _children_vec.size(); i++)
		if (_children_vec.at(i).is_running)
			running++;
	return running;
}

}
;
