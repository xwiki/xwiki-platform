This is a Eclipse project used to easily debug a complete XWiki Enterprise (it can also be used for XWiki Enterprise Manager since it comes with it dependencies).

= PREREQUISITES =

* You need M2EClipse 0.9.9 or superior, if Eclipse does not propose it automatically in updates it probably mean it's still the dev version. In that case you should try http://m2eclipse.sonatype.org/update-dev/ as update site.

= INSTALL =

Make sure to import the project as existing Eclipse project and not as Maven project otherwise M2Eclipse will try to rebuild the configuration and could break some things.

There is still some things to do once the project is imported into Eclipse workspace.

== Set configuration ==

This project comes with some example configuration you need to copy/past and set as you need it to:
* WebContent/WEB-INF/xwiki.cfg.default -> WebContent/WEB-INF/xwiki.cfg
* WebContent/WEB-INF/xwiki.properties.default -> WebContent/WEB-INF/xwiki.properties
* WebContent/WEB-INF/hibernate.<database>.cfg.xml.default -> WebContent/WEB-INF/hibernate.cfg.xml

= TODO =

* add support for GWT WYSIWYG