#include "MonitorController.h"

namespace monitor {
	
	
  //////////////////////////////////////////////////////////////////////
  // CONSTRUTORES                                                     //
  //////////////////////////////////////////////////////////////////////

  /**
   * Class constructor. The 'peer'parameter is the PeerToPeer instance
   * (AJECT or MONITOR) used in the synchronization.
   */
  MonitorController::MonitorController(int local_port)
    :   PeerToPeer(PeerToPeer::UDP, local_port, BUFFER_SIZE)
  {
    open();
    _current_attack = 0;
  }
    
  MonitorController::~MonitorController()
  {
    terminate();
  }

  //////////////////////////////////////////////////////////////////////
  // GETS / SETS                                                      //
  //////////////////////////////////////////////////////////////////////
  unsigned int MonitorController::CurrentAttack() { return _current_attack; }

  //////////////////////////////////////////////////////////////////////
  // METODOS                                                          //
  //////////////////////////////////////////////////////////////////////

  /**
   * This method is responsible for handling synchronization protocol
   * messages.
   * @return Returns the type of message received.
   */
  int MonitorController::synchronizeAttack() {
    short type = 0;
    Bytes received = recFrom();

    type            = btoi(&received.data[0], 2);
    _current_attack = btoi(&received.data[MC_FIELD1_SIZE], 4);

    if (type == MC_SYNC_RESET && _current_attack == 0)
      type = MC_EXIT_MONITOR;
    
    return type;
  }
  
  
  /**
   * Metodo responsavel por enviar uma mensagem de ACK (acusar a recepcao
   * de uma mensagem recebida) ao injector de ataques.
   * Para ser chamada pelo Monitor.
   */
  bool MonitorController::sendACK() {
    Bytes ack(MC_FIELD1_SIZE + MC_FIELD2_SIZE); // 2 + 4 bytes
    ack.addInteger(MC_SYNC_ACK,       MC_FIELD1_SIZE); // 2 bytes
    ack.addInteger(_current_attack,   MC_FIELD2_SIZE); // 4 bytes
    return sendTo(ack);
  }
  /**
   * Metodo responsavel por enviar uma mensagem de ACK + dados de
   * monitorizacao.
   */
  bool MonitorController::sendMonitoringData(const Bytes& data) {
    Bytes ack(MC_FIELD1_SIZE + MC_FIELD2_SIZE + data.length);
    ack.addInteger(MC_SYNC_DATA,      MC_FIELD1_SIZE);
    ack.addInteger(_current_attack,   MC_FIELD2_SIZE);
    ack += data;
    return sendTo(ack);
  }

};




