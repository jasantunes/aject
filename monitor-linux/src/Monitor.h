#ifndef _MONITOR_H_
#define _MONITOR_H_

#include "Basic.h"
#include <unistd.h>
#include <sys/errno.h>
#include <fstream>
#include <iostream>

#include "PeerToPeer.h"
#include "MonitorController.h"
#include "ExecutionMonitor.h"



#define OUTPUT_FILE_SIGNALS  "signals.log"

using namespace monitor;
using namespace std;

namespace monitor {

  string toString(int value);

  class Monitor
    {
    private:                // campos
      pid_t                 _pid;
      char**                _target_command;
      bool                  _inetd;
      bool                  _inetd_setup;
      int                   _sock_inetd;
      ExecutionMonitor      _exec_monitor;

      Thread                _thread;


    public:                 // construtores e destrutor
      Monitor(char** full_command);
      virtual              ~Monitor();

      void                  setStandalone(bool standalone=true);
      bool                  start(Signalize* signalize);
      bool                  stop();
      string                toString();
      Bytes                 getData();
      bool                  monitor_process();
      void					clearMonitoring();
      void					cleanup();

      void		    setAttackId(int attack_id);
      pid_t		    PID();
      bool                  isExecuting();

      bool                  setInetd(int inetd_port);
      static void           initializeData();

    private:
      int                   _listenInetd();

    };

};
#endif //_MONITOR_H_
