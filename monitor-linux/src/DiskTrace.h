#ifndef DISKTRACE_H_
#define DISKTRACE_H_

#include "Basic.h"

#include <glibtop.h>
#include <glibtop/union.h>

/* _run */
#include <unistd.h>    // pipe fork
#include <sys/types.h> //      fork wait
#include <sys/wait.h>  //           wait
#include <sys/ptrace.h>// ptrace

#include "TracePID.h"

using namespace std;


/* lsof */
#ifndef LSOF
#  define LSOF   "lsof"
#endif


namespace monitor
{

  //enum file_access_t { FA_UNKNOWN = 0, FA_RD = 'r', FA_WR = 'w', FA_RW = 'u' };
  enum file_type_t   { FT_UNKNOWN = 0, FT_REG = 1, FT_SOC = 2 };


  typedef struct {
    string          name;            // file name
    unsigned int    inode;           // file identifiers
    int             fd;              // file descriptor (-1 unkown)
    //pid_t           pid;             // pid currently using file
    
    //file_access_t   access;        // bug in lsof: always 'r'
    file_type_t     type;            // file type
    unsigned int    size;            // size at the time of open
    
  } lsof_file_t;

  typedef struct {
    string          name;            // file name
    unsigned int    inode;           // file identifiers
    vector<pid_t>   pids_using;      // processes with file opened
    unsigned int    epoch;           // epoch when file was still open

    file_type_t     type;            // file type

    unsigned int    size;            // current size
    unsigned int    initial_size;    // size at the time of open
    unsigned int    bytes_written;   // total bytes written so far
    
  } file_t;

  
  class DiskTrace : public TracePID
    {
      
    private:
      char           _lsof_command[256];

      static vector<file_t>  _file_vec;

      static unsigned long    _epoch;   // current snapshot id (epoch)

      /* disk usage (currently) */
      static long  _usage;             // current disk usage
      static long  _files;             // current # of simultaneous open files
    
      /* disk usage (maximum) */
      static long  _max_usage;         // maximum disk usage
      static long  _max_files;         // maximum # of simultaneous open files
    
      /* disk usage (since beginning) */
      static long  _total_usage;       // total bytes written
      static long  _total_files;       // total used files

    public:
      /* constructor and destructor */
      DiskTrace(pid_t pid);
      virtual         ~DiskTrace();
      static void     cleanup();

      /* TracePID */
      void           trace(const status_t &status);
      static string  toString();
      static Bytes   Data();

      /* may be a good idea to move this to a superclass (e.g. runnable) */
    private:
      static int     run(const char *command, vector<string> &output);
      static int     _run(      char **argv,   vector<string> &output);

      
      void           _process_lsof(vector<string> &lsof);
      void           _process_files();
      void           _add_file(lsof_file_t &file);

      static bool    _lsof_uint(string &line, unsigned int *output);
      static string  _lsof_string(string &line);
      static void    _file_to_string(const file_t& file);
      static bool    _same_file(const lsof_file_t& lsof, const file_t& file);

      static bool    _file_used_by(file_t &file, pid_t pid);
      bool           _file_removed_from(file_t &file, pid_t pid);

    };
  
  ostream& operator<<(ostream& out, const file_t& file);
  
};

#endif /*DISKTRACE_H_*/
