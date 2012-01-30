#include "MemTrace.h"

namespace monitor
{

  unsigned long MemTrace::_page_size = (unsigned long) sysconf(_SC_PAGESIZE);
  vector<mem_t*> MemTrace::_mem_vec;
  long MemTrace::_global_usage       = 0;
  long MemTrace::_global_max_usage   = 0;
  long MemTrace::_global_total_usage = 0;

  //////////////////////////////////////////////////////////////////////
  // CONSTRUTORES                                                     //
  //////////////////////////////////////////////////////////////////////

  MemTrace::MemTrace(pid_t pid)
    : TracePID(pid)
  {
    //_page_size = (unsigned long) sysconf(_SC_PAGESIZE);
    _mem = new mem_t;
    _mem->pid         = pid;
    _mem->usage       = 0;
    _mem->total_usage = 0;
    _mem_vec.push_back(_mem);

    // look at memory usage everytime except upon execve (SYSCALL(11))
    // because it relates to OS memory mangement, allocating and immediately
    // deallocating memory.
    // TODO: We can also ignore some known non-memory related system calls.
    //       It's safer than just adding the known related-ones
    //       (we may miss some).
    //_ignore_syscall(11, true);   _ignore_syscall(11, false);
    _register_syscall(45, false);   //  exits from syscall brk
    _register_syscall(11, false);   //  exits from syscall execve
    _register_syscall(1,   false);  //  exits from syscall exit
    _register_syscall(2,   false);  //  exits from syscall fork
    _register_syscall(190, false);  //  exits from syscall vfork
    _register_syscall(90,  false);  //  exits from syscall mmap
    _register_syscall(192, false);  //  exits from syscall mmap2
    _register_syscall(163, false);  //  exits from syscall mremap
    _register_syscall(91,  false);  //  exits from syscall munmap
    _register_syscall(257, false);  //  exits from syscall remap_file_pages (?)
    _register_syscall(102, false);  //  exits from syscall socketcall
    //_register_syscall(142, false);  //  exits from syscall newselect
    _register_user();

  }

  MemTrace::~MemTrace() { }

  void MemTrace::cleanup() {
    vector<mem_t*>::iterator iter = _mem_vec.begin();
    while (iter != _mem_vec.end()) {
      delete (*iter);
      _mem_vec.erase(iter);
    }
    _global_usage       = 0;
    _global_max_usage   = 0;
    _global_total_usage = 0;
  }




  //////////////////////////////////////////////////////////////////////
  // METODOS                                                          //
  //////////////////////////////////////////////////////////////////////

  void MemTrace::trace(const status_t &status) {
      /* Obtain memory usage information if syscall is related with memory operations. */
    if (_is_registered(status)) {

      glibtop_get_proc_mem (&_glib_proc_mem,  _pid);
//      long new_usage = (_glib_proc_mem.rss - _glib_proc_mem.share) / _page_size;
      long new_usage = _glib_proc_mem.rss / _page_size;
      //printf(">  MEMORY = %ld\n", new_usage);
      //printf(">      rss: %ld\n", (long) _glib_proc_mem.rss / _page_size);
      //printf(">     size: %ld\n", (long) _glib_proc_mem.size / _page_size);
      //printf(">    vsize: %ld\n", (long) _glib_proc_mem.vsize / _page_size);
      //printf("> resident: %ld\n", (long) _glib_proc_mem.resident / _page_size);
      //printf(">    share: %ld\n", (long) _glib_proc_mem.share / _page_size);
/*
      printf(">\t%ld\t%ld\t%ld\t%ld\t%ld\n",
		(long) _glib_proc_mem.rss / _page_size,
		(long) _glib_proc_mem.resident / _page_size,
		(long) _glib_proc_mem.size / _page_size,
		(long) _glib_proc_mem.vsize / _page_size,
		(long) _glib_proc_mem.share / _page_size);
*/
      //printf(">-----------------------\n");

      /* update _mem->usage and _mem->total_usage */
      if (new_usage > _mem->usage)
    	  _mem->total_usage += new_usage - _mem->usage; // add any memory variation to total_usage
      _mem->usage = new_usage;

      /* update global memory usage (current and since beginning) */
      vector<mem_t*>::iterator iter = _mem_vec.begin();
      _global_usage       = 0;
      _global_total_usage = 0;
      while (iter != _mem_vec.end()) {
        _global_usage       += (*iter)->usage;
        _global_total_usage += (*iter)->total_usage;
        iter++;
      }

      /* memory usage (maximum) */
      _global_max_usage = maximum(_global_usage, _global_max_usage);
    }
  }

  string MemTrace::toString() {
    ostringstream o;
    o << "   memory: "
      <<"\t"<< _global_usage
      <<"\t"<< _global_max_usage
      <<"\t"<< _global_total_usage;
    return o.str();
  }

  Bytes MemTrace::Data() {
    Bytes data;
    data.addInteger(_global_usage,       4);
    data.addInteger(_global_max_usage,   4);
    data.addInteger(_global_total_usage, 4);
    return data;
  }

};

