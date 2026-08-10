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
REM   JETTY_OPTS - optional parameters passed to Jetty's start.jar. See
REM       http://www.eclipse.org/jetty/documentation/current/start-jar.html for options.
REM   JETTY_STOP_PORT - the port on which Jetty listens for a Stop command, 8079 by default.
REM -------------------------------------------------------------------------

setlocal EnableDelayedExpansion

REM Ensure that the commands below are always executed from the directory where this script is located, and that the
REM paths we pass to Jetty are absolute. Jetty does not work well with a relative base directory (e.g. "."), so we
REM always compute the absolute location of this script (%~dp0 always ends with a backslash).
cd /d "%~dp0"
set "XWIKI_HOME=%~dp0"
if "%XWIKI_HOME:~-1%"=="\" set "XWIKI_HOME=%XWIKI_HOME:~0,-1%"

REM The port on which Jetty listens for a Stop command can be defined in an environment variable called
REM JETTY_STOP_PORT.
if not defined JETTY_STOP_PORT set JETTY_STOP_PORT=8079

REM Parse script parameters.
:parseArgs
if "%~1"=="" goto endParseArgs
if /i "%~1"=="-sp" ( set "JETTY_STOP_PORT=%~2" & shift & shift & goto parseArgs )
if /i "%~1"=="--stopport" ( set "JETTY_STOP_PORT=%~2" & shift & shift & goto parseArgs )
if /i "%~1"=="-h" ( call :usage & exit /b 1 )
if /i "%~1"=="--help" ( call :usage & exit /b 1 )
echo Unknown option: %~1
call :usage
exit /b 1
:endParseArgs

REM Discover java.exe from the latest properly installed JRE.
for /f tokens^=2^ delims^=^" %%i in ('reg query HKEY_CLASSES_ROOT\jarfile\shell\open\command /ve') do set JAVAW_PATH=%%i
set JAVA_PATH=%JAVAW_PATH:\javaw.exe=%\java.exe
if "%JAVA_PATH%"=="" set JAVA_PATH=java
REM Handle the case when JAVA_HOME is set by the user.
if not "%JAVA_HOME%" == "" set JAVA_PATH=%JAVA_HOME%\bin\java.exe

REM Specify Jetty's home and base directories using absolute paths.
set "JETTY_BASE=%XWIKI_HOME%"
set "JETTY_HOME=%JETTY_BASE%\jetty"
set XWIKI_OPTS=%XWIKI_OPTS% -Djetty.home="%JETTY_HOME%" -Djetty.base="%JETTY_BASE%"

REM Specify port and key to stop a running Jetty instance.
set JETTY_OPTS=%JETTY_OPTS% STOP.KEY=xwiki STOP.PORT=%JETTY_STOP_PORT%

"%JAVA_PATH%" %XWIKI_OPTS% -jar "%JETTY_HOME%\start.jar" --stop %JETTY_OPTS%

REM Pause so that the command window used to run this script doesn't close automatically in case of problem
REM (like when the JDK/JRE is not installed).
PAUSE
exit /b 0

:usage
echo Usage: stop_xwiki.bat ^<optional parameters^>
echo -sp, --stopport: The Jetty stop port to use. Overrides any value from JETTY_STOP_PORT. Defaults to 8079.
echo.
echo Example: stop_xwiki.bat -sp 8079
goto :eof
