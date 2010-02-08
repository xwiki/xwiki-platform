#!/bin/bash

XE_VERSION=2.3-SNAPSHOT;
GWT_VERSION=2.0.0;
GWT_INCUBATOR_VERSION=20091216-r1739;
SMART_GWT_VERSION=1.2;

M2_REPO=~/.m2/repository;
APP_DIR=`dirname $0`/webapps/xwiki;

java \
-Xmx1024m \
-Xdebug \
-Xnoagent \
-Djava.compiler=NONE \
-Xrunjdwp:transport=dt_socket,server=y,address=5006,suspend=y \
-cp \
$M2_REPO/com/google/gwt/gwt-dev/$GWT_VERSION/gwt-dev-$GWT_VERSION.jar:\
$M2_REPO/com/google/gwt/gwt-user/$GWT_VERSION/gwt-user-$GWT_VERSION.jar:\
$M2_REPO/org/xwiki/platform/xwiki-web-gwt-dom/$XE_VERSION/xwiki-web-gwt-dom-$XE_VERSION.jar:\
$M2_REPO/com/google/gwt/gwt-incubator/$GWT_INCUBATOR_VERSION/gwt-incubator-$GWT_INCUBATOR_VERSION.jar:\
$M2_REPO/org/xwiki/platform/xwiki-web-gwt-user/$XE_VERSION/xwiki-web-gwt-user-$XE_VERSION.jar:\
$M2_REPO/com/smartgwt/smartgwt/$SMART_GWT_VERSION/smartgwt-$SMART_GWT_VERSION.jar:\
$M2_REPO/org/xwiki/platform/xwiki-core-component-api/$XE_VERSION/xwiki-core-component-api-$XE_VERSION.jar:\
$M2_REPO/org/xwiki/platform/xwiki-web-gwt-wysiwyg-client/$XE_VERSION/xwiki-web-gwt-wysiwyg-client-$XE_VERSION.jar \
com.google.gwt.dev.DevMode \
-noserver \
-port 8080 \
-war $APP_DIR/resources/js/xwiki/wysiwyg \
-startupUrl xwiki/bin/edit/Sandbox/Test?editor=wysiwyg \
org.xwiki.gwt.wysiwyg.Wysiwyg