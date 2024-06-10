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
    // Before trying to connect to the realtime session, and after leaving it.
    DISCONNECTED: 0,
    // While trying to connect to the realtime session.
    CONNECTING: 1,
    // After successfully connecting to the realtime session, as long as the edited content can be patched.
    CONNECTED: 2,
    // While connected to the realtime session, when the edited content can't be patched (e.g. because the content is
    // being refreshed after a macro was inserted, which requires server-side rendering).
    PAUSED: 3
  };

  class RealtimeEditor {
    constructor(editor, realtimeContext) {
      this._editor = editor;
      this._realtimeContext = realtimeContext;

      // The editor wrapper used to smoothly update (patch) the edited content without losing the caret position.
      this._patchedEditor = new Patches(editor);

      // The channel used to synchronize the edited content (notify others when you make a change and be notified when
      // others make changes).
      this._channel = realtimeContext.channels[EDITOR_TYPE];

      // The channel used to synchronize the content (auto)save (notify others when you save and be notified when others
      // save, in order to avoid merge conflicts and creating unnecessary document revisions).
      this._eventsChannel = realtimeContext.channels.events;

      // The channel used to synchronize the user caret position (notify others when your caret position changes and be
      // notified when others' caret position changes).
      this._userDataChannel = realtimeContext.channels.userdata;
  
      Interface.realtimeAllowed(realtimeContext.realtimeEnabled);
      this._createAllowRealtimeCheckbox();

      this._connection = {
        status: ConnectionStatus.DISCONNECTED
      };

      if (realtimeContext.realtimeEnabled) {
        this._startRealtimeSync();
      }
    }

    setEditable(editable) {
      this._editor.setReadOnly(!editable);
      $('.buttons [name^="action_save"], .buttons [name^="action_preview"]').prop('disabled', !editable);
    }

    async lockDocument() {
      const getDocumentLock = new Promise((resolve, reject) => {
        if (XWiki.DocumentLock) {
          resolve(XWiki.DocumentLock);
        } else {
          require(['xwiki-document-lock'], resolve, reject);
        }
      });
      XWiki.DocumentLock = await getDocumentLock;
      XWiki.EditLock = new XWiki.DocumentLock();
      return XWiki.EditLock.lock();
    }

    _startRealtimeSync() {
      this._connection.status = ConnectionStatus.CONNECTING;

      // List of pretty names of all users (mapped with their server ID).
      this._connection.userData = {};

      // Don't let the user edit until the real-time framework is ready.
      this.setEditable(false);

      this._connection.realtimeInput = ChainPadNetflux.start(this._getRealtimeOptions());

      // Notify the others that we're editing in realtime.
      this._realtimeContext.setRealtimeEnabled(true);
  
      // Listen to local changes and propagate them to the other users.
      this._editor.onChange(() => {
        if (this._connection.status === ConnectionStatus.CONNECTED) {
          this._saver.destroyDialog();
          this._saver.setLocalEditFlag(true);
          this._onLocal();
        }
      });

      this._editor.onLock(this._onLock.bind(this));
      this._editor.onUnlock(() => {
        // The editor is usually unlocked after the content is refreshed (e.g. after a macro is inserted). We execute
        // our handler on the next tick because our handler can trigger a new refresh (e.g. if we received remote
        // changes that either add a new macro or modify the parameters of an existing macro), and we want to avoid
        // executing "nested" refresh (async) commands because CKEditor doesn't handle them well.
        setTimeout(this._onUnlock.bind(this), 0);
      });

      // Leave the realtime session and stop the autosave when the editor is destroyed. We have to do this because the
      // editor can be destroyed without the page being reloaded (e.g. when editing in-place).
      this._editor.onBeforeDestroy(() => {
        // Notify the others that we're not editing anymore.
        this._realtimeContext.destroy();
        this._onAbort();
      });

      this._addNetfluxChannelToSubmittedData();

      // Export the typing tests to the window.
      // Call like `test = easyTest()`
      // Terminate the test like `test.cancel()`
      window.easyTest = this._easyTest.bind(this);
    }

    _addNetfluxChannelToSubmittedData() {
      // Indicate the Netflux channel used to synchronize the edited content when performing the HTML conversion (e.g.
      // when refreshing the content after a macro is inserted) in order to render the content using the effective
      // author associated with this channel (and thus prevent privilege escalation through script injection in the
      // realtime session).
      $(document).off('xwiki:wysiwyg:convertHTML.realtime')
        .on('xwiki:wysiwyg:convertHTML.realtime', (event, conversionParams) => {
          conversionParams.netfluxChannel = this._channel;
        });
      
        const fieldSet = this._editor.getToolBar().closest('form, .form, body')
          .querySelector('input[name=form_token]').parentNode;
        let netfluxChannelInput = fieldSet.querySelector(
          `input[name=netfluxChannel][data-for="${CSS.escape(this._editor.getFormFieldName())}"]`);
        if (!netfluxChannelInput) {
          netfluxChannelInput = document.createElement('input');
          netfluxChannelInput.setAttribute('type', 'hidden');
          netfluxChannelInput.setAttribute('name', 'netfluxChannel');
          netfluxChannelInput.setAttribute('data-for', this._editor.getFormFieldName());
          fieldSet.prepend(netfluxChannelInput);
        }
        netfluxChannelInput.value = this._channel;
    }

    /**
     * Update the channels keys for reconnecting WebSocket.
     */
    async _updateChannels() {
      const channels = await this._realtimeContext.updateChannels();
      this._channel = channels[EDITOR_TYPE] || this._channel;
      this._eventsChannel = channels.events || this._eventsChannel;
      this._userDataChannel = channels.userdata || this._userDataChannel;
      return channels;
    }

    _createAllowRealtimeCheckbox() {
      const realtimeEnabled = this._realtimeContext.realtimeEnabled;
      // Don't display the checkbox in the following cases:
      // * realtimeEnabled 0 (instead of true/false) => we can't connect to the websocket service
      // * realtime is disabled and we're not an advanced user
      if (realtimeEnabled !== 0 && (realtimeEnabled || this._realtimeContext.user.advanced)) {
        const allowRealtimeCheckbox = Interface.createAllowRealtimeCheckbox(Interface.realtimeAllowed());
        const realtimeToggleHandler = () => {
          if (allowRealtimeCheckbox.prop('checked')) {
            // Disable the checkbox while we're fetching the channels.
            allowRealtimeCheckbox.prop('disabled', true);
            // We need to fetch the channels before we can connect to the realtime session because:
            // * the channels might have been closed since we left the realtime session
            // * the channels might have changed since we last connected to the realtime session
            // * the channels might not have been created yet because we started editing with realtime disabled.
            this._updateChannels().then(() => {
              Interface.realtimeAllowed(true);
              this._startRealtimeSync();
            }).catch(() => {
              // We failed to fetch the channels so we can't connect to the realtime session.
              allowRealtimeCheckbox.prop('checked', false);
            }).finally(() => {
              // Re-enable the checkbox so that the user can try again.
              allowRealtimeCheckbox.prop('disabled', false);
            });
          } else {
            this._realtimeContext.displayDisableModal((state) => {
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

    async _createSaver(info, userName) {
      const saverConfig = {
        editorType: EDITOR_TYPE,
        editorName: 'WYSIWYG',
        // Id of the wiki page form.
        formId: RealtimeEditor._getFormId(),
        userList: info.userList,
        userName,
        network: info.network,
        channel: this._eventsChannel,
        setTextValue: (newText) => {
          this._patchedEditor.setHTML(newText, true);
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
      this._saver = await new Saver(saverConfig).toBeReady();
      this._saver._lastSaved.mergeMessage = Interface.createMergeMessageElement(
        this._connection.toolbar.toolbar.find('.rt-toolbar-rightside'));
      this._saver.setLastSavedContent(this._editor.getOutputHTML());
    }

    static _getFormId() {
      if (window.XWiki.editor === 'wysiwyg') {
        if (window.XWiki.contextaction === 'view') {
          return 'inplace-editing';
        } else {
          return 'edit';
        }
      } else {
        return 'inline';
      }
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
      this._connection.userList.users.filter(id => updatedData[id]?.['cursor_' + EDITOR_TYPE]).forEach(id => {
        const data = updatedData[id];
        const name = RealtimeEditor._getPrettyName(data.name);
        // Set the user position.
        const element = ownerDocument.evaluate(data['cursor_' + EDITOR_TYPE], this._editor.getContentWrapper(), null,
          XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
        if (!element) {
          return;
        }
        const top = $(element).position().top;
        if (!positions[top]) {
          positions[top] = [id];
        } else {
          positions[top].push(id);
        }
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
          // Use the default top value (which is normally the top padding of the rich text area) if the element holding
          // the caret has 0 or negative top value (this can happen for instance if the caret is directly under the root
          // element, e.g. the BODY element for the standalone edit mode).
          'top': top > 0 ? top + 'px' : ''
        });
        $(this._editor.getContentWrapper()).after($indicator);
      });
    }

    _getRealtimeOptions() {
      return {
        initialState: this._patchedEditor.getHyperJSON() || '{}',
        websocketURL: this._realtimeContext.webSocketURL,
        userName: this._realtimeContext.user.name,
        channel: this._channel,
        crypto: Crypto,
        network: this._realtimeContext.network,

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
            this.lockDocument();
          }
          oldUsers = JSON.parse(JSON.stringify(info.userList.users || []));
        }
      });
    }

    async _onReady(info) {
      if (this._connection.status !== ConnectionStatus.CONNECTING) {
        return;
      }

      this._connection.chainpad = info.realtime;

      if (!this._connection.isOnReadyPreviouslyCalled) {
        this._connection.isOnReadyPreviouslyCalled = true;
        // Update the user list to link the wiki name to the user id.
        const userDataConfig = {
          myId: info.myId,
          userName: this._realtimeContext.user.name,
          userAvatar: this._realtimeContext.user.avatarURL,
          onChange: this._connection.userList.onChange,
          crypto: Crypto,
          editor: EDITOR_TYPE,
          getCursor: () => {
            // We take into account only the first selection range when showing the user cursor.
            let node = this._editor.getSelection()[0]?.startContainer;
            if (!node) {
              return '';
            }
            node = (node.nodeName === '#text') ? node.parentNode : node;
            return this._getXPath(node);
          }
        };
        if (!realtimeConfig.marginAvatar) {
          delete userDataConfig.getCursor;
        }

        this._connection.userData = await UserData.start(info.network, this._userDataChannel, userDataConfig);
        this._connection.userList.change.push(this._changeUserIcons.bind(this));
      }

      await this._createSaver(info, this._realtimeContext.user.name);

      this._connection.status = ConnectionStatus.CONNECTED;

      // Initialize the edited content with the content from the realtime session.
      await this._onRemote(info);

      console.debug('Unlocking editor');
      this.setEditable(true);
    }

    _onLocal(localContent) {
      if (this._connection.status !== ConnectionStatus.CONNECTED) {
        return;
      }
      if (typeof localContent !== 'string') {
        // Stringify the JSON and send it into ChainPad.
        localContent = this._patchedEditor.getHyperJSON();
      }
      console.debug('Push local content: ' + localContent);
      this._connection.chainpad.contentUpdate(localContent);

      const remoteContent = this._connection.chainpad.getUserDoc();
      if (remoteContent !== localContent) {
        console.warn('Unexpected remote content after synchronization: ', {
          expected: localContent,
          actual: remoteContent,
          diff: ChainPad.Diff.diff(localContent, remoteContent)
        });
      }
    }

    async _onRemote(info) {
      if (this._connection.status !== ConnectionStatus.CONNECTED) {
        return;
      }

      let remoteContent = info.realtime.getUserDoc();
      console.debug('Received remote content: ' + remoteContent);

      // Build a DOM from HyperJSON, diff and patch the editor, then wait for the widgets to be ready (in case they had
      // to be reloaded, e.g. rendering macros have to be rendered server-side).
      await this._patchedEditor.setHyperJSON(remoteContent);

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
      console.debug('Connection status: ' + info.state);
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
      this._updateChannels().then(() => {
        if (this._channel === oldChannel) {
          // The Netflux channel used before the WebSocket connection closed is still available so we can still use it.
          callback(this._channel, this._patchedEditor.getHyperJSON());
        } else {
          // The Netflux channel used before the WebSocket connection closed is not available anymore so we have to
          // abort the current realtime session.
          this.setEditable(false);
          this._onAbort();
          if (!this._saver.getLocalEditFlag()) {
            // Fortunately we don't have any unsaved local changes so we can rejoin the realtime session using the new
            // Netflux channel.
            this._startRealtimeSync();
          } else {
            // We can't rejoin the realtime session using the new Netflux channel because we would lose the unsaved
            // local changes. Let the user decide what to do.
            Interface.getAllowRealtimeCheckbox().prop('checked', false);
            this._realtimeContext.displayReloadModal();
          }
        }
      });
    }

    _onAbort(info, reason, debug) {
      if (this._connection.status === ConnectionStatus.DISCONNECTED) {
        // We already left the realtime session.
        return;
      }

      console.debug("Aborting the realtime session!");
      this._connection.status = ConnectionStatus.DISCONNECTED;

      // Stop the realtime content synchronization (leave the WYSIWYG editor Netflux channel associated with the edited
      // document field).
      this._connection.realtimeInput.stop();

      // Notify the others that we're editing offline (outside of the realtime session).
      this._realtimeContext.setRealtimeEnabled(false);

      // Stop the autosave (and leave the events Netflux channel associated with the edited document).
      this._saver.stop();

      // Remove the realtime toolbar.
      this._connection.toolbar.failed();
      this._connection.toolbar.toolbar.remove();

      // Stop receiving user caret updates (leave the user data Netflux channel associated with the edited document).
      this._connection.userData.stop?.();
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

    _onLock() {
      if (this._connection.status === ConnectionStatus.CONNECTED) {
        this._connection.status = ConnectionStatus.PAUSED;
        this._connection.remoteContentBeforeLock = this._connection.chainpad.getUserDoc();
      }
    }

    _onUnlock() {
      if (this._connection.status === ConnectionStatus.PAUSED) {
        this._connection.status = ConnectionStatus.CONNECTED;
        const remoteContentAfterLock = this._connection.chainpad.getUserDoc();
        const localContentAfterLock = this._patchedEditor.getHyperJSON();
        if (remoteContentAfterLock === this._connection.remoteContentBeforeLock) {
          // We didn't receive any remote changes while the editor was locked.
          if (localContentAfterLock !== this._connection.remoteContentBeforeLock) {
            // The local content has changed while the editor was locked (e.g. because one of the inserted macros is
            // editable in-place and its rendering added nested editable areas).
            this._onLocal();
          }
        } else if (localContentAfterLock === this._connection.remoteContentBeforeLock) {
          // The local content didn't change while the editor was locked, but we received remote changes. Let's apply
          // them.
          this._onRemote({
            realtime: this._connection.chainpad
          });
        } else {
          // The local content and the remote content have diverged. We need a 3-way merge.
          this._onLocal(Patches.merge(this._connection.remoteContentBeforeLock, remoteContentAfterLock,
            localContentAfterLock));
          this._onRemote({
            realtime: this._connection.chainpad
          });
        }
      }
    }

    _easyTest() {
      return TypingTest.testInput(
        () => this._editor.getContentWrapper()?.ownerDocument.defaultView.getSelection(),
        this._onLocal.bind(this)
      );
    }

    _getXPath(element) {
      let xpath = [];
      const root = this._editor.getContentWrapper();
      while (element && element.nodeType === Node.ELEMENT_NODE && element !== root) {
        let index = $(element.parentNode).children(element.tagName).index(element) + 1;
        // Specify the index only if it's not the first element of its kind.
        index = index > 1 ? '[' + index + ']' : '';
        xpath.push(element.tagName.toLowerCase() + index);
        element = element.parentNode;
      }
      // The returned XPath must be relative to the content wrapper because the HTML structure beyond that is different
      // for different edit modes (e.g. the standalone edit mode uses an iframe where the content wrapper is the BODY
      // element while the in-place edit mode uses a DIV element).
      if (element === root) {
        xpath.push('.');
      }
      // The path needs to start from the top-most element.
      xpath = xpath.reverse();
      return xpath.join('/');
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
