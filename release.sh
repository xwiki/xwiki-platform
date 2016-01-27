#!/bin/bash
mvn release:prepare -U -DautoVersionSubmodules=true -Pintegration-tests,quality -DskipTests=true -Darguments="-DskipTests=true" && mvn release:perform -Pintegration-tests,quality -DskipTests=true -Darguments="-DskipTests=true"
