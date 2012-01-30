#ifndef _MONITORCONTROLLER_H_
#define _MONITORCONTROLLER_H_

#include "Basic.h"
#include "PeerToPeer.h"


using namespace std;
using namespace communication;
using namespace utils;


/* sync protocol */

/* 1 packet with 2 fields */
#define  MC_FIELD1_SIZE  2
#define  MC_FIELD2_SIZE  4

/* sent by:                      [i] injector   [m] monitor */
#define  MC_SYNC_RESET   1    // [i] [stop target and monitoring] execute target
#define  MC_SYNC_DATA    2    // [i] [m] get/send monitoring data
#define  MC_SYNC_ACK     3    // [i] [m] acknowledge last message

#define  MC_EXIT_MONITOR -1    // [i] quit monitor (not in protocol)

#define  BUFFER_SIZE	(70  + 1000)		// (2+4) + (64) + (2*500)   msg spec + resources + 500 signals 

namespace monitor {
  
  class MonitorController : public PeerToPeer
    {
    private:
      int                 _current_attack;
        
    
    public:               // get e sets
      MonitorController(int local_port);
      virtual             ~MonitorController();
      unsigned int        CurrentAttack();

        
    public:               // metodos publicos
      int                 synchronizeAttack();
      bool                sendACK();
      bool                sendMonitoringData(const Bytes& data);
    
    };

};

#endif //_MONITORCONTROLLER_H_
