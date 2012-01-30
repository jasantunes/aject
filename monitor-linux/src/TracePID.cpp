
#include "TracePID.h"

namespace monitor {

  /********************************************************
   * status_t
   ********************************************************/
  status_t _status_t(status_type_t type, int code, bool in_syscall) {
    status_t status;
    status.pid        = 0;
    status.id         = 0;
    status.type       = type;
    status.code       = code;
    status.in_syscall = in_syscall;
    return status;
  }

  bool cannot_proceed(const status_t& status){
    return (status.type == EXITED     ||
	    status.type == TERMINATED ||
	    (status.type == SIGNAL    && status.code == SIGSEGV));
  }


  string status_to_string(const status_t &status) {
    string type;
    switch (status.type) {
    case TERMINATED: type = "TERM"; break;
    case EXITED:     type = "EXIT"; break;
    case SIGNAL:     type = "SIGN"; break;
    case SYSCALL:    type = "SYSC"; break;
    case USER:       type = "USER"; break;
    default:         type = "????"; break;
    }

    // e.g.: [1]SYSC(200)
    return "["+utils::itos(status.id)+"]" + type + "("+utils::itos(status.code)+")";
  }
  ostream& operator<<(ostream& out, const status_t& status) {
	  out << status_to_string(status);
	  return out;
  }



  /********************************************************
   * TracePID API
   ********************************************************/
  TracePID::TracePID(pid_t pid) : _pid(pid) { }
  TracePID::~TracePID() { }
  pid_t TracePID::PID() { return _pid; }



  /* status_t being traced */
  bool TracePID::_is_in_registered_status(const status_t &status) {
    vector<status_t>::iterator start = _registered_status.begin();

    if (start == _registered_status.end()) return true;

    while (start != _registered_status.end()) {
      if ((                            (*start).type == status.type) &&
	  ((*start).code == 0     ||       (*start).code == status.code) &&
	  (status.type != SYSCALL || (*start).in_syscall == status.in_syscall))
	return true;
      start++;
    }
    return false;
  }
  bool TracePID::_is_in_ignored_status(const status_t &status) {
    vector<status_t>::iterator start = _ignored_status.begin();

    while (start != _ignored_status.end()) {
      if ((                                (*start).type == status.type) &&
	  ((*start).code == 0     ||       (*start).code == status.code) &&
	  (status.type != SYSCALL || (*start).in_syscall == status.in_syscall))
	return true;
      start++;
    }
    return false;
  }
  bool TracePID::_is_registered(const status_t &status) {
    return (_is_in_registered_status(status) && !_is_in_ignored_status(status));
  }



  void TracePID::_register(const status_t &status) {
      _registered_status.push_back(status);
  }

  void TracePID::_register_status(status_type_t type, int code, bool in_syscall) {
    _register(_status_t(type, code, in_syscall));
  }

  void TracePID::_register_signal(int signal) {
    _register(_status_t(SIGNAL, signal, false));
  }

  void TracePID::_register_syscall(int syscall, bool when_entering) {
    _register(_status_t(SYSCALL, syscall, when_entering));
  }
  void TracePID::_register_cannot_proceed() {
    _register(_status_t(EXITED,     0,       false));
    _register(_status_t(TERMINATED, 0,       false));
    _register(_status_t(SIGNAL,     SIGSEGV, false));
  }

  void TracePID::_register_user() {
    //_register(_status_t(USER, 0, false));
    _register(_status_t(SIGNAL, SIGCONT, false));
  }

  void TracePID::_ignore(const status_t &status) {
      _ignored_status.push_back(status);
  }
  void TracePID::_ignore_status(status_type_t type, int code, bool in_syscall) {
    _ignore(_status_t(type, code, in_syscall));
  }

  void TracePID::_ignore_signal(int signal) {
    _ignore(_status_t(SIGNAL, signal, false));
  }

  void TracePID::_ignore_syscall(int syscall, bool when_entering) {
    _ignore(_status_t(SYSCALL, syscall, when_entering));
  }
  void TracePID::_ignore_cannot_proceed() {
    _ignore(_status_t(EXITED,     0,       false));
    _ignore(_status_t(TERMINATED, 0,       false));
    _ignore(_status_t(SIGNAL,     SIGSEGV, false));
  }

};

