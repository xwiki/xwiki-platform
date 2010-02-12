This is a Eclipse project used to easily debug a complete XWiki Enterprise (it can also be used for XWiki Enterprise Manager since it comes with it dependencies).

= PREREQUISITES =

* You need M2EClipse 0.9.9 or superior, if Eclipse does not propose it automatically in updates it probably mean it's still the dev version. In that case you should try http://m2eclipse.sonatype.org/update-dev/ as update site.

= INSTALL =

Make sure to import the project as existing Eclipse project and not as Maven project otherwise M2Eclipse will try to rebuild the configuration and could break some things.

There is still some things to do once the project is imported into Eclipse workspace.

== Add Eclipse linked resources ==

Most of this project resources are "virtual", meaning that this project is full of linked folders and files targeting the sources in different checkouted projects on the filesystem.

Best thing is generally to checkout http(s)://svn.xwiki.org/svnroot/xwiki/platform/trunks/.

For theses links to work for everyone this project is using Eclipse linked resource to find the sources. Go to Window->Preferences->General->Workspace->Linked Resources and add the following links:
* XWIKI_CORE: path to the xwiki-core-parent maven project (/core sub-folder of platform/trunks)
* XWIKI_SKINS: path to xwiki-skins maven project (/skins sub-folder of platform/trunks)
* XWIKI_WEB: path to the xwiki-web-parent maven project (/web sub-folder of platform/trunks)

Notes: this will be replaced by internal links at subversion level when svn.xwiki.org will be using Subversion 1.6

== Set configuration ==

This project comes with some example configuration you need to copy/past and set as you need it to:
* WebContent/WEB-INF/xwiki.cfg.default -> WebContent/WEB-INF/xwiki.cfg
* WebContent/WEB-INF/xwiki.properties.default -> WebContent/WEB-INF/xwiki.properties
* WebContent/WEB-INF/hibernate.<database>.cfg.xml.default -> WebContent/WEB-INF/hibernate.cfg.xml

= TODO =

* add support for GWT WYSIWYG
* replace Eclipse linked resources by subversion 1.6 internal links when svn.xwiki.org will be upgraded to 1.6