#!/bin/bash
# ---------------------------------------------------------------------------
# See the NOTICE file distributed with this work for additional
# information regarding copyright ownership.
#
# This is free software; you can redistribute it and/or modify it
# under the terms of the GNU Lesser General Public License as
# published by the Free Software Foundation; either version 2.1 of
# the License, or (at your option) any later version.
#
# This software is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with this software; if not, write to the Free
# Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
# 02110-1301 USA, or see the FSF site: http://www.fsf.org.
# ---------------------------------------------------------------------------

set -e

function first_start() {
  configure
  touch /usr/local/tomcat/webapps/ROOT/.first_start_completed
}

function other_starts() {
  mkdir -p /usr/local/xwiki/data
  restoreConfigurationFile 'hibernate.cfg.xml'
  restoreConfigurationFile 'xwiki.cfg'
  restoreConfigurationFile 'xwiki.properties'
}

# \$1 - the path to xwiki.[cfg|properties]
# \$2 - the setting/property to set
# \$3 - the new value
function xwiki_replace() {
  sed -i s~"\\#\\? \\?\$2 \\?=.*"~"\$2=\$3"~g "\$1"
}

# \$1 - the setting/property to set
# \$2 - the new value
function xwiki_set_cfg() {
  xwiki_replace /usr/local/tomcat/webapps/ROOT/WEB-INF/xwiki.cfg "\$1" "\$2"
}

# \$1 - the setting/property to set
# \$2 - the new value
function xwiki_set_properties() {
  xwiki_replace /usr/local/tomcat/webapps/ROOT/WEB-INF/xwiki.properties "\$1" "\$2"
}

# Allows to use sed but with user input which can contain special sed characters such as \\, / or &.
# \$1 - the text to search for
# \$2 - the replacement text
# \$3 - the file in which to do the search/replace
function safesed {
  sed -i "s/\$(echo \$1 | sed -e 's/\\([[\\/.*]\\|\\]\\)/\\\\&/g')/\$(echo \$2 | sed -e 's/[\\/&]/\\\\&/g')/g" \$3
}

# \$1 - the config file name found in WEB-INF (e.g. "xwiki.cfg")
function saveConfigurationFile() {
  if [ -f "/usr/local/xwiki/data/\$1" ]; then
     echo "  Reusing existing config file \$1..."
     cp "/usr/local/xwiki/data/\$1" "/usr/local/tomcat/webapps/ROOT/WEB-INF/\$1"
  else
     echo "  Saving config file \$1..."
     cp "/usr/local/tomcat/webapps/ROOT/WEB-INF/\$1" "/usr/local/xwiki/data/\$1"
  fi
}

# \$1 - the config file name to restore in WEB-INF (e.g. "xwiki.cfg")
function restoreConfigurationFile() {
  if [ -f "/usr/local/xwiki/data/\$1" ]; then
     echo "  Synchronizing config file \$1..."
     cp "/usr/local/xwiki/data/\$1" "/usr/local/tomcat/webapps/ROOT/WEB-INF/\$1"
  else
     echo "  No config file \$1 found, using default from container..."
     cp "/usr/local/tomcat/webapps/ROOT/WEB-INF/\$1" "/usr/local/xwiki/data/\$1"
  fi
}

function configure() {
  echo 'Configuring XWiki...'
  safesed "replaceuser" \${DB_USER:-xwiki} /usr/local/tomcat/webapps/ROOT/WEB-INF/hibernate.cfg.xml
  safesed "replacepassword" \${DB_PASSWORD:-xwiki} /usr/local/tomcat/webapps/ROOT/WEB-INF/hibernate.cfg.xml
  safesed "replacecontainer" \${DB_HOST:-db} /usr/local/tomcat/webapps/ROOT/WEB-INF/hibernate.cfg.xml
  safesed "replacedatabase" \${DB_DATABASE:-xwiki} /usr/local/tomcat/webapps/ROOT/WEB-INF/hibernate.cfg.xml

  echo '  Using filesystem-based attachments...'
  xwiki_set_cfg 'xwiki.store.attachment.hint' 'file'
  xwiki_set_cfg 'xwiki.store.attachment.versioning.hint' 'file'
  xwiki_set_cfg 'xwiki.store.attachment.recyclebin.hint' 'file'
  echo '  Generating authentication validation and encryption keys...'
  xwiki_set_cfg 'xwiki.authentication.validationKey' "\$(cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 32 | head -n 1)"
  xwiki_set_cfg 'xwiki.authentication.encryptionKey' "\$(cat /dev/urandom | tr -dc 'a-zA-Z0-9' | fold -w 32 | head -n 1)"

  echo '  Setting permanent directory...'
  xwiki_set_properties 'environment.permanentDirectory' '/usr/local/xwiki/data'
  echo '  Configure libreoffice...'
  xwiki_set_properties 'openoffice.autoStart' 'true'

  # If the files already exist then copy them to the XWiki's WEB-INF directory. Otherwise copy the default config
  # files to the permanent directory so that they can be easily modified by the user. They'll be synced at the next
  # start.
  mkdir -p /usr/local/xwiki/data
  saveConfigurationFile 'hibernate.cfg.xml'
  saveConfigurationFile 'xwiki.cfg'
  saveConfigurationFile 'xwiki.properties'
}

# This if will check if the first argument is a flag but only works if all arguments require a hyphenated flag
# -v; -SL; -f arg; etc will work, but not arg1 arg2
if [ "\${1:0:1}" = '-' ]; then
    set -- xwiki "\$@"
fi

# Check for the expected command
if [ "\$1" = 'xwiki' ]; then
  if [[ ! -f /usr/local/tomcat/webapps/ROOT/.first_start_completed ]]; then
    first_start
  else
    other_starts
  fi
  shift
  set -- catalina.sh run "\$@"
fi

# Else default to run whatever the user wanted like "bash"
exec "\$@"
