#!/usr/bin/env bash

limit=$1
id=$2
for i in $(seq 1 $limit) ; do
  uid="${i}-${id}"
  mvn clean install -Dit.test=NotificationsIT#simpleNotifications \
    -Dgradle.cache.local.enabled=false -Dgradle.cache.remote.enabled=false \
    -Dxwiki.test.ui.servletEngine=tomcat -Dxwiki.test.ui.servletEngineTag=10-jdk17 \
    -pl :xwiki-platform-notifications-test-docker \
    -Pintegration-tests,docker,quality \
    -Dmaven.build.dir=target/test${uid} -l /tmp/results${uid}.txt
done

