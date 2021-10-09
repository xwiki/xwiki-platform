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
    name: 'Wiki'
  };

  var parseKeyData = function(channels) {
    var keys = {};
    var eventsChannel = channels.getByPath([doc.language, 'events', '1.0']);
    var userDataChannel = channels.getByPath([doc.language, 'events', 'userdata']);
    var editorChannel = channels.getByPath([doc.language, 'content', editorId]);
    if (!eventsChannel || !userDataChannel || !editorChannel) {
      console.error('Missing document channels.');
    } else {
      keys = $.extend(keys, {
        [editorId]: editorChannel.key,
        [editorId + '_users']: editorChannel.userCount,
        events: eventsChannel.key,
        userdata: userDataChannel.key,
        active: {}
      });
      // Collect the other active real-time editing session (for the document content field) that are using a different
      // editor (e.g. the WYSIWYG editor).
      channels.getByPathPrefix([doc.language, 'content']).forEach(channel => {
        if (channel.userCount > 0 && JSON.stringify(channel.path) !== JSON.stringify(editorChannel.path)) {
          keys.active[channel.path.slice(2).join('/')] = channel;
        }
      });
    }
    return keys;
  };

  var updateKeys = function() {
    return doc.getChannels({
      path: [
        doc.language + '/events/1.0',
        doc.language + '/events/userdata',
        doc.language + '/content/' + editorId,
        // Check also if the content is edited in real-time with other editors at the same time.
        doc.language + '/content/',
      ],
      create: true
    }).then(parseKeyData);
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
    require(['jquery', 'xwiki-realtime-wikiEditor'], function ($, RealtimeWikiEditor) {
      if (RealtimeWikiEditor && RealtimeWikiEditor.main) {
        keys._update = updateKeys;
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
    updateKeys().done(function(keys) {
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
