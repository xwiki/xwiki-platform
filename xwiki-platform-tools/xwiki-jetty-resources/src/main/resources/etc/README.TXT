----------------------------------------------------------------------
$Id: README.TXT,v 3.122 2005/06/05 15:57:23 gregwilkins Exp $
----------------------------------------------------------------------

Jetty 5
=======

Jetty is an Open Source HTTP Servlet Server written in 100% Java.
It is designed to be light weight, high performance, embeddable,
extensible and flexible, thus making it an ideal platform for serving
dynamic HTTP requests from any Java application.



Downloading
============
Jetty is can be downloaded in one of the following distribution files:

  Jetty-5.0.X.tar.gz  -  This is the binary release for JDK 1.4

  Jetty-5.0.X-extra.tar.gz  -  Extra modules for Jetty, including:
     Jetty demo
     JettyPlus
     jdk 1.2 & 1.3 support
     log4j
     ftp client
     win32 and unix start scripts

  Jetty-5.0.X-src.tar.gz  -  A source only distribution which needs
  to be build using ant. This distribution also excludes the demo.

  Jetty-5.0.X-all.tar.gz  -  Everything in one jar.




Quick Start
===========

Unpack the jetty release Jetty-5.0.x.tar.gz or Jetty-5.0.x-all.tar.gz 
using an archive tool such as tar or winzip.

The default configuration file (etc/jetty.xml) can be run with:

   cd Jetty-5.0.x
   java -jar start.jar

Then point your browser at http://localhost:8080 


The demo server (in the all or extra bundle ) can be run with the commands

   cd Jetty-5.0.x
   java -jar start.jar etc/demo.xml


The JMX variants may be started with 

   java -Dmain.class=org.mortbay.xml.XmlConfiguration -jar start.jar etc/jetty-jmx.xml

or

   java -Dmain.class=org.mortbay.xml.XmlConfiguration -jar start.jar etc/demo-jmx.xml

[ NB. When using java >= 1.5 you will need to add -Djavax.management.builder.initial=mx4j.server.MX4JMBeanServerBuilder
to the command line to avoid a clash with the builting JMX implementation ]


Jetty start.jar 
===============

The start.jar file in the Jetty distribution may be run with

  java -jar start.jar <configfile> ...

For example

  java -jar start.jar etc/admin.xml etc/demo.xml


On some systems with graphical file browsers, it may be possible to start
jetty by double clicking on this jar file. For more details see 

  $JETTY_HOME/src/org/mortbay/start/README.txt
or
  http://jetty.mortbay.org/javadoc/org/mortbay/start/Main.html


If jetty is not being started from the current directory, then 
the jetty.home property should be set:

  java -Djetty.home=C:\jetty -jar start.jar <configfile> ...


If JSPs do not work with this start method, you may want to clearly specify 
the location of the JDK so the compiler can be accessed:

  java -Djava.home=C:\j2sdk1.4.1 -jar start.jar <configfile> ...


If an extra classpaths are needed, they can be added with the jetty.class.path
property:
 
  java -Djetty.class.path=/usr/share/java -jar start.jar <configfile> ...

If no configuration file is provided, start.jar will use etc/admin.xml and 
etc/jetty.xml

The system propoerty -DDEBUG can be set to see the workings of start.jar

  java -DDEBUG -jar start.jar

If you have your own start configuration file (see javadoc), you can use
it with

  java -DSTART=mystart.config -jar start.jar



Jetty stop.jar 
==============

Programs started with the start.jar mechanism may be stopped with the
stop.jar:

   java -jar stop.jar

This connects via a local port to stop the server. The default port can be set with the 
STOP.PORT system property (default 8079 and a port of < 0 disables the stop mechanism). 
If the STOP.KEY system property is set, then a random key is generated and 
written to stdout. This key  must be passed to the stop.jar. eg

  [540] java -DSTOP.PORT=9999 -DSTOP.KEY -jar start.jar
  1rukbu7owi3uo
  17:39:49.082 EVENT  Starting Jetty/4.2.10pre0
  ...


  [526] java -DSTOP.PORT=9999 -DSTOP.KEY=1rukbu7owi3uo -jar stop.jar 


