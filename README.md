# application-tour
This application provides for the users a possibility to create bootstrap-tours for every page they want.

* Extension Page: http://extensions.xwiki.org/xwiki/bin/view/Extension/Tour+Application.
* Bug Tracker: http://jira.xwiki.org/browse/TOUR.
* License: LGPL 2.1+.

Translations
==
You can contribute to translate this application in various languages on http://l10n.xwiki.org/xwiki/bin/view/Contrib/TourApplication.

You don't need to have technical knowledge to contribute to the translations, feel free to do it!

How to build
==
```
mvn clean install -Pquality --settings maven-settings.xml
```

Developers: commit new translations
==
To get the translations done on the [l10n.xwiki.org](http://l10n.xwiki.org/xwiki/bin/view/Contrib/TourApplication) website and commit them into the application, you need to execute the `get-translations.sh` command:

```
# ./get-translations.sh
## Look at the new translations
# git status
## Add changes (example)
# git add src/main/resources/TourCode/TourTranslations.fr.xml
## Commit changes
# git commit
## Push them (or make a pull request)
# git push origin master
```

It should be done before every release.