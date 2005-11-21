set LANG=fr_FR.ISO8859-1
set  JETTY_HOME=.
set JETTY_PORT=8080

java -Dfile.encoding=iso-8859-1 -Djetty.home=%JETTY_HOME% -Djetty.port=%JETTY_PORT% -jar %JETTY_HOME%/start.jar
