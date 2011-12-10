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
set XWIKI_OPTS=-Xmx512m -XX:MaxPermSize=128m

REM Ensure the logs directory exists as otherwise Jetty reports an error
if not exist %JETTY_HOME%\logs mkdir %JETTY_HOME%\logs

REM Ensure the work directory exists so that Jetty uses it for its temporary files.
if not exist %JETTY_HOME%\work mkdir %JETTY_HOME%\work

REM Ensure the data directory exists so that XWiki can use it for storing permanent data.
if not exist data mkdir data

REM Specify port on which HTTP requests will be handled
set XWIKI_OPTS=%XWIKI_OPTS% -Djetty.port=%JETTY_PORT%

REM Specify Jetty's home directory
set XWIKI_OPTS=%XWIKI_OPTS% -Djetty.home=%JETTY_HOME%

REM Specify port and key to stop a running Jetty instance
set XWIKI_OPTS=%XWIKI_OPTS% -DSTOP.KEY=xwiki -DSTOP.PORT=8079

REM Specify the encoding to use
set XWIKI_OPTS=%XWIKI_OPTS% -Dfile.encoding=UTF8

REM In order to avoid getting a "java.lang.IllegalStateException: Form too large" error
REM when editing large page in XWiki we need to tell Jetty to allow for large content
REM since by default it only allows for 20K. We do this by passing the
REM org.mortbay.http.HttpRequest.maxFormContentSize property.
REM Note that setting this value too high can leave your server vulnerable to denial of
REM service attacks.
set XWIKI_OPTS=%XWIKI_OPTS% -Dorg.mortbay.jetty.Request.maxFormContentSize=1000000

java %XWIKI_OPTS% %2 %3 %4 %5 %6 %7 %8 %9 -jar %JETTY_HOME%/start.jar
