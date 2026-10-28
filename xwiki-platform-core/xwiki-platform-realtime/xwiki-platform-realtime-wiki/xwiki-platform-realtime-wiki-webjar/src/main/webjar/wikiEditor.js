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
  'jquery',
  'xwiki-l10n!xwiki-realtime-messages'
], function(
  /* jshint maxparams:false */
  Toolbar, ChainPadNetflux, UserData, TextCursor, Interface, Saver, ChainPad, Crypto, $, Messages
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
      const savedContent = await this.getSavedContent();
      this.hideChangesFromSaver(this.setValueWithCursor.bind(this, savedContent));
      this.onLocal();
    }

    async getSavedContent() {
      try {
        const data = await $.get(XWiki.currentDocument.getRestURL('', $.param({media:'json'})));
        return data.content;
      } catch (error) {
        if (error?.status === 404) {
          // The edited document was never saved.
          return '';
        }
        throw error;
      }
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
        onConnectionChange: this.onConnectionChange.bind(this),
        onError: this.onRealtimeError.bind(this)
      };
    }

    onInit(info) {
      this.toolbar.onConnectionStatusChange(1 /* connecting */, info.myID);
    }

    async onReady(info) {
      try {
        await this.joinRealtimeSession(info);
      } catch (error) {
        // We failed to join the user data or the saver channel.
        this.onRealtimeError(error);
      }
    }

    async joinRealtimeSession(info) {
      this.chainpad = info.realtime;
      this.leaveChannel = info.leave;

      // Update the user list to link the wiki name to the user id.
      this.userData = await UserData.start(info.network, this.userDataChannel, {
        myId: info.myId,
        user: this.editorConfig.user,
        onChange: info.userList.onChange,
        crypto: Crypto,
        editor: this.editorId,
        // Used to check if the user data channel is still available when the WebSocket connection is restored.
        getChannel: async () => (await this.editorConfig.updateChannels()).userData
      });
      this.onUserListChange(info.userList, this.userData);
      info.userList.change.push(this.onUserListChange.bind(this, info.userList));

      // When we rejoin the realtime session after the WebSocket connection was closed we create a new saver. If we use
      // the same saver channel, the new saver gets our saver state from the other users, but if the saver channel has
      // been removed in the mean time (because we were the only one using it) then the new saver doesn't know about the
      // changes we didn't save before the connection was closed.
      const hasUnsavedChanges = this.saver?.isDirty() && this.joinedSaverChannel !== this.saverChannel;
      this.saver = await this.createSaver(info);
      this.joinedSaverChannel = this.saverChannel;
      if (hasUnsavedChanges) {
        // Mark the content as dirty in order to show the right save status and to trigger the auto-save.
        this.saver.contentModifiedLocally();
      }
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
      // We may not have finished joining the realtime session (e.g. if we failed to join one of the channels).
      this.chainpad?.abort();
      this.leaveChannel?.();
      this.aborted = true;
      this.saver?.stop();
      this.toolbar.destroy();
      this.userData?.stop?.();
    }

    /**
     * Called when we fail to join or rejoin the realtime session. We leave the realtime session and let the user
     * continue editing alone.
     *
     * @param {*} error the reason why we failed to join the realtime session
     */
    onRealtimeError(error) {
      if (this.aborted) {
        // We already left the realtime session (e.g. because of a previous error).
        return;
      }

      console.error('Failed to join the realtime session.', error);
      this.onAbort();
      this.setEditable(true);
      // Let the user know that they're not editing in realtime anymore.
      Interface.getAllowRealtimeCheckbox().prop('checked', false).prop('disabled', false);
      this.editorConfig.setRealtimeEnabled(false);
      XWiki.widgets.Notification.show(Messages['join.error'], 'error');
    }

    async beforeReconnecting(callback) {
      try {
        const oldChannel = this.channel;
        await this.updateKeys();
        if (this.channel === oldChannel) {
          // The channel used before the WebSocket connection closed is still available so we can continue to use it.
          callback(this.channel, this.editor.getValue());
        } else {
          await this.restartRealtimeSync();
        }
      } catch (error) {
        // We failed to fetch the channels (e.g. because the user session has expired).
        this.onRealtimeError(error);
      }
    }

    /**
     * Called when the channel used before the WebSocket connection closed is not available anymore (it was removed
     * because we were the only one using it). ChainPad-Netflux would join the new channel using the same ChainPad
     * instance, whose history exists only in the removed channel, so the users joining the new channel would get an
     * empty history: they wouldn't see our content and would ignore our next changes, whose parent they don't know.
     * We start a new realtime synchronization instead. Its initial state is the saved content, i.e. the content the
     * users joining after us start with, because ChainPad requires all the users to have the same initial state (and
     * it sends the initial state only along with the first change). Our content, which may have unsaved changes, is
     * recorded as a local change before the channel history is received, so that ChainPad merges it with the changes
     * made by the users that joined the new channel before us, if any.
     */
    async restartRealtimeSync() {
      const initialState = this.canonicalize(await this.getSavedContent() || '');
      try {
        this.realtimeInput.stop();
      } catch (error) {
        // The WebSocket connection used by the previous realtime synchronization is already closed.
        console.debug('Failed to stop the previous realtime synchronization.', error);
      }
      this.realtimeInput = ChainPadNetflux.start({
        ...this.getRealtimeOptions(),
        initialState,
        onInit: info => {
          this.onInit(info);
          // Called before the channel history is requested.
          info.realtime.contentUpdate(this.canonicalize(this.editor.getValue()));
        }
      });
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
