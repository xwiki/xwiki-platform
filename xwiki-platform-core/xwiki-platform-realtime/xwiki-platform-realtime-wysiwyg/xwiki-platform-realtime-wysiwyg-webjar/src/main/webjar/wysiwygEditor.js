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
define('xwiki-realtime-wysiwyg', [
  'jquery',
  'xwiki-realtime-config',
  'xwiki-l10n!xwiki-realtime-messages',
  'xwiki-realtime-errorBox',
  'xwiki-realtime-toolbar',
  'chainpad-netflux',
  'xwiki-realtime-userData',
  'xwiki-realtime-typingTests',
  'xwiki-realtime-interface',
  'xwiki-realtime-saver',
  'chainpad',
  'xwiki-realtime-crypto',
  'xwiki-realtime-wysiwyg-patches'
], function (
  /* jshint maxparams:false */
  $, realtimeConfig, Messages, ErrorBox, Toolbar, ChainPadNetflux, UserData, TypingTest, Interface, Saver,
  ChainPad, Crypto, Patches
) {
  'use strict';

  const EDITOR_TYPE = 'wysiwyg';

  const ConnectionStatus = {
    DISCONNECTED: 0,
    CONNECTING: 1,
    CONNECTED: 2
  };

  class RealtimeEditor {
    constructor(editor, editorConfig, docKeys, useRt) {
      this._editor = editor;
      this._editorConfig = editorConfig;

      // The editor wrapper used to smoothly update (patch) the edited content without losing the caret position.
      this._patchedEditor = new Patches(editor);

      this._docKeys = docKeys;

      // The channel used to synchronize the edited content (notify others when you make a change and be notified when
      // others make changes).
      this._channel = docKeys[EDITOR_TYPE];

      // The channel used to synchronize the content (auto)save (notify others when you save and be notified when others
      // save, in order to avoid merge conflicts and creating unnecessary document revisions).
      this._eventsChannel = docKeys.events;

      // The channel used to synchronize the user caret position (notify others when your caret position changes and be
      // notified when others' caret position changes).
      this._userDataChannel = docKeys.userdata;
  
      Interface.realtimeAllowed(useRt);
      this._createAllowRealtimeCheckbox(useRt);

      this._connection = {
        status: ConnectionStatus.DISCONNECTED
      };

      if (useRt) {
        this._startRealtimeSync();
      }
    }

    setEditable(editable) {
      this._editor.getContentWrapper().setAttribute('contenteditable', editable);
      $('.buttons [name^="action_save"], .buttons [name^="action_preview"]').prop('disabled', !editable);
    }

    _startRealtimeSync() {
      this._connection.status = ConnectionStatus.CONNECTING;

      // List of pretty names of all users (mapped with their server ID).
      this._connection.userData = {};

      // Don't let the user edit until the real-time framework is ready.
      this.setEditable(false);

      // Start the auto-save.
      Saver.configure({
        chainpad: ChainPad,
        editorType: EDITOR_TYPE,
        editorName: 'WYSIWYG',
        isHTML: true,
        mergeContent: realtimeConfig.enableMerge !== 0
      });

      this._connection.realtimeInput = ChainPadNetflux.start(this._getRealtimeOptions());

      // Notify the others that we're editing in realtime.
      this._editorConfig.setRealtimeEditing(true);
  
      // Listen to local changes and propagate them to the other users.
      this._editor.onChange(() => {
        if (this._connection.status === ConnectionStatus.CONNECTED) {
          Saver.destroyDialog();
          Saver.setLocalEditFlag(true);
          this._onLocal();
        }
      });

      // Leave the realtime session and stop the autosave when the editor is destroyed. We have to do this because the
      // editor can be destroyed without the page being reloaded (e.g. when editing in-place).
      this._editor.onBeforeDestroy(this._onAbort.bind(this));

      // Export the typing tests to the window.
      // Call like `test = easyTest()`
      // Terminate the test like `test.cancel()`
      window.easyTest = this._easyTest.bind(this);
    }

    /**
     * Update the channels keys for reconnecting WebSocket.
     */
    async _updateKeys() {
      const keys = await this._docKeys._update();
      this._channel = keys[EDITOR_TYPE] || this._channel;
      this._eventsChannel = keys.events || this._eventsChannel;
      this._userDataChannel = keys.userdata || this._userDataChannel;
      return keys;
    }

    _createAllowRealtimeCheckbox(useRt) {
      // Don't display the checkbox in the following cases:
      // * useRt 0 (instead of true/false) => we can't connect to the websocket service
      // * realtime is disabled and we're not an advanced user
      if (useRt !== 0 && (useRt || this._editorConfig.isAdvancedUser)) {
        const allowRealtimeCheckbox = Interface.createAllowRealtimeCheckbox(Interface.realtimeAllowed());
        const realtimeToggleHandler = () => {
          if (allowRealtimeCheckbox.prop('checked')) {
            Interface.realtimeAllowed(true);
            this._startRealtimeSync();
          } else {
            this._editorConfig.displayDisableModal((state) => {
              if (!state) {
                allowRealtimeCheckbox.prop('checked', true);
              } else {
                Interface.realtimeAllowed(false);
                this._onAbort();
              }
            });
          }
        };
        allowRealtimeCheckbox.on('change', realtimeToggleHandler);
        this._editor.onBeforeDestroy(() => {
          allowRealtimeCheckbox.off('change', realtimeToggleHandler);
        });
      }
    }

    _createSaver(info, userName) {
      Saver.lastSaved.mergeMessage = Interface.createMergeMessageElement(
        this._connection.toolbar.toolbar.find('.rt-toolbar-rightside'));
      Saver.setLastSavedContent(this._editor.getOutputHTML());
      const saverCreateConfig = {
        // Id of the wiki page form.
        formId: window.XWiki.editor === 'wysiwyg' ? 'edit' : 'inline',
        realtime: info.realtime,
        userList: info.userList,
        userName,
        network: info.network,
        channel: this._eventsChannel,
        setTextValue: (newText) => {
          this._patchedEditor.setHTML(newText, true);
        },
        getSaveValue: () => {
          const fieldName = this._editor.getFormFieldName();
          return {
            [fieldName]: this._editor.getOutputHTML(),
            RequiresHTMLConversion: fieldName,
            [`${fieldName}_syntax`]: 'xwiki/2.1'
          };
        },
        getTextValue: () => {
          try {
            return this._editor.getOutputHTML();
          } catch (e) {
            this._editor.showNotification(Messages['realtime.editor.getContentFailed'], 'warning');
            return null;
          }
        },
        getTextAtCurrentRevision: (revision) => {
          return $.get(XWiki.currentDocument.getURL('get', $.param({
            xpage:'get',
            outputSyntax:'annotatedhtml',
            outputSyntaxVersion:'5.0',
            transformations:'macro',
            rev:revision
          })));
        },
        safeCrash: (reason, debugLog) => {
          this._onAbort(null, reason, debugLog);
        }
      };
      Saver.create(saverCreateConfig);
    }

    _changeUserIcons(newdata) {
      if (!realtimeConfig.marginAvatar) {
        return;
      }

      // If no new data (someone has just joined or left the channel), get the latest known values.
      const updatedData = newdata || this._connection.userData;

      const ownerDocument = this._editor.getContentWrapper().ownerDocument;
      $(ownerDocument).find('.rt-user-position').remove();
      const positions = {};
      const avatarWidth = 15, spacing = 3;
      let requiredPadding = 0;
      this._connection.userList.users.filter(id => updatedData[id]?.['cursor_' + EDITOR_TYPE]).forEach(id => {
        const data = updatedData[id];
        const name = RealtimeEditor._getPrettyName(data.name);
        // Set the user position.
        const element = ownerDocument.evaluate(data['cursor_' + EDITOR_TYPE], ownerDocument, null,
          XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
        if (!element) {
          return;
        }
        const pos = $(element).offset();
        if (!positions[pos.top]) {
          positions[pos.top] = [id];
        } else {
          positions[pos.top].push(id);
        }
        const index = positions[pos.top].length - 1;
        const posTop = pos.top + spacing;
        const posLeft = spacing + index * (avatarWidth + spacing);
        requiredPadding = Math.max(requiredPadding, (posLeft + 2 * spacing));
        let $indicator;
        if (data.avatar) {
          $indicator = $('<img alt=""/>').attr('src', data.avatar);
        } else {
          $indicator = $('<div></div>').text(name.substring(0, 1));
        }
        $indicator.addClass('rt-non-realtime rt-user-position').attr({
          id: 'rt-user-' + id,
          title: name,
          contenteditable: 'false'
        }).css({
          'left': posLeft + 'px',
          'top': posTop + 'px'
        });
        $(this._editor.getContentWrapper()).after($indicator);
      });

      $(this._editor.getContentWrapper()).css('padding-left',
        requiredPadding === 0 ? '' : ((requiredPadding + avatarWidth) + 'px'));
    }

    _getRealtimeOptions() {
      return {
        initialState: this._patchedEditor.getHyperJSON() || '{}',
        websocketURL: this._editorConfig.WebsocketURL,
        userName: this._editorConfig.userName,
        channel: this._channel,
        crypto: Crypto,
        network: this._editorConfig.network,

        // Operational Transformation
        // The synchronization is done on JSON so we need to make sure the output of the synchronization is always
        // valid JSON.
        patchTransformer: ChainPad.NaiveJSONTransformer,

        validateContent: (content) => {
          try {
            JSON.parse(content || '{}');
            return true;
          } catch (e) {
            console.error("Failed to parse JSON content, rejecting patch.", {
              content,
              error: e
            });
            return false;
          }
        },

        onInit: this._onInit.bind(this),
        onReady: this._onReady.bind(this),

        // This function resets the realtime fields after coming back from source mode.
        onLocalFromSource: () => {
          this._onLocal();
        },

        onLocal: this._onLocal.bind(this),
        onRemote: this._onRemote.bind(this),
        onConnectionChange: this._onConnectionChange.bind(this),
        beforeReconnecting: this._beforeReconnecting.bind(this),
        onAbort: this._onAbort.bind(this),
      };
    }

    _onInit(info) {
      // List of users still connected to the channel (server IDs).
      this._connection.userList = info.userList;
      const config = {
        userData: this._connection.userData,
        onUsernameClick: (id) => {
          const editableContentLocation = this._editor.getContentWrapper().ownerDocument.defaultView.location;
          const baseHref = editableContentLocation.href.split('#')[0] || '';
          editableContentLocation.href = baseHref + '#rt-user-' + id;
        }
      };
      // The real-time toolbar, showing the list of connected users, the merge message, the spinner and the lag.
      this._connection.toolbar = Toolbar.create({
        '$container': $(this._editor.getToolBar()),
        myUserName: info.myID,
        realtime: info.realtime,
        getLag: info.getLag,
        userList: info.userList,
        config
      });
      // When someone leaves, if they used Save&View, it removes the locks from the document. We're going to add it
      // again to be sure new users will see the lock page and be able to join.
      let oldUsers = JSON.parse(JSON.stringify(info.userList.users || []));
      info.userList.change.push(() => {
        if (info.userList.length) {
          // If someone has left, try to get the lock.
          if (oldUsers.some(user => info.userList.users.indexOf(user) === -1)) {
            XWiki.EditLock = new XWiki.DocumentLock();
            XWiki.EditLock.lock();
          }
          oldUsers = JSON.parse(JSON.stringify(info.userList.users || []));
        }
      });
    }

    _onReady(info) {
      if (this._connection.status !== ConnectionStatus.CONNECTING) {
        return;
      }

      this._connection.chainpad = info.realtime;

      if (!this._connection.isOnReadyPreviouslyCalled) {
        this._connection.isOnReadyPreviouslyCalled = true;
        // Update the user list to link the wiki name to the user id.
        const userDataConfig = {
          myId: info.myId,
          userName: this._editorConfig.userName,
          userAvatar: this._editorConfig.userAvatarURL,
          onChange: this._connection.userList.onChange,
          crypto: Crypto,
          editor: EDITOR_TYPE,
          getCursor: () => {
            const selection = this._editor.getSelection();
            let node = selection?.rangeCount && selection.getRangeAt(0).startContainer;
            if (!node) {
              return '';
            }
            node = (node.nodeName === '#text') ? node.parentNode : node;
            return RealtimeEditor._getXPath(node);
          }
        };
        if (!realtimeConfig.marginAvatar) {
          delete userDataConfig.getCursor;
        }

        this._connection.userData = UserData.start(info.network, this._userDataChannel, userDataConfig);
        this._connection.userList.change.push(this._changeUserIcons.bind(this));
      }

      const shjson = this._connection.chainpad.getUserDoc();
      this._patchedEditor.setHyperJSON(shjson);

      console.log('Unlocking editor');
      this._connection.status = ConnectionStatus.CONNECTED;
      this.setEditable(true);

      this._onLocal();
      this._createSaver(info, this._editorConfig.userName);
    }

    _onLocal() {
      if (this._connection.status !== ConnectionStatus.CONNECTED) {
        return;
      }
      // Stringify the JSON and send it into ChainPad.
      const localContent = this._patchedEditor.getHyperJSON();
      console.log('Push local content: ' + localContent);
      this._connection.chainpad.contentUpdate(localContent);

      const remoteContent = this._connection.chainpad.getUserDoc();
      if (remoteContent !== localContent) {
        console.error('Unexpected remote content after synchronization: ', {
          expected: localContent,
          actual: remoteContent,
          diff: ChainPad.Diff.diff(localContent, remoteContent)
        });
      }
    }

    _onRemote(info) {
      if (this._connection.status !== ConnectionStatus.CONNECTED) {
        return;
      }

      const remoteContent = info.realtime.getUserDoc();
      console.log('Received remote content: ' + remoteContent);

      // Build a DOM from HyperJSON, diff and patch the editor.
      this._patchedEditor.setHyperJSON(remoteContent);

      const localContent = this._patchedEditor.getHyperJSON();
      if (localContent !== remoteContent) {
        console.warn('Unexpected local content after synchronization: ', {
          expected: remoteContent,
          actual: localContent,
          diff: ChainPad.Diff.diff(remoteContent, localContent)
        });
      }
    }

    _onConnectionChange(info) {
      if (this._connection.status === ConnectionStatus.DISCONNECTED) {
        return;
      }
      console.log('Connection status: ' + info.state);
      this._connection.toolbar.failed();
      if (info.state) {
        this._connection.status = ConnectionStatus.CONNECTING;
        this._connection.toolbar.reconnecting(info.myId);
      } else {
        this._connection.chainpad.abort();
        this.setEditable(false);
      }
    }

    _beforeReconnecting(callback) {
      const oldChannel = this._channel;
      this._updateKeys().then(() => {
        if (this._channel !== oldChannel) {
          this._editorConfig.onKeysChanged();
          this.setEditable(false);
          Interface.getAllowRealtimeCheckbox().prop('checked', false);
          this._onAbort();
        } else {
          callback(this._channel, this._patchedEditor.getHyperJSON());
        }
      });
    }

    _onAbort(info, reason, debug) {
      if (this._connection.status === ConnectionStatus.DISCONNECTED) {
        // We already left the realtime session.
        return;
      }

      console.log("Aborting the realtime session!");
      this._connection.status = ConnectionStatus.DISCONNECTED;

      // Stop the realtime content synchronization (leave the WYSIWYG editor Netflux channel associated with the edited
      // document field).
      this._connection.realtimeInput.stop();

      // Notify the others that we're editing offline (outside of the realtime session).
      this._editorConfig.setRealtimeEditing(false);

      // Stop the autosave (and leave the events Netflux channel associated with the edited document).
      Saver.stop();

      // Remove the realtime toolbar.
      this._connection.toolbar.failed();
      this._connection.toolbar.toolbar.remove();

      // Leave the user data Netflux channel associated with the edited document, in order to stop receiving user caret
      // updates.
      this._connection.userData.leave?.();
      // And remove the user caret indicators.
      this._changeUserIcons({});

      // Typing tests require the realtime session to be active.
      delete window.easyTest;

      // Cleanup connection data.
      this._connection = {
        status: ConnectionStatus.DISCONNECTED
      };

      if (reason || debug) {
        ErrorBox.show(reason || 'disconnected', debug);
      }
    }

    _easyTest() {
      let container, offset;
      const selection = this._editor.getSelection();
      const range = selection?.rangeCount && selection.getRangeAt(0);
      if (range) {
        container = range.startContainer;
        offset = range.startOffset;
      }
      const test = TypingTest.testInput(this._editor.getContentWrapper(), container, offset, this._onLocal);
      this._onLocal();
      return test;
    }

    static _getXPath(element) {
      let xpath = '';
      for ( ; element && element.nodeType == 1; element = element.parentNode ) {
        let id = $(element.parentNode).children(element.tagName).index(element) + 1;
        id = id > 1 ? '[' + id + ']' : '';
        xpath = '/' + element.tagName.toLowerCase() + id + xpath;
      }
      return xpath;
    }
  
    static _getPrettyName(userName) {
      return userName ? userName.replace(/^.*-([^-]*)%2d\d*$/, function(all, one) { 
        return decodeURIComponent(one);
      }) : userName;
    }
  }

  window.REALTIME_DEBUG = window.REALTIME_DEBUG || {};
  window.REALTIME_DEBUG.logs = [];
  ['debug', 'error', 'info', 'log', 'trace', 'warn'].forEach(level => {
    const original = console[level];
    console[level] = function (...args) {
      original(...args);
      window.REALTIME_DEBUG.logs.push([level, ...args]);
    };
  });

  return RealtimeEditor;
});
