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

set JETTY_HOME=jetty
set JETTY_PORT=8080
set JAVA_OPTS=-Xmx300m

REM Ensure the logs directory exists as otherwise Jetty reports an error
if not exist %JETTY_HOME%\logs mkdir %JETTY_HOME%\logs

REM Ensure the work directory exists so that Jetty uses it for its temporary files.
if not exist %JETTY_HOME%\work mkdir %JETTY_HOME%\work

REM Specify port and key to stop a running Jetty instance
set JAVA_OPTS=%JAVA_OPTS% -DSTOP.KEY=xwiki -DSTOP.PORT=8079

java %JAVA_OPTS% -Dfile.encoding=UTF8 -Djetty.home=%JETTY_HOME% -Djetty.port=%JETTY_PORT% -jar %JETTY_HOME%/start.jar
