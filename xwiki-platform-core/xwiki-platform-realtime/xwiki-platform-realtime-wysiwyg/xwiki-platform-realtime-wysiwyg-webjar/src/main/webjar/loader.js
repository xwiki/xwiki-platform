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
  'xwiki-realtime-loader',
  'xwiki-realtime-document',
  'xwiki-realtime-errorBox'
], function($, Loader, doc, ErrorBox) {
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

  // TODO: This JavaScript code is not loaded anymore on the edit lock page so we need to decide what to do with it
  // (either drop it or find a clean way to load it on the edit lock page).
  var lock = Loader.getDocLock();

  var editorId = 'wysiwyg', info = {
    type: editorId,
    href: '&editor=wysiwyg&force=1',
    name: 'WYSIWYG',
    compatible: ['wysiwyg', 'wiki']
  };

  var createRtCalled = false,
  createRt = function() {
    if (createRtCalled) {
      return;
    }
    createRtCalled = true;
    var $saveButton = $('#mainEditArea').find('input[name="action_saveandcontinue"]');
    if ($saveButton.length) {
      var comment = $('#commentinput');
      var previousComment = comment.val();
      comment.val(Loader.messages.autoAcceptSave);
      $saveButton.click();
      $(document).one('xwiki:document:saved.createRt', function() {
        $(document).off('xwiki:document:saveFailed.createRt');
        comment.val(previousComment);
        window.location.href = Loader.getEditorURL(window.location.href, info);
      });
      $(document).one('xwiki:document:saveFailed.createRt', function() {
        $(document).off('xwiki:document:saved.createRt');
        comment.val(previousComment);
        Loader.displayRequestErrorModal();
      });
    }
  };
  Loader.setAvailableRt('wysiwyg', info, createRt);

  var whenCkReady = function(cb) {
    var iframe = $('iframe');
    if (window.CKEDITOR &&
      window.CKEDITOR.instances &&
      window.CKEDITOR.instances.content &&
      iframe.length &&
      iframe[0].contentWindow &&
      iframe[0].contentWindow.body) {
      return void cb();
    }
    setTimeout(function () {
      whenCkReady(cb);
    }, 100);
  };
  var launchRealtime = function(keys, realtime) {
    require(['xwiki-realtime-wysiwygEditor'], function(RealtimeWysiwygEditor) {
      if (RealtimeWysiwygEditor && RealtimeWysiwygEditor.main) {
        keys._update = $.proxy(Loader, 'updateKeys', editorId);
        var config = Loader.getConfig();
        config.rtURL = Loader.getEditorURL(window.location.href, info);
        RealtimeWysiwygEditor.main(config, keys, realtime);
        whenCkReady(function() {
          var editor = window.CKEDITOR.instances.content;
          RealtimeWysiwygEditor.currentMode = editor.mode;
          if (realtime) {
            $('.cke_button__source').remove();
          }
        });
      } else {
        console.error("Couldn't find RealtimeWysiwygEditor.main, aborting.");
      }
    });
  };

  var lockCk = function () {
    var iframe = jQuery('iframe')[0];
    var inner = iframe.contentWindow.body;
    inner.setAttribute('contenteditable', false);
  },
  unlockCk = function () {
    var iframe = jQuery('iframe')[0];
    var inner = iframe.contentWindow.body;
    inner.setAttribute('contenteditable', true);
  };

  if (lock) {
    // found a lock link : check active sessions
    Loader.checkSessions(info);
  } else {
    var todo = function (keys, needRt) {
      if (!needRt) {
        return void launchRealtime(keys);
      }
      var done = false;
      whenCkReady(function () {
        if (done) { return; }
        setTimeout(lockCk);
      });
      Loader.whenReady(function (wsAvailable) {
        done = true;
        // 3rd argument is "enable realtime"
        Loader.isRt = wsAvailable;
        if (!wsAvailable) { setTimeout(unlockCk); }
        launchRealtime(keys, wsAvailable || 0);
      });
    };
    Loader.updateKeys(editorId).done(function(keys) {
      if(!keys[editorId] || !keys.events || !keys.userdata) {
        ErrorBox.show('unavailable');
        console.error("You are not allowed to create a new realtime session for that document.");
      }
      var realtime = Loader.isRt;
      if (Object.keys(keys.active).length > 0) {
        // Should only happen when there is a realtime session with another editor (wiki, inline...)
        if (keys[editorId + '_users'] > 0) {
          todo(keys, realtime);
        } else {
          var callback = function() {
            todo(keys, true);
          };
          console.log("Join the existing realtime session or create a new one");
          Loader.displayModal(editorId, Object.keys(keys.active), callback, info);
        }
      } else {
        todo(keys, realtime);
      }
    });
  }

  var displayButtonModal = function() {
    var button, br = new Element('br');
    if ($('.realtime-button-' + editorId).length) {
      button = new Element('button', {'class': 'btn btn-success'});
      button.insert(Loader.messages.get('redirectDialog.join', 'WYSIWYG'));
      $('.realtime-button-' + editorId).prepend(button);
      $('.realtime-button-' + editorId).prepend(br);
      $(button).on('click', function() {
        window.location.href = Loader.getEditorURL(window.location.href, info);
      });
    } else if (lock) {
      button = new Element('button', {'class': 'btn btn-primary'});
      button.insert(Loader.messages.get('redirectDialog.create', 'WYSIWYG'));
      var buttons = $('.realtime-buttons');
      buttons.append(br).append(button);
      var modal = buttons.data('modal');
      $(button).on('click', function() {
        buttons.find('button').hide();
        var waiting = $('<div>', {style:'text-align:center;'}).appendTo(buttons);
        waiting.append($('<span>', {
          'class': 'fa fa-spinner fa-2x fa-spin',
          style: 'vertical-align: middle'
        }));
        waiting.append($('<span>', {
          style: 'vertical-align: middle'
        }).text(Loader.messages.waiting));
        var autoForce = $('<div>').appendTo(buttons);
        var i = 60;
        var it = setInterval(function () {
          i--;
          autoForce.html('<br>' + Loader.messages['redirectDialog.autoForce'] + i + "s");
          if (i <= 0) {
            clearInterval(it);
            window.location.href = Loader.getEditorURL(window.location.href, info);
          }
        }, 1000);
        Loader.requestRt('wysiwyg', function (state) {
          // We've received an answer
          clearInterval(it);
          if (state === false || state === 2) {
            // false: Nobody in the channel
            // 2: Rt should already exist
            console.error(state === false ? "EEMPTY" : "EEXISTS"); // FIXME
            window.location.href = Loader.getEditorURL(window.location.href, info);
            return;
          }
          if (state === 1) {
            // Accepted
            var whenReady = function (cb) {
              Loader.updateKeys(editorId).done(function(k) {
                if (k[editorId + '_users'] > 0) { return void cb(); }
                setTimeout(function () {
                  whenReady(cb);
                }, 1000);
              });
            };
            whenReady(function () {
              var i = {href: '&editor=wysiwyg'};
              window.location.href = Loader.getEditorURL(window.location.href, i);
            });
            return;
          }
        });
      });
    }
  };
  displayButtonModal();
  $(document).on('insertButton', displayButtonModal);
});
