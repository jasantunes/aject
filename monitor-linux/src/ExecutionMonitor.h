#ifndef EXECUTIONMONITOR_H_
#define EXECUTIONMONITOR_H_

#include "Basic.h"

#include <sys/errno.h>
#include <errno.h>
#include <unistd.h>         // fork()
#include <sys/types.h>      // fork() wait()
#include <wait.h>	    //        wait()
#include <stdlib.h>         // exit()

#include <sys/ptrace.h>     // ptrace
#include <sys/user.h>     // struct user_regs_struct uregs;
#include <linux/unistd.h>   // __NR_exit
#include <linux/ptrace.h>   // EAX
#include <pthread.h>   // threads


#include <iostream>     // PARA TIRAR
#include <vector>

#include "TimeTrace.h"
#include "MemTrace.h"
#include "DiskTrace.h"



using namespace std;



namespace monitor
{

  typedef struct {
    pid_t            pid;
    unsigned short   id;   // nth pid (1 = 1st child, 2 = 2nd child, etc.)
    bool             is_running;

    /* Resource Usage */
    TimeTrace*       time_tracing;
    MemTrace*        mem_tracing;
    DiskTrace*       disk_tracing;
  } child_t;

  
  enum thread_action_t { START=1, UPDATE, DONE };

 class ExecutionMonitor
    {
    private:
      pthread_mutex_t        _mutex;
      pid_t                  _pid;              // pid of the target process
      static vector<child_t> _children_vec;
      vector<status_t>       _status_vec;
      unsigned int           _max_running_children;
      int                    _last_syscall;

      bool                   _standalone;
      int                    _PTRACE_RESUME;    // PTRACE_CONT | PTRACE_SYSCALL

      /* thread: get monitoring data */
      static thread_action_t _action;
      Signalize              _sig;
      
    public:
      /* constructor and destructor */
      ExecutionMonitor();
      virtual            ~ExecutionMonitor();
      void                cleanup();
      void                clearMonitoring();

      /* main methods */
      void                exec(char** full_command);

      /* methods related with the tracing components */
      void                setStandalone(bool standalone=true);
      bool                start(pid_t pid, Signalize* signalize);
      bool                stop();
      string              toString();
//      void                clearSignals();
      Bytes               getData();
      bool                wait_for_SIGCONT();



    private:
      /* status_t */
      status_t            _status();
      bool                _resume(const status_t &proc_status);
      static string       _toString(const status_t &proc_status);

      /* child_t */
      child_t*            _get_child(pid_t pid);
      child_t&            _create_child(pid_t pid);
      bool                _stop_child(child_t& child);
//      bool                _kill_child(child_t& child);
      static void         _update_child(child_t& child, const status_t& status);
      static void         _update_children(const status_t& status);
      unsigned int        _children_running();
      static bool         _ptrace(pid_t pid, int request);
      static bool         _ptrace(pid_t pid, int request, void *param);
      
      static void         _handle_forceUpdate();
      
    };



};

#endif //EXECUTIONMONITOR_H_
