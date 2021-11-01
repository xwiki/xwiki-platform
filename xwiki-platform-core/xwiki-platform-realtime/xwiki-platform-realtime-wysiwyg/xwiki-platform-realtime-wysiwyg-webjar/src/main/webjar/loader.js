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
define('xwiki-realtime-wysiwygEditor-loader', [
  'jquery',
  'xwiki-realtime-loader'
], function($, Loader) {
  'use strict';

  // TODO: Check if this is really needed.
  // Protect against CKEditor cache on Firefox.
  if (sessionStorage.refreshCk) {
    if ($('#edit').length) {
      try {
        $('#edit')[0].reset();
      } catch (e) {}
    }
    sessionStorage.refreshCk = '';
  }

  var editorId = 'wysiwyg', info = {
    type: editorId,
    href: '&editor=wysiwyg&force=1',
    name: 'WYSIWYG',
    compatible: ['wysiwyg', 'wiki']
  };

  Loader.bootstrap(info).done(function(keys) {
    require(['xwiki-realtime-wysiwygEditor'], function(RealtimeWysiwygEditor) {
      if (RealtimeWysiwygEditor && RealtimeWysiwygEditor.main) {
        keys._update = $.proxy(Loader, 'updateKeys', editorId);
        var config = Loader.getConfig();
        config.rtURL = Loader.getEditorURL(window.location.href, info);
        RealtimeWysiwygEditor.main(config, keys, Loader.isRt).done(function(editor) {
          RealtimeWysiwygEditor.currentMode = editor.mode;
          if (Loader.isRt) {
            $('.cke_button__source').remove();
          }
        });
      } else {
        console.error("Couldn't find RealtimeWysiwygEditor.main, aborting.");
      }
    });
  });

  var displayButtonModal = function() {
    // TODO: This JavaScript code is not loaded anymore on the edit lock page so we need to decide what to do with it
    // (either drop it or find a clean way to load it on the edit lock page).
    var lock = Loader.getDocLock();
    if ($('.realtime-button-' + info.type).length) {
      $('<button class="btn btn-success"/>').click(function() {
        window.location.href = Loader.getEditorURL(window.location.href, info);
      }).text(Loader.messages.get('redirectDialog.join', info.name))
        .prependTo('.realtime-button-' + info.type)
        .before('<br/>');
    } else if (lock) {
      var button = $('<button class="btn btn-primary"/>').text(Loader.messages.get('redirectDialog.create', info.name));
      var buttons = $('.realtime-buttons');
      buttons.append('<br/>').append(button);
      var modal = buttons.data('modal');
      $(button).click(function() {
        buttons.find('button').hide();
        var waiting = $('<div/>', {style: 'text-align:center;'}).appendTo(buttons);
        waiting.append($('<span/>', {
          'class': 'fa fa-spinner fa-2x fa-spin',
          style: 'vertical-align: middle'
        })).append($('<span/>', {
          style: 'vertical-align: middle'
        }).text(Loader.messages.waiting));
        var autoForce = $('<div/>').appendTo(buttons);
        var i = 60;
        var it = setInterval(function() {
          i--;
          autoForce.html('<br/>' + Loader.messages['redirectDialog.autoForce'] + i + 's');
          if (i <= 0) {
            clearInterval(it);
            window.location.href = Loader.getEditorURL(window.location.href, info);
          }
        }, 1000);
        Loader.requestRt('wysiwyg', function(state) {
          // We've received an answer.
          clearInterval(it);
          if (state === false || state === 2) {
            // false: Nobody in the channel
            // 2: Rt should already exist
            console.error(state === false ? "EEMPTY" : "EEXISTS"); // FIXME
            window.location.href = Loader.getEditorURL(window.location.href, info);
          } else if (state === 1) {
            // Accepted
            var whenReady = function(callback) {
              Loader.updateKeys(editorId).done(function(keys) {
                if (keys[editorId + '_users'] > 0) {
                  return void callback();
                }
                setTimeout(function() {
                  whenReady(callback);
                }, 1000);
              });
            };
            whenReady(function() {
              window.location.href = Loader.getEditorURL(window.location.href, {href: '&editor=wysiwyg'});
            });
          }
        });
      });
    }
  };
  displayButtonModal();
  $(document).on('insertButton', displayButtonModal);
});
