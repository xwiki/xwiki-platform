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
define('xwiki-realtime-wikiEditor-loader', [
  'jquery',
  'xwiki-realtime-config',
  'xwiki-realtime-loader',
  'xwiki-realtime-document',
  'xwiki-realtime-errorBox'
], function($, realtimeConfig, Loader, doc, ErrorBox) {
  'use strict';

  if (!Loader) {
    return;
  }

  var getWikiLock = function() {
    var force = document.querySelectorAll('a[href*="editor=wiki"][href*="force=1"][href*="/edit/"]');
    return !!force.length;
  };

  var lock = Loader.getDocLock();
  var wikiLock = getWikiLock();

  var editorId = 'wiki', info = {
    type: editorId,
    href: '&editor=wiki&force=1',
    name: 'Wiki',
    compatible: ['wiki', 'wysiwyg']
  };

  var beforeLaunchRealtime = function(keys) {
    if (Loader.isRt) {
      Loader.whenReady(function(wsAvailable) {
        Loader.isRt = wsAvailable;
        launchRealtime(keys);
      });
    } else {
      launchRealtime(keys);
    }
  };

  var launchRealtime = function(keys) {
    require(['xwiki-realtime-wikiEditor'], function (RealtimeWikiEditor) {
      if (RealtimeWikiEditor && RealtimeWikiEditor.main) {
        keys._update = $.proxy(Loader, 'updateKeys', editorId);
        var config = Loader.getConfig();
        config.rtURL = Loader.getEditorURL(window.location.href, info);
        RealtimeWikiEditor.main(config, keys);
      } else {
        console.error("Couldn't find RealtimeWikiEditor.main, aborting.");
      }
    });
  };

  if (lock) {
    // Found a lock link. Check active sessions.
    Loader.checkSessions(info);
  } else if (window.XWiki.editor === 'wiki') {
    // No lock and we are using wiki editor. Start realtime.
    Loader.updateKeys(editorId).done(function(keys) {
      if (!keys[editorId] || !keys.events) {
        ErrorBox.show('unavailable');
        console.error('You are not allowed to create a new realtime session for that document.');
      }
      if (Object.keys(keys.active).length > 0) {
        if (keys[editorId + '_users'] > 0 || Loader.isForced) {
          beforeLaunchRealtime(keys);
        } else {
          console.log('Join the existing realtime session or create a new one.');
          Loader.displayModal(editorId, Object.keys(keys.active), $.proxy(beforeLaunchRealtime, null, keys), info);
        }
      } else {
        beforeLaunchRealtime(keys);
      }
    });
  }

  var displayButtonModal = function() {
    var button = $();
    if ($('.realtime-button-' + editorId).length) {
      button = $('<button class="btn btn-success"/>').text(Loader.messages.get('redirectDialog.join', 'Wiki'));
      $('.realtime-button-' + editorId).prepend(button).prepend('<br/>');
    } else if (lock && wikiLock) {
      button = $('<button class="btn btn-primary"/>').text(Loader.messages.get('redirectDialog.create', 'Wiki'));
      $('.realtime-buttons').append('<br/>').append(button);
    }
    button.click(function() {
      window.location.href = Loader.getEditorURL(window.location.href, info);
    });
  };

  displayButtonModal();
  $(document).on('insertButton', displayButtonModal);
});
