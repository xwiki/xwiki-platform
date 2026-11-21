Put here the channels configuration files or use embedded JGroups configuration files (recommended).

The channel configuration file should match the channel identifier with ".xml" extension. For example for channel "udp" the file is "udp.xml".

A configuration file placed here takes precedence over the embedded ones, which are looked up in this order:
* the configuration files shipped by XWiki (in the xwiki-platform-observation-remote jar, folder observation/remote/jgroups/):
  * kubernetes.xml: TCP transport with DNS based discovery, for instances running in Kubernetes. It requires the system property jgroups.dns.dns_query (the DNS name of a headless Service selecting the XWiki pods, with publishNotReadyAddresses enabled) and accepts jgroups.bind_addr, jgroups.bind_port (7800), jgroups.port_range (0) and jgroups.join_timeout (3s). See the comment at the top of the file for the details.
* the configuration files embedded in the jgroups jar file located in WEB-INF/lib/ folder (for example tcp.xml and udp.xml).

You can find detailed documentation on what to put in JGroups configuration files at http://www.jgroups.org/manual5/index.html
