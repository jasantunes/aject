	#include "DiskTrace.h"

namespace monitor
{
	
  //////////////////////////////////////////////////////////////////////
  // CONSTRUTORES                                                     //
  //////////////////////////////////////////////////////////////////////
  // TODO: separar files em: file desc, files e sockets

  vector<file_t> DiskTrace::_file_vec;
  unsigned long  DiskTrace::_epoch       = 0;
  long           DiskTrace::_usage       = 0;
  long           DiskTrace::_files       = 0;
  long           DiskTrace::_max_usage   = 0;
  long           DiskTrace::_max_files   = 0;
  long           DiskTrace::_total_usage = 0;
  long           DiskTrace::_total_files = 0;
  

  DiskTrace::DiskTrace(pid_t pid)
    : TracePID(pid)
  {
    sprintf(&_lsof_command[0], LSOF" -n -O -b -w  -F ftsin -p %d", pid);
    
//	_register_syscall(4, false);    //  exits from syscall write
    _register_syscall(5, false);    //  exits from syscall open
    _register_syscall(6, true);     //  entries in syscall close (instead of write)
  //    _register_syscall(6, false);    //  exits from syscall close
    //_register_exit();
//    _register_syscall(102, false);  //  exits from syscall socketcall
    //  _register_syscall(120, false);  //  exits from syscall clone
    //_register_syscall(2, false);    //  exits from syscall fork
    //_register_syscall(11, false);   //  exits from syscall execve
    //_register_syscall(42, false);   //  exits from syscall pipe
  //    _register_syscall(63, false);   //  exits from syscall dup2

//    _register_user();

//    _register_syscall(500, false);    //  bogus: to ignore disk trace

  }

  DiskTrace::~DiskTrace() { }

  // TODO: criar tb nas outras TracePID
  void DiskTrace::cleanup() {
    vector<file_t>::iterator iter = _file_vec.begin();
    while(iter != _file_vec.end())
      _file_vec.erase(iter);
    _epoch       = 0;
    _usage       = 0;
    _files       = 0;
    _max_usage   = 0;
    _max_files   = 0;
    _total_usage = 0;
    _total_files = 0;
  }


  //////////////////////////////////////////////////////////////////////
  // METODOS                                                          //
  //////////////////////////////////////////////////////////////////////

  void DiskTrace::trace(const status_t &status) {
    vector<string>   output;

    if (_is_registered(status)) {

      // if final state
      if (cannot_proceed(status)) {
      }

      // not final state
      else {
	if (run(_lsof_command, output) >= 0) {
	
	//for (u_int i=0; i<output.size(); i++) cout << output.at(i) << endl;
	  _process_lsof(output);
	  _process_files();
	  //for (u_int i=0; i<_file_vec.size(); i++)
	  //  cout << status <<"\t"<< _file_vec.at(i);
	}
      }
    }
  }

  void DiskTrace::_process_files() {
    vector<file_t>::iterator iter = _file_vec.begin();
    _usage       = 0;
    _files       = 0;
    _total_usage = 0;
    _total_files = 0;

    // check for total bytes written since last check
    // and for the total number of opened files
    while(iter != _file_vec.end()) {
      // if epoch is old, file was not in use in current snapshot
      // so we remove our pid
      if ((*iter).epoch < _epoch)
		_file_removed_from(*iter, _pid);

      /* in the meanwhile update other disk usage values */
      if ((*iter).pids_using.size() > 0) _files++;
      _usage       += (*iter).size - (*iter).initial_size;
      _total_usage += (*iter).bytes_written;
      iter++;
    }
    
    // update class fields
    _max_usage = maximum(_max_usage, _usage);
    _max_files = maximum(_max_files, _files);
    _total_files = _file_vec.size();


  }

  string DiskTrace::toString() {
    ostringstream o;
    o << "     disk: "
      <<"\t"<< _usage <<"\t"<< _max_usage <<"\t"<< _total_usage <<endl;
    o << "    files: "
      <<"\t"<< _files <<"\t"<< _max_files <<"\t"<< _total_files <<endl;
    return o.str();
  }

