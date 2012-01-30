#ifndef _TRACE_H_
#define _TRACE_H_

#include "Basic.h" // itos
#include <sys/signal.h>
#include <sys/types.h>
#include <iostream>
#include <sstream>
#include <string>
#include <vector>



using namespace std;

namespace monitor {

  // SYSCALL codes in /usr/include/asm-i386/unistd.h
  // SIGNAL  codes in /usr/include/asm-i386/signal.h
  enum status_type_t {UNKNOWN=0, EXITED=1, TERMINATED, SIGNAL, SYSCALL, USER};
  
  /********************************************************
   * status_t
   ********************************************************/
  typedef struct {
    pid_t           pid;
    unsigned short  id;   // nth pid (1 = 1st child, 2 = 2nd child, etc.)
    status_type_t   type; // UNKNOWN, EXITED, TERMINATED, SIGNAL, SYSCALL, USER
    int             code; // status, signal, or system call (see type)
    bool            in_syscall;
  } status_t;
  
  status_t  _status_t(status_type_t type, int code, bool in_syscall);

  bool     cannot_proceed(const status_t& status);
  string   status_to_string(const status_t &status);
  ostream& operator<<(ostream& out, const status_t& status);


  
  
  /********************************************************
   * TracePID
   ********************************************************/
  class TracePID
    {
    
    protected:                 // campos
      int                      _pid;
      vector<status_t>         _registered_status;
      vector<status_t>         _ignored_status;
      
      /********************************************************
       * API
       ********************************************************/
    public:
      TracePID(pid_t pid);
      virtual               ~TracePID();
      pid_t                 PID();

      virtual void          trace(const status_t &status) = 0;
      //virtual string        Data()   = 0;  // returns string representation

      
    protected:
      bool                  _is_registered(const status_t &status);
      bool                  _is_in_ignored_status(const status_t &status);
      bool                  _is_in_registered_status(const status_t &status);

      /* to be traced */
      void                  _register(const status_t &status);
      void                  _register_status(status_type_t  type,
					     int            code,
					     bool           in_syscall);
      void                  _register_signal(int signal);
      void                  _register_syscall(int syscall, bool when_entering);
      void                  _register_cannot_proceed();
      void                  _register_user();

      /* to be ignored */
      void                  _ignore(const status_t &status);
      void                  _ignore_status(status_type_t  type,
					     int            code,
					     bool           in_syscall);
      void                  _ignore_signal(int signal);
      void                  _ignore_syscall(int syscall, bool when_entering);
      void                  _ignore_cannot_proceed();

    };

};
#endif //_TRACE_H_
