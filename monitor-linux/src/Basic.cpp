#include "Basic.h"

//////////////////////////////////////////////////////////////////////
// Utils
//////////////////////////////////////////////////////////////////////

namespace utils {

/* int */
string uitos(unsigned int value) {
	std::ostringstream o;
	o << value;
	return o.str();
}

string itos(int value) {
	std::ostringstream o;
	o << value;
	return o.str();
}

unsigned int stoui(string str) {
	unsigned int value;
	std::istringstream i(str);
	i >> value;
	return value;
}

int stoi(string str) {
	int value;
	std::istringstream i(str);
	i >> value;
	return value;
}

/* long long */
string ulltos(unsigned long long value) {
	std::ostringstream o;
	o << value;
	return o.str();
}

string lltos(long long value) {
	std::ostringstream o;
	o << value;
	return o.str();
}

/* byte */
int _btoi(const byte b) {
	return b & 0xFF;
}
int btoi(const byte* bytes, int length) {
	int integer = 0;
	int to = length - 1;
	for (int i = 0; i < length; i++, to--)
		integer |= _btoi(bytes[to]) << (i * 8);

	return integer;
}

string btos(const byte* bytes, int size) {
	return std::string((const char *) bytes, size);
}

/* string */
bool is_digit(string &str) {
	if (str.size() == 0)
		return false;
	for (string::iterator start = str.begin(); start != str.end(); start++) {
		if (!isdigit(*start))
			return false;
	}
	return true;
}

/* timeval */
long long tvtoll(const struct timeval& time) {
	long long value;
	value = time.tv_sec * 1000000;
	value += time.tv_usec;
	return value;
}

}

//////////////////////////////////////////////////////////////////////
// Bytes
//////////////////////////////////////////////////////////////////////

Bytes::Bytes(unsigned int capacity) :
		length(0), _capacity(capacity) {
	data = (_capacity > 0) ? new byte[_capacity] : NULL;
}

Bytes::Bytes(const Bytes &copy) {
	this->_capacity = copy._capacity;
	/* copy data */
	this->data = new byte[this->_capacity];
	for (this->length = 0; this->length < copy.length; this->length++)
		this->data[this->length] = copy.data[this->length];
}

Bytes::Bytes(const byte* data, unsigned int length) {
	this->_capacity = length;
	this->data = new byte[_capacity];
	for (this->length = 0; this->length < length; this->length++)
		this->data[this->length] = data[this->length];
}

Bytes::~Bytes() {
	//cout << "freeing " << length << " bytes" << endl;
	if (_capacity)
		delete[] data;
}

//unsigned int Bytes::Capacity() { return _capacity; }

/**
 * Converts a number to a previously allocated byte array.
 * @param dest byte array, pointing at the position where to start copying.
 * @param integer number to convert.
 * @param length number of bytes to be converted from integer.
 */
void Bytes::addInteger(const int integer, unsigned int len) {
	int byte_pos = (len - 1) * 8;
	u_int max_length = minimum((int)len, 4);
	u_int i = 0;

	/* check if we need a bigger byte array to accomodate all data*/
	if ((length + len) > _capacity)
		resize(length + len);

	/* append integer */
	for (; i < max_length; i++, length++)
		data[length] = (byte) ((integer << i * 8) >> byte_pos);

	/* fill the remain of the array with 0s */
	for (; i < len; i++, length++)
		data[length] = 0;
}

/* Long to Network (byte order is reversed) */
void Bytes::addLong(const long long integer) {
	const long long *pointer = &integer;
	int *a = (int *) pointer;
	int *b = (int *) (pointer + 4);
	addInteger(*b, 4);
	addInteger(*a, 4);
}

//void Bytes::addInteger(const int integer, unsigned int len) {
//	add((byte*) &integer, len);
//}
void Bytes::add(const byte* to_append, unsigned int len) {
	/* check if we need a bigger byte array to accomodate all data */
	if ((length + len) > _capacity)
		resize(length + len);

	/* append */
	for (u_int i = 0; i < len; i++, length++)
		data[length] = to_append[i];
}

/**
 * Appends 'src' bytes to 'dest'.
 * @param src byte array to copy to dest.
 * @param length number of bytes to be copied from src.
 */
void Bytes::add(const Bytes &src) {
	/* check if we need a bigger byte array to accomodate all data */
	if ((length + src.length) > _capacity)
		resize(length + src.length);

	/* append */
	for (u_int i = 0; i < src.length; i++, length++)
		data[length] = src.data[i];
}

void Bytes::resize(unsigned int new_capacity) {
	_capacity = new_capacity;
	byte *new_data = new byte[_capacity];
	/* copy old data to new (bigger) array */
	for (u_int i = 0; i < length; i++)
		new_data[i] = data[i];
	if (length)
		delete[] data;
	data = new_data;
}

