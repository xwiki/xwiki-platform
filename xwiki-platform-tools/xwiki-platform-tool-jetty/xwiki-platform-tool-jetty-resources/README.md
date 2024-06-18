XWiki Jetty Configuration
=========================

These instructions are useful when upgrading the Jetty version used.

We brought the following changes from the default Jetty files obtained from the `org.eclipse.jetty:jetty-home` package:

1. `etc/jetty-deploy.xml`: we avoid TLD scanning by replacing:
   ```
   <Call name="setContextAttribute">
     <Arg>org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern</Arg>
     <Arg>.*/jetty-servlet-api-[^/]*\.jar$|.*/javax.servlet.jsp.jstl-.*\.jar$|.*/org.apache.taglibs.taglibs-standard-impl-.*\.jar$</Arg>
   </Call>
   ``` 
   with:
   ```
   <!-- Note: We don't need to define a "org.eclipse.jetty.server.webapp.ContainerIncludeJarPattern" attribute
        since 1) we don't use tlds and 2) starting with Jetty 8.x, jetty employs a Servlet 3.0 way of finding
        tlds, see http://wiki.eclipse.org/Jetty/Howto/Configure_JSP#Jetty_8.x -->

   <!-- Prevent any JAR scanning for tlds, etc, in order to improve startup speed. Seems it makes us win about
        10% on startup time. -->
   <Call name="setContextAttribute">
     <Arg>org.eclipse.jetty.server.webapp.WebInfIncludeJarPattern</Arg>
     <Arg>somethingnotmatching.jar</Arg>
   </Call>
   ```
2. `etc/webdefault.xml`: remove support for JSP (since XWiki doesn't use JSPs) by removing the following:
   ```
   <servlet id="jsp">
   ...
   </servlet>
   ```
   Also remove the `<servlet-mapping>` just below it.
   Under `<welcome-file-list>` alors remove the `<welcome-file>index.jsp</welcome-file>` line.
4. `etc/console-capture.xml`: send logs to both the console and files. Namely we wrapp:
   ```
   <Arg>
     <New class="org.eclipse.jetty.util.RolloverFileOutputStream">
     ...
   </Arg>
   ```
   With:
   ```
   <Arg>
     <!-- XWiki wants to log to both the console and also to file, and thus we're wrapping the RolloverFileOutputStream
          class with a TeeOutputStream to split the logs. -->
     <New class="org.apache.commons.io.output.TeeOutputStream">
       ...
       <!-- Since we can't send to both out and err, we choose to send to out since logs are not errors by default,
            so it seems the most logical one to use -->
       <Arg><Get class="java.lang.System" name="out"/></Arg>
     </New>
    </Arg>
   ```