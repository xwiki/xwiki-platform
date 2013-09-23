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
set JETTY_STOP_PORT=8079
set XWIKI_OPTS=-Xmx512m -XX:MaxPermSize=196m

REM Location where XWiki stores generated data and where database files are.
set XWIKI_DATA_DIR=${xwikiDataDir}
set XWIKI_OPTS=%XWIKI_OPTS% -Dxwiki.data.dir=%XWIKI_DATA_DIR%

REM Ensure the data directory exists so that XWiki can use it for storing permanent data.
if not exist %XWIKI_DATA_DIR% mkdir %XWIKI_DATA_DIR%

REM Ensure the logs directory exists as otherwise Jetty reports an error
if not exist %XWIKI_DATA_DIR%\logs mkdir %XWIKI_DATA_DIR%\logs

REM The port on which to start Jetty can be passed to this script as the first argument
IF NOT [%1]==[] set JETTY_PORT=%1
set XWIKI_OPTS=%XWIKI_OPTS% -Djetty.port=%JETTY_PORT%

REM Specify Jetty's home directory
set XWIKI_OPTS=%XWIKI_OPTS% -Djetty.home=%JETTY_HOME%

REM The port on which to stop Jetty can be passed to this script as the second argument
IF NOT [%2]==[] set JETTY_STOP_PORT=%2
set XWIKI_OPTS=%XWIKI_OPTS% -DSTOP.KEY=xwiki -DSTOP.PORT=%JETTY_STOP_PORT%

REM Specify the encoding to use
set XWIKI_OPTS=%XWIKI_OPTS% -Dfile.encoding=UTF8

REM In order to avoid getting a "java.lang.IllegalStateException: Form too large" error
REM when editing large page in XWiki we need to tell Jetty to allow for large content
REM since by default it only allows for 20K. We do this by passing the
REM org.eclipse.jetty.server.Request.maxFormContentSize property.
REM Note that setting this value too high can leave your server vulnerable to denial of
REM service attacks.
set XWIKI_OPTS=%XWIKI_OPTS% -Dorg.eclipse.jetty.server.Request.maxFormContentSize=1000000

java %XWIKI_OPTS% %3 %4 %5 %6 %7 %8 %9 -jar %JETTY_HOME%/start.jar
