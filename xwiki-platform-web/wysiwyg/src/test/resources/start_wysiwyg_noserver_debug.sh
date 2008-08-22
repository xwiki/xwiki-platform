#!/bin/bash

LIB_DIR=`dirname $0`/webapps/xwiki;
JAVA32_HOME=/usr/lib/jvm/ia32-java-1.5.0-sun-1.5.0.13/bin;
GWT_HOME=/tmp/xwiki/gwt/gwt-linux-1.5.0-rc1;

$JAVA32_HOME/java \
-Xmx1024m \
-Xdebug \
-Xnoagent \
-Djava.compiler=NONE \
-Xrunjdwp:transport=dt_socket,server=y,address=5006,suspend=y \
-cp \
$LIB_DIR/xwiki-web-wysiwyg-0.1-SNAPSHOT.jar:\
$LIB_DIR/xwiki-web-wysiwyg-0.1-SNAPSHOT-sources.jar:\
$LIB_DIR/xwiki-web-gwt-1.6-SNAPSHOT-sources.jar:\
$LIB_DIR/junit-3.8.1.jar:\
$GWT_HOME/gwt-dev-linux.jar:\
$GWT_HOME/gwt-user.jar \
com.google.gwt.dev.GWTShell \
-logLevel WARN \
-style DETAILED \
-noserver \
-port 8080 \
-out $APP_DIR/com.xpn.xwiki.wysiwyg.Wysiwyg \
xwiki/com.xpn.xwiki.wysiwyg.Wysiwyg/Wysiwyg.html