Jetty with Ant
==============

If you have the ant java build tool ant installed, you may use that to
start jetty.  The demo can be started with:

  Unix:  ant demo
  Win32: ant.bat demo


The JMX enabled demo can be started with

  Unix:  ant demo.jmx 
  Win32: ant.bat demo.jmx


The default jetty.xml configuration can be started with:

  Unix:  ant run
  Win32: ant.bat run


An alternative configuration file can be run with

  Unix:  ant -Djetty.run=acme.xml run
  Win32: ant.bat -Djetty.run=acme.xml run



Manual Start
============

If you are using jdk1.4, you should use the path (or non-unix equivalent):

  CLASSPATH=\
    $JETTY_HOME/lib/org.mortbay.jetty.jar:\
    $JETTY_HOME/lib/javax.servlet.jar:\
    $JETTY_HOME/ext/jasper-runtime.jar:\
    $JETTY_HOME/ext/jasper-compiler.jar:\
    $JETTY_HOME/ext/xercesImpl.jar


For jdk1.2 or jdk1.3 us the following classpath, you need to use an
alternative jetty jars and add the XML support. Thus the classpath 
to use is:

  CLASSPATH=\
    $JETTY_HOME/lib/org.mortbay.jetty-jdk1.2.jar:\
    $JETTY_HOME/lib/javax.servlet.jar:\
    $JETTY_HOME/ext/jasper-runtime.jar:\
    $JETTY_HOME/ext/jasper-compiler.jar:\
    $JETTY_HOME/ext/xercesImpl.jar:\
    $JETTY_HOME/ext/xml-apis.jar:\
    $JETTY_HOME/ext/xmlParserAPIs.jar


To use the default SSL provider you may also need to add the the jars below, 
which is a merge of the jsse, jnet and jcert jars from the JSSE package
(see $JETTY_HOME/demo/webapps/jetty/doc/JsseSSL.html for more details):

    $JETTY_HOME/ext/jnet.jar     (not required for JDK 1.4.1)
    $JETTY_HOME/ext/jcert.jar
    $JETTY_HOME/ext/jsse.jar


To use JMX the classpath must also include:

    $JETTY_HOME/ext/jmxri.jar
    $JETTY_HOME/ext/jmxtools.jar 


To run JSP with the JDK compiler, you will also need to add the
jar containing the JDK compiler (if you wish to use another compiler
you will have to configure jasper) and the ant.jar from jakarta-ant 1.5 
or later.

If your JVM supports the -server flag, then it can be a useful
option to include when starting Jetty.  It can reduce memory size and
avoid some JVM crashes.


External Jars
=============

The ext directories within the Jetty hierarchy contain external libraries
that are required to compile run Jetty. Many of these jars may now be
provided by your JVM or elsewhere on your classpath.  If so, these jars
can probably be safely removed from the Jetty distribution.

A SAX XML parser is required by webapp components of Jetty and the xerces
parser is included as well as the javax apis for this:

   $JETTY_HOME/ext/xercesImpl.jar      
   $JETTY_HOME/ext/xml-apis.jar
   $JETTY_HOME/ext/xmlParserAPIs.jar


If JSP is to be used, then the Jasper jars from jakarta and an ant jar must
be present.  Jetty includes Jasper2, but jasper1 may be safely substituted:

   $JETTY_HOME/ext/jasper-compiler.jar
   $JETTY_HOME/ext/jasper-runtime.jar
   $JETTY_HOME/ext/ant.jar

If SSL is to be used then the JSSE libraries may need to be present plus 
the reference implementation from sun:

   $JETTY_HOME/ext/jnet.jar
   $JETTY_HOME/ext/jcert.jar
   $JETTY_HOME/ext/jsse.jar

If SSL is to be used, then the JMX library and tools need present:

   $JETTY_HOME/ext/jmxri.jar
   $JETTY_HOME/ext/jmxtools.jar


