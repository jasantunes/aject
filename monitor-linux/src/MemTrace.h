#ifndef MEMTRACE_H_
#define MEMTRACE_H_

#include "Basic.h"

#include <glibtop.h>
#include <glibtop/union.h>

#include "TracePID.h"


using namespace std;


namespace monitor
{

  // memory usage is expressed in rss - share pages
  typedef struct {
    pid_t               pid;
    long           usage;       // memory usage (current)
    long           total_usage; // memory usage (since beginning)

  } mem_t;
      class MemTrace : public TracePID
    {
    private:
      static unsigned long  _page_size;
      static vector<mem_t*> _mem_vec;
      mem_t*                _mem;

      /* global memory usage */
      static long      _global_usage;
      static long      _global_max_usage;
      static long      _global_total_usage;

      glibtop_proc_mem      _glib_proc_mem;


    public:
      /* constructor and destructor */
      MemTrace(pid_t pid);
      virtual              ~MemTrace();

      /* TracePID */
      void                 trace(const status_t &status);
      static string        toString();
      static Bytes         Data();
      static void          cleanup();

    };
  
};

#endif /*MEMTRACE_H_*/
