#!/bin/bash

db=/Users/hritcu/xwiki/xwiki/trunk/src/main/xwiki/xwiki10-db

current=`pwd`
source export.sh $db
cd $db
svn update
svn commit -m "Updated database"
#svn diff | less
cd $current
#source import.sh $db