There are additional ext jars that may be included in the extra/ext
directory.  These are docuented elsewhere.


LOGGING AND DEBUGGING
=====================

Since 5.0, Jetty has used jakarta commons logging.  This is a facade over
other logging mechanism.   By default, this is configured in start.config
to use the Jetty logger. Debugging can be enabled with -DDEBUG or see the
jetty.xml and demo.xml files for more examples.

To use the JDK logger:
  java -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Jdk14Logger -jar start.jar

To use log4j:
  java -Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Log4JLogger -jar start.jar

Note that any jetty logging config should be removed from the configuration files before using another log mechanism

For more information see the Jetty Tutorial
http://jetty.mortbay.org/jetty/tut/logging.html


Package contents
================

  src     - The source for the HTTP server and servlet container.
  lib     - The jars of the compile jetty code.
  ext     - The external jars needed for the main jetty server.
  etc     - The config files and resources.
  demo    - Demos of the HTTP server and servlet container.
  test    - Test harnesses.
  webapps - The default location for war files or webapps.
  webapps/template
          - A template webapp that can be used to base your own on.
  extra   - Directory of optional extras
            /plus          - Extended jetty with JNDI, transaction, JAAS & log4j
            /ftp           - FTP client
            /j2ee          - J2EE distributions and integration 
            /jdk1.2        - Classes for JDK < 1.4
            /loadbalancer  - A simple load balancer

Support FAQ
===========
 + The jetty-support@yahoogroups.com list is for jetty related support 
   questions.  General questions about servlets and/or java should not 
   be posted here.

 + Before posting, please check the archives that the question has
   not been asked before by checking the archives at 
       http://groups.yahoo.com/group/jetty-support

 + Please also read the improving documentation at
       http://jetty.mortbay.org/jetty/resources.html
       
   Please read the tutorial, then read it again.

 + Jetty 4 configuration is based around the concept of contexts,
   which were introduced in the 2.2 servlet specification.  It is
   important to understand contexts when configuring Jetty and it may
   be worth while reading the 2.4 servlet specification in addtion to
   the Jetty documentation.

 + Before posting, try increasing the debug output of Jetty.  Debugging 
   control is described in the README file.  Even if you cannot spot the
   problem in the output, it may be valuable to make the trace available
   to whoever helps you with your problem.

 + When posting a support question, please provide as much information as
   possible, which should include:
      - The version of Jetty.
      - The version of java.
      - Your Operating system.
      - Overview of your configuration.
      - What URL was tried.
      - What browser and what it rendered.
      - Details of the problem, including stack traces and/or log files.
      - Details of what you have tried to make it work.
      
 + For JSP problems, consider checking JSP forums first. Jetty uses the
   Jasper engine from jakarta.apache.org - bugs and all.
   If you can write a small JSP that demonstrates your problem, you 
   will get much faster results.

 + For general class loading problems, the solution is often best to start
   with your classes on the system classpath until you are more familiar
   with the configuration of Contexts. 

 + If in doubt about if you should post or not - please post, we are pretty
   friendly and will tell you nicely if it is not appropriate.



Building from source
====================
An Ant build file is included in build.xml.   Ant is available from

    http://jakarta.apache.org/ant/index.html

You will also need the optional ant junit task and the junit jar in the
lib directory of ant (this is a common setup for ant distributions).
The ant.properties file can be edited to customize the environment and
then the following ant targets can be used to build and run Jetty:

    ant         - Build source
    ant all     - Build everything
    ant test    - Build and run tests
    ant demo    - Build and run the demo
    ant dist    - Build the distriutions
    ant clean   - Remove generated files


Test Harnesses
==============
The class org.mortbay.http.TestRFC2616 is a test harness linked
to the RFC document.  This and the other test harnesses can be run with

  java org.mortbay.util.TestHarness
  java org.mortbay.http.TestHarness

Note that the test harnesses are not in the jar file, so you will need
to build all the source and run with the source hierarchy in the CLASSPATH.


