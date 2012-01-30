#                                               -*- Autoconf -*-
# Process this file with autoconf to produce a configure script.

#AC_PREREQ(2.60)
#AC_CONFIG_SRCDIR([src/main.cpp])
#AC_CONFIG_HEADER([src/config.h])
AC_INIT
AM_INIT_AUTOMAKE(monitor, 3.0)
AM_CONFIG_HEADER(src/config.h)

# Custom
AC_DEFINE(VERBOSE, [], [If defined, produces additional output.])
AC_PATH_PROG(LSOF, lsof)
AC_DEFINE_UNQUOTED([LSOF], "$LSOF", [lsof path to binary])

# Checks for programs.
AC_PROG_AWK
AC_PROG_CXX
AC_PROG_CC

# Checks for libraries.
AC_CHECK_LIB(papi, PAPI_library_init)
AC_CHECK_LIB(gtop-2.0, glibtop_get_proc_uid)
AC_CHECK_LIB(pthread, pthread_create)

# Checks for header files.
AC_HEADER_STDC
AC_HEADER_SYS_WAIT
AC_CHECK_HEADERS([arpa/inet.h netdb.h stdlib.h sys/socket.h sys/time.h unistd.h])
AC_CHECK_HEADERS([glibtop.h papi.h pthread.h])
AC_CHECK_HEADERS([glibtop.h pthread.h])

# Checks for typedefs, structures, and compiler characteristics.
AC_HEADER_STDBOOL
AC_C_CONST
AC_TYPE_PID_T
AC_HEADER_TIME

# Checks for library functions.
AC_FUNC_FORK
AC_FUNC_MALLOC
AC_FUNC_SELECT_ARGTYPES
AC_TYPE_SIGNAL
AC_CHECK_FUNCS([bzero dup2 gethostbyaddr gethostbyname gettimeofday inet_ntoa select socket strerror])

AC_CONFIG_FILES([Makefile src/Makefile])
AC_OUTPUT