  Bytes DiskTrace::Data() {
    Bytes data;
    data.addInteger(      _usage, 4);
    data.addInteger(  _max_usage, 4);
    data.addInteger(_total_usage, 4);
    data.addInteger(      _files, 4);
    data.addInteger(  _max_files, 4);
    data.addInteger(_total_files, 4);
    return data;
  }


  void DiskTrace::_add_file(lsof_file_t& lsof_file) {
    vector<file_t>::iterator iter = _file_vec.begin();
    bool found = false;
    

    // must be a file
    if (lsof_file.type == FT_UNKNOWN) // || lsof_file.fd <=2 )
      return;

    // must be a file
    if (lsof_file.type != FT_REG)
      lsof_file.size = 0;



    /* check for existing file entry
     * if exists update 'size', 'bytes_written' and 'is_open' fields
     * else add it */
    while(iter != _file_vec.end()) {
      // same inode
      if ((*iter).inode == lsof_file.inode) {
	found = true;

	// bytes written is the positive diff between current and previous size
	if (lsof_file.type == FT_REG) {
		if (lsof_file.size > (*iter).size)
		  (*iter).bytes_written += lsof_file.size - (*iter).size;
		(*iter).size = lsof_file.size;
	}
	if (!_file_used_by(*iter, _pid))
	  (*iter).pids_using.push_back(_pid);
	(*iter).epoch = _epoch;

	break;
      }
      iter++;
    }

    if (!found) {
      file_t new_file;
      new_file.name          = lsof_file.name;
      new_file.inode         = lsof_file.inode;
      new_file.pids_using.push_back(_pid);
      new_file.epoch         = _epoch;
      new_file.type          = lsof_file.type;
      new_file.initial_size  = lsof_file.size;
      new_file.size          = lsof_file.size;
      new_file.bytes_written = 0;
      _file_vec.push_back(new_file);
    }

  }
  
  void DiskTrace::_process_lsof(vector<string> &lsof) {
    vector<string>::iterator iter = lsof.begin();
    //unsigned int pid;
    lsof_file_t lsof_file;
    _epoch++; // update snapshot id (epoch)
    
    while (iter != lsof.end()) {
      string &line = *iter;
      
      if (line.size() > 0) {
	switch ( line.at(0) ) {
	  
	  /*****************************************
	   * File descriptor
	   *****************************************/
	case 'f': {
	  //if (lsof_file.inode > 0)
		_add_file(lsof_file);

	  // create new file
	  lsof_file.name   = "";
	  lsof_file.inode  = 0;
	  lsof_file.fd     = -1;
	  lsof_file.type   = FT_UNKNOWN;
	  lsof_file.size   = 0;
	  
	  
	  // get file descriptor
	  string file_desc = _lsof_string(line);
	  if (utils::is_digit(file_desc))
	    lsof_file.fd = utils::stoi(file_desc);

	  break;
	}

	  
	  /*****************************************
	   * File type
	   *****************************************/
	case 't': {
	  string type = _lsof_string(line);
	  if      (type == "REG") //  ||  type == "CHR")
		lsof_file.type = FT_REG;
	  else if (type == "unix" ||
			   //type == "IPv4" || // do pai (ignorar)
			   //type == "IPV6" ||
			   type == "sock")
	  	lsof_file.type = FT_SOC; // name = socket
	  else
		lsof_file.type = FT_UNKNOWN;
	  break;
	}

	  
	  /*****************************************
	   * File size
	   *****************************************/
	case 's':
	  _lsof_uint(line, &lsof_file.size);
	  break;

	  
	  /*****************************************
	   * File inode
	   *****************************************/
	case 'i':
	  _lsof_uint(line, &lsof_file.inode);
	  break;

	  
	  /*****************************************
	   * File name
	   *****************************************/
	case 'n':
	  lsof_file.name = _lsof_string(line);
	  break;
	  
	  
	default:
	  break;
	}
      }
      iter++;
    }
    
    //if (lsof_file.inode > 0) 
		_add_file(lsof_file);
  }

      
  
