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
define('xwiki-realtime-saver', [
  'jquery',
  'chainpad',
  'chainpad-netflux',
  'json.sortify',
  'xwiki-realtime-crypto',
  'xwiki-realtime-document',
  'xwiki-l10n!xwiki-realtime-messages'
], function(
  /* jshint maxparams:false */
  $, ChainPad, ChainPadNetflux, jsonSortify, Crypto, xwikiDocument, Messages
) {
  'use strict';

  function warn(...args) {
    log('warn', ...args);
  }

  function debug(...args) {
    log('debug', ...args);
  }

  function log(level, ...args) {
    console[level]('[Saver] ', ...args);
  }

  // The interval between two consecutive saves (when the content is modified). Using a slightly different value for
  // each client may help reduce the chances of conflicts.
  const SAVE_INTERVAL = 60000 + Math.random() * 6000;

  // How long to wait after broadcasting the intention to save, before actually saving the content. This helps reduce
  // the chances of concurrent saves (which often lead to merge conflicts).
  const SAVE_DELAY = 1000;

  // How long to wait for the result of a save request before giving up. This is a safety net for the case where neither
  // the save success nor the save failure event is fired, which would otherwise block the autosave forever. The value
  // is well above any realistic save round-trip because on timeout the content remains dirty and is saved again, so a
  // shorter value would risk saving twice (creating an extra version) when the request is only slow.
  const SUBMIT_TIMEOUT = 120000;

  /**
   * Replicates the saver state of each client taking part in the realtime editing session.
   */
  class SaveTransport {
    /**
     * @param {Object} config the saver configuration
     * @param {Saver} saver the saver using this transport, notified when the remote states change
     */
    constructor(config, saver) {
      this._config = config;
      this._saver = saver;

      // The state of the local client, that gets propagated to the other clients.
      this._state = {
        // The number of local changes since this saver was created. This is used to determine if there are unsaved
        // local changes.
        updateCount: 0,

        // The number of changes for each client that were last saved by this client.
        savedUpdateCount: {},

        // Whether there are unsaved local changes. This is determined by comparing the local update count with the
        // saved update count of all clients.
        dirty: false,

        // Whether this client is currently saving the content. A value greater than 0 means the client is currently
        // attempting to save with that priority. Depending on the save target, manual save may have for instance
        // higher priority than autosave.
        saving: 0
      };
    }

    /**
     * Connect to the realtime channel and start receiving the states of the other clients. Called after both the
     * transport and the target have been created, so that the saver is ready to be notified.
     */
    initialize() {
      // Must be implemented by subclasses.
    }

    /**
     * @returns {Promise} a promise that resolves when the transport is connected and the states of the other clients
     *   are available
     */
    async toBeReady() {
      // Must be implemented by subclasses.
    }

    /**
     * @returns {String} the identifier of the local client, used as key in the map returned by {@link #getStates()}
     */
    getClientId() {
      // Must be implemented by subclasses.
      return '';
    }

    /**
     * @returns {Object} the saver state of each client taking part in the editing session, keyed by client identifier
     */
    getStates() {
      // Must be implemented by subclasses.
      return {};
    }

    /**
     * This is normally the entry that {@link #getStates()} holds for {@link #getClientId()}, but not always: a
     * transport can receive a remote snapshot that doesn't include the local client yet, in which case it has to keep
     * the local state aside until the next push re-inserts it in the map. Reading the local state through this method,
     * rather than from the map, is what makes the saver immune to that window.
     *
     * @returns {Object} the saver state of the local client
     */
    getLocalState() {
      return this._state;
    }

    /**
     * Apply the given changes to the local saver state and optionally propagate them to the other clients.
     *
     * @param {Object} patch the state properties to modify
     * @param {Object} [options] the options of this update
     * @param {boolean} [options.push] whether to propagate the new state to the other clients
     * @param {boolean} [options.immediate] whether to wait for the new state to reach the other clients
     * @returns {Promise} a promise that resolves when the update has been applied (and propagated, when asked to)
     */
    async updateLocalState(patch, {push, immediate} = {}) {
      // Must be implemented by subclasses.
    }

    /**
     * @param {Object} state the saver state of a client
     * @returns {boolean} whether the client owning the given state is still taking part in the editing session
     */
    isConnected(state) {
      return true;
    }

    /**
     * @returns {Promise} a promise that resolves when the local state has been received by the other clients
     */
    whenSettled() {
      return Promise.resolve();
    }

    /**
     * Disconnect from the realtime channel and revert the changes made to the environment.
     */
    async dispose() {
      // Must be implemented by subclasses.
    }
  }

  /**
   * Synchronizes the saver states using ChainPad.
   */
  class ChainPadSaveTransport extends SaveTransport {
    constructor(config, saver) {
      super(config, saver);

      this._revertList = [];

      this._initializing = new Promise(resolve => {
        this._notifyReady = () => {
          // Mark the transport as ready right away (rather than using a promise callback which would be called on the
          // next tick), to be visible to the code executed right after _notifyReady is called.
          this._initializing = false;
          resolve();
        };
      });

      // The cached states of all the clients.
      this._states = {
        [this.getClientId()]: this._state
      };
    }

    initialize() {
      this._realtimeInput = ChainPadNetflux.start(this._getRealtimeConfig());
      this._revertList.push(() => {
        this._realtimeInput?.stop();
        delete this._realtimeInput;
      });

      const visibilityChangeListener = () => {
        if (document.visibilityState === 'hidden') {
          // Push uncommitted changes to the server because when a document is hidden its window can be closed without
          // notice, so this might be the last chance to propagate our local state to the other collaborators.
          this.updateLocalState({}, {push: true, immediate: true});
        }
      };
      document.addEventListener('visibilitychange', visibilityChangeListener);
      this._revertList.push(() => {
        document.removeEventListener('visibilitychange', visibilityChangeListener);
      });
    }

    async toBeReady() {
      // Resolved right away once we're connected, because _initializing is set to false by then.
      await this._initializing;
    }

    getClientId() {
      return this._config.userName;
    }

    getStates() {
      return this._states;
    }

    async updateLocalState(patch, {push, immediate} = {}) {
      Object.assign(this._state, patch);
      if (!this._chainpad) {
        // We're not connected (yet, or anymore) so we can only keep the new state locally.
        return;
      }
      this._state.id = this._myId;
      this._states[this.getClientId()] = this._state;
      if (push) {
        this._onLocal();
      }
      if (immediate) {
        this._chainpad.sync();
        await this.whenSettled();
      }
    }

    isConnected(state) {
      return this._userList.users.includes(state.id);
    }

    whenSettled() {
      return new Promise(resolve => {
        if (this._chainpad) {
          this._chainpad.onSettle(resolve);
        } else {
          resolve();
        }
      });
    }

    _getRealtimeConfig() {
      return {
        initialState: '{}',
        network: this._config.network,
        userName: this._config.userName || '',
        channel: this._config.channel,
        crypto: Crypto,
        // Operational Transformation
        patchTransformer: ChainPad.SmartJSONTransformer,

        onRemote: this._onRemote.bind(this),
        onReady: this._onReady.bind(this),
        onLocal: this._onLocal.bind(this),
        onAbort: () => this._saver.stop()
      };
    }

    _onReady(info) {
      this._myId = info.myId;
      this._chainpad = info.realtime;
      this._userList = info.userList;
      this._notifyReady();
      this._onLocal();
    }

    _onRemote() {
      if (this._initializing) {
        return;
      }

      const remoteStates = this._chainpad.getUserDoc();
      debug('Received remote states: ', remoteStates);

      try {
        this._states = JSON.parse(remoteStates);
        // The remote document may not include our own state yet, in which case we keep the state we have until the
        // next push re-inserts it in the map.
        this._state = this._states[this.getClientId()] || this._state;
        this._saver.onRemoteStatesChanged();
      } catch (e) {
        warn("Unable to parse remote states.", e);
      }
    }

    _onLocal() {
      if (this._initializing) {
        return;
      }
      const localStates = jsonSortify(this._states);
      debug('Push local states: ', localStates);
      this._chainpad.contentUpdate(localStates);
      const remoteStates = this._chainpad.getUserDoc();
      if (remoteStates !== localStates) {
        warn("Unexpected remote states after synchronization: ", {
          expected: localStates,
          actual: remoteStates
        });
      }
    }

    async dispose() {
      delete this._chainpad;

      // Disconnect from the realtime channel and remove the event listeners.
      this._revertList.forEach(revert => revert());
    }
  }

  /**
   * Saves the edited content, and reports what the other clients need to know about the result.
   */
  class SaveTarget {
    /**
     * @param {Object} config the saver configuration
     * @param {Saver} saver the saver using this target, asked to perform the manual saves
     */
    constructor(config, saver) {
      this._config = config;
      this._saver = saver;
    }

    /**
     * Called when the transport is ready, to install the listeners used to detect and intercept the save requests
     * (e.g. when the user clicks on the save button). This has to wait for the transport because a save accepted
     * before the states of the other clients are known would elect no client and thus save nothing.
     */
    initialize() {
      // Must be implemented by subclasses.
    }

    /**
     * @param {Object} context the save context, holding the save button in case of a manual save
     * @returns {Number} the priority of this save; the client with the highest priority wins the save election
     */
    getSavePriority(context) {
      // By default all clients have the same priority when saving. Subclasses may override this method to give higher
      // priority to manual saves, for instance (i.e. when the user clicks on the save button).
      return 1;
    }

    /**
     * Save the edited content.
     *
     * @param {Object} context the save context, holding the save button in case of a manual save
     * @returns {Promise<Object>} a promise that resolves with the save result, holding the created version, if any
     */
    async submit(context) {
      // Must be implemented by subclasses.
      return {};
    }

    /**
     * Called whenever the saver states change, so that the target can react (e.g. take into account the version
     * created by another client, in order to prevent a merge conflict on the next save).
     *
     * @param {Object} states the saver state of each client, keyed by client identifier
     * @param {String} localClientId the identifier of the local client
     */
    onStatesChanged(states, localClientId) {
      // Must be implemented by subclasses.
    }

    /**
     * Revert the changes made to the environment.
     */
    dispose() {
      // Must be implemented by subclasses.
    }
  }

  /**
   * Saves the content edited with an XWiki edit form, by submitting that form.
   */
  class XWikiFormSaveTarget extends SaveTarget {
    constructor(config, saver) {
      super(config, saver);

      this._revertList = [];
    }

    initialize() {
      // There's a very small chance that the preview button might cause problems, so let's just get rid of it.
      const form = document.getElementById(this._config.formId);
      const $previewButton = $(form).find('input[name="action_preview"]');
      if ($previewButton.is(':visible')) {
        $previewButton.hide();
        this._revertList.push(() => {
          $previewButton.show();
        });
      }

      this._overwriteAjaxSaveAndContinue(form);

      const beforeSaveHandler = event => {
        if (!this._saver.isSaving()) {
          event.preventDefault();
          event.stopImmediatePropagation();
          // The save failure is already logged by the saver and reported to the user by the save notification.
          this._saver.save({button: event.target}).catch(() => {});
        }
      };
      $(form).on('xwiki:actions:beforeSave.realtime-saver', beforeSaveHandler);
      this._revertList.push(() => {
        $(form).off('xwiki:actions:beforeSave.realtime-saver', beforeSaveHandler);
      });

      this._notifyInitialVersion();
    }

    /**
     * Retrieve information about the initial version, when joining the editing session, without blocking the saver
     * ready state.
     */
    _notifyInitialVersion() {
      if (xwikiDocument.isNew) {
        return;
      }
      xwikiDocument.getRevision(xwikiDocument.version).then(revision => {
        this._config.onCreateVersion({
          number: revision.version,
          date: new Date(revision.modified).getTime(),
          author: {
            reference: this._getAbsoluteUserReference(revision.author),
            name: revision.authorName
          }
        });
      }).catch(error => {
        console.debug('Failed to retrieve information about the initial version.', error);
      });
    }

    _overwriteAjaxSaveAndContinue(form) {
      const saver = this._saver;
      const originalAjaxSaveAndContinue = $.extend({}, XWiki.actionButtons.AjaxSaveAndContinue.prototype);
      const newAjaxSaveAndContinue = {
        // Prevent the save buttons from reloading the page. Instead, reset the editor's content.
        // FIXME: The in-place editor is also overriding reloadEditor, before this code is executed, so here we're
        // actually overwriting in-place editor's behavior.
        reloadEditor: () => {
          xwikiDocument.reload();
          // HACK: Replicate the behavior from the in-place editor.
          setTimeout(() => {
            $(form).trigger('xwiki:actions:reload');
          }, 0);
        },
        // Redirect only after we have confirmation that the saver state has been propagated to all clients.
        maybeRedirect: function(continueEditing) {
          if (continueEditing) {
            return originalAjaxSaveAndContinue.maybeRedirect.apply(this, arguments);
          } else {
            saver.whenSettled().then(() => {
              originalAjaxSaveAndContinue.maybeRedirect.apply(this, arguments);
            });
            return true;
          }
        }
      };
      $.extend(XWiki.actionButtons.AjaxSaveAndContinue.prototype, newAjaxSaveAndContinue);
      this._revertList.push(() => {
        // Revert only if the method has not been overridden by another script.
        for(const [methodName, method] of Object.entries(newAjaxSaveAndContinue)) {
          if (XWiki.actionButtons.AjaxSaveAndContinue.prototype[methodName] === method) {
            XWiki.actionButtons.AjaxSaveAndContinue.prototype[methodName] = originalAjaxSaveAndContinue[methodName];
          }
        }
      });
    }

    onStatesChanged(states, localClientId) {
      let latestVersion = '0.0';
      let savedBy;
      for (const [clientId, state] of Object.entries(states)) {
        if (this._compareVersions(state.version || '0.0', latestVersion) > 0) {
          latestVersion = state.version;
          savedBy = clientId;
        }
      }
      if (this._compareVersions(latestVersion, xwikiDocument.version) > 0) {
        xwikiDocument.update({
          version: latestVersion,
          modified: Date.now(),
          isNew: false
        });
        if (savedBy !== localClientId) {
          this._config.onCreateVersion({
            number: latestVersion,
            date: xwikiDocument.modified,
            author: savedBy
          });
        }
      }
    }

    _getAbsoluteUserReference(userReference) {
      const usersSpaceReference = XWiki.Model.resolve('XWiki', XWiki.EntityType.SPACE, xwikiDocument.documentReference);
      return XWiki.Model.serialize(XWiki.Model.resolve(userReference, XWiki.EntityType.DOCUMENT, usersSpaceReference));
    }

    _compareVersions(a, b) {
      const [aMajor, aMinor] = (a + '').split('.').map(Number);
      const [bMajor, bMinor] = (b + '').split('.').map(Number);
      return aMajor - bMajor || aMinor - bMinor;
    }

    getSavePriority({button}) {
      // Give higher priority to manual saves (when the user clicks on the save button). Also give higher priority to
      // Save & View over Save & Continue. The former leaves the edit mode so we want to make sure we don't lose unsaved
      // changes, while the latter keeps the user in the edit mode where we have autosave.
      if (button) {
        // Manual save
        return button.getAttribute('name') === 'action_save' ? 3 : 2;
      } else {
        // Autosave
        return super.getSavePriority({});
      }
    }

    async submit({button}) {
      // The merge conflict modal is already displayed (from a previous save attempt). Clicking the save button again
      // would reopen the same modal and reset the fields the user did not submit yet. We don't want that.
      if ($('#previewDiffModal').is(':visible')) {
        throw new Error('Merge conflict prevents save.');
      }

      const isAutoSave = !button;
      button = button || this.getSaveButton(true);
      if (!$(button).is(':enabled')) {
        throw new Error('The save button is disabled or missing.');
      }

      const form = document.getElementById(this._config.formId);
      const removeListeners = [];
      const submitResultPromise = this._getSubmitResult(form, removeListeners, SUBMIT_TIMEOUT);

      let savePrevented = true;
      $(button).on('xwiki:actions:save.realtime-saver', event => {
        savePrevented = event.isDefaultPrevented();
      });

      const restoreVersionSummary = this._maybeSetAutoSaveVersionSummary(form, isAutoSave);
      $(button).click();
      $(button).off('xwiki:actions:save.realtime-saver');
      restoreVersionSummary?.();

      if (savePrevented) {
        // The save is prevented if the form has invalid data (e.g. missing mandatory title). In this case the
        // xwiki:document:saved and xwiki:document:saveFailed events are not triggered, so we need to remove the
        // corresponding event listeners and reject the save.
        removeListeners.forEach(removeListener => removeListener());
        throw new Error('Save prevented. Verify that the form has valid data.');
      }

      return this._afterSave(await submitResultPromise);
    }

    _maybeSetAutoSaveVersionSummary(form, isAutoSave) {
      const commentInput = form?.querySelector('input[name="comment"]');
      if (commentInput && isAutoSave) {
        // Backup the version summary before setting the auto-save value.
        const versionSummary = commentInput.value;
        commentInput.value = Messages.autoSaveSummary;
        return () => {
          // Restore the version summary after the auto-save was triggered.
          commentInput.value = versionSummary;
        };
      }
    }

    getSaveButton(continueEditing) {
      const form = document.getElementById(this._config.formId);
      return form.querySelector('input[name="action_save' + (continueEditing ? 'andcontinue' : '') + '"]');
    }

    /**
     * @param {Element} form the edit form that is being submitted
     * @param {Array<Function>} removeListeners the list of functions to call in order to stop waiting for the result
     * @param {Number} [timeout] how long to wait for the save result before rejecting; when not specified we wait
     *   indefinitely (e.g. while the user is dealing with the merge conflict modal)
     * @returns {Promise} a promise that resolves with the save result or rejects if the save fails
     */
    _getSubmitResult(form, removeListeners, timeout) {
      return new Promise((resolve, reject) => {
        if (timeout) {
          const timer = setTimeout(() => {
            // Stop waiting for the save result, including for the events that are part of the same group.
            removeListeners.forEach(removeListener => removeListener());
            reject(new Error('Timeout while waiting for the save result.'));
          }, timeout);
          // Disarm the timer as soon as we receive the save result.
          removeListeners.push(() => clearTimeout(timer));
        }
        this._once(form, removeListeners, 'xwiki:document:saved.realtime-saver', (event, data) => {
          resolve(data);
        });
        this._once(form, removeListeners, 'xwiki:document:saveFailed.realtime-saver', (event, data) => {
          if (data.response.status === 409) {
            debug('Save blocked by merge conflict');
            // Keep the saving flag while the user deals with the merge conflict modal (i.e. we don't want the merge
            // conflict to be handled by multiple users because this leads to more merge conflicts).
            this._waitForMergeConflictResolution(form).then(resolve, reject);
          } else {
            reject(new Error('Failed to save.'));
          }
        });
      });
    }

    async _waitForMergeConflictResolution(form) {
      // There are multiple events that signal the merge conflict resolution. We want to wait for which one comes first
      // and then remove the other listeners. For this, we collect all the remove listener functions.
      const removeListeners = [];
      return new Promise((resolve, reject) => {
        // Wait for the document to be saved (after the merge conflict is resolved) or for the save to fail (which is
        // triggered also when the merge conflict modal fails to be fetched from the server).
        this._getSubmitResult(form, removeListeners).then(resolve, reject);
        // ... or for the editor to be reloaded, if the user decides to discard the local changes.
        this._once(form, removeListeners, 'xwiki:actions:reload', () => {
          reject(new Error('Discarding local changes by reloading the editor.'));
        });
        // ... or for the merge conflict modal to be closed without resolving the conflict.
        this._once(document, removeListeners, 'hide.bs.modal.realtime-saver', '#previewDiffModal', () => {
          if ($('#previewDiffModal').data('action') === 'cancel') {
            reject(new Error('Save canceled.'));
          } else {
            // The modal was closed but not canceled so we still need to wait for a save (successful or not) or reload
            // event. Keep the other event listeners in the group.
            return true;
          }
        });
      });
    }

    /**
     * Do something when any of the events from a group is triggered for the first time (once).
     *
     * @param {Element} target the target element on which the event listener is registered
     * @param {Array<Function>} removeListeners the list of event listeners to remove after an event from the group is
     *   triggered
     * @param {...any} args the arguments passed when registering the event listener
     */
    _once(target, removeListeners, ...args) {
      // Wrap the original handler so that we can remove all the event listeners in the group after one of them is
      // triggered.
      const originalHandler = args.at(-1);
      args[args.length - 1] = (...params) => {
        const result = originalHandler(...params);
        if (result !== true) {
          // Cleanup.
          removeListeners.forEach(removeListener => removeListener());
        }
        return result;
      };
      $(target).one(...args);
      removeListeners.push(() => $(target).off(...args));
    }

    _afterSave({newVersion}) {
      if (newVersion === xwikiDocument.version) {
        // The version didn't change because the document hasn't been modified.
        return {};
      } else if (newVersion === '1.1') {
        debug('Created document version 1.1');
      } else {
        debug(`Version bumped from ${xwikiDocument.version} to ${newVersion}.`);
      }
      this._config.onCreateVersion({
        number: newVersion,
        date: Date.now(),
        author: this._saver.getClientId()
      });
      return {version: newVersion};
    }

    dispose() {
      // Remove the event listeners and restore the action buttons behaviour.
      this._revertList.forEach(revert => revert());
    }
  }

  /**
   * Generic auto-saver that keeps track of the local update count and schedules saves when the content is modified.
   * The way the saver states are synchronized between the clients is delegated to a {@link SaveTransport} and the way
   * the content is saved is delegated to a {@link SaveTarget}.
   */
  class Saver {
    /**
     * @param {Object} config the saver configuration
     * @param {Function} createTransport creates the transport used to synchronize the saver states, called with this
     *   saver
     * @param {Function} createTarget creates the target used to save the edited content, called with this saver
     */
    constructor(config, createTransport, createTarget) {
      this._config = config;

      // The highest number of local changes that we know have been saved, by us or by another client. We remember the
      // highest value ever seen rather than looking it up in the saver states each time, because the state of the
      // client that performed the save can disappear (e.g. when that client leaves the editing session) and we would
      // then wrongly consider our changes unsaved.
      this._savedUpdateCount = 0;

      // Whether this saver was stopped, in which case no new save must be scheduled.
      this._stopped = false;

      this._transport = createTransport(this);
      this._target = createTarget(this);

      // Connect only after both the transport and the target have been created, because the transport starts notifying
      // us as soon as it is initialized.
      this._transport.initialize();
    }

    /**
     * @returns {Promise} a promise that resolves when the saver is connected and ready to save
     */
    async toBeReady() {
      await this._transport.toBeReady();
      this._target.initialize();
      this._notifyStatusChange();
    }

    /**
     * Called each time the edited content is modified locally.
     */
    contentModifiedLocally() {
      const updateCount = this._transport.getLocalState().updateCount + 1;
      this._updateState({updateCount}, true);
      this._scheduleSave();
    }

    isDirty() {
      return !!this._transport.getLocalState().dirty;
    }

    isSaving() {
      return !!this._transport.getLocalState().saving;
    }

    getClientId() {
      return this._transport.getClientId();
    }

    getSaveButton(continueEditing) {
      return this._target.getSaveButton(continueEditing);
    }

    /**
     * @returns {Promise} a promise that resolves when the local state has been received by the other clients
     */
    whenSettled() {
      return this._transport.whenSettled();
    }

    /**
     * Called by the transport when the saver states of the other clients have changed.
     */
    onRemoteStatesChanged() {
      this._updateState();
    }

    _scheduleSave() {
      // Cancel the previous scheduled save.
      clearTimeout(this._saveTimer);
      if (this._stopped) {
        // Don't schedule a new save after the saver was stopped (e.g. when the user leaves the edit mode).
        return;
      }
      if (!this._dirtyTimestamp || Date.now() - this._dirtyTimestamp < SAVE_INTERVAL) {
        this._saveTimer = setTimeout(this._maybeSave.bind(this), SAVE_INTERVAL);
      } else {
        // Save right away because too much time has passed since the last time the content became dirty.
        this._maybeSave();
      }
    }

    /**
     * Recompute the local state, optionally propagating it to the other clients.
     *
     * @param {Object} [patch] the local state properties to modify
     * @param {boolean} [push] whether to propagate the new state to the other clients
     * @param {boolean} [immediate] whether to wait for the new state to reach the other clients
     */
    _updateState(patch, push, immediate) {
      const localState = {...this._transport.getLocalState(), ...patch};
      const wasDirty = !!this._transport.getLocalState().dirty;
      const dirty = this._isDirty(localState);
      if (wasDirty !== dirty) {
        // Dirty state changed.
        if (wasDirty) {
          // Notify immediately that the content is clean, otherwise, if the user saving the content is not the one that
          // made the changes, the save status will remain dirty after the save success notification.
          push = immediate = true;
        } else {
          // Remember the last time when the content became dirty in order to be able to save immediately when the save
          // interval is reached (even if the user is still making changes).
          this._dirtyTimestamp = Date.now();
        }
      } else if (this._isSomeoneSaving()) {
        // Avoid auto-saving more often than the SAVE_INTERVAL. It's possible that the SAVE_INTERVAL is reached for
        // multiple users that are editing at the same time. In this case the auto-save should be triggered for only one
        // of them. For the others the auto-save should be delayed until the SAVE_INTERVAL is reached again.
        delete this._dirtyTimestamp;
      }
      // We don't wait for the new state to reach the other clients because the callers don't depend on it.
      this._transport.updateLocalState({...patch, dirty}, {push, immediate});

      this._notifyStatusChange();
      this._target.onStatesChanged(this._transport.getStates(), this._transport.getClientId());
    }

    /**
     * @param {Object} localState the new local state
     * @returns {boolean} whether the local content has changes that no client has saved yet
     */
    _isDirty(localState) {
      const clientId = this._transport.getClientId();
      for (const state of Object.values(this._transport.getStates())) {
        this._savedUpdateCount = Math.max(this._savedUpdateCount, state.savedUpdateCount?.[clientId] || 0);
      }
      return (localState.updateCount || 0) > this._savedUpdateCount;
    }

    _notifyStatusChange() {
      const localState = this._transport.getLocalState();
      const localStatus = (localState.saving && 1) || (localState.dirty ? 0 : 2);
      if (this._previousLocalStatus !== localStatus) {
        this._previousLocalStatus = localStatus;
        this._config.onLocalStatusChange(localStatus);
      }

      const globalStatus = (this._isSomeoneSaving() && 1) || (this._isSomeoneDirty() ? 0 : 2);
      if (this._previousGlobalStatus !== globalStatus) {
        this._previousGlobalStatus = globalStatus;
        this._config.onStatusChange(globalStatus);
      }
    }

    _maybeSave() {
      if (!this._isSomeoneSaving() && this._isSomeoneDirty()) {
        // The autosave failure is already logged by the saver and a new save attempt is scheduled.
        this.save().catch(() => {});
      }
    }

    _isSomeoneSaving() {
      return this._someState(state => state.saving && this._transport.isConnected(state));
    }

    _isSomeoneDirty() {
      return this._someState(state => state.dirty && this._transport.isConnected(state));
    }

    _someState(predicate) {
      return Object.values(this._transport.getStates()).some(state => predicate(state));
    }

    _getConnectedStates() {
      return Object.fromEntries(Object.entries(this._transport.getStates())
        .filter(([clientId, state]) => this._transport.isConnected(state)));
    }

    /**
     * Save the edited content, provided that this client wins the save election.
     *
     * @param {Object} [context] the save context, holding the save button in case of a manual save
     * @returns {Promise} a promise that rejects if the content could not be saved
     */
    async save(context) {
      context = context || {};

      // Let the others know immediately that we are saving, in order to reduce concurrent saves.
      this._updateState({saving: this._target.getSavePriority(context)}, true, true);

      try {
        const savingClientId = await this._getSavingClientId();
        if (savingClientId === this._transport.getClientId()) {
          const savedUpdateCount = this._getUpdateCounts();
          debug("Saving ", savedUpdateCount);

          const {version} = await this._target.submit(context) || {};
          // Record the save result locally: the finally block below propagates it to the other clients.
          await this._transport.updateLocalState(version ? {savedUpdateCount, version} : {savedUpdateCount});
        }
      } catch (error) {
        warn("Failed to save.", error);
        // Let the caller know that the content has not been saved.
        throw error;
      } finally {
        // Propagate the state immediately after the save attempt because the user may leave the edit mode and this will
        // close the WebSocket connection.
        this._updateState({saving: 0}, true, true);

        if (this.isDirty()) {
          // The content is still dirty, either because the save failed or because another client was elected to save
          // and didn't manage to save yet. Schedule a new save attempt.
          this._scheduleSave();
        }
      }
    }

    /**
     * The autosave can be triggered on multiple clients at the same time (i.e. multiple clients can set their own
     * saving flag before they received the saving flag from the other clients). This method is used to determine which
     * client should save the content in this case. By default the client with the highest save priority and the lowest
     * id (in alphabetical order) wins.
     *
     * @returns the id of the client that should save the content
     */
    _getSavingClientId() {
      return new Promise(resolve => {
        setTimeout(() => {
          // Initialize with minimum save priority.
          let savePriority = 1, savingClientId;
          for (const [clientId, state] of Object.entries(this._getConnectedStates())) {
            if (state.saving > savePriority || (state.saving === savePriority &&
                (!savingClientId || savingClientId > clientId))) {
              savePriority = state.saving;
              savingClientId = clientId;
            }
          }
          resolve(savingClientId);
        }, SAVE_DELAY);
      });
    }

    _getUpdateCounts() {
      const updateCounts = {};
      for (const [clientId, state] of Object.entries(this._transport.getStates())) {
        updateCounts[clientId] = state.updateCount || 0;
      }
      return updateCounts;
    }

    /**
     * Stop the autosave when the user disallows realtime or when the WebSocket is disconnected.
     */
    async stop() {
      this._stopped = true;
      // Cancel the scheduled save.
      clearTimeout(this._saveTimer);

      // Push uncommitted changes to the server before disconnecting.
      await this._transport.updateLocalState({}, {push: true, immediate: true});

      await this._transport.dispose();
      this._target.dispose();
    }
  }

  /**
   * An auto-saver that synchronizes the saver states using ChainPad and that saves the content by submitting the XWiki
   * edit form.
   */
  class XWikiSaver {
    constructor(config) {
      config = {
        formId: 'edit',
        onLocalStatusChange: () => {},
        onStatusChange: () => {},
        onCreateVersion: () => {},
        ...config
      };
      this._saver = new Saver(config,
        saver => new ChainPadSaveTransport(config, saver),
        saver => new XWikiFormSaveTarget(config, saver)
      );
    }

    async toBeReady() {
      await this._saver.toBeReady();
      return this;
    }

    contentModifiedLocally() {
      this._saver.contentModifiedLocally();
    }

    isDirty() {
      return this._saver.isDirty();
    }

    save(continueEditing) {
      return this._saver.save({button: this._saver.getSaveButton(continueEditing)});
    }

    stop() {
      return this._saver.stop();
    }
  }

  return XWikiSaver;
});
