# what flags you want to pass to the C compiler & linker
#CFLAGS      = -g --pedantic -Wall
#AM_LDFLAGS  =
AM_CXXFLAGS  = -g -Wall #-fprofile-arcs -ftest-coverage
#AM_CPPFLAGS = 

bin_PROGRAMS = monitor

monitor_CPPFLAGS = `pkg-config libgtop-2.0 --cflags`
monitor_SOURCES = main.cpp \
	Basic.cpp Monitor.cpp PeerToPeer.cpp MonitorController.cpp \
	ExecutionMonitor.cpp TracePID.cpp \
	TimeTrace.cpp MemTrace.cpp DiskTrace.cpp \
	Basic.h Monitor.h PeerToPeer.h MonitorController.h \
	ExecutionMonitor.h TracePID.h \
	TimeTrace.h MemTrace.h DiskTrace.h