   bool DiskTrace::_lsof_uint(string &line, unsigned int *output) {
    string::iterator curr = line.begin();
    string::iterator end  = line.end();
    
    if (curr != end) {
      curr++;
      string s(curr, end);
      if (s.size()>0) {
	*output = utils::stoui(s);
	return true;
      }
    }
    return false;

  }
  
  string DiskTrace::_lsof_string(string &line) {
    string::iterator curr = line.begin();
    string::iterator end  = line.end();
    
    if (curr != end) {
      curr++;
      return string(curr, end);
    }
    return string();
  }


  int DiskTrace::run(const char *command, vector<string> &output) {
    bool          retval;
    unsigned int  i;
    const char    delimiters[] = " ";
    char          *token, *cp;
    vector<char*> _args;
    char          ** args;

    /* Convert 'char*' to 'char**'. */
    cp = strdupa (command);               /* Make writable copy.  */
    token = strtok (cp, delimiters);      /* token => "words" */
    while (token != NULL) {
      _args.push_back(token);
      token = strtok( NULL, delimiters);
    }

    /* Convert 'vector<char*>' to 'char**'. */
    args = (char **) malloc(sizeof(char*) * (_args.size()+1));
    for (i=0; i<_args.size(); i++)
      args[i] = _args.at(i);
    args[i] = NULL;

    retval = _run(args, output);
    free(args);
    return retval;
  }
  
  
  int DiskTrace::_run(char **argv, vector<string> &output) {
    int fd[2];
    pid_t pid;
    char buf;
    int status;

    if (pipe(fd) == -1) { perror("pipe");  return false; }

//    printf("##############################################\n");
//    printf("##############################################\n");
//    printf("##############################################\n");
    switch ( pid=fork() ) {
    case -1: 
      perror("fork");
      return -1;
      break;
      
    case 0:
      close(fd[0]);    // close unused read end
      dup2(fd[1], 1);  // stdout -> fd[1]
      execvp(argv[0], argv);
      break;
      
    default:
      {
	std::ostringstream line;
	close(fd[1]);  // close unused write end
	
	/* read from pipe */
	while (read(fd[0], &buf, 1) > 0) {
	  
	  // if newline, append line to output vector
	  if (buf == '\f' || buf == '\n' || buf == '\r') {
	    output.push_back(line.str());
	    line.str("");
	  } else line << buf;
	}
	
	close(fd[0]);
      }
      break;
    } // switch 
    
    return (waitpid(pid, &status, 0)>0) ? WEXITSTATUS(status) : -1;
  }

  
  //////////////////////////////////////////////////////////////////////
  // file_t
  //////////////////////////////////////////////////////////////////////

  bool DiskTrace::_file_used_by(file_t &file, pid_t pid) {
    vector<pid_t>::iterator iter = file.pids_using.begin();
    vector<pid_t>::iterator end = file.pids_using.end();
    while(iter != end) {
      if (*iter == pid) return true;
      iter++;
    }
    return false;
  }

  bool DiskTrace::_file_removed_from(file_t &file, pid_t pid) {
    vector<pid_t>::iterator iter = file.pids_using.begin();
    while(iter != file.pids_using.end()) {
      if (*iter == pid) {
	file.pids_using.erase(iter);
	return true;
      }
      iter++;
    }
    return false;
  }
  
  void DiskTrace::_file_to_string(const file_t& file) {
    cout << "         name: " << file.name << endl;
    cout << "        inode: " << file.inode << endl;
    cout << "         type: " << file.type << endl;
    cout << " initial_size: " << file.initial_size << endl;
    cout << "bytes_written: " << file.bytes_written << endl;
  }

  ostream& operator<<(ostream& out, const file_t& file) {
    out << " ["  << file.inode << "] "
	<< file.type
	<< "\t(" << file.initial_size  <<"/"<< file.size  <<" bytes)"
	<< "\t[" << file.name << "]"    << endl;
    return out;
  }

  
};

