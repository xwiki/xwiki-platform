#!/bin/bash

XE_VERSION=1.8-SNAPSHOT;
GWT_VERSION=1.5.3;

APP_DIR=`dirname $0`/webapps/xwiki;
LIB_DIR=$APP_DIR/WEB-INF/lib;
JAVA32_HOME=/usr/lib/jvm/ia32-java-1.5.0-sun/bin;
GWT_REPO=~/.m2/repository/com/google/gwt;
WYSIWYG_PATH=resources/js/xwiki/wysiwyg/gwt/com.xpn.xwiki.wysiwyg.Wysiwyg;

$JAVA32_HOME/java \
-Xmx1024m \
-cp \
$LIB_DIR/xwiki-web-wysiwyg-$XE_VERSION.jar:\
$LIB_DIR/xwiki-web-wysiwyg-$XE_VERSION-sources.jar:\
$LIB_DIR/xwiki-web-gwt-$XE_VERSION-sources.jar:\
$LIB_DIR/junit-3.8.1.jar:\
$LIB_DIR/incubator-glasspanel-r729.jar:\
$GWT_REPO/gwt-dev/$GWT_VERSION/gwt-dev-$GWT_VERSION-linux.jar:\
$GWT_REPO/gwt-user/$GWT_VERSION/gwt-user-$GWT_VERSION.jar \
com.google.gwt.dev.GWTShell \
-logLevel WARN \
-style DETAILED \
-noserver \
-port 8080 \
-out $APP_DIR/$WYSIWYG_PATH \
xwiki/$WYSIWYG_PATH/Wysiwyg.html