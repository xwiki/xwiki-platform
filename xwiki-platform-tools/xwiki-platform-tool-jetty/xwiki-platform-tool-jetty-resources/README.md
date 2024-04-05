XWiki Jetty Configuration
=========================

These instructions are useful when upgrading the Jetty version used.

We brought the following changes from the default Jetty files obtained from the Jetty zip file (in `jetty-home`):

1. Addition of XWiki license headers to all files
2. In `etc/jetty-deploy.xml` we avoid TLD scanning by replacing:
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
3. Addition of `modules/xwiki.mod`, to group all modules we depend on.
4. Addition of `start.d/xwiki.ini` to configure the following properties:
   1. Disable WAR scanning/hot deployment (since we use static deployment, and it speeds up 
      Jetty) by changing the default values for:
      ```
      jetty.deploy.scanInterval=0
      jetty.deploy.extractWars=false
      ```
   2. Configure Jetty to use RFC3986 for URLs (Jetty 10.0.3+ has added a protection in URLs so that encoded characters 
      such as % are prohibited by default. Since XWiki uses them, we need to configure Jetty to allow for it. See
      https://www.eclipse.org/jetty/documentation/jetty-10/operations-guide/index.html#og-module-server-compliance):
      ```
      jetty.httpConfig.uriCompliance=RFC3986
      ```
5. Addition of `etc/jetty-xwiki.xml` to print a message in the console when XWiki is started.
6. Remove support for JSP (since XWiki doesn't use JSPs) by removing the following from `etc/webdefault.xml`:
   ```
   <servlet id="jsp">
   ...
   </servlet>
   ```
   Also remove the `<servlet-mapping>` just below it.
   Under `<welcome-file-list>` alors remove the `<welcome-file>index.jsp</welcome-file>` line.
7. Remove alpn (we don't need TLS/SSL for a demo packaging) and http2 support by:
   1. Remove `lib/jetty-alpn-client-${jetty.version}.jar` from `modules/client.mod`
   2. Remove references to `alpn` and `http2` module from `modules/https.mod`:
8. Addition of `modules/xwiki-logging.mod` to configure logging for XWiki (provides the Jetty `logging` module name)
9. Modification of `etc/console-capture.xml` to send logs to both the console and files. Namely we wrapp:
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
10. Note that we don't include all `etc/*.xml` files nor all `modules/*.mod` files since we don't use these extra
    features. Note that we kept `apache-jsp.mod` which is needed by the Hibernate Validator (see XWIKI-19314) 