Running Watchdog Tests
======================
See the etc/watchdog.xml configuration file comments for instructions
on how to run the Jakarta Watchdog 4.0 tests.


Distribution
============
Jetty distributions and information is available from:

   http://jetty.mortbay.org
   http://sourceforge.net/project/jetty
   ftp://jetty.mortbay.org/pub



How to Contribute
=================
Comments, contributions, feedback, bugs, testing, etc. please....

The mailing lists are the best place to start:

  http://www.yahoogroups.com/group/jetty-announce/
  http://www.yahoogroups.com/group/jetty-discuss/
  http://www.yahoogroups.com/group/jetty-support/
  http://sourceforge.net/mail/?group_id=7322



NOTES
=====

HTTP Server vs Servlet Server
-----------------------------
Jetty can be considered both as a HTTP server with it own powerful
extension architecture and as a Servlet Container implementing the
standards for web applications.  The style of configuration of these
two view is significantly different.  The Jetty tutorial provided
by the demo server should be read in detail to understand the 
difference between these two approaches.


JDK 1.4 Support
---------------
Jetty is written for JDK 1.4.  However it can be built and run on 1.3
and 1.2.  The build file for this and the source changes required are
in the $JETTY_HOME/extra/jdk1.2 directory.

These build an alternate jetty jar called org.mortbay.jetty-jdk1.2.jar



Minimal HTTP jar
----------------
A minimal HTTP server can be built with the ant target mini.http.jar.
This builds the lib/org.mortbay.http.jar file which contains:
  + Full HTTP/1.1 server
  + Basic Authentication support
  + ResourceHandler with cache and range support.
  + DumpHandler as dynamic content demo.
  + NCSA request log
  + javax.servlet.http.Cookie support.

The mini server can be run with

  java -jar lib/org.mortbay.http.jar



JSP Engine
----------
Jetty uses the Jasper JSP engine from jakarta to provide java
server pages. Jetty switched to use Jasper2 for the 4.1.x releases,
as it promises greater performance and standards compliance.
However, jasper2 requires the ant.jar in the classpath and still
has some backwards compatibility issues with older JSPs.

The jasper2 engine can simply be replace with the jasper1 engine
by replacing the org.apache.jasper.jar from a Jetty 4.0 release.


JSP Classpaths
--------------
The context classloader is passed to the Jasper JspServlet and
is used as the parent loader of the JspLoader.
A file classpath is also passed for used by the JSP compiler. Note
that if the context is a packed WAR file, then there is no
reasonable file based classpath that can be passed to the compiler.

If this proves to be a problem for you, then unpack your WAR files
(which is what most other containers do by default anyway).



Unix SystemV Init
-----------------
Jetty includes a SystemV Init script that has been tested under
RedHat Linux 6.2. This script allows Jetty to be automatically started
and stoped when the operating system is booted or shutdown.

To use the script, follow these steps:

  - Login as root.

  - Make the script executable:

       chmod u+x $JETTY_HOME/bin/jetty.sysvinit

  - Place a  symbolic link to the script in the /etc/rc.d/init.d directory:

       ln -s $JETTY_HOME/bin/jetty.sysvinit /etc/rc.d/init.d/jetty

    you can also copy the script there. Just remember to make it executable.

  - Edit the script and make sure that the JETTY_HOME and JAVA_HOME
    variables are set properly.

  - Enable the script:

      chkconfig --add jetty
      chkconfig --level 345 jetty on

    you can also use ntsysv if available for this purpose.

To start the jetty server for the first time without rebooting the machine,
execute:

   /etc/rc.d/init.d/jetty start

Check /var/log/messages to see if everything worked fine.

The Jetty SystemV Init script uses the $JETTY_HOME/bin/jetty.sh shell
script to launch Jetty. Read the shell script's documentation above,
or the one in the script's initial comments to learn about available
configuration options.


Win32 Service
-------------
The win32 directory contains instructions of how to start Jetty as
a win32 service.


