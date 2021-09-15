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
define('xwiki-realtime-wikitext', [
  'xwiki-realtime-errorBox',
  'xwiki-realtime-toolbar',
  'chainpad-netflux',
  'xwiki-realtime-userData',
  'xwiki-realtime-typingTests',
  'json.sortify',
  'xwiki-realtime-textCursor',
  'xwiki-realtime-interface',
  'xwiki-realtime-saver',
  'chainpad',
  'xwiki-realtime-crypto',
  'jquery'
], function(
  /* jshint maxparams:false */
  ErrorBox, Toolbar, realtimeInput, UserData, TypingTest, JSONSortify, TextCursor, Interface, Saver, Chainpad, Crypto, $
) {
  'use strict';

  var canonicalize = function(text) {
    return text.replace(/\r\n/g, '\n');
  };

  var module = {}, editorId = 'wikitext';

  var main = module.main = function(editorConfig, docKeys) {
    var webSocketURL = editorConfig.WebsocketURL;
    var userName = editorConfig.userName;
    var DEMO_MODE = editorConfig.DEMO_MODE;
    var language = editorConfig.language;
    var userAvatar = editorConfig.userAvatarURL;

    var parsedConfig;
    try {
      parsedConfig = JSON.parse($('#realtime-config').text());
    } catch (e) {
      console.error(e);
    }

    var saverConfig = $.extend(editorConfig.saverConfig, {
      chainpad: Chainpad,
      editorType: editorId,
      editorName: 'Wiki',
      isHTML: false,
      mergeContent: parsedConfig?.enableMerge !== 0
    });

    // Key in the localStore which indicates realtime activity should be disallowed.
    var LOCALSTORAGE_DISALLOW = editorConfig.LOCALSTORAGE_DISALLOW;

    var $contentInner = $('#xwikieditcontentinner');
    var $textArea = $('#content');

    var channel = docKeys[editorId];
    var eventsChannel = docKeys.events;
    var userdataChannel = docKeys.userdata;

    /**
     * Update the channel keys for reconnecting WebSocket.
     */
    var updateKeys = function(callback) {
      docKeys._update(function(keys) {
        var changes = [];
        if (keys[editorId] && keys[editorId] !== channel) {
          channel = keys[editorId];
          changes.push(editorId);
        }
        if (keys.events && keys.events !== eventsChannel) {
          eventsChannel = keys.events;
          changes.push('events');
        }
        if (keys.userdata && keys.userdata !== userdataChannel) {
          userdataChannel = keys.userdata;
          changes.push('userdata');
        }
        callback(changes);
      });
    };

    // START DISALLOW REALTIME
    var uid = Interface.uid;
    var allowRealtimeCbId = uid();
    Interface.setLocalStorageDisallow(LOCALSTORAGE_DISALLOW);

    Interface.createAllowRealtimeCheckbox(allowRealtimeCbId, Interface.realtimeAllowed(),
      saverConfig.messages.allowRealtime);

    var $disallowButton = $('#' + allowRealtimeCbId);
    $disallowButton.change(function() {
      var checked = $(this).prop('checked');
      if (checked || DEMO_MODE) {
        Interface.realtimeAllowed(true);
        // TODO: Join the realtime session without reloading the page.
        window.location.reload();
      } else {
        Interface.realtimeAllowed(false);
        module.onAbort();
      }
    });

    if (!Interface.realtimeAllowed()) {
      console.log('Realtime is disallowed. Quitting');
      return;
    }
    // END DISALLOW REALTIME

    // Configure Saver with the merge URL and language settings.
    Saver.configure(saverConfig);

    console.log("Creating realtime toggle");

    var whenReady = function() {
      var codemirror = !!$('.CodeMirror').prop('CodeMirror');

      var cursorToPos = function(cursor, oldText) {
        var cLine = cursor.line;
        var cCh = cursor.ch;
        var pos = 0;
        var textLines = oldText.split('\n');
        for (var line = 0; line <= cLine; line++) {
          if (line < cLine) {
            pos += textLines[line].length + 1;
          }
          else if (line === cLine) {
            pos += cCh;
          }
        }
        return pos;
      };

      var posToCursor = function(position, newText) {
        var cursor = {
          line: 0,
          ch: 0
        };
        var textLines = newText.substr(0, position).split('\n');
        cursor.line = textLines.length - 1;
        cursor.ch = textLines[cursor.line].length;
        return cursor;
      };

      // Default wiki behaviour.
      var editor = {
        getValue: function() {
          return $textArea.val();
        },
        setValue: function(text) {
          $textArea.val(text);
        },
        setReadOnly: function(bool) {
          $textArea.prop('disabled', bool);
        },
        getCursor: function() {
          // Should return an object with selectionStart and selectionEnd fields.
          return $textArea[0];
        },
        setCursor: function(start, end) {
          $textArea[0].selectionStart = start;
          $textArea[0].selectionEnd = end;
        },
        onChange: function(handler) {
          $textArea.on('change keyup', handler);
        },
        _: $textArea
      };

      // Wiki
      var useCodeMirror = function() {
        editor._ = $('.CodeMirror')[0].CodeMirror;
        editor.getValue = function() { return editor._.getValue(); };
        editor.setValue = function (text) {
          editor._.setValue(text);
          editor._.save();
        };
        editor.setReadOnly = function (bool) {
          editor._.setOption('readOnly', bool);
        };
        editor.onChange = function (handler) {
          editor._.off('change');
          editor._.on('change', handler);
        };
        editor.getCursor = function () {
          var doc = canonicalize(editor.getValue());
          return {
            selectionStart : cursorToPos(editor._.getCursor('from'), doc),
            selectionEnd : cursorToPos(editor._.getCursor('to'), doc)
          };
        };
        editor.setCursor = function (start, end) {
          var doc = canonicalize(editor.getValue());
          if(start === end) {
            editor._.setCursor(posToCursor(start, doc));
          }
          else {
            editor._.setSelection(posToCursor(start, doc), posToCursor(end, doc));
          }
        };
        editor.onChange(onChangeHandler);
      };

      if (codemirror) { useCodeMirror(); }
      // Change the editor to CodeMirror if it is completely loaded after the initializaion of real-time wiki editor.
      $('body').on('DOMNodeInserted', function(e) {
        if ($(e.target).is('.CodeMirror')) {
          var enableCodeMirror = function() {
            if ($(e.target)[0] && $(e.target)[0].CodeMirror) {
              useCodeMirror();
            } else {
              setTimeout(enableCodeMirror, 100);
            }
          };
          enableCodeMirror();
        }
      });

      var setEditable = module.setEditable = function (bool) {
        editor.setReadOnly(!bool);
      };

      // don't let the user edit until the pad is ready
      setEditable(false);

      var initializing = true;

      var userData; // List of pretty name of all users (mapped with their server ID)
      var userList; // List of users still connected to the channel (server IDs)
      var myId; // My server ID

      var realtimeOptions = {
        // provide initialstate...
        initialState: editor.getValue() || '',

        // the websocket URL
        websocketURL: webSocketURL,

        // our username
        userName: userName,

        // the channel we will communicate over
        channel: channel,

        // Crypto object to avoid loading it twice in Cryptpad
        crypto: Crypto,
      };

      var setValueWithCursor = function (newValue) {
        var oldValue = canonicalize(editor.getValue());

        var ops = Chainpad.Diff.diff(oldValue, newValue);

        var oldCursor = editor.getCursor();
        var selects = ['selectionStart', 'selectionEnd'].map(function (attr) {
          return TextCursor.transformCursor(oldCursor[attr], ops);
        });

        editor.setValue(newValue);

        editor.setCursor(selects[0], selects[1]);
      };

      var createSaver = function (info) {
        if(!DEMO_MODE) {
          // this function displays a message notifying users that there was a merge
          Saver.lastSaved.mergeMessage = Interface.createMergeMessageElement(toolbar.toolbar
            .find('.rt-toolbar-rightside'),
            saverConfig.messages);
          Saver.setLastSavedContent(editor.getValue());
          var saverCreateConfig = {
            formId: "edit", // Id of the wiki page form
            setTextValue: function(newText, toConvert, callback) {
              setValueWithCursor(newText);
              callback();
              onLocal();
            },
            getSaveValue: function() {
              return Object.toQueryString({ content: editor.getValue() });
            },
            getTextValue: function() { return editor.getValue(); },
            realtime: info.realtime,
            userList: info.userList,
            userName: userName,
            network: info.network,
            channel: eventsChannel,
            demoMode: DEMO_MODE,
            safeCrash: function(reason, debugLog) { module.onAbort(null, reason, debugLog); }
          };
          Saver.create(saverCreateConfig);
        }
      };

      var onRemote = realtimeOptions.onRemote = function (info) {
        if (initializing) { return; }

        var newValue = info.realtime.getUserDoc();
        setValueWithCursor(newValue);
      };

      var onInit = realtimeOptions.onInit = function (info) {
        // Create the toolbar
        var $bar = $contentInner;
        userList = info.userList;
        var config = {
          userData: userData
        };
        toolbar = Toolbar.create({
          '$container': $bar,
          'myUserName': info.myID,
          'realtime': info.realtime,
          'getLag': info.getLag,
          'userList': info.userList,
          config
        });
      };

      var onReady = realtimeOptions.onReady = function (info) {
        module.chainpad = info.realtime;
        module.leaveChannel = info.leave;
        var userDoc = module.chainpad.getUserDoc();
        myId = info.myId;

        // Update the user list to link the wiki name to the user id
        var userdataConfig = {
          myId: info.myId,
          userName: userName,
          userAvatar: userAvatar,
          onChange: userList.onChange,
          crypto: Crypto,
          editor: editorId
        };

        userData = UserData.start(info.network, userdataChannel, userdataConfig);

        editor.setValue(userDoc);

        console.log("Unlocking editor");
        initializing = false;
        setEditable(true);

        onLocal();
        createSaver(info);
      };

      var onAbort = module.onAbort = realtimeOptions.onAbort = function (info, reason, debug) {
        console.log("Aborting the session!");
        var msg = reason || 'disconnected';
        module.chainpad.abort();
        module.leaveChannel();
        module.aborted = true;
        Saver.stop();
        toolbar.failed();
        toolbar.toolbar.remove();
        if (userData.leave && typeof userData.leave === "function") { userData.leave(); }
        if($disallowButton[0].checked && !module.aborted) {
          ErrorBox.show(msg, debug);
        }
      };

      var onConnectionChange = realtimeOptions.onConnectionChange = function (info) {
        console.log("Connection status : "+info.state);
        toolbar.failed();
        if (info.state) {
          ErrorBox.hide();
          initializing = true;
          toolbar.reconnecting(info.myId);
        } else {
          setEditable(false);
          ErrorBox.show('disconnected');
        }
      };

      var beforeReconnecting = realtimeOptions.beforeReconnecting = function (callback) {
        updateKeys(function () {
          callback(channel, editor.getValue());
        });
      };

      var onLocal = realtimeOptions.onLocal = function () {
        if (initializing) { return; }

        // serialize your DOM into an object
        var shjson = canonicalize(editor.getValue());

        module.chainpad.contentUpdate(shjson);

        if (module.chainpad.getUserDoc() !== shjson) {
          console.error("chainpad.getUserDoc() !== shjson");
          module.chainpad.contentUpdate(shjson, true);
        }
      };

      var rti = module.realtimeInput = realtimeInput.start(realtimeOptions);

      var onChangeHandler = function() {
        // We can't destroy the dialog here otherwise sometimes it is impossible to take an action
        // during a merge conflict :
        // Saver.destroyDialog();
        Saver.setLocalEditFlag(true);
        onLocal();
      };
      editor.onChange(onChangeHandler);
    };

    whenReady();
  };

  return module;
});
