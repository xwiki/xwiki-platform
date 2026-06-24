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
REM   XWIKI_OPTS - parameters passed to the Java VM when running XWiki e.g. to increase the memory allocated to the
REM       JVM to 1GB, use set XWIKI_OPTS=-Xmx1024m
REM   JETTY_OPTS - optional parameters passed to Jetty's start.jar. For example to list the configuration that will
REM       execute, try setting it to "--list-config". See
REM       http://www.eclipse.org/jetty/documentation/current/start-jar.html for more options.
REM   JETTY_PORT - the port on which to start Jetty, 8080 by default.
REM   JETTY_STOP_PORT - the port on which Jetty listens for a Stop command, 8079 by default.
REM   JETTY_DEBUG_PORT - the port to use for debugging purpose (default: 5005).
REM -------------------------------------------------------------------------

setlocal EnableDelayedExpansion

REM Ensure that the commands below are always executed from the directory where this script is located, and that the
REM paths we pass to Jetty are absolute. Jetty does not work well with a relative base directory (e.g. "."), so we
REM always compute the absolute location of this script (%~dp0 always ends with a backslash).
cd /d "%~dp0"
set "XWIKI_HOME=%~dp0"
if "%XWIKI_HOME:~-1%"=="\" set "XWIKI_HOME=%XWIKI_HOME:~0,-1%"

REM If no XWIKI_OPTS env variable has been defined use default values.
if not defined XWIKI_OPTS set XWIKI_OPTS=-Xmx1024m

REM The port on which to start Jetty can be defined in an environment variable called JETTY_PORT.
if not defined JETTY_PORT set JETTY_PORT=8080

REM The port on which Jetty listens for a Stop command can be defined in an environment variable called
REM JETTY_STOP_PORT.
if not defined JETTY_STOP_PORT set JETTY_STOP_PORT=8079

REM The port on which to listen for debugging operations.
if not defined JETTY_DEBUG_PORT set JETTY_DEBUG_PORT=5005

REM By default suspend is false for debug.
set SUSPEND=n

REM Parse script parameters.
:parseArgs
if "%~1"=="" goto endParseArgs
if /i "%~1"=="-p" ( set "JETTY_PORT=%~2" & shift & shift & goto parseArgs )
if /i "%~1"=="--port" ( set "JETTY_PORT=%~2" & shift & shift & goto parseArgs )
if /i "%~1"=="-sp" ( set "JETTY_STOP_PORT=%~2" & shift & shift & goto parseArgs )
if /i "%~1"=="--stopport" ( set "JETTY_STOP_PORT=%~2" & shift & shift & goto parseArgs )
if /i "%~1"=="-j" ( set "JETTY_OPTS=%JETTY_OPTS% --module=jmx" & shift & goto parseArgs )
if /i "%~1"=="--jmx" ( set "JETTY_OPTS=%JETTY_OPTS% --module=jmx" & shift & goto parseArgs )
if /i "%~1"=="-ni" ( set XWIKI_NONINTERACTIVE=true & shift & goto parseArgs )
if /i "%~1"=="--noninteractive" ( set XWIKI_NONINTERACTIVE=true & shift & goto parseArgs )
if /i "%~1"=="-d" ( set DEBUG=true & shift & goto parseArgs )
if /i "%~1"=="--debug" ( set DEBUG=true & shift & goto parseArgs )
if /i "%~1"=="--suspend" ( set SUSPEND=y & shift & goto parseArgs )
if /i "%~1"=="-dp" ( set "JETTY_DEBUG_PORT=%~2" & shift & shift & goto parseArgs )
if /i "%~1"=="--debugPort" ( set "JETTY_DEBUG_PORT=%~2" & shift & shift & goto parseArgs )
if /i "%~1"=="-yp" ( set "YOURKIT_PATH=%~2" & shift & shift & goto parseArgs )
if /i "%~1"=="--yourkitpath" ( set "YOURKIT_PATH=%~2" & shift & shift & goto parseArgs )
if /i "%~1"=="-h" ( call :usage & exit /b 1 )
if /i "%~1"=="--help" ( call :usage & exit /b 1 )
echo Unknown option: %~1
call :usage
exit /b 1
:endParseArgs

