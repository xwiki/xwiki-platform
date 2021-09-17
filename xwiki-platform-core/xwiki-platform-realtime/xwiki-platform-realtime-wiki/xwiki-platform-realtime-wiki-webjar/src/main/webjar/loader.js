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
  'xwiki-realtime-errorBox'
], function($, realtimeConfig, Loader, ErrorBox) {
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
    name: 'Wiki'
  };

  var getKeyData = function(config) {
    return [
      {doc: config.reference, mod: config.language + '/events', editor: '1.0'},
      {doc: config.reference, mod: config.language + '/events', editor: 'userdata'},
      {doc: config.reference, mod: config.language + '/content', editor: editorId}
    ];
  };

  var parseKeyData = function(config, keysResultDoc) {
    var keys = {},
      keysResult = keysResultDoc[config.reference],
      keysResultContent = keysResult[config.language + '/content'],
      keysResultEvents = keysResult[config.language + '/events'];

    if (!keysResult) {
      console.error('Unexpected error with the document keys.');
    } else if (!keysResultContent) {
      console.error('Missing content keys in the document keys.');
    } else if (!keysResultEvents) {
      console.error('Missing event keys in the document keys.');
    } else if (!keysResultContent[editorId] || !keysResultEvents['1.0']) {
      console.error(`Missing mandatory "${editorId}" key in the document keys.`);
    } else {
      keys[editorId] = keysResultContent[editorId].key;
      keys[editorId + '_users'] = keysResultContent[editorId].users;
      keys.events = keysResultEvents['1.0'].key;
      keys.userdata = keysResultEvents.userdata.key;

      keys.active = {};
      for (var key in keysResultContent) {
        if (key !== editorId && keysResultContent[key].users > 0) {
          keys.active[key] = keysResultContent[key];
        }
      }
    }

    return keys;
  };

  var updateKeys = function(callback) {
    var config = Loader.getConfig();
    Loader.getKeys(getKeyData(config), function(keysResultDoc) {
      var keys = parseKeyData(config, keysResultDoc);
      callback(keys);
    });
  };

  var launchRealtime = function(config, keys) {
    require(['jquery', 'xwiki-realtime-wikiEditor'], function ($, RTWiki) {
      if (RTWiki && RTWiki.main) {
        keys._update = updateKeys;
        RTWiki.main(config, keys);
      } else {
        console.error("Couldn't find RTWiki.main, aborting.");
      }
    });
  };

  if (lock) {
    // Found a lock link. Check active sessions.
    Loader.checkSessions(info);
  } else if (window.XWiki.editor === 'wiki' || realtimeConfig.demoMode) {
    // No lock and we are using wiki editor. Start realtime.
    var config = Loader.getConfig();
    updateKeys(function (keys) {
      if (!keys[editorId] || !keys.events) {
        ErrorBox.show('unavailable');
        console.error("You are not allowed to create a new realtime session for that document.");
      }
      if (Object.keys(keys.active).length > 0) {
        if (keys[editorId + '_users'] > 0 || Loader.isForced) {
          launchRealtime(config, keys);
        } else {
          var callback = function() {
            launchRealtime(config, keys);
          };
          console.log("Join the existing realtime session or create a new one");
          Loader.displayModal(editorId, Object.keys(keys.active), callback, info);
        }
      } else {
        launchRealtime(config, keys);
      }
    });
  }

  var displayButtonModal = function() {
    var button;
    if ($('.realtime-button-rtwiki').length) {
      button = $('<button class="btn btn-success"/>').text(Loader.messages.get('redirectDialog.join', 'Wiki'));
      $('.realtime-button-rtwiki').prepend(button).prepend('<br/>');
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
