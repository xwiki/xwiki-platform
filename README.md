# CKEditor Integration with XWiki

Adds support for editing wiki pages using [CKEditor](http://ckeditor.com/).

Starting with XWiki 8.2 this is the default WYSIWYG content editor. On older versions of XWiki this application extends the Edit menu with a new entry called 'CKEditor' that loads a new edit mode where you can edit the content of the wiki page using the CKEditor.

* Project Lead: [Marius Dumitru Florea](http://www.xwiki.org/xwiki/bin/view/XWiki/mflorea)
* [Documentation & Download](http://extensions.xwiki.org/xwiki/bin/view/Extension/CKEditor+Integration)
* [Issue Tracker](http://jira.xwiki.org/browse/CKEDITOR)
* Communication: [Mailing List](http://dev.xwiki.org/xwiki/bin/view/Community/MailingLists>), [IRC]( http://dev.xwiki.org/xwiki/bin/view/Community/IRC)
* [Development Practices](http://dev.xwiki.org)
* Minimal XWiki version supported: XWiki 6.2.5
* License: LGPL 2.1+
* [Translations](http://l10n.xwiki.org/xwiki/bin/view/Contrib/CKEditorIntegration)
* Sonar Dashboard: N/A
* Continuous Integration Status: [![Build Status](http://ci.xwiki.org/job/XWiki%20Contrib/job/application-ckeditor/job/master/badge/icon)](http://ci.xwiki.org/view/Contrib/job/XWiki%20Contrib/job/application-ckeditor/job/master/)

## Building

You need Maven 3.1+ in order to build this extension.

## Release Steps

    ## Release the new version in JIRA and create the next version.

    ## Update the translations.
    ## * download the translation pack from l10n and unpack
    ##   http://l10n.xwiki.org/xwiki/bin/view/L10NCode/GetTranslationFile?name=Contrib.CKEditorIntegration&app=Contrib
    ## * copy the translation pages to the UI module sources
    ## * apply XAR format and review the changes
    ## * commit only the significant changes

    ## Prepare the tag for the new version.
    mvn org.apache.maven.plugins:maven-release-plugin:2.5:prepare -DautoVersionSubmodules -DskipTests -Darguments="-DskipTests" -Pintegration-tests

    ## Select Java 7 in order to silence the enforcer (even if we don't have Java code ATM,
    ## a WebJar is a Jar and we support older versions of XWiki that run on Java 7).
    sudo update-alternatives --config java
    sudo update-alternatives --config javac

    ## Perform the release
    ## We don't release the tests module ATM because it requires Java 8 and we need to build with Java 7.
    mvn org.apache.maven.plugins:maven-release-plugin:2.5:perform

    ## Restore the Java version.
    sudo update-alternatives --config java
    sudo update-alternatives --config javac

    ## Finish the release on http://nexus.xwiki.org (Staging Repositories)

    ## Update the documentation page on http://extensions.xwiki.org
    ## Keep the release notes (the list of JIRA issues) only for the 2 most recent releases.

    ## Announce the release

    ## Update the version used in XWiki Enterprise
