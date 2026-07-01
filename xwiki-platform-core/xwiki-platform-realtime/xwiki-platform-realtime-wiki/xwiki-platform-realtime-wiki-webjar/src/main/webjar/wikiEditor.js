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
  Toolbar, ChainPadNetflux, UserData, TextCursor, Interface, Saver, ChainPad, Crypto, $
) {
  'use strict';

  class RealtimeWikiEditor {
    constructor(editorConfig) {
      this.editorId = 'wiki';
      this.editorConfig = editorConfig;
      this.channel = editorConfig.channels[this.editorId];
      this.saverChannel = editorConfig.channels.saver;
      this.userDataChannel = editorConfig.channels.userData;

      this.createAllowRealtimeCheckbox();
      this.createEditor();

      // Don't let the user edit until the editor is ready.
      this.setEditable(false);

      this.initializing = true;

      // List of pretty name of all users (mapped with their session ID).
      this.userData = {};

      // The real-time toolbar, showing the list of connected users.
      this.toolbar = new Toolbar({
        save: (...args) => this.saver?.save(...args),
        leave: () => Interface.getAllowRealtimeCheckbox().click()
      });

      this.realtimeInput = ChainPadNetflux.start(this.getRealtimeOptions());

      // Notify the others that we're editing in realtime.
      editorConfig.setRealtimeEnabled(true);
    }

    canonicalize(text) {
      return text.replaceAll('\r\n', '\n');
    }

    /**
     * Update the channel keys for reconnecting WebSocket.
     */
    updateKeys() {
      return this.editorConfig.updateChannels().then(keys => {
        this.channel = keys[this.editorId] || channel;
        this.saverChannel = keys.saver || saverChannel;
        this.userDataChannel = keys.userData || userDataChannel;
        return keys;
      });
    }

    createAllowRealtimeCheckbox() {
      Interface.createAllowRealtimeCheckbox({
        checked: this.editorConfig.realtimeEnabled,
        join: () => {
          return new Promise(() => {
            // TODO: Join the realtime editing session without reloading the entire page.
            globalThis.location.href = this.editorConfig.rtURL;
          });
        },
        leave: this.onAbort.bind(this)
      });
    }

    cursorToPos(cursor, oldText) {
      const cLine = cursor.line;
      const cCh = cursor.ch;
      let pos = 0;
      const textLines = oldText.split('\n');
      for (let line = 0; line <= cLine; line++) {
        if (line < cLine) {
          pos += textLines[line].length + 1;
        } else if (line === cLine) {
          pos += cCh;
        }
      }
      return pos;
    }

    posToCursor(position, newText) {
      const cursor = {
        line: 0,
        ch: 0
      };
      const textLines = newText.substr(0, position).split('\n');
      cursor.line = textLines.length - 1;
      cursor.ch = textLines[cursor.line].length;
      return cursor;
    }

    createEditor() {
      this.editor = this.createPlainEditor();
      this.editor.onChange(this.onChange.bind(this));
      this.getCodeMirrorInstance().then(codeMirrorInstance => {
        this.editor = this.createCodeMirrorEditor(codeMirrorInstance);
        this.editor.onChange(this.onChange.bind(this));
      });
    }

    createPlainEditor() {
      const $textArea = $('#content');
      return {
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
          return {
            selectionStart: $textArea[0].selectionStart,
            selectionEnd: $textArea[0].selectionEnd
          };
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
    }

    createCodeMirrorEditor(codeMirrorInstance) {
      return {
        getValue: () => {
          return codeMirrorInstance.getValue();
        },
        setValue: text => {
          codeMirrorInstance.setValue(text);
          codeMirrorInstance.save();
        },
        setReadOnly: bool => {
          codeMirrorInstance.setOption('readOnly', bool);
        },
        onChange: handler => {
          codeMirrorInstance.off('change');
          codeMirrorInstance.on('change', handler);
        },
        getCursor: () => {
          const doc = this.canonicalize(codeMirrorInstance.getValue());
          return {
            selectionStart: this.cursorToPos(codeMirrorInstance.getCursor('from'), doc),
            selectionEnd: this.cursorToPos(codeMirrorInstance.getCursor('to'), doc)
          };
        },
        setCursor: (start, end) => {
          const doc = this.canonicalize(codeMirrorInstance.getValue());
          if (start === end) {
            codeMirrorInstance.setCursor(this.posToCursor(start, doc));
          } else {
            codeMirrorInstance.setSelection(this.posToCursor(start, doc), this.posToCursor(end, doc));
          }
        },
        _: codeMirrorInstance
      };
    }

    async getCodeMirrorInstance() {
      const codeMirrorInstance = $('#content ~ .CodeMirror').prop('CodeMirror');
      if (codeMirrorInstance) {
        // The content text area has a CodeMirror instance attached. Use it for real-time editing.
        return codeMirrorInstance;
      } else {
        return await new Promise(resolve => {
          // Detect when a CodeMirror instance is attached to the content text area and update the real-time editor.
          require(['deferred!SyntaxHighlighting_cm/lib/codemirror'], async function(codeMirrorPromise) {
            const CodeMirror = await codeMirrorPromise;
            CodeMirror.defineInitHook(instance => {
              if ($(instance.getTextArea?.()).attr('id') === 'content') {
                resolve(instance);
              }
            });
          });
        });
      }
    }

    setEditable(bool) {
      this.editor.setReadOnly(!bool);
    }

    setValueWithCursor(newValue) {
      const oldValue = this.canonicalize(this.editor.getValue());
      const ops = ChainPad.Diff.diff(oldValue, newValue);
      const oldCursor = this.editor.getCursor();
      this.editor.setValue(newValue);
      this.editor.setCursor(
        TextCursor.transformCursor(oldCursor.selectionStart, ops),
        TextCursor.transformCursor(oldCursor.selectionEnd, ops)
      );
    }

    async createSaver(info) {
      $('#edit').on('xwiki:actions:reload', this.resetContent.bind(this));

      return await new Saver({
        userName: this.editorConfig.user.sessionId,
        network: info.network,
        channel: this.saverChannel,
        onStatusChange: this.toolbar.onSaveStatusChange.bind(this.toolbar),
        onCreateVersion: version => {
          version.author = Object.values(this.userData).find(
            user => (user?.sessionId && user.sessionId === version.author) ||
              (user?.reference && user.reference === version.author?.reference)
          ) || version.author;
          this.toolbar.onCreateVersion(version);
        }
      }).toBeReady();
    }

    async resetContent() {
      const data = await $.get(XWiki.currentDocument.getRestURL('', $.param({media:'json'})));
      this.hideChangesFromSaver(this.setValueWithCursor.bind(this, data.content));
      this.onLocal();
    }

    hideChangesFromSaver(task) {
      const contentModifiedLocally = this.saver.contentModifiedLocally;
      this.saver.contentModifiedLocally = () => {};
      try {
        task();
      } finally {
        this.saver.contentModifiedLocally = contentModifiedLocally;
      }
    }

    onUserListChange(userList, newUserData) {
      // If no new data (someone has just joined or left the channel), get the latest known values.
      this.userData = newUserData || this.userData;
      const users = userList.users.filter(id => this.userData[id]).map(id => ({id, ...this.userData[id]}));
      this.toolbar.onUserListChange(users);
    }

    getRealtimeOptions() {
      return {
        // Provide initial state...
        initialState: this.editor.getValue() || '',
        websocketURL: this.editorConfig.webSocketURL,
        userName: this.editorConfig.user.sessionId,
        // The channel we will communicate over.
        channel: this.channel,
        // The object responsible for encrypting the messages.
        crypto: Crypto,
        network: this.editorConfig.network,

        onInit: this.onInit.bind(this),
        onReady: this.onReady.bind(this),
        onRemote: this.onRemote.bind(this),
        onLocal: this.onLocal.bind(this),
        onAbort: this.onAbort.bind(this),
        beforeReconnecting: this.beforeReconnecting.bind(this),
        onConnectionChange: this.onConnectionChange.bind(this)
      };
    }

    onInit(info) {
      this.toolbar.onConnectionStatusChange(1 /* connecting */, info.myID);
    }

    async onReady(info) {
      this.chainpad = info.realtime;
      this.leaveChannel = info.leave;

      // Update the user list to link the wiki name to the user id.
      this.userData = await UserData.start(info.network, this.userDataChannel, {
        myId: info.myId,
        user: this.editorConfig.user,
        onChange: info.userList.onChange,
        crypto: Crypto,
        editor: this.editorId
      });
      this.onUserListChange(info.userList, this.userData);
      info.userList.change.push(this.onUserListChange.bind(this, info.userList));

      this.saver = await this.createSaver(info);
      this.initializing = false;
      // Initialize the edited content with the content from the realtime session.
      this.onRemote(info);

      console.log('Unlocking editor');
      this.setEditable(true);
      this.toolbar.onConnectionStatusChange(2 /* connected */, info.myId);

      // Renable the allow real-time checkbox now that the framework is ready.
      Interface.getAllowRealtimeCheckbox().prop('disabled', false);
    }

    onRemote(info) {
      if (!this.initializing) {
        this.setValueWithCursor(info.realtime.getUserDoc());
      }
    }

    onLocal() {
      if (this.initializing) {
        return;
      }

      // Serialize your DOM into an object.
      const serializedHyperJSON = this.canonicalize(this.editor.getValue());

      this.chainpad.contentUpdate(serializedHyperJSON);

      if (this.chainpad.getUserDoc() !== serializedHyperJSON) {
        console.error('chainpad.getUserDoc() !== serializedHyperJSON');
        this.chainpad.contentUpdate(serializedHyperJSON, true);
      }
    }

    onAbort() {
      console.log('Aborting the session!');
      this.chainpad.abort();
      this.leaveChannel();
      this.aborted = true;
      this.saver.stop();
      this.toolbar.destroy();
      this.userData.stop?.();
    }

    async beforeReconnecting(callback) {
      await this.updateKeys();
      callback(this.channel, this.editor.getValue());
    }

    onConnectionChange(info) {
      console.log('Connection status: ' + info.state);
      this.initializing = true;
      if (info.state) {
        // Reconnecting.
        this.toolbar.onConnectionStatusChange(1 /* connecting */, info.myId);
      } else {
        // Disconnected.
        this.toolbar.onConnectionStatusChange(0 /* disconnected */);
        this.setEditable(false);
      }
    }

    onChange() {
      this.onLocal();
      this.saver.contentModifiedLocally();
    }
  }

  return RealtimeWikiEditor;
});
