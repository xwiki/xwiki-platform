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
define('xwiki-realtime-wikiEditor', [
  'xwiki-realtime-errorBox',
  'xwiki-realtime-toolbar',
  'chainpad-netflux',
  'xwiki-realtime-userData',
  'xwiki-realtime-textCursor',
  'xwiki-realtime-interface',
  'xwiki-realtime-saver',
  'chainpad',
  'xwiki-realtime-crypto',
  'jquery'
], function(
  /* jshint maxparams:false */
  ErrorBox, Toolbar, ChainPadNetflux, UserData, TextCursor, Interface, Saver, ChainPad, Crypto, $
) {
  'use strict';

  var canonicalize = function(text) {
    return text.replace(/\r\n/g, '\n');
  };

  var module = {}, editorId = 'wiki';

  module.main = function(editorConfig) {
    var channel = editorConfig.channels[editorId],
    eventsChannel = editorConfig.channels.events,
    userdataChannel = editorConfig.channels.userdata;

    /**
     * Update the channel keys for reconnecting WebSocket.
     */
    var updateKeys = function() {
      return editorConfig.updateChannels().then(keys => {
        if (keys[editorId] && keys[editorId] !== channel) {
          channel = keys[editorId];
        }
        if (keys.events && keys.events !== eventsChannel) {
          eventsChannel = keys.events;
        }
        if (keys.userdata && keys.userdata !== userdataChannel) {
          userdataChannel = keys.userdata;
        }
        return keys;
      });
    };

    Interface.realtimeAllowed(true);
    var allowRealtimeCheckbox = $('.buttons input[type=checkbox].realtime-allow');
    if (!allowRealtimeCheckbox.length) {
      allowRealtimeCheckbox = Interface.createAllowRealtimeCheckbox(true);
      allowRealtimeCheckbox.on('change', function() {
        if (allowRealtimeCheckbox.prop('checked')) {
          // TODO: Allow for enabling realtime without reloading the entire page.
          window.location.href = editorConfig.rtURL;
        } else {
          Interface.realtimeAllowed(false);
          module.onAbort();
        }
      });
    }
    // Disable while real-time framework is loading.
    allowRealtimeCheckbox.prop('disabled', true);

    console.log("Creating realtime toggle");

    var whenReady = function() {
      var cursorToPos = function(cursor, oldText) {
        var cLine = cursor.line;
        var cCh = cursor.ch;
        var pos = 0;
        var textLines = oldText.split('\n');
        for (var line = 0; line <= cLine; line++) {
          if (line < cLine) {
            pos += textLines[line].length + 1;
          } else if (line === cLine) {
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
      var $textArea = $('#content');
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
      var useCodeMirror = function(codeMirrorInstance) {
        editor._ = codeMirrorInstance;
        editor.getValue = function() {
          return editor._.getValue();
        };
        editor.setValue = function (text) {
          editor._.setValue(text);
          editor._.save();
        };
        editor.setReadOnly = function(bool) {
          editor._.setOption('readOnly', bool);
        };
        editor.onChange = function(handler) {
          editor._.off('change');
          editor._.on('change', handler);
        };
        editor.getCursor = function() {
          var doc = canonicalize(editor.getValue());
          return {
            selectionStart : cursorToPos(editor._.getCursor('from'), doc),
            selectionEnd : cursorToPos(editor._.getCursor('to'), doc)
          };
        };
        editor.setCursor = function(start, end) {
          var doc = canonicalize(editor.getValue());
          if (start === end) {
            editor._.setCursor(posToCursor(start, doc));
          } else {
            editor._.setSelection(posToCursor(start, doc), posToCursor(end, doc));
          }
        };
        editor.onChange(onChangeHandler);
      };

      if ($('#content ~ .CodeMirror').prop('CodeMirror')) {
        // The content text area has a CodeMirror instance attached. Use it for real-time editing.
        useCodeMirror($('#content ~ .CodeMirror').prop('CodeMirror'));
      } else {
        // Detect when a CodeMirror instance is attached to the content text area and update the real-time editor.
        require(['deferred!SyntaxHighlighting_cm/lib/codemirror'], async function(codeMirrorPromise) {
          CodeMirror = await codeMirrorPromise;
          CodeMirror.defineInitHook(instance => {
            if ($(instance.getTextArea?.()).attr('id') === 'content') {
              useCodeMirror(instance);
            }
          });
        });
      }

      module.setEditable = function(bool) {
        editor.setReadOnly(!bool);
      };

      // Don't let the user edit until the editor is ready.
      module.setEditable(false);

      var initializing = true,

      // List of pretty name of all users (mapped with their server ID).
      userData,

      // The real-time toolbar, showing the list of connected users, the merge message, the spinner and the lag.
      toolbar;

      var setValueWithCursor = function(newValue) {
        var oldValue = canonicalize(editor.getValue());
        var ops = ChainPad.Diff.diff(oldValue, newValue);
        var oldCursor = editor.getCursor();
        var selects = ['selectionStart', 'selectionEnd'].map(function (attr) {
          return TextCursor.transformCursor(oldCursor[attr], ops);
        });

        editor.setValue(newValue);
        editor.setCursor(selects[0], selects[1]);
      };

      function createSaver(info) {
        return new Saver({
          editorType: editorId,
          editorName: 'Wiki',
          userList: info.userList,
          userName: editorConfig.user.name,
          network: info.network,
          channel: eventsChannel,
          // This function displays a message notifying users that there was a merge.
          showNotification: Interface.createMergeMessageElement(toolbar.toolbar.find('.rt-toolbar-rightside')),
          setTextValue: function(newText, toConvert, callback) {
            setValueWithCursor(newText);
            callback();
            realtimeOptions.onLocal();
          },
          getTextValue: function() {
            return editor.getValue();
          },
          getTextAtCurrentRevision: function() {
            return $.get(XWiki.currentDocument.getRestURL('', $.param({media:'json'}))).then(data => {
              return data.content;
            });
          }
        });
      }

      var realtimeOptions = {
        // Provide initial state...
        initialState: editor.getValue() || '',
        websocketURL: editorConfig.webSocketURL,
        userName: editorConfig.user.name,
        // The channel we will communicate over.
        channel,
        // The object responsible for encrypting the messages.
        crypto: Crypto,
        network: editorConfig.network,

        onInit: function(info) {
          // Create the toolbar.
          toolbar = Toolbar.create({
            // Prepend the real-time toolbar to the existing Wiki Editor toolbar.
            '$container': $('.leftmenu2'),
            myUserName: info.myID,
            realtime: info.realtime,
            getLag: info.getLag,
            userList: info.userList,
            config: {userData}
          });
        },

        onReady: async function(info) {
          module.chainpad = info.realtime;
          module.leaveChannel = info.leave;

          // Update the user list to link the wiki name to the user id.
          userData = await UserData.start(info.network, userdataChannel, {
            myId: info.myId,
            userName: editorConfig.user.name,
            userAvatar: editorConfig.user.avatarURL,
            onChange: info.userList.onChange,
            crypto: Crypto,
            editor: editorId
          });

          editor.setValue(module.chainpad.getUserDoc());

          console.log('Unlocking editor');
          initializing = false;
          module.setEditable(true);
          // Renable the allow real-time checkbox now that the framework is ready.
          allowRealtimeCheckbox.prop('disabled', false);

          this.onLocal();
          module.saver = createSaver(info);
        },

        onRemote: function(info) {
          if (!initializing) {
            setValueWithCursor(info.realtime.getUserDoc());
          }
        },

        onLocal: function() {
          if (initializing) {
            return;
          }

          // Serialize your DOM into an object.
          var serializedHyperJSON = canonicalize(editor.getValue());

          module.chainpad.contentUpdate(serializedHyperJSON);

          if (module.chainpad.getUserDoc() !== serializedHyperJSON) {
            console.error('chainpad.getUserDoc() !== serializedHyperJSON');
            module.chainpad.contentUpdate(serializedHyperJSON, true);
          }
        },

        onAbort: function(info, reason, debug) {
          console.log('Aborting the session!');
          module.chainpad.abort();
          module.leaveChannel();
          module.aborted = true;
          module.saver.stop();
          toolbar.failed();
          toolbar.toolbar.remove();
          userData.stop?.();
          if (reason || debug) {
            ErrorBox.show(reason || 'disconnected', debug);
          }
        },

        beforeReconnecting: function (callback) {
          updateKeys().then(() => {
            callback(channel, editor.getValue());
          });
        },

        onConnectionChange: function(info) {
          console.log('Connection status: ' + info.state);
          toolbar.failed();
          if (info.state) {
            ErrorBox.hide();
            initializing = true;
            toolbar.reconnecting(info.myId);
          } else {
            module.setEditable(false);
            ErrorBox.show('disconnected');
          }
        }
      };

      module.onAbort = realtimeOptions.onAbort;
      module.realtimeInput = ChainPadNetflux.start(realtimeOptions);

      // Notify the others that we're editing in realtime.
      editorConfig.setRealtimeEnabled(true);

      function onChangeHandler() {
        realtimeOptions.onLocal();
        module.saver.contentModifiedLocally();
      }
      editor.onChange(onChangeHandler);
    };

    whenReady();
  };

  return module;
});
