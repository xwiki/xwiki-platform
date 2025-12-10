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
  'xwiki-realtime-wysiwyg-patches',
  'xwiki-l10n!xwiki-realtime-messages'
], function (
  /* jshint maxparams:false */
  $, Toolbar, ChainPadNetflux, UserData, TypingTest, Interface, Saver, ChainPad, Crypto, xwikiDocument, Patches,
  Messages
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

      this._connection = {
        status: ConnectionStatus.DISCONNECTED
      };

      // Don't create the checkbox if we can't connect to the WebSocket service.
      if (realtimeContext.realtimeEnabled !== 0) {
        this._createAllowRealtimeCheckbox();
      }

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
      if (this._connection.status === ConnectionStatus.CONNECTED && status !== ConnectionStatus.CONNECTED) {
        // Remember the content before the connection is lost, in order to be able to perform a 3-way merge when / if
        // the connection is re-established.
        this._connection.remoteContentBeforeDisconnect = this._connection.chainpad.getUserDoc();
      }
      this._connection.status = status;
      this._editor.setConnectionStatus(status);
    }

    async toBeConnected() {
      return await this._connection.promise;
    }

    _startRealtimeSync() {
      this._connection.promise = new Promise(resolve => {
        this._connection.resolve = resolve;
      });

      this._setConnectionStatus(ConnectionStatus.CONNECTING);

      // List of pretty names of all users (mapped with their server ID).
      this._connection.userData = {};

      // Don't let the user edit until the real-time framework is ready.
      this.setEditable(false);

      this._createToolbar();

      // Check the session storage for unsaved changes from a previous collaboration session.
      this._maybeRestoreUnsavedChanges();

      this._connection.realtimeInput = ChainPadNetflux.start(this._getRealtimeOptions());

      // Notify the others that we're editing in realtime.
      this._realtimeContext.setRealtimeEnabled(true);

      // Make sure the Allow Realtime Collaboration checkbox matches the connection state.
      Interface.getAllowRealtimeCheckbox().prop('checked', true).prop('disabled', true);

      this._addNetfluxChannelToSubmittedData();
      this._registerEventListeners();

      // Export the typing tests to the window.
      // Call like `test = easyTest()`
      // Terminate the test like `test.cancel()`
      globalThis.easyTest = this._easyTest.bind(this);

      return this._connection.promise;
    }

    _maybeRestoreUnsavedChanges() {
      // We use the session storage to keep unsaved changes.
      this._sessionStorageKey = 'xwiki.realtime.unsavedChanges/' +
        XWiki.Model.serialize(xwikiDocument.documentReference) + '/' + this._editor.getFormFieldName();

      // If present, the remote content before disconnect is always more recent than the last saved content, so it wins.
      if (typeof this._connection.remoteContentBeforeDisconnect !== 'string') {
        // The remote content before disconnect is not known, so we check if there are unsaved changes in the session
        // storage, from a previous collaboration session.
        const {previous, current} = JSON.parse(sessionStorage.getItem(this._sessionStorageKey) || '{}');
        if (typeof previous === 'string' && typeof current === 'string') {
          // Rebase unsaved changes on top of the current content.
          const next = this._patchedEditor.getHyperJSON();
          if (next === current) {
            // It looks like our unsaved changes were saved in the end, so there's nothing to restore.
            return;
          }
          const merged = this._merge(previous, next, current);
          let failedToRestore = this._connection.lastMergeFailed;
          this._patchedEditor.setHyperJSON(merged);
          // Rebase the local unsaved changes on top of the remote content when connecting to the realtime session.
          this._connection.remoteContentBeforeDisconnect = previous;
          this.toBeConnected().then(() => {
            failedToRestore = failedToRestore || this._connection.lastMergeFailed;
            if (failedToRestore) {
              this._editor.showNotification(Messages.unsavedChangesRestoreFailed, 'warning');
            } else {
              this._editor.showNotification(Messages.unsavedChangesRestored, 'info');
            }
          });
        }
      }
    }

    _registerEventListeners() {
      let formActionInProgress = false;

      this._connection.listeners = [
        // Listen to local changes and propagate them to the other users.
        this._editor.onChange(() => {
          formActionInProgress = false;
          this._scheduleLocalChangeHandling();
        }),

        this._editor.onLock(this._pauseRealtimeSync.bind(this)),
        this._editor.onUnlock(() => {
          // The editor is usually unlocked after the content is refreshed (e.g. after a macro is inserted). We execute
          // our handler on the next tick because our handler can trigger a new refresh (e.g. if we received remote
          // changes that either add a new macro or modify the parameters of an existing macro), and we want to avoid
          // executing "nested" refresh (async) commands because CKEditor doesn't handle them well.
          setTimeout(this._resumeRealtimeSync.bind(this), 0);
        })
      ];

      const form = document.getElementById(RealtimeEditor._getFormId());
      const onFormAction = () => {
        // Flush the uncommitted work back to the server on actions that might cause the editor to be destroyed without
        // the beforeDestroy event being called.
        this._flushUncommittedWork();

        // Don't keep and restore unsaved changes if the user leaves the edit session as a result of a form action:
        // * for cancel action the user explicitly decided to discard the unsaved changes
        // * for save action there are no unsaved changes
        // * for reload action we need to take the latest version of the content from the server
        formActionInProgress = true;
      };
      $(form).on('xwiki:actions:cancel xwiki:actions:save xwiki:actions:reload', onFormAction);
      this._connection.listeners.push({
        removeListener: () => {
          $(form).off('xwiki:actions:cancel xwiki:actions:save xwiki:actions:reload', onFormAction);
        }
      });

      const resetContent = this._resetContent.bind(this);
      $(form).on('xwiki:actions:reload', resetContent);
      this._connection.listeners.push({
        removeListener: () => {
          $(form).off('xwiki:actions:reload', resetContent);
        }
      });

      // Leave the realtime session and stop the autosave when the editor is destroyed. We have to do this because the
      // editor can be destroyed without the page being reloaded (e.g. when editing in-place).
      this._connection.listeners.push(this._editor.onBeforeDestroy((event) => {
        // Notify the others that we're not editing anymore.
        this._realtimeContext.destroy();

        // Destroy the editor only after all uncommitted work has been pushed to the server.
        event.data.promises.push(this._onAbort());
      }, true));

      // Push unsaved changes to the session storage when the page becomes hidden. We don't save because it can lead to
      // a merge conflict that would not be seen and resolved until the page is visible again (blocking the save for the
      // other realtime collaborators in the mean time). Even if we save by forcing an overwrite, we might not be able
      // to propagate the new version (that we get in the response) to the other collaborators (e.g. if the page was
      // hidden because the user navigated to a different page or closed the browser tab), which would lead to a merge
      // conflict on their side. Moreover, saving whenever the user switches to a different tab affects our automated
      // functional tests which rely a lot on tab switching. Last but not least, we could have multiple users leaving
      // the collaboration session at the same time, e.g. at the end of a meeting, which would lead to many revisions
      // overwriting each other. Ideally, we would have an "auto-save" bot server-side that connects to the realtime
      // session and uses its own ChainPad instance to reconstruct the content from the received patches, but it's not
      // easy to run ChainPad (JavaScript) code server-side.
      const visibilityChangeListener = () => {
        if (document.visibilityState === 'hidden') {
          // The user is either switching to a different window / tab, or closing this window / tab or navigating away.
          // In all these cases the user is not actively editing anymore so first we make sure their local changes are
          // propagated to the other collaborators and then we store the unsaved changes in the session storage.
          //
          // 1. Push the local changes to the other collaborators immediately (instead of waiting for the scheduled
          //    timer). This will mark the content as dirty in the Saver if there are unsaved local changes.
          this._flushUncommittedWork();
          // 2. Store the unsaved local changes in the session storage, unless the user is leaving the edit session
          //    as a result of a form action (e.g. using the Cancel shortcut key).
          if (this._saver?.isDirty() && !formActionInProgress) {
            // The user might be leaving the editing session by accident (without using the form actions) and they have
            // unsaved changes that they might not want to lose.
            sessionStorage.setItem(this._sessionStorageKey, JSON.stringify({
              // We keep the last saved content as well in order to be able to perform a 3-way merge when the user joins
              // back the realtime collaboration session.
              previous: this._connection.lastSavedContent,
              current: this._patchedEditor.getHyperJSON()
            }));
          }
        } else if (document.visibilityState == 'visible') {
          // The user is actively editing again. Clean up the session storage.
          sessionStorage.removeItem(this._sessionStorageKey);
        }
      };
      document.addEventListener('visibilitychange', visibilityChangeListener);
      this._connection.listeners.push({
        removeListener: () => {
          document.removeEventListener('visibilitychange', visibilityChangeListener);
        }
      });
    }

    async _flushUncommittedWork() {
      if (this._connection.status === ConnectionStatus.CONNECTED) {
        // Commit local changes right away (because they are debounced otherwise).
        this._onLocal();
        // Push commits to the server.
        this._connection.chainpad.sync();
        // Wait for aknowledgement.
        await new Promise(resolve => this._connection.chainpad.onSettle(resolve));
      }
    }

    _scheduleLocalChangeHandling() {
      clearTimeout(this._localContentChangeTimeout);
      this._localContentChangeTimeout = setTimeout(this._onLocal.bind(this), 100);
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
        netfluxChannelInput.dataset.for = this._editor.getFormFieldName();
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
          await this._startRealtimeSync();
          // The standard edit toolbar (that triggered the join) is replaced with the realtime toolbar, so we have to
          // move the focus somewhere else, and the best candidate is the editor itself. The user can use the Tab key to
          // reach the edit toolbar.
          this._editor.focus();
        },
        leave: async () => {
          await this._onAbort();
          // The realtime toolbar (that triggered the leave) is replaced with the standard edit toolbar, so we have to
          // move the focus somewhere else, and the best candidate is the editor itself. The user can use the Tab key to
          // reach the edit toolbar again.
          this._editor.focus();
        }
      });
      this._editor.onBeforeDestroy(() => {
        // While editing in-place the editor can be destroyed without the page being reloaded.
        Interface.getAllowRealtimeCheckbox().off();
      });
    }

    async _createSaver(info) {
      // Remember the last saved content in order to be able to restore unsaved changes in case the user leaves the edit
      // mode or closes the browser tab / window without saving.
      this._connection.lastSavedContent = this._patchedEditor.getHyperJSON() || '{}';
      this._saver = await new Saver({
        // Edit form ID.
        formId: RealtimeEditor._getFormId(),
        userName: this._realtimeContext.user.sessionId,
        network: info.network,
        channel: this._saverChannel,
        onLocalStatusChange: (localStatus) => {
          if (localStatus === 2 /* clean */) {
            // All local changes are saved. Remember this version of the content in order to be able to restore future
            // local changes in case the user leaves without saving.
            this._connection.lastSavedContent = this._connection.chainpad.getUserDoc();
            // No unsaved local changes to restore.
            sessionStorage.removeItem(this._sessionStorageKey);
          }
        },
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
      if (globalThis.XWiki.editor === 'wysiwyg') {
        if (globalThis.XWiki.contextaction === 'view') {
          return 'inplace-editing';
        } else {
          return 'edit';
        }
      } else {
        return 'inline';
      }
    }

    /**
     * This is called when someone joins or leaves the realtime collaboration session (in which case there is no user
     * data passed), or when someone's user data changes (in which case the new user data is passed).
     *
     * @param {Object} } userData the updated user data
     */
    _onUserDataChange(userData) {
      // If no new data (someone has just joined or left the channel), get the latest known values.
      this._connection.userData = userData = userData || this._connection.userData;
      const userDataJSON = JSON.stringify(userData);
      if (userDataJSON === this._connection.oldUserDataJSON) {
        // User data didn't change so the UI doesn't need to be updated.
        return;
      }
      this._connection.oldUserDataJSON = userDataJSON;

      // Update the list of users displayed on the toolbar.
      const users = this._connection.userList.users.filter(id => userData[id]).map(id => ({id, ...userData[id]}));
      this._connection.toolbar.onUserListChange(users);

      // Update the user caret indicators displayed on the side of the text area.
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
        } else {
          // The element might be missing because the content update was slower than the user data update. We should try
          // again next time, even if the user data doesn't change, because the content might change.
          this._connection.oldUserDataJSON = null;
        }
      });
    }

    _getRealtimeOptions() {
      return {
        // Start from the last known remote content, if we were previously connected, in order to force a merge if there
        // are local changes (otherwise the remote content may be overwritten by the local content).
        initialState: this._connection.remoteContentBeforeDisconnect || this._patchedEditor.getHyperJSON() || '{}',
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
      let oldUsers = structuredClone(info.userList.users || []);
      info.userList.change.push(() => {
        if (info.userList.length) {
          // If someone has left, try to get the lock.
          if (oldUsers.some(user => !info.userList.users.includes(user))) {
            this.lockDocument();
          }
          oldUsers = structuredClone(info.userList.users || []);
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
          // Trigger user list change when user data changes. This will end up calling _onUserDataChange (see below).
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

        // Update the UI with the initial user data.
        this._onUserDataChange(this._connection.userData);

        // Update the list of users displayed on the toolbar and the user caret indicators displayed on the side of the
        // text area whenever someone joins or leaves the realtime collaboration session.
        this._connection.userList.change.push(this._onUserDataChange.bind(this));
      }

      await this._createSaver(info);

      this._setConnectionStatus(ConnectionStatus.CONNECTED);
      await this._initializeContent(info);

      console.debug('Unlocking editor');
      this.setEditable(true);

      // Allow the user to leave the realtime editing session now that we're connected.
      Interface.getAllowRealtimeCheckbox().prop('disabled', false);

      this._connection.toolbar.onConnectionStatusChange(2 /* connected */, info.myId);
      this._connection.resolve(this);
    }

    async _initializeContent(info) {
      if (this._connection.remoteContentBeforeDisconnect) {
        // We were previously connected to the realtime session so we should perform a 3-way merge between the content
        // before we left (previous), the current local content (current) and the current remote content (next). We do
        // this in order to integrate the changes made outside the realtime session.
        //
        // Backup remoteContentBeforeDisconnect because _pauseRealtimeSync overwrites it.
        const remoteContentBeforeDisconnect = this._connection.remoteContentBeforeDisconnect;
        this._pauseRealtimeSync();
        // Restore remoteContentBeforeDisconnect.
        this._connection.remoteContentBeforeDisconnect = remoteContentBeforeDisconnect;
        // Perform the 3-way merge (if needed).
        await this._resumeRealtimeSync();
      } else {
        // Use the remote content as the initial content, since this is the first time we connect.
        await this._onRemote(info);
      }
    }

    _onLocal(localContent) {
      if (this._connection.status !== ConnectionStatus.CONNECTED) {
        return;
      }
      if (typeof localContent !== 'string') {
        // Get the local content from the editor.
        localContent = this._patchedEditor.getHyperJSON();
      }
      let remoteContent = this._connection.chainpad.getUserDoc();
      if (localContent === remoteContent) {
        // Nothing changed.
        return;
      }

      console.debug('Push local content: ' + localContent);
      this._connection.chainpad.contentUpdate(localContent);
      this._saver.contentModifiedLocally();

      remoteContent = this._connection.chainpad.getUserDoc();
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

        // User data is synchronized through a different channel than the edited content, which means that user data
        // updates can be received before content updates. We can't update the user caret indicators if the content is
        // not yet synchronized because the target elements might be missing. For this reason we trigger a user data
        // update after receiving a remote content update. This, in turn, will trigger a UI update only if the user data
        // actually changed since the last UI update.
        this._onUserDataChange();
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

    async _beforeReconnecting(callback) {
      const oldChannel = this._channel;
      await this._updateChannels();
      if (this._channel === oldChannel) {
        // The Netflux channel used before the WebSocket connection closed is still available so we can still use it.
        callback(this._channel, this._patchedEditor.getHyperJSON());
      } else {
        // The Netflux channels used before the WebSocket connection closed are not available anymore so we have to
        // abort the current realtime session and then rejoin using the new channels.
        await this._onAbort();

        // The editor was previously put in read-only mode when we got disconnected from the WebSocket (i.e. when
        // the WebSocket connection status changed, see above). The editor takes into account nested calls to
        // setReadOnly so we need to make sure the previous setEditable(false) has a corresponding call to
        // setEditable(true). The user won't be able to edit right away because the editor is put back in read-only
        // mode while we reconnect to the realtime session (in _startRealtimeSync).
        this.setEditable(true);
        await this._startRealtimeSync();
      }
    }

    async _onAbort() {
      switch (this._connection.status) {
        case ConnectionStatus.DISCONNECTED:
          // We already left the realtime session.
          return;
        case ConnectionStatus.CONNECTING:
          // The editor has been put in read-only mode before we attempted to (re)connect to the realtime session. We
          // need to restore the editable state before aborting the realtime session, in order to leave the editor in
          // the state it was before we initiated the connection.
          this.setEditable(true);
          break;
        case ConnectionStatus.PAUSED:
        case ConnectionStatus.CONNECTED:
          // Avoid losing uncommitted work when leaving the realtime session.
          await this._flushUncommittedWork();
          break;
      }

      console.debug("Aborting the realtime session!");
      this._setConnectionStatus(ConnectionStatus.DISCONNECTED);

      // Remove all event listeners.
      this._connection.listeners.forEach(listener => listener.removeListener());

      // Stop the realtime content synchronization (leave the WYSIWYG editor Netflux channel associated with the edited
      // document field).
      this._connection.realtimeInput.stop();

      // Notify the others that we're editing offline (outside of the realtime session).
      this._realtimeContext.setRealtimeEnabled(false);

      // Stop the autosave (and leave the saver Netflux channel associated with the edited document).
      await this._saver?.stop();

      // Remove the realtime edit toolbar.
      this._connection.toolbar.destroy();

      // Stop receiving user caret updates (leave the user data Netflux channel associated with the edited document).
      this._connection.userData.stop?.();
      // Remove the user caret indicators (note that the toolbar is already destroyed, so there are no users displayed
      // there anymore).
      this._onUserDataChange({});

      // Make sure the Allow Realtime Collaboration checkbox matches the connection state.
      Interface.getAllowRealtimeCheckbox().prop('checked', false);

      // Don't include the channel in the submitted data if we're not connected to the realtime session.
      this._removeNetfluxChannelFromSubmittedData();

      // Typing tests require the realtime session to be active.
      delete globalThis.easyTest;

      // Cleanup the connection data and prepare for the case the user decides to rejoin the realtime session.
      this._connection = {
        status: ConnectionStatus.DISCONNECTED,
        remoteContentBeforeDisconnect: this._connection.remoteContentBeforeDisconnect
      };
    }

    _pauseRealtimeSync() {
      if (this._connection.status === ConnectionStatus.CONNECTED) {
        this._setConnectionStatus(ConnectionStatus.PAUSED);
        this._connection.pauseDepth = 1;
      } else if (this._connection.status === ConnectionStatus.PAUSED) {
        this._connection.pauseDepth++;
      }
    }

    async _resumeRealtimeSync() {
      if (this._connection.status === ConnectionStatus.PAUSED && --this._connection.pauseDepth === 0) {
        this._setConnectionStatus(ConnectionStatus.CONNECTED);
        const remoteContentAfterPause = this._connection.chainpad.getUserDoc();
        const localContentAfterPause = this._patchedEditor.getHyperJSON();
        if (remoteContentAfterPause === this._connection.remoteContentBeforeDisconnect) {
          // We didn't receive any remote changes while the realtime sync was paused.
          if (localContentAfterPause !== this._connection.remoteContentBeforeDisconnect) {
            // The local content has changed while the realtime sync was paused (e.g. because one of the inserted macros
            // is editable in-place and its rendering added nested editable areas).
            this._onLocal();
          }
        } else if (localContentAfterPause === this._connection.remoteContentBeforeDisconnect) {
          // The local content didn't change while the realtime sync was paused, but we received remote changes. Let's
          // apply them.
          await this._onRemote({
            realtime: this._connection.chainpad
          });
        } else if (localContentAfterPause !== remoteContentAfterPause) {
          // The local content and the remote content have diverged. We need a 3-way merge.
          console.debug('Performing 3-way merge: ', {
            previous: this._connection.remoteContentBeforeDisconnect,
            next: remoteContentAfterPause,
            current: localContentAfterPause
          });
          this._onLocal(this._merge(this._connection.remoteContentBeforeDisconnect, remoteContentAfterPause,
            localContentAfterPause));
          await this._onRemote({
            realtime: this._connection.chainpad
          });
        }
      }
    }

    _merge(previous, next, current) {
      /* jshint camelcase:false */
      const debug = ChainPad.Common.global.REALTIME_DEBUG = ChainPad.Common.global.REALTIME_DEBUG || {};
      const previousParseError = debug.ot_parseError;
      const previousApplyError = debug.ot_applyError;
      delete debug.ot_parseError;
      delete debug.ot_applyError;
      try {
        return Patches.merge(previous, next, current);
      } finally {
        // Merged failed.
        this._connection.lastMergeFailed = debug.ot_parseError || debug.ot_applyError;

        // Restore the previous errors.
        if (!debug.ot_parseError) {
          debug.ot_parseError = previousParseError;
        }
        if (!debug.ot_applyError) {
          debug.ot_applyError = previousApplyError;
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

  globalThis.REALTIME_DEBUG = globalThis.REALTIME_DEBUG || {};
  globalThis.REALTIME_DEBUG.logs = [];
  ['debug', 'error', 'info', 'log', 'trace', 'warn'].forEach(level => {
    const original = console[level];
    console[level] = function (...args) {
      original(...args);
      globalThis.REALTIME_DEBUG.logs.push([level, ...args]);
    };
  });

  return RealtimeEditor;
});
