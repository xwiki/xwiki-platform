/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
define('xwiki-developer-shortcuts-messages', {
  prefix: 'core.shortcuts.developer.user.',
  keys: [
    'type',
    'type.error',
    'displayHiddenDocs',
    'displayHiddenDocs.error',
    'ajax.inprogress',
    'ajax.success'
  ]
});

require([
  'xwiki-l10n!xwiki-developer-shortcuts-messages'
], function(l10n) {
  const currentUserPropertiesURL = XWiki.contextPath + '/rest/currentuser/properties';

  /**
   * Perform a PUT on the given REST API. If the request is successful, reload the page.
   *
   * We use this function in order to quickly edit the user properties for developer shortcuts.
   *
   * @param restUrl the URL to use
   * @param errorMessage the message to display if an error occurred in the request
   */
  const developerShortcutsRestCall = async function(restUrl, errorMessage) {
    const notification = new XWiki.widgets.Notification(l10n['ajax.inprogress'], 'inprogress');

    try {
      const response = await fetch(restUrl, {'method': 'PUT'});
      if (response.ok) {
        // Reload the page to apply the user modifications
        notification.replace(new XWiki.widgets.Notification(l10n['ajax.success'], 'done'));
        location.reload();
      } else if (response.status === 500) {
        // The server reports what went wrong in the response body.
        notification.replace(new XWiki.widgets.Notification(await response.text(), 'error'));
      } else {
        notification.replace(new XWiki.widgets.Notification(errorMessage, 'error'));
      }
    } catch (error) {
      console.error(`Failed to update the user property at [${restUrl}]`, error);
      notification.replace(new XWiki.widgets.Notification(errorMessage, 'error'));
    }
  };

  // Append developer shortcuts for toggling userType and hiddenDocuments in the current user profile
  shortcut.add(l10n['type'], function() {
    developerShortcutsRestCall(currentUserPropertiesURL + '/usertype/next', l10n['type.error']);
  }, {'type': shortcut.type.SEQUENCE, 'disable_in_input': true});

  shortcut.add(l10n['displayHiddenDocs'], function() {
    developerShortcutsRestCall(currentUserPropertiesURL + '/displayHiddenDocuments/next',
      l10n['displayHiddenDocs.error']);
  }, {'type': shortcut.type.SEQUENCE, 'disable_in_input': true});
});
