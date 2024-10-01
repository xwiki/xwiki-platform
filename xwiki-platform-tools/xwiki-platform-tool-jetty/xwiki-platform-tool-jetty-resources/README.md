XWiki Jetty Configuration
=========================

These instructions are useful when upgrading the Jetty version used.

We brought the following changes from the default Jetty files obtained from the `org.eclipse.jetty:jetty-home` package:

1. Addition of `modules/xwiki.mod`, to group all modules we depend on.
1. Addition of `start.d/xwiki.ini` to configure the following properties:
   1. Disable WAR scanning/hot deployment (since we use static deployment, and it speeds up 
      Jetty) by changing the default values for:
      ```
      jetty.deploy.scanInterval=0
      jetty.deploy.extractWars=false
      ```
   1. Configure Jetty to use RFC3986 for URLs + allow for ambiguous elements in the URLs as XWiki currently needs 
      them (see the doc in start.d/xwiki.ini).
      ```
      jetty.httpConfig.uriCompliance=RFC3986,AMBIGUOUS_PATH_ENCODING,AMBIGUOUS_EMPTY_SEGMENT,AMBIGUOUS_PATH_SEPARATOR
      ``` 
1. Addition of `etc/jetty-xwiki.xml` to print a message in the console when XWiki is started.
1. Modification of `etc/console-capture.xml` to send logs to both the console and files. Namely, we wrap:
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
1. We exclude various files we know we don't need in this context to reduce the size of the resulting zip file