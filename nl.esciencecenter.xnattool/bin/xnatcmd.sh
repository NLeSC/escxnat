#!/bin/bash
##
# (C) 2013 Netherlands eScience Center 
#
# Script: xnatcmd.sh
#
# info  :
#          Command interface to the XnatTool .  
# 

infoPrint()
{
   # default to stderr so that stdout can be redirected to a file.  
   echo "$@" >&2
}

infoPrint "VERSION=${VERSION}"

## 
# bootstrap startup directory:

#beware of spaces:
DIRNAME=`dirname "$0"`
BASE_DIR=`cd "$DIRNAME/.." ; pwd`
MAIN_CLASS=nl.esciencecenter.xnattool.util.XnatCMD

##
#JAVA

if [ -n "${JAVA_HOME}" ] ; then 
   JAVA="$JAVA_HOME/bin/java"
else
   JAVA=java
fi

##
# info

infoPrint " VERSION    = ${VERSION}"
infoPrint " JAVA       = ${JAVA}"
infoPrint " BASE_DIR   = ${BASE_DIR}"
infoPrint " MAIN_CLASS = ${MAIN_CLAS}"

#starting
"${JAVA}" -jar "${BASE_DIR}/lib/escxnat.jar" -startClass ${MAIN_CLASS} $@
result=?$

