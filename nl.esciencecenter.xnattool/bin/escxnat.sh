#!/bin/bash
##
# 
# Script: escxnat.sh
#

echo "VERSION=${VERSION}"

## 
# bootstrap startup directory:

#beware of spaces:
DIRNAME=`dirname "$0"`
BASE_DIR=`cd "$DIRNAME/.." ; pwd`

##
#JAVA

if [ -n "${JAVA_HOME}" ] ; then 
   JAVA="$JAVA_HOME/bin/java"
else
   JAVA=java
fi

##
# info

echo " VERSION    = ${VERSION}"
echo " JAVA       = ${JAVA}"
echo " BASE_DIR   = ${BASE_DIR}"
echo " MAIN_CLASS = ${MAIN_CLAS}"

#starting
"${JAVA}" -jar "${BASE_DIR}/lib/escxnat.jar" $@
result=?$

