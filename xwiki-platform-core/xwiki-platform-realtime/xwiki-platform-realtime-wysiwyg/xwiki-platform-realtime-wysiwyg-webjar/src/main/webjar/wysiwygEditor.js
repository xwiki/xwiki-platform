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
  'xwiki-realtime-toolbar',
  'chainpad-netflux',
  'xwiki-realtime-userData',
  'xwiki-realtime-typingTests',
  'xwiki-realtime-interface',
  'xwiki-realtime-saver',
  'chainpad',
  'xwiki-realtime-crypto',
  'xwiki-realtime-document',
  'xwiki-realtime-wysiwyg-patches'
], function (
  /* jshint maxparams:false */
  $, Toolbar, ChainPadNetflux, UserData, TypingTest, Interface, Saver, ChainPad, Crypto, xwikiDocument, Patches
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
      this._saverChannel = realtimeContext.channels.saver;

      // The channel used to synchronize the user caret position (notify others when your caret position changes and be
      // notified when others' caret position changes).
      this._userDataChannel = realtimeContext.channels.userData;
  
      // Don't create the checkbox if we can't connect to the WebSocket service.
      if (realtimeContext.realtimeEnabled !== 0) {
        this._createAllowRealtimeCheckbox();
      }

      this._connection = {
        status: ConnectionStatus.DISCONNECTED
      };

      if (realtimeContext.realtimeEnabled) {
        this._startRealtimeSync();
      }
    }

    setEditable(editable) {
      this._editor.setReadOnly(!editable);
      $('.buttons [name^="action_save"]').prop('disabled', !editable);
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

    _setConnectionStatus(status) {
      this._connection.status = status;
      this._editor.setConnectionStatus(status);
    }

    _startRealtimeSync() {
      this._setConnectionStatus(ConnectionStatus.CONNECTING);

      // List of pretty names of all users (mapped with their server ID).
      this._connection.userData = {};

      // Don't let the user edit until the real-time framework is ready.
      this.setEditable(false);

      this._createToolbar();

      this._connection.realtimeInput = ChainPadNetflux.start(this._getRealtimeOptions());

      // Notify the others that we're editing in realtime.
      this._realtimeContext.setRealtimeEnabled(true);

      // Listen to local changes and propagate them to the other users.
      this._editor.onChange(() => {
        if (this._connection.status === ConnectionStatus.CONNECTED) {
          this._onLocal();
          this._saver.contentModifiedLocally();
        }
      });

      this._editor.onLock(this._pauseRealtimeSync.bind(this));
      this._editor.onUnlock(() => {
        // The editor is usually unlocked after the content is refreshed (e.g. after a macro is inserted). We execute
        // our handler on the next tick because our handler can trigger a new refresh (e.g. if we received remote
        // changes that either add a new macro or modify the parameters of an existing macro), and we want to avoid
        // executing "nested" refresh (async) commands because CKEditor doesn't handle them well.
        setTimeout(this._resumeRealtimeSync.bind(this), 0);
      });

      // Flush the uncommitted work back to the server on actions that might cause the editor to be destroyed without
      // the beforeDestroy event being called.
      const flushUncommittedWork = () => {
        if (this._connection.status === ConnectionStatus.CONNECTED) {
          this._connection.chainpad.sync();
        }
      };
      const form = document.getElementById(RealtimeEditor._getFormId());
      $(form).on('xwiki:actions:cancel xwiki:actions:save xwiki:actions:reload', flushUncommittedWork);

      const resetContent = this._resetContent.bind(this);
      $(form).on('xwiki:actions:reload', resetContent);

      // Leave the realtime session and stop the autosave when the editor is destroyed. We have to do this because the
      // editor can be destroyed without the page being reloaded (e.g. when editing in-place).
      this._editor.onBeforeDestroy(() => {
        // Flush the uncommitted work back to the server. There is no guarantee that the work is actually committed but
        // at least we try.
        flushUncommittedWork();
        $(form).off('xwiki:actions:cancel xwiki:actions:save xwiki:actions:reload', flushUncommittedWork);

        $(form).off('xwiki:actions:reload', resetContent);

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

    _createToolbar() {
      this._connection.toolbar = new Toolbar({
        save: (...args) => this._saver?.save(...args),
        leave: () => Interface.getAllowRealtimeCheckbox().click(),
        selectUser: userId => {
          const editableContentLocation = this._editor.getContentWrapper().ownerDocument.defaultView.location;
          const baseHref = editableContentLocation.href.split('#')[0] || '';
          editableContentLocation.href = baseHref + '#rt-user-' + userId;
        }
      });
    }

    _addNetfluxChannelToSubmittedData() {
      // Indicate the Netflux channel used to synchronize the edited content when performing the HTML conversion (e.g.
      // when refreshing the content after a macro is inserted) in order to render the content using the effective
      // author associated with this channel (and thus prevent privilege escalation through script injection in the
      // realtime session).
      const convertHTMLListener = (event, conversionParams) => {
        conversionParams.netfluxChannel = this._channel;
      };
      $(document).on('xwiki:wysiwyg:convertHTML', convertHTMLListener);

      // Indicate the Netflux channel used to synchronize the edited content when saving the content.
      const fieldSet = this._connection.toolbar.getForm().querySelector('input[name=form_token]').parentNode;
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

      // Cleanup when we leave the realtime session.
      this._removeNetfluxChannelFromSubmittedData = () => {
        $(document).off('xwiki:wysiwyg:convertHTML', convertHTMLListener);
        netfluxChannelInput.value = '';
      };
    }

    /**
     * Update the channels keys for reconnecting WebSocket.
     */
    async _updateChannels() {
      const channels = await this._realtimeContext.updateChannels();
      this._channel = channels[EDITOR_TYPE] || this._channel;
      this._saverChannel = channels.saver || this._saverChannel;
      this._userDataChannel = channels.userData || this._userDataChannel;
      return channels;
    }

    _createAllowRealtimeCheckbox() {
      Interface.createAllowRealtimeCheckbox({
        checked: this._realtimeContext.realtimeEnabled,
        join: async () => {
          // We need to fetch the channels before we can connect to the realtime session because:
          // * the channels might have been closed since we left the realtime session
          // * the channels might have changed since we last connected to the realtime session
          // * the channels might not have been created yet because we started editing with realtime disabled.
          await this._updateChannels();
          this._startRealtimeSync();
        },
        leave: this._onAbort.bind(this)
      });
      this._editor.onBeforeDestroy(() => {
        // While editing in-place the editor can be destroyed without the page being reloaded.
        Interface.getAllowRealtimeCheckbox().off();
      });
    }

    async _createSaver(info) {
      this._saver = await new Saver({
        // Edit form ID.
        formId: RealtimeEditor._getFormId(),
        userName: this._realtimeContext.user.sessionId,
        network: info.network,
        channel: this._saverChannel,
        onStatusChange: this._connection.toolbar.onSaveStatusChange.bind(this._connection.toolbar),
        onCreateVersion: version => {
          version.author = Object.values(this._connection.userData).find(
            user => (user?.sessionId && user.sessionId === version.author) ||
              (user?.reference && user.reference === version.author?.reference)
          ) || version.author;
          this._connection.toolbar?.onCreateVersion(version);
        }
      }).toBeReady();
    }

    async _resetContent() {
      const html = await $.get(xwikiDocument.getURL('get', $.param({
        xpage:'get',
        outputSyntax:'annotatedhtml',
        outputSyntaxVersion:'5.0',
        transformations:'macro',
        language: xwikiDocument.language
      })));
      this._hideChangesFromSaver(this._patchedEditor.setHTML(html, true));
    }

    async _hideChangesFromSaver(promise) {
      const contentModifiedLocally = this._saver.contentModifiedLocally;
      this._saver.contentModifiedLocally = () => {};
      try {
        await promise;
      } finally {
        this._saver.contentModifiedLocally = contentModifiedLocally;
      }
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

    _changeUserIcons(userData) {
      // If no new data (someone has just joined or left the channel), get the latest known values.
      this._connection.userData = userData = userData || this._connection.userData;
      const users = this._connection.userList.users.filter(id => userData[id]).map(id => ({id, ...userData[id]}));
      this._connection.toolbar.onUserListChange(users);

      const contentWrapper = this._editor.getContentWrapper();
      const contentWrapperTop = $(contentWrapper).offset().top;
      const ownerDocument = contentWrapper.ownerDocument;
      $(ownerDocument).find('.realtime-user-position').remove();
      users.filter(user => user['cursor_' + EDITOR_TYPE]).forEach(user => {
        // Set the user position.
        const element = ownerDocument.evaluate(user['cursor_' + EDITOR_TYPE], contentWrapper, null,
          XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
        if (element) {
          const top = $(element).offset().top - contentWrapperTop;
          $('<img/>').attr({
            class: 'realtime-user-position',
            id: 'rt-user-' + user.id,
            src: user.avatar,
            alt: user.name,
            title: user.name
          }).css({
            // Use the default top value (which is normally the top padding of the rich text area) if the element
            // holding the caret has 0 or negative top value (this can happen for instance if the caret is directly
            // under the root element, e.g. the BODY element for the standalone edit mode).
            top: top > 0 ? top + 'px' : ''
          }).insertAfter($(contentWrapper));
        }
      });
    }

    _getRealtimeOptions() {
      return {
        initialState: this._patchedEditor.getHyperJSON() || '{}',
        websocketURL: this._realtimeContext.webSocketURL,
        userName: this._realtimeContext.user.sessionId,
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
        beforeReconnecting: this._beforeReconnecting.bind(this)
      };
    }

    _onInit(info) {
      // List of users still connected to the channel (server IDs).
      this._connection.userList = info.userList;
      this._connection.toolbar.onConnectionStatusChange(1 /* connecting */, info.myID);
      // When someone leaves, if they used Save&View, it removes the lock from the document. We're going to add it
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
          user: this._realtimeContext.user,
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

        this._connection.userData = await UserData.start(info.network, this._userDataChannel, userDataConfig);
        this._changeUserIcons(this._connection.userData);
        this._connection.userList.change.push(this._changeUserIcons.bind(this));
      }

      await this._createSaver(info);

      this._setConnectionStatus(ConnectionStatus.CONNECTED);

      // Initialize the edited content with the content from the realtime session.
      await this._onRemote(info);

      console.debug('Unlocking editor');
      this.setEditable(true);

      // Allow the user to leave the realtime editing session now that we're connected.
      Interface.getAllowRealtimeCheckbox().prop('disabled', false);

      this._connection.toolbar.onConnectionStatusChange(2 /* connected */, info.myId);
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

      // We have to pause the realtime sync while we apply remote changes because reloading the content (e.g. when a
      // rendering macro is inserted or updated) and restoring the selection are asynchronous operations (macros have to
      // be rendered server-side and selection restore uses a Web Worker to perform the diff).
      this._pauseRealtimeSync();

      try {
        let remoteContent = info.realtime.getUserDoc();
        console.debug("Received remote content: " + remoteContent);

        // Build a DOM from HyperJSON, diff and patch the editor, then wait for the widgets to be ready (in case they
        // had to be reloaded, e.g. rendering macros have to be rendered server-side).
        await this._patchedEditor.setHyperJSON(remoteContent);

        const localContent = this._patchedEditor.getHyperJSON();
        if (localContent !== remoteContent) {
          console.warn("Unexpected local content after synchronization: ", {
            expected: remoteContent,
            actual: localContent,
            diff: ChainPad.Diff.diff(remoteContent, localContent),
          });
        }
      } finally {
        await this._resumeRealtimeSync();
      }
    }

    _onConnectionChange(info) {
      console.debug('Connection status: ' + info.state);
      if (info.state) {
        // Reconnecting.
        this._connection.toolbar.onConnectionStatusChange(1 /* connecting */, info.myId);
      } else {
        // Temporarily disconnected.
        // The internal state is set to 'connecting' because 'disconnected' is used when the user leaves the realtime
        // session. We show 'Disconnected' on the toolbar to indicate that the user is offline.
        this._setConnectionStatus(ConnectionStatus.CONNECTING);
        this._connection.toolbar.onConnectionStatusChange(0 /* disconnected */);
        // Disable the editor while we're disconnected because ChainPad doesn't support very well merging changes made
        // offline, especially if we stay offline for a long time.
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
          this._onAbort();
          if (
            !this._saver.isDirty() ||
            !this._realtimeContext.channels.wysiwyg_users // jshint ignore:line
          ) {
            // Either we don't have any unsaved local changes or there's no one else connected to the realtime session.
            // We can rejoin the realtime session using the new Netflux channel.
            //
            // The editor was previously put in read-only mode when we got disconnected from the WebSocket (i.e. when
            // the WebSocket connection status changed, see above). The editor takes into account nested calls to
            // setReadOnly so we need to make sure the previous setEditable(false) has a corresponding call to
            // setEditable(true). The user won't be able to edit right away because the editor is put back in read-only
            // mode while we reconnect to the realtime session (in _startRealtimeSync).
            this.setEditable(true);
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

    _onAbort() {
      if (this._connection.status === ConnectionStatus.DISCONNECTED) {
        // We already left the realtime session.
        return;
      } else if (this._connection.status === ConnectionStatus.CONNECTING) {
        // The editor has been put in read-only mode before we attempted to (re)connect to the realtime session. We need
        // to restore the editable state before aborting the realtime session, in order to leave the editor in the state
        // it was before we initiated the connection.
        this.setEditable(true);
      }

      console.debug("Aborting the realtime session!");
      this._setConnectionStatus(ConnectionStatus.DISCONNECTED);

      // Stop the realtime content synchronization (leave the WYSIWYG editor Netflux channel associated with the edited
      // document field).
      this._connection.realtimeInput.stop();

      // Notify the others that we're editing offline (outside of the realtime session).
      this._realtimeContext.setRealtimeEnabled(false);

      // Stop the autosave (and leave the saver Netflux channel associated with the edited document).
      this._saver?.stop();

      // Remove the realtime edit toolbar.
      this._connection.toolbar.destroy();

      // Stop receiving user caret updates (leave the user data Netflux channel associated with the edited document).
      this._connection.userData.stop?.();
      // And remove the user caret indicators.
      this._changeUserIcons({});

      // Don't include the channel in the submitted data if we're not connected to the realtime session.
      this._removeNetfluxChannelFromSubmittedData();

      // Typing tests require the realtime session to be active.
      delete window.easyTest;

      // Cleanup connection data.
      this._connection = {
        status: ConnectionStatus.DISCONNECTED
      };
    }

    _pauseRealtimeSync() {
      if (this._connection.status === ConnectionStatus.CONNECTED) {
        this._setConnectionStatus(ConnectionStatus.PAUSED);
        this._connection.pauseDepth = 1;
        this._connection.remoteContentBeforePause = this._connection.chainpad.getUserDoc();
      } else if (this._connection.status === ConnectionStatus.PAUSED) {
        this._connection.pauseDepth++;
      }
    }

    async _resumeRealtimeSync() {
      if (this._connection.status === ConnectionStatus.PAUSED && --this._connection.pauseDepth === 0) {
        this._setConnectionStatus(ConnectionStatus.CONNECTED);
        const remoteContentAfterPause = this._connection.chainpad.getUserDoc();
        const localContentAfterPause = this._patchedEditor.getHyperJSON();
        if (remoteContentAfterPause === this._connection.remoteContentBeforePause) {
          // We didn't receive any remote changes while the realtime sync was paused.
          if (localContentAfterPause !== this._connection.remoteContentBeforePause) {
            // The local content has changed while the realtime sync was paused (e.g. because one of the inserted macros
            // is editable in-place and its rendering added nested editable areas).
            this._onLocal();
          }
        } else if (localContentAfterPause === this._connection.remoteContentBeforePause) {
          // The local content didn't change while the realtime sync was paused, but we received remote changes. Let's
          // apply them.
          await this._onRemote({
            realtime: this._connection.chainpad
          });
        } else {
          // The local content and the remote content have diverged. We need a 3-way merge.
          this._onLocal(Patches.merge(this._connection.remoteContentBeforePause, remoteContentAfterPause,
            localContentAfterPause));
          await this._onRemote({
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
