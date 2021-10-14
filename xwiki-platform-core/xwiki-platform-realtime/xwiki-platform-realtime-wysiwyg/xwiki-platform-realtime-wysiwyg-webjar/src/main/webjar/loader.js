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
  'xwiki-realtime-errorBox'
], function($, Loader, ErrorBox) {
  'use strict';

  // Protect against ckeditor cache on firefox
  if (sessionStorage.refreshCk) {
    if (jQuery('#edit').length) {
      try {
        jQuery('#edit')[0].reset();
      } catch (e) {}
    }
    sessionStorage.refreshCk = "";
  }

  var getWysiwygLock = function () {
    var selector = 'a[href*="editor=wysiwyg"][href*="force=1"][href*="/edit/"]';
    var force = document.querySelectorAll(selector);
    return force.length? true : false;
  };

  var lock = Loader.getDocLock();
  var wysiwygLock = getWysiwygLock();

  var info = {
    type: 'rtwysiwyg',
    href: '&editor=wysiwyg&force=1&realtime=1',
    name: "WYSIWYG",
    compatible: ['wysiwyg', 'wiki']
  };

  var $saveButton = $('#mainEditArea').find('input[name="action_saveandcontinue"]');
  var createRtCalled = false;
  var createRt = function () {
    if (createRtCalled) { return; }
    createRtCalled = true;
    if ($saveButton.length) {
      var comment = $('#commentinput');
      var old;
      if (comment.length) {
        old = comment.val() || '';
        comment.val(Loader.messages.autoAcceptSave);
      }
      $saveButton.click();
      var onSaved = function () {
        if (CKEDITOR) {
          try {
            CKEDITOR.instances.content.resetDirty();
          } catch (e) {}
        }
        if (comment.length) { comment.val(old); }
        window.location.href = Loader.getEditorURL(window.location.href, info);
      };
      document.observe('xwiki:document:saved', onSaved);
      document.observe('xwiki:document:saveFailed', function () {
        if (comment.length) { comment.val(old); }
        Loader.displayRequestErrorModal();
      });
    }
  };
  Loader.setAvailableRt('wysiwyg', info, createRt);

  var getKeyData = function(config) {
    return [
      {doc: config.reference, mod: config.language+'/events', editor: "1.0"},
      {doc: config.reference, mod: config.language+'/events', editor: "userdata"},
      {doc: config.reference, mod: config.language+'/content',editor: "rtwysiwyg"}
    ];
  };

  var parseKeyData = function(config, keysResultDoc) {
    var keys = {};
    var keysResult = keysResultDoc[config.reference];
    if (!keysResult) { console.error("Unexpected error with the document keys"); return keys; }

    var keysResultContent = keysResult[config.language+'/content'];
    if (!keysResultContent) { console.error("Missing content keys in the document keys"); return keys; }

    var keysResultEvents = keysResult[config.language+'/events'];
    if (!keysResultEvents) { console.error("Missing event keys in the document keys"); return keys; }

    if (keysResultContent.rtwysiwyg && keysResultEvents["1.0"] && keysResultEvents["userdata"]) {
      keys.rtwysiwyg = keysResultContent.rtwysiwyg.key;
      keys.rtwysiwyg_users = keysResultContent.rtwysiwyg.users;
      keys.events = keysResultEvents["1.0"].key;
      keys.userdata = keysResultEvents["userdata"].key;
    }
    else { console.error("Missing mandatory RTWysiwyg key in the document keys"); return keys; }

    var activeKeys = keys.active = {};
    for (var key in keysResultContent) {
      if (key !== "rtwysiwyg" && keysResultContent[key].users > 0) {
        activeKeys[key] = keysResultContent[key];
      }
    }
    return keys;
  };

  var updateKeys = function (cb) {
    var config = Loader.getConfig();
    var keysData = getKeyData(config);
    Loader.getKeys(keysData, function(keysResultDoc) {
      var keys = parseKeyData(config, keysResultDoc);
      cb(keys);
    });
  };

  var whenCkReady = function (cb) {
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
  var launchRealtime = function (config, keys, realtime) {
    require(['jquery', 'xwiki-realtime-wysiwygEditor'], function($, RTWysiwyg) {
      if (RTWysiwyg && RTWysiwyg.main) {
        keys._update = updateKeys;
        RTWysiwyg.main(config, keys, realtime);
        whenCkReady(function () {
          var editor = window.CKEDITOR.instances.content;
          RTWysiwyg.currentMode = editor.mode;
          if (realtime) {
            $('.cke_button__source').remove();
          }
          return;
        });
      } else {
        console.error("Couldn't find RTWysiwyg.main, aborting");
      }
    });
  };

  var lockCk = function () {
    var iframe = jQuery('iframe')[0];
    var inner = iframe.contentWindow.body;
    inner.setAttribute('contenteditable', false);
  };
  var unlockCk = function () {
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
        var config = Loader.getConfig();
        config.rtURL = Loader.getEditorURL(window.location.href, info);
        return void launchRealtime(config, keys);
      }
      var done = false;
      whenCkReady(function () {
        if (done) { return; }
        setTimeout(lockCk);
      });
      Loader.whenReady(function (wsAvailable) {
        done = true;
        var config = Loader.getConfig();
        config.rtURL = Loader.getEditorURL(window.location.href, info);
        // 3rd argument is "enable realtime"
        Loader.isRt = wsAvailable;
        if (!wsAvailable) { setTimeout(unlockCk); }
        launchRealtime(config, keys, wsAvailable || 0);
      });
    };
    updateKeys(function (keys) {
      if(!keys.rtwysiwyg || !keys.events || !keys.userdata) {
        ErrorBox.show('unavailable');
        console.error("You are not allowed to create a new realtime session for that document.");
      }
      var realtime = /*keys.rtwysiwyg_users > 0 || */Loader.isRt;
      if (Object.keys(keys.active).length > 0) {
        // Should only happen when there is a realtime session with another editor (wiki, inline...)
        if (keys.rtwysiwyg_users > 0) {
          todo(keys, realtime);
        } else {
          var callback = function() {
            todo(keys, true);
          };
          console.log("Join the existing realtime session or create a new one");
          Loader.displayModal("rtwysiwyg", Object.keys(keys.active), callback, info);
        }
      } else {
        todo(keys, realtime);
      }
    });
  }

  var displayButtonModal = function() {
    if ($('.realtime-button-rtwysiwyg').length) {
      var button = new Element('button', {'class': 'btn btn-success'});
      var br =  new Element('br');
      button.insert(Loader.messages.redirectDialog_join.replace(/\{0\}/g, "Wysiwyg"));
      $('.realtime-button-rtwysiwyg').prepend(button);
      $('.realtime-button-rtwysiwyg').prepend(br);
      $(button).on('click', function() {
        window.location.href = Loader.getEditorURL(window.location.href, info);
      });
    } else if(lock) {
      var button = new Element('button', {'class': 'btn btn-primary'});
      var br =  new Element('br');
      button.insert(Loader.messages.redirectDialog_create.replace(/\{0\}/g, "Wysiwyg"));
      var buttons = $('.realtime-buttons');
      buttons.append(br).append(button);
      var modal = buttons.data('modal');
      $(button).on('click', function() {
        //modal.closeDialog();
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
          autoForce.html('<br>' + Loader.messages.redirectDialog_autoForce + i + "s");
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
              updateKeys(function (k) {
                if (k.rtwysiwyg_users > 0) { return void cb(); }
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
