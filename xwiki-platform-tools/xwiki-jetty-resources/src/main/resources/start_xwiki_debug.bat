@echo off
set LANG=fr_FR.ISO8859-1
set JETTY_HOME=jetty
set JETTY_PORT=8080
set JAVA_OPTS=-Xmx300m
set JAVA_OPTS=%JAVA_OPTS% -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005

REM Jetty requires a logs directory to exist
if not exist %JETTY_HOME%\logs mkdir %JETTY_HOME%\logs

REM For enabling YourKit Profiling.
REM $3 must the path where Yourkit can find the agent.
REM For example: "C:\PROGRA~1\YOURKI~1.11\bin\win32"
REM No spaces are allowed in this path, 8.3 DOS path format should be used.
REM Use the "dir /X" command (in command prompt) within a directory to get 8.3 DOS path
if "%2"=="profiler" (
  set OLD_PATH=%PATH%
  set PATH=%3;%PATH%
  set JAVA_TOOL_OPTIONS=-agentlib:yjpagent
)

REM Specify port and key to stop a running Jetty instance
set JAVA_OPTS=%JAVA_OPTS% -DSTOP.KEY=xwiki -DSTOP.PORT=8079

java %JAVA_OPTS% -Dfile.encoding=UTF8 -Djetty.home=%JETTY_HOME% -Djetty.port=%JETTY_PORT% -jar %JETTY_HOME%/start.jar

if %2==profiler set PATH=%OLD_PATH%