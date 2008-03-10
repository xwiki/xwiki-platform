@echo off
set LANG=fr_FR.ISO8859-1
set JETTY_HOME=jetty
set JETTY_PORT=8080
set JAVA_OPTS=-Xmx300m

REM In order to avoid getting a "java.lang.IllegalStateException: Form too large" error
REM when editing large page in XWiki we need to tell Jetty to allow for large content
REM since by default it only allows for 20K. We do this by passing the
REM org.mortbay.http.HttpRequest.maxFormContentSize property.
REM Note that setting this value too high can leave your server vulnerable to denial of
REM service attacks.
set JAVA_OPTS=%JAVA_OPTS% -Dorg.mortbay.http.HttpRequest.maxFormContentSize=1000000

REM Ensure the logs directory exists as otherwise Jetty reports an error
if not exist %JETTY_HOME%\logs mkdir %JETTY_HOME%\logs

REM Ensure the work directory exists so that Jetty uses it for its temporary files.
if not exist %JETTY_HOME%\work mkdir %JETTY_HOME%\work

java %JAVA_OPTS% -Dfile.encoding=iso-8859-1 -Djetty.home=%JETTY_HOME% -Djetty.port=%JETTY_PORT% -jar %JETTY_HOME%/start.jar
