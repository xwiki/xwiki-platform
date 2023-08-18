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

REM The port on which Jetty listens for a Stop command can be defined in an environment variable called JETTY_STOP_PORT
if not defined JETTY_STOP_PORT (
  REM Alternatively, it can be passed to this script as the first argument
  set JETTY_STOP_PORT=%1
  if not defined JETTY_STOP_PORT (
    set JETTY_STOP_PORT=8079
  )
)

REM Discover java.exe from the latest properly installed JRE
for /f tokens^=2^ delims^=^" %%i in ('reg query HKEY_CLASSES_ROOT\jarfile\shell\open\command /ve') do set JAVAW_PATH=%%i
set JAVA_PATH=%JAVAW_PATH:\javaw.exe=%\java.exe
if "%JAVA_PATH%"=="" set JAVA_PATH=java
REM Handle the case when JAVA_HOME is set by the user
if not "%JAVA_HOME%" == "" set JAVA_PATH=%JAVA_HOME%\bin\java.exe

REM Location where XWiki stores generated data and where database files are.
set XWIKI_DATA_DIR=${xwikiDataDir}

REM Specify Jetty's home and base directories
set JETTY_HOME=jetty
set JETTY_BASE=.
set XWIKI_OPTS=%XWIKI_OPTS% -Djetty.home="%JETTY_HOME%" -Djetty.base="%JETTY_BASE%"

REM Specify port and key to stop a running Jetty instance
set JETTY_OPTS=%JETTY_OPTS% STOP.KEY=xwiki STOP.PORT=%JETTY_STOP_PORT%

"%JAVA_PATH%" %XWIKI_OPTS% -jar "%JETTY_HOME%/start.jar" --stop %JETTY_OPTS%

REM Pause so that the command window used to run this script doesn't close automatically in case of problem
REM (like when the JDK/JRE is not installed)
PAUSE