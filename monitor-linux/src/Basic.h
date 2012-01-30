#ifndef _BASIC_H_
#define _BASIC_H_

#ifdef HAVE_CONFIG_H
#include <config.h>
#endif

#include <iostream>
#include <sstream>
#include <string>
#include <sys/time.h>
#include <cstdio>
#include <cstdlib>

//#include <sys/signal.h>
#include <sys/errno.h>
#include <pthread.h>   // pthread
//#include <stdexcept>

using namespace std;

//////////////////////////////////////////////////////////////////////
// DEBUG
//////////////////////////////////////////////////////////////////////
//see configure.ac and config.h
#ifdef VERBOSE
#define dout cout
#define LINE cout << "#################### "         \
                  << __FILE__ << ":" << __LINE__     \
                  << " ####################" << endl
#else
#define dout if (false) cout
#define LINE
#endif

//////////////////////////////////////////////////////////////////////
// TYPES and MACROS
//////////////////////////////////////////////////////////////////////
typedef unsigned char byte;
typedef unsigned int u_int;

//#define	maximum(a,b)	((a)>(b)?(a):(b))
//#define	minimum(a,b)	((a)<(b)?(a):(b))
#define minimum(a,b)    ({ typeof (a) _a = (a); \
                           typeof (b) _b = (b); \
                           _a < _b ? _a : _b; })
#define maximum(a,b)    ({ typeof (a) _a = (a); \
                           typeof (b) _b = (b); \
                           _a > _b ? _a : _b; })
#define	PAUSE()         cout << "------------------------- " \
                             << "  HIT ENTER TO CONTINUE   " \
                             << "------------------------- " \
                             << endl; getchar()

//////////////////////////////////////////////////////////////////////
// Utils
//////////////////////////////////////////////////////////////////////
namespace utils {

/* int */
string uitos(unsigned int value);
string itos(int value);
unsigned int stoui(string str);
int stoi(string str);

/* long long */
string ulltos(unsigned long long value);
string lltos(long long value);

/* byte */
int _btoi(byte b);
int btoi(const byte* bytes, int length);
string btos(const byte* bytes, int size);

/* string */
bool is_digit(string &str);

/* timeval */
long long tvtoll(const struct timeval& time);

/* process */
//bool run (const char *command,  vector<string>& output);
//bool _run(char **command, vector<string>& output);


}
;

//////////////////////////////////////////////////////////////////////
// Bytes
//////////////////////////////////////////////////////////////////////
class Bytes {
public:
	byte* data;
	unsigned int length; // current index
	unsigned int _capacity;

public:
	//Bytes();
	Bytes(unsigned int capacity = 16);
	Bytes(const Bytes &copy);
	//Bytes(const string &str);
	Bytes(const byte* data, unsigned int length);
	virtual ~Bytes();

	unsigned int Capacity();
	void resize(unsigned int new_capacity);
	void addInteger(const int integer, unsigned int len);
	void addLong(const long long integer);
	void add(const byte *data, unsigned int len);
	void add(const Bytes &src);

	/* OPERATORS */
	Bytes operator+(const Bytes &bytes);
	Bytes& operator+=(const Bytes &bytes);

};

//////////////////////////////////////////////////////////////////////
// Thread
//////////////////////////////////////////////////////////////////////
class Thread {
private:
	pthread_t _thread;

public:
	Thread();
	virtual ~Thread();
	void run(void(*func)(void));
	bool terminate();

	static void signal(int signal_id);
	static int wait();
};

//////////////////////////////////////////////////////////////////////
// Signalize
//////////////////////////////////////////////////////////////////////
class Signalize {
public:
	static const int IDLE = 0;
	static const int QUIT = -1;
	static const int DONE = -2;
	int _current_signal;
	pthread_cond_t _cond;
	pthread_mutex_t _mutex;

public:
	Signalize();
	virtual ~Signalize();
	void init();
	void destroy();
	void reset();
	void signal(int signal);
	void signal(int signal, int sigwait);
	int wait();
	bool wait(int signal);
	void lock();
	void unlock();
};

#endif //_BASIC_H_