REM Enable debug.
if defined DEBUG set XWIKI_OPTS=%XWIKI_OPTS% -agentlib:jdwp=transport=dt_socket,server=y,suspend=%SUSPEND%,address=*:%JETTY_DEBUG_PORT%

REM Enabling YourKit Profiling.
if defined YOURKIT_PATH (
  set XWIKI_OPTS=%XWIKI_OPTS% -agentlib:yjpagent
  set "PATH=%YOURKIT_PATH%;%PATH%"
)

REM Create environments folder if missing.
if not exist "%XWIKI_HOME%\environments" mkdir "%XWIKI_HOME%\environments"

REM Discover java.exe from the latest properly installed JRE
for /f tokens^=2^ delims^=^" %%i in ('reg query HKEY_CLASSES_ROOT\jarfile\shell\open\command /ve') do set JAVAW_PATH=%%i
set JAVA_PATH=%JAVAW_PATH:\javaw.exe=%\java.exe
if "%JAVA_PATH%"=="" set JAVA_PATH=java
REM Handle the case when JAVA_HOME is set by the user.
if not "%JAVA_HOME%" == "" set JAVA_PATH=%JAVA_HOME%\bin\java.exe

REM Check the version of Java. We first dump the output of "java -version" to a temporary file instead of piping it
REM directly to findstr: piping a quoted executable path that contains spaces (e.g. "C:\Program Files\Java\...")
REM inside a FOR /F command is unreliable in batch and ends up breaking the path on the first space.
set JAVA_VERSION=
set JAVA_VERSION_STRING=
set "JAVA_VERSION_FILE=%TEMP%\xwiki_java_version_%RANDOM%.txt"
"%JAVA_PATH%" -version 2> "%JAVA_VERSION_FILE%"
for /f "tokens=3" %%g in ('findstr /i "version" "%JAVA_VERSION_FILE%"') do (
  if not defined JAVA_VERSION_STRING set "JAVA_VERSION_STRING=%%~g"
)
del "%JAVA_VERSION_FILE%" 2>nul
if not defined JAVA_VERSION_STRING (
  echo No Java found. You need Java installed for XWiki to work.
  goto endWithError
)
REM JAVA_VERSION_STRING looks like 17.0.1 or 1.8.0_292: keep the major version (8 for 1.8.x, 17 for 17.x, etc.).
for /f "delims=._ tokens=1,2" %%a in ("%JAVA_VERSION_STRING%") do (
  if "%%a"=="1" ( set JAVA_VERSION=%%b ) else ( set JAVA_VERSION=%%a )
)
if %JAVA_VERSION% LSS ${xwiki.java.version} (
  echo This version of XWiki requires Java ${xwiki.java.version} or greater.
  goto endWithError
)
if %JAVA_VERSION% GTR ${xwiki.java.version.support} (
  if defined XWIKI_NONINTERACTIVE (
    echo Warning: you're using Java %JAVA_VERSION% which XWiki doesn't fully support yet.
  ) else (
    set /p XWIKI_CONTINUE="You're using Java %JAVA_VERSION% which XWiki doesn't fully support yet. Continue (y/N)? "
    if /i not "!XWIKI_CONTINUE!"=="y" goto endWithError
  )
)

REM TODO: Remove once https://jira.xwiki.org/browse/XCOMMONS-2852 is fixed. In summary we need this to allow the XWiki
REM code or 3rd party code to use reflection to access private variables (setAccessible() calls).
REM See https://tinyurl.com/tdhkn6mp
set XWIKI_OPENS_LANG=--add-opens java.base/java.lang=ALL-UNNAMED
set XWIKI_OPENS_IO=--add-opens java.base/java.io=ALL-UNNAMED
set XWIKI_OPENS_UTIL=--add-opens java.base/java.util=ALL-UNNAMED
set XWIKI_OPENS_CONCURRENT=--add-opens java.base/java.util.concurrent=ALL-UNNAMED
set XWIKI_OPTS=%XWIKI_OPENS_LANG% %XWIKI_OPENS_IO% %XWIKI_OPENS_UTIL% %XWIKI_OPENS_CONCURRENT% %XWIKI_OPTS%

