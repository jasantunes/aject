#ifndef TIMETRACE_H_
#define TIMETRACE_H_

#include "Basic.h"

#ifdef HAVE_LIBPAPI
# include <papi.h>
#endif
# include <glibtop.h>
# include <glibtop/union.h>

#include <sys/time.h>  // gettimeofday
#include <time.h>      // gettimeofday

#include "TracePID.h"


using namespace std;


namespace monitor
{

  typedef struct {
    pid_t            pid;
    long long        cpu_time;     // PAPI_TOT_CYC
    unsigned long    cpu_glibtime; // glib_proc_time.rtime
    long long        chrono_start; // gettimeofday at constructor
    long long        chrono_last;  // gettimeofday at trace
  } time_t;
    

  class TimeTrace : public TracePID
    {
    private:
      static vector<time_t*> _time_vec;
#ifdef HAVE_LIBPAPI
      static bool            _papi_initialized;
      int                    _event_set;
#endif
      glibtop_proc_time      _glib_proc_time;
      time_t*                _time;


    public:
      /* constructor and destructor */
      TimeTrace(pid_t pid);               // PAPI_library_init
      virtual             ~TimeTrace();
      static void         cleanup();

      /* TracePID */
      void                trace(const status_t &status);
      static string       toString();
      static Bytes        Data();

    private:
      static bool         _papi(const char* debug_name, int papi_retval);
      static long long    _elapsed_time(const long long& chrono_start);
      static long long    _current_time();

      
    };


};

#endif //TIMETRACE_H_