Web Application Security
------------------------
Jetty makes the following interpretations for the configuration of security
constraints within a web.xml file:

 + Methods PUT, DELETE and GET are disabled unless explicitly enabled.

 + If multiple security-constraints are defined, the most specific
   applies to a request.

 + A security-constraint an empty auth-constraint forbids all access by any
   user:

    <security-constraint>
      <web-resource-collection>
       <web-resource-name>Forbidden</web-resource-name>
        <url-pattern>/auth/noaccess/*</url-pattern>
      </web-resource-collection>
      <auth-constraint/>
    </security-constraint>


 + A security constraint with an auth constraint with a role of "*"
   gives access to all authenticated users:

    <security-constraint>
      <web-resource-collection>
        <web-resource-name>Any User</web-resource-name>
        <url-pattern>/auth/*</url-pattern>
      </web-resource-collection>
      <auth-constraint>
        <role-name>*</role-name>
      </auth-constraint>
    </security-constraint>

 + A security-constraint with no auth-constraint and no data contraint
   gives access to any request:

    <security-constraint>
      <web-resource-collection>
        <web-resource-name>Relax</web-resource-name>
        <url-pattern>/auth/relax/*</url-pattern>
      </web-resource-collection>
    </security-constraint>


 + By default or when the org.mortbay.util.FileResource.checkAliases is not false, 
   then the FileResouce class compares the absolutePath and canonicalPath and
   treats the resource as not found if they do not match.  THIS means
   that win32 platforms need to exactly match the case of drive letters
   and filenames.

 + Dynamic servlets by default, can only be loaded from the context classpath.
   Use ServletHandler.setServeDynamicSystemServlets to control this behaivour.


It is strongly recommended that secure WebApplications take following
approach.  All access should be denied by default with

    <security-constraint>
      <web-resource-collection>
       <web-resource-name>Default</web-resource-name>
        <url-pattern>/</url-pattern>
      </web-resource-collection>
      <auth-constraint/>
    </security-constraint>


Specific access should then be granted with constraints like:

  <security-constraint>
    <web-resource-collection>
      <url-pattern>/public/*</url-pattern>
      <url-pattern>/images/*</url-pattern>
      <http-method>GET</http-method>
      <http-method>HEAD</http-method>
    </web-resource-collection>
    <web-resource-collection>
      <url-pattern>/servlet/*</url-pattern>
      <http-method>GET</http-method>
      <http-method>HEAD</http-method>
      <http-method>POST</http-method>
    </web-resource-collection>
    <auth-constraint>
      <role-name>*</role-name>
    </auth-constraint>
  </security-constraint>


Session Security
----------------
Jetty uses the standard java.util.Random class to generate session IDs.  This
may be insufficient for high security sites.  The SessionManager instance can
be initialized with a more secure random number generator, such as
java.security.SecureRandom. The XML to do this to a webapplication is:

  <Call name="addWebApplication">
    <Arg>/myapp/*</Arg>
    <Arg><SystemProperty name="jetty.home" default="."/>/demo/webapps/myapp</Arg>
    <Arg><SystemProperty name="jetty.home" default="."/>/etc/webdefault.xml</Arg>
    <Arg type="boolean">false</Arg> 
    <Call name="getServletHandler">
      <Set name="sessionManager">
        <New class="org.mortbay.jetty.servlet.HashSessionManager">
          <Arg><New class="java.security.SecureRandom"/></Arg>
        </New>
      </Set>
    </Call>
  </Call>

Note: initialising the SecureRandom object is a one-off time consuming
operation which may cause the initial request to take much longer.


Authentication Realms
---------------------
The authentication mechanisms use and abstract
org.mortbay.http.UserRealm interface for authentication. The default
implementation is a hashtable that is initialized from a properties
file. Jetty also includes org.mortbay.http.JDBCUserRealm for SQL based
authentication data.  Users may simply implement their own Realms and
platforms such as JBoss use this mechanism to link the Jetty
authentication mechanism to their own.

