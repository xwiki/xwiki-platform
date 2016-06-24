# Tour Application
This application provides for the users a possibility to create bootstrap-tours for every page they want.

* Project Lead: [Guillaume Delhumeau](http://www.xwiki.org/xwiki/bin/view/XWiki/gdelhumeau)
* [Documentation & Download](http://extensions.xwiki.org/xwiki/bin/view/Extension/Tour+Application)
* [Issue Tracker](http://jira.xwiki.org/browse/TOUR)
* Communication: [Mailing List](http://dev.xwiki.org/xwiki/bin/view/Community/MailingLists>), [IRC]( http://dev.xwiki.org/xwiki/bin/view/Community/IRC)
* [Development Practices](http://dev.xwiki.org)
*  Minimal XWiki version supported: XWiki 6.4.1
* License: LGPL 2.1+.
* [Translations](http://l10n.xwiki.org/xwiki/bin/view/Contrib/TourApplication) You don't need to have technical knowledge to contribute to the translations, feel free to do it!
* Sonar Dashboard: N/A
* Continuous Integration Status: [![Build Status](http://ci.xwiki.org/buildStatus/icon?job=Contrib%20-%20Tour%20Application)](http://ci.xwiki.org/job/Contrib%20-%20Tour%20Application/)

## Developers

### How to build
```
mvn clean install -Pquality --settings maven-settings.xml
```

### Commit new translations
To get the translations done on the [l10n.xwiki.org](http://l10n.xwiki.org/xwiki/bin/view/Contrib/TourApplication) website and commit them into the application, you need to execute the `get-translations.sh` command:

```
## Get the translations
./get-translations.sh

## Look at the new translations
git status

## Add changes (example)
git add src/main/resources/TourCode/TourTranslations.fr.xml

## Commit changes
git commit

## Push them (or make a pull request)
git push origin master
```

It should be done before every release.
