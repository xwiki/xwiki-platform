@echo off
REM -------------------------------------------------------------------------
REM See the NOTICE file distributed with this work for additional
REM information regarding copyright ownership.
REM
REM This is free software; you can redistribute it and/or modify it
REM under the terms of the GNU Lesser General Public License as
REM published by the Free Software Foundation; either version 2.1 of
REM the License, or (at your option) any later version.
REM
REM This software is distributed in the hope that it will be useful,
REM but WITHOUT ANY WARRANTY; without even the implied warranty of
REM MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
REM Lesser General Public License for more details.
REM
REM You should have received a copy of the GNU Lesser General Public
REM License along with this software; if not, write to the Free
REM Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
REM 02110-1301 USA, or see the FSF site: http://www.fsf.org.
REM -------------------------------------------------------------------------

REM -------------------------------------------------------------------------
REM Optional ENV vars
REM -----------------
REM   XWIKI_OPTS - parameters passed to the Java VM when running Jetty
REM     e.g. to increase the memory allocated to the JVM to 1GB, use
REM       set XWIKI_OPTS=-Xmx1024m
REM   JETTY_OPTS - optional parameters passed to Jetty's start.jar. For example to list the configuration that will
REM       execute, try setting it to "--list-config". See
REM       http://www.eclipse.org/jetty/documentation/current/start-jar.html for more options.
REM   JETTY_PORT - the port on which to start Jetty, 8080 by default
REM   JETTY_STOP_PORT - the port on which Jetty listens for a Stop command, 8079 by default
REM -------------------------------------------------------------------------

setlocal EnableDelayedExpansion

if not defined XWIKI_OPTS set XWIKI_OPTS=-Xmx1024m
set XWIKI_OPTS=%XWIKI_OPTS% -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005

REM The port on which to start Jetty can be defined in an enviroment variable called JETTY_PORT
if not defined JETTY_PORT (
  REM Alternatively, it can be passed to this script as the first argument
  set JETTY_PORT=%1
  if not defined JETTY_PORT (
    set JETTY_PORT=8080
  )
)

REM The port on which Jetty listens for a Stop command can be defined in an enviroment variable called JETTY_STOP_PORT
if not defined JETTY_STOP_PORT (
  set JETTY_STOP_PORT=8079
)

echo Starting Jetty on port %JETTY_PORT%, please wait...

REM For enabling YourKit Profiling.
REM %3 must the path where Yourkit can find the agent.
REM For example: "C:\PROGRA~1\YOURKI~1.11\bin\win32"
REM No spaces are allowed in this path, 8.3 DOS path format should be used.
REM Use the "dir /X" command (in command prompt) within a directory to get 8.3 DOS path
if "%2"=="profiler" (
  set OLD_PATH=%PATH%
  set PATH=%3;%PATH%
  set JAVA_TOOL_OPTIONS=-agentlib:yjpagent
)

REM Discover java.exe from the latest properly installed JRE
for /f tokens^=2^ delims^=^" %%i in ('reg query HKEY_CLASSES_ROOT\jarfile\shell\open\command /ve') do set JAVAW_PATH=%%i
set JAVA_PATH=%JAVAW_PATH:\javaw.exe=%\java.exe
if "%JAVA_PATH%"=="" set JAVA_PATH=java
REM Handle the case when JAVA_HOME is set by the user
if not "%JAVA_HOME%" == "" set JAVA_PATH=%JAVA_HOME%\bin\java.exe

REM Location where XWiki stores generated data and where database files are.
set XWIKI_DATA_DIR=${xwikiDataDir}
set XWIKI_OPTS=%XWIKI_OPTS% -Dxwiki.data.dir="%XWIKI_DATA_DIR%"

REM Catch any Out Of Memory to make easier to analyze it
set XWIKI_OPTS=%XWIKI_OPTS% -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath="%XWIKI_DATA_DIR%"

REM Ensure the data directory exists so that XWiki can use it for storing permanent data.
if not exist "%XWIKI_DATA_DIR%" mkdir "%XWIKI_DATA_DIR%"

REM Ensure the logs directory exists as otherwise Jetty reports an error
if not exist "%XWIKI_DATA_DIR%\logs" mkdir "%XWIKI_DATA_DIR%\logs"

REM Set up the Jetty Base directory (used for custom Jetty configuration) to point to the Data Directory
REM Also created some Jetty directorie that Jetty would otherwise create at first startup. We do this to avoid
REM cryptic messages in the logs such as: "MKDIR: ${jetty.base}/lib"
set JETTY_BASE=%XWIKI_DATA_DIR%\jetty
if not exist "%JETTY_BASE%" mkdir "%JETTY_BASE%"
if not exist "%JETTY_BASE%\lib" mkdir "%JETTY_BASE%\lib"
if not exist "%JETTY_BASE%\lib\ext" mkdir "%JETTY_BASE%\lib\ext"
if not exist "%JETTY_BASE%\logs" mkdir "%JETTY_BASE%\logs"
if not exist "%JETTY_BASE%\resources" mkdir "%JETTY_BASE%\resources"
if not exist "%JETTY_BASE%\webapps" mkdir "%JETTY_BASE%\webapps"

REM Specify Jetty's home and base directories
set JETTY_HOME=jetty
set XWIKI_OPTS=%XWIKI_OPTS% -Djetty.home="%JETTY_HOME%" -Djetty.base="%JETTY_BASE%"

REM Specify the encoding to use
set XWIKI_OPTS=%XWIKI_OPTS% -Dfile.encoding=UTF8

REM Specify port on which HTTP requests will be handled
set JETTY_OPTS=%JETTY_OPTS% jetty.port=%JETTY_PORT%
REM In order to print a nice friendly message to the user when Jetty has finished loading the XWiki webapp, we pass
REM the port we use as a System Property
set XWIKI_OPTS=%XWIKI_OPTS% -Djetty.port=%JETTY_PORT%

REM Specify port and key to stop a running Jetty instance
set JETTY_OPTS=%JETTY_OPTS% STOP.KEY=xwiki STOP.PORT=%JETTY_STOP_PORT%

"%JAVA_PATH%" %XWIKI_OPTS% %4 %5 %6 %7 %8 %9 -jar "%JETTY_HOME%/start.jar" --module=xwiki %JETTY_OPTS%

if %2==profiler set PATH=%OLD_PATH%

REM Pause so that the command window used to run this script doesn't close automatically in case of problem
REM (like when the JDK/JRE is not installed)
PAUSE
