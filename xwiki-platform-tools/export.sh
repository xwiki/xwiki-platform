#!/bin/bash

source env.sh

if [ -z "$1" ]
then
	db="./db/"   # Default, if no directory specified.
else
	db=$1
fi 

java groovy.lang.GroovyShell export.groovy $db