/**
 * OPERATORS
 */
Bytes Bytes::operator+(const Bytes &bytes) {
	Bytes temp(bytes.length + this->length);
	temp.add(*this);
	temp.add(bytes);
	return temp;
}

Bytes& Bytes::operator+=(const Bytes &bytes) {
	this->add(bytes);
	return *this;
}

//////////////////////////////////////////////////////////////////////
// Thread
//////////////////////////////////////////////////////////////////////

//////////////////////////////////////////////////////////////////////
// CONSTRUTORES                                                     //
//////////////////////////////////////////////////////////////////////

Thread::Thread() {
	/* set thread options */
	// para nao aparecer FATAL: exception not rethrown
	//TODO: ver se e necessario
	//pthread_setcancelstate(PTHREAD_CANCEL_ENABLE, NULL);
	//pthread_setcanceltype(PTHREAD_CANCEL_ASYNCHRONOUS, NULL);
}

Thread::~Thread() {
}

bool Thread::terminate() {
	//switch(pthread_kill(_thread, SIGTERM)) // suicide :(
	switch (pthread_cancel(_thread)) {
	case 0:
		return true;
	case ESRCH:
		// no thread could be found corresponding to that specified
		// by the thread ID
		cout << "Thread inexistent (maybe already killed)" << endl;
		return false;
	default:
		cout << "UNKNOWN" << endl;
		return false;
	}
}

void Thread::run(void(*func)(void)) {
	//pthread_attr_t attr;
	//pthread_attr_init(&attr);
	//pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);
	/*pthread_create(&_thread, &attr, (void * (*)(void *)) startThread, (void *)_thread_arg);*/
	//pthread_create(&_thread, &attr, (void * (*)(void *)) func, NULL);
	pthread_create(&_thread, NULL, (void * (*)(void *)) func, NULL);
}

//////////////////////////////////////////////////////////////////////
// Signalize
//////////////////////////////////////////////////////////////////////

Signalize::Signalize() :
		_current_signal(0) {
	init();
}
Signalize::~Signalize() {
	destroy();
}

void Signalize::init() {
	_current_signal = IDLE;
	pthread_cond_init(&_cond, NULL); // PTHREAD_COND_INITIALIZER
	pthread_mutex_init(&_mutex, NULL); // PTHREAD_MUTEX_INITIALIZER
}
void Signalize::destroy() {
	_current_signal = QUIT;
	pthread_cond_destroy(&_cond);
	pthread_mutex_destroy(&_mutex);
}
void Signalize::reset() {
	_current_signal = IDLE;
}
//////////////////////////////////////////////////////////////////////
// METODOS                                                          //
//////////////////////////////////////////////////////////////////////

void Signalize::signal(int signal) {
	/* change shared var */
	pthread_mutex_lock(&_mutex);
	_current_signal = signal;
	pthread_mutex_unlock(&_mutex);

	/* signal other thread */
	if (pthread_cond_broadcast(&_cond) != 0)
		perror("pthread_cond_signal");
}

void Signalize::signal(int signal, int sigwait) {
	pthread_mutex_lock(&_mutex);

	/* signal */
	_current_signal = signal;

	/* signal other thread */
	if (pthread_cond_broadcast(&_cond) != 0)
		perror("pthread_cond_signal");

	/* wait for signal */
	while (_current_signal != sigwait)
		pthread_cond_wait(&_cond, &_mutex);

	/* re-set shared var */
	_current_signal = IDLE;

	pthread_mutex_unlock(&_mutex);
}

int Signalize::wait() {
	int signal;

	/* wait for shared var to change */
	pthread_mutex_lock(&_mutex);
	//_current_signal = IDLE;
	while (_current_signal == IDLE)
		pthread_cond_wait(&_cond, &_mutex);

	/* re-set shared var */
	signal = _current_signal;
	_current_signal = IDLE;
	pthread_mutex_unlock(&_mutex);
	return signal;
}

bool Signalize::wait(int signal) {
	bool retval = false;

	/* wait for shared var to change */
	pthread_mutex_lock(&_mutex);
	//_current_signal = IDLE;
	while (_current_signal != signal && _current_signal != QUIT)
		pthread_cond_wait(&_cond, &_mutex);
	retval = (_current_signal == signal);

	/* re-set shared var */
	_current_signal = IDLE;
	pthread_mutex_unlock(&_mutex);

	return retval;
}

void Signalize::lock() {
	pthread_mutex_lock(&_mutex);
}
void Signalize::unlock() {
	pthread_mutex_unlock(&_mutex);
}