REM Location where XWiki stores generated data and where database files are.
set XWIKI_DATA_DIR=${xwikiDataDir}
set XWIKI_OPTS=%XWIKI_OPTS% -Dxwiki.data.dir="%XWIKI_DATA_DIR%"

REM Make sure the standard Java tmpdir is isolated per instance (by default Jetty provides applications work dir in
REM the Java tmpdir). We use an absolute path so that it is independent from the current directory.
set "JAVA_TMP=%XWIKI_HOME%\tmp"
set XWIKI_OPTS=%XWIKI_OPTS% -Djava.io.tmpdir="%JAVA_TMP%"
REM Make sure the Java tmpdir exist since Jenkins does not create it.
if not exist "%JAVA_TMP%" mkdir "%JAVA_TMP%"

REM Catch any Out Of Memory to make easier to analyze it.
set XWIKI_OPTS=%XWIKI_OPTS% -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath="%XWIKI_DATA_DIR%"

REM Ensure the data directory exists so that XWiki can use it for storing permanent data.
if not exist "%XWIKI_DATA_DIR%" mkdir "%XWIKI_DATA_DIR%"

REM Ensure the logs directory exists as otherwise Jetty reports an error.
if not exist "%XWIKI_DATA_DIR%\logs" mkdir "%XWIKI_DATA_DIR%\logs"

REM Set up the Jetty Base directory (used for custom Jetty configuration) to be the directory where this file is.
REM Jetty does not work well with a relative directory, so we use the absolute one.
set "JETTY_BASE=%XWIKI_HOME%"
REM Also make sure the log directory exists since Jetty won't create it.
if not exist "%JETTY_BASE%\logs" mkdir "%JETTY_BASE%\logs"

REM Specify Jetty's home directory to be the directory named "jetty" inside the Jetty base directory.
set "JETTY_HOME=%JETTY_BASE%\jetty"
set XWIKI_OPTS=%XWIKI_OPTS% -Djetty.home="%JETTY_HOME%" -Djetty.base="%JETTY_BASE%"

REM Specify the encoding to use.
set XWIKI_OPTS=%XWIKI_OPTS% -Dfile.encoding=UTF8

REM Specify port on which HTTP requests will be handled.
set JETTY_OPTS=%JETTY_OPTS% jetty.http.port=%JETTY_PORT%
REM In order to print a nice friendly message to the user when Jetty has finished loading the XWiki webapp, we pass
REM the port we use as a System Property.
set XWIKI_OPTS=%XWIKI_OPTS% -Djetty.http.port=%JETTY_PORT%

REM Specify port and key to stop a running Jetty instance.
set JETTY_OPTS=%JETTY_OPTS% STOP.KEY=xwiki STOP.PORT=%JETTY_STOP_PORT%

"%JAVA_PATH%" %XWIKI_OPTS% -jar "%JETTY_HOME%\start.jar" %JETTY_OPTS%

REM Pause so that the command window used to run this script doesn't close automatically in case of problem
REM (like when the JDK/JRE is not installed).
PAUSE
exit /b 0

:endWithError
REM Pause so that the user can read the error message before the command window closes.
PAUSE
exit /b 1

:usage
echo Usage: start_xwiki.bat ^<optional parameters^>
echo -p, --port: The Jetty HTTP port to use. Overrides any value from JETTY_PORT. Defaults to 8080.
echo -sp, --stopport: The Jetty stop port to use. Overrides any value from JETTY_STOP_PORT. Defaults to 8079.
echo -j, --jmx: Allows monitoring/managing Jetty through JMX.
echo -ni, --noninteractive: Don't ask questions to the user. Useful when called in an automated script.
echo -d, --debug: Start the JVM in debug mode.
echo -dp, --debugPort: The Jetty JVM port to use for remote debugging. Defaults to 5005.
echo --suspend: if defined then debug is in suspend mode (i.e. wait for a debugger to connect before progressing).
echo -yp, --yourkitpath: The path where YourKit can find the agent. If not passed then YourKit won't be enabled.
echo.
echo Example: start_xwiki.bat -p 8080 -sp 8079
goto :eof
