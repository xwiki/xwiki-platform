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
  'xwiki-realtime-loader'
], function($, Loader) {
  'use strict';

  var editorId = 'wiki', info = {
    type: editorId,
    // FIXME: Don't hard-code the field name. We should be able to edit any document field (e.g. TextArea xobject
    // properties), not just the document content.
    field: 'content',
    href: '&editor=wiki&force=1',
    name: 'Wiki',
    compatible: ['wiki', 'wysiwyg']
  };

  Loader.bootstrap(info).then(keys => {
    require(['xwiki-realtime-wikiEditor'], function (RealtimeWikiEditor) {
      if (RealtimeWikiEditor && RealtimeWikiEditor.main) {
        keys._update = Loader.updateKeys.bind(Loader, info.field, editorId);
        var config = Loader.getConfig();
        config.rtURL = Loader.getEditorURL(window.location.href, info);
        RealtimeWikiEditor.main(config, keys);
      } else {
        console.error("Couldn't find RealtimeWikiEditor.main, aborting.");
      }
    });
  });

  var getWikiLock = function() {
    var force = document.querySelectorAll('a[href*="editor=wiki"][href*="force=1"][href*="/edit/"]');
    return !!force.length;
  };

  var displayButtonModal = function() {
    // TODO: This JavaScript code is not loaded anymore on the edit lock page so we need to decide what to do with it
    // (either drop it or find a clean way to load it on the edit lock page).
    var lock = Loader.getDocLock();
    var wikiLock = getWikiLock();
    var button = $();
    if ($('.realtime-button-' + info.type).length) {
      button = $('<button class="btn btn-success"></button>').text(
        Loader.messages.get('redirectDialog.join', info.name));
      $('.realtime-button-' + info.type).prepend(button).prepend('<br/>');
    } else if (lock && wikiLock) {
      button = $('<button class="btn btn-primary"></button>').text(
        Loader.messages.get('redirectDialog.create', info.name));
      $('.realtime-buttons').append('<br/>').append(button);
    }
    button.on('click', function() {
      window.location.href = Loader.getEditorURL(window.location.href, info);
    });
  };

  displayButtonModal();
  $(document).on('insertButton', displayButtonModal);
});
