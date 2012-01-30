#include "TimeTrace.h"

namespace monitor
{
  
  vector<time_t*> TimeTrace::_time_vec;
#ifdef HAVE_LIBPAPI
  bool TimeTrace::_papi_initialized = false;
#endif
  //////////////////////////////////////////////////////////////////////
  // CONSTRUTORES                                                     //
  //////////////////////////////////////////////////////////////////////
  
  TimeTrace::TimeTrace(pid_t pid) :
    TracePID(pid)
#   ifdef HAVE_LIBPAPI
    ,_event_set(PAPI_NULL)
#   endif
  {

#ifdef HAVE_LIBPAPI
    if (_papi_initialized == false) {
      /* PAPI initialization */
      if (PAPI_library_init(PAPI_VER_CURRENT) != PAPI_VER_CURRENT) {
	fprintf(stderr, "Try loading the perfctr module and/or "
		"check for its device permissions:\n");
	fprintf(stderr, "> sudo modprobe perfctr\n");
	fprintf(stderr, "> sudo chmod 644 /dev/perfctr\n");
	exit(1);
      }
      _papi_initialized = true;
    }

    /* PAPI monitoring */
    if (!(_papi("PAPI_create_eventset", PAPI_create_eventset(&_event_set)) &&
	  _papi("PAPI_add_event",PAPI_add_event(_event_set, PAPI_TOT_CYC)) &&
	  _papi("PAPI_attach",      PAPI_attach(_event_set, _pid))         &&
	//_papi("PAPI_thread_init", PAPI_thread_init(pthread_self))        &&
	  _papi("PAPI_start",       PAPI_start(_event_set)))) {
      cout << "WARNING: CPU usage will not be measured PID " << _pid << " (children=" << _time_vec.size() << ")" << endl;
      
      /* We don't exit if PAPI has created time structures. 
       * There is a limit of 1018 children, so beyond that we don't count time,
       * but don't exit either */
      if (_time_vec.size() < 1)
    	  exit(1);  
    }
    _register_cannot_proceed();
#endif

    _register_signal(0);         // measure time upon signals
    //_register_syscall(0, false); // and syscall exits
    _register_user();

    _time               = new time_t;
    _time->pid          = pid;
    _time->cpu_time     = 0;
    _time->cpu_glibtime = 0;
    _time->chrono_start = _current_time();
    _time->chrono_last  = _time->chrono_start;
    _time_vec.push_back(_time);
    
  }
  
  TimeTrace::~TimeTrace() {
    cout << "########## TimeTrace destructor called ##########" << endl;
#ifdef HAVE_LIBPAPI
    /* these calls are mandatory because for each attach, the monitor opens
       a vperfctr file, which is only closed after PAPI_detach() */
    _papi("PAPI_detach"         ,  PAPI_detach(_event_set));
    _papi("PAPI_cleanup_eventset", PAPI_cleanup_eventset(_event_set));
    _papi("PAPI_destroy_eventset", PAPI_destroy_eventset(&_event_set));
#endif
  }

  void TimeTrace::cleanup() {
    vector<time_t*>::iterator iter = _time_vec.begin();
    while (iter != _time_vec.end()) {
      delete (*iter);
      _time_vec.erase(iter);
    }
  }
  
  //////////////////////////////////////////////////////////////////////
  // METODOS                                                          //
  //////////////////////////////////////////////////////////////////////
  
  
  void TimeTrace::trace(const status_t &status) {
    if (_is_registered(status)) {
      /* update cpu time */
#     ifdef HAVE_LIBPAPI
      /* PAPI */
      if (cannot_proceed(status))
    	  _papi("PAPI_stop", PAPI_stop(_event_set, &_time->cpu_time));
      else
    	  _papi("PAPI_read", PAPI_read(_event_set, &_time->cpu_time));
#     endif
      /* glibtop */
      /* Upon every signal or exits from syscalls */
      if (!cannot_proceed(status)) {
      
	glibtop_get_proc_time(&_glib_proc_time, _pid);
	_time->cpu_glibtime = maximum(_time->cpu_glibtime, _glib_proc_time.rtime);
      }

      /* update wall time */
      _time->chrono_last = _current_time();
    }
  }
  
  string TimeTrace::toString() {
    vector<time_t*>::iterator iter = _time_vec.begin();
    ostringstream o;
    long wall_time           = 0;  // max "real" time of any process (parent)
    long long total_cpu_time = 0;  // total cpu time of all processes
    long long total_cpu_glibtime = 0;  // total cpu time of all processes

    while (iter != _time_vec.end()) {
      wall_time = maximum(wall_time,
			  (*iter)->chrono_last - (*iter)->chrono_start);
      //printf("--------- %d %lld\n", (*iter)->pid, (*iter)->cpu_time);
      total_cpu_time     += (*iter)->cpu_time;
      total_cpu_glibtime += (*iter)->cpu_glibtime;
      iter++;
    }
	  
    o << "     time: " << "\t" <<  total_cpu_time << "\t" <<  total_cpu_glibtime << "\t" <<  wall_time;
    return o.str();
  }

  Bytes TimeTrace::Data() {
    vector<time_t*>::iterator iter = _time_vec.begin();
    Bytes data;
    long wall_time = 0;            // max "real" time of any process (parent)
    long long total_cpu_time = 0;  // total cpu time of all processes
    unsigned long total_cpu_glibtime = 0;  // total cpu time of all processes

    while (iter != _time_vec.end()) {
      wall_time = maximum(wall_time,
			  (*iter)->chrono_last - (*iter)->chrono_start);
      total_cpu_time     += (*iter)->cpu_time;
      total_cpu_glibtime += (*iter)->cpu_glibtime;
      iter++;
    }
    //data.add((byte*) &total_cpu_time,   8);
    data.addLong(total_cpu_time);
    data.addInteger(total_cpu_glibtime, 4);
    data.addInteger(wall_time,          4);
    return data;
  }


  ///////////////////////////////////////////////////////////////
  // PAPI FUNCTIONS
  ///////////////////////////////////////////////////////////////
#ifdef HAVE_LIBPAPI
  bool TimeTrace::_papi(const char* debug_name, int papi_retval) {
    char error_name[256+1];
    if (papi_retval != PAPI_OK) {
      printf("####################### PAPI_ERROR #######################\n");
      PAPI_perror(papi_retval, error_name, 256);
      fprintf(stderr, "%s: ", debug_name);
      perror(error_name);
      printf("##########################################################\n");
      return false;
    }
    return true;
  }
#endif

  ///////////////////////////////////////////////////////////////
  // GETTIMEOFDAY (Wall Time)
  ///////////////////////////////////////////////////////////////
  /**
   * returns milliseconds
   */
  long long TimeTrace::_current_time() {
    struct timeval current_time;
    gettimeofday(&current_time, NULL);
    return current_time.tv_sec*1000 + current_time.tv_usec/1000;
  }

  
};


