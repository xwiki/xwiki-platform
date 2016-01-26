#!/bin/bash
USER="$L10N_USER"
PASS="$L10N_PASSWORD"

PROJECT_TRUNKS=`pwd`

function fix_author() {
    find ./ -name '*.xml' -exec sed -i -e 's#<creator>XWiki.Admin</creator>#<creator>xwiki:XWiki.Admin</creator>#' -e 's#<author>XWiki.Admin</author>#<author>xwiki:XWiki.Admin</author>#' -e 's#<contentAuthor>XWiki.Admin</contentAuthor>#<contentAuthor>xwiki:XWiki.Admin</contentAuthor>#' {} \; -print
}

function format_xar() {
    ## due to https://github.com/mycila/license-maven-plugin/issues/37 we need to perform "mvn xar:format" twice.
    mvn xar:format --settings maven-settings.xml
    mvn xar:format --settings maven-settings.xml
}

function download_translations() {
    wget $1 --user="${USER}" --password="${PASS}" --auth-no-challenge -O ./translations.zip ##&&
    unzip -o translations.zip &&
    rm translations.zip || $(git clean -dxf && exit -1)
    fix_author
}

function read_user_and_password() {
    if [[ -z "$USER" || -z "$PASS" ]]; then
        echo -e "\033[0;32mEnter your l10n.xwiki.org credentials:\033[0m"
        read -e -p "user> " USER
        read -e -s -p "pass> " PASS
        echo ""
    fi

    if [[ -z "$USER" || -z "$PASS" ]]; then
      echo -e "\033[1;31mPlease provide both user and password in order to be able to get the translations from l10n.xwiki.org.\033[0m"
      exit -1
    fi
}

##
## Download the translations and format them.
## Note: it is the responsability of the developer to actually commit them.
##
read_user_and_password
cd ${PROJECT_TRUNKS}/src/main/resources/  || exit -1
download_translations 'http://l10n.xwiki.org/xwiki/bin/view/L10NCode/GetTranslationFile?name=Contrib.TourApplication&app=Contrib'
cd ${PROJECT_TRUNKS} && format_xar
