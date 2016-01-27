#!/bin/bash
mvn release:prepare -U -DautoVersionSubmodules=true -Pintegration-tests,quality -DskipTests=true -Darguments="-DskipTests=true" --settings maven-settings.xml && mvn release:perform -Pintegration-tests,quality -DskipTests=true -Darguments="-DskipTests=true" --settings maven-settings.xml
