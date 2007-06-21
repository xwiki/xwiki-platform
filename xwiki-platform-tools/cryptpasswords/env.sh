#!/bin/bash

XWIKIHOME=../../xwiki/trunk
XWIKILIB=${XWIKIHOME}/lib
TOOLSLIB=./lib

TOOLSPATH="${TOOLSLIB}/commons-cli-1.0.jar"

XWIKIPATH="${XWIKIHOME}/release/xwiki.jar"
for lib in `ls "${XWIKILIB}"`
do
	XWIKIPATH="${XWIKIPATH}:${XWIKILIB}/$lib"
done

export CP="${TOOLSPATH}:${XWIKIPATH}"
# For windows + cygwin:
#export CLASSPATH=`cygpath --path --windows "$CP"`
export CLASSPATH="${TOOLSPATH}:${XWIKIPATH}"
