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
  'chainpad',
  'chainpad-netflux',
  'json.sortify',
  'xwiki-autosave',
  'xwiki-realtime-crypto',
  'xwiki-realtime-document',
  'xwiki-l10n!xwiki-realtime-messages'
], function(
  /* jshint maxparams:false */
  ChainPad, ChainPadNetflux, jsonSortify, AutoSave, Crypto, xwikiDocument, Messages
) {
  'use strict';

  const {SaveTarget, SaveTransport, Saver} = AutoSave;

  function warn(...args) {
    log('warn', ...args);
  }

  function debug(...args) {
    log('debug', ...args);
  }

  function log(level, ...args) {
    console[level]('[Saver] ', ...args);
  }

  /**
   * Load RequireJS modules asynchronously.
   *
   * @param {...String} ids the identifiers of the modules to load
   * @returns {Promise} a promise that resolves with the loaded module, or with the array of loaded modules when
   *   multiple identifiers are given
   */
  function loadById(...ids) {
    return new Promise((resolve, reject) => {
      require(ids, (...modules) => {
        resolve(ids.length === 1 ? modules[0] : modules);
      }, reject);
    });
  }

  // How long to wait for the result of a save request before giving up. This is a safety net for the case where neither
  // the save success nor the save failure event is fired, which would otherwise block the autosave forever. The value
  // is well above any realistic save round-trip because on timeout the content remains dirty and is saved again, so a
  // shorter value would risk saving twice (creating an extra version) when the request is only slow.
  const SUBMIT_TIMEOUT = 120000;

  /**
   * Synchronizes the saver states using ChainPad.
   */
  class ChainPadSaveTransport extends SaveTransport {
    constructor(config, saver) {
      super(saver);

      this._config = config;
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
        [this.getClientId()]: this.state
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
      Object.assign(this.state, patch);
      if (!this._chainpad) {
        // We're not connected (yet, or anymore) so we can only keep the new state locally.
        return;
      }
      this.state.id = this._myId;
      this._states[this.getClientId()] = this.state;
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
        onAbort: () => this.saver.stop()
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
        this.state = this._states[this.getClientId()] || this.state;
        this.saver.onRemoteStatesChanged();
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
   * Saves the content edited with an XWiki edit form, by submitting that form.
   */
  class XWikiFormSaveTarget extends SaveTarget {
    constructor(config, saver) {
      super(saver);

      this._config = config;
      this._revertList = [];
    }

    async initialize() {
      // jQuery is needed only by this save target, so it's loaded on demand rather than being a dependency of the
      // entire module.
      this._$ = await loadById('jquery');

      // There's a very small chance that the preview button might cause problems, so let's just get rid of it.
      const form = document.getElementById(this._config.formId);
      const $previewButton = this._$(form).find('input[name="action_preview"]');
      if ($previewButton.is(':visible')) {
        $previewButton.hide();
        this._revertList.push(() => {
          $previewButton.show();
        });
      }

      this._overwriteAjaxSaveAndContinue(form);

      const beforeSaveHandler = event => {
        if (!this.saver.isSaving()) {
          event.preventDefault();
          event.stopImmediatePropagation();
          // The save failure is already logged by the saver and reported to the user by the save notification.
          this.saver.save({button: event.target}).catch(() => {});
        }
      };
      this._$(form).on('xwiki:actions:beforeSave.realtime-saver', beforeSaveHandler);
      this._revertList.push(() => {
        this._$(form).off('xwiki:actions:beforeSave.realtime-saver', beforeSaveHandler);
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
      const saver = this.saver;
      const prototype = XWiki.actionButtons.AjaxSaveAndContinue.prototype;
      // Keep a reference to the methods we override, in order to call and later restore them.
      const originalAjaxSaveAndContinue = {
        reloadEditor: prototype.reloadEditor,
        maybeRedirect: prototype.maybeRedirect
      };
      const newAjaxSaveAndContinue = {
        // Prevent the save buttons from reloading the page. Instead, reset the editor's content.
        // FIXME: The in-place editor is also overriding reloadEditor, before this code is executed, so here we're
        // actually overwriting in-place editor's behavior.
        reloadEditor: () => {
          xwikiDocument.reload();
          // HACK: Replicate the behavior from the in-place editor.
          setTimeout(() => {
            this._$(form).trigger('xwiki:actions:reload');
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
      Object.assign(prototype, newAjaxSaveAndContinue);
      this._revertList.push(() => {
        // Revert only if the method has not been overridden by another script.
        for(const [methodName, method] of Object.entries(newAjaxSaveAndContinue)) {
          if (prototype[methodName] === method) {
            prototype[methodName] = originalAjaxSaveAndContinue[methodName];
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
      if (this._$('#previewDiffModal').is(':visible')) {
        throw new Error('Merge conflict prevents save.');
      }

      const isAutoSave = !button;
      button = button || this.getSaveButton(true);
      if (!button?.matches(':enabled')) {
        throw new Error('The save button is disabled or missing.');
      }

      const form = document.getElementById(this._config.formId);
      const removeListeners = [];
      const submitResultPromise = this._getSubmitResult(form, removeListeners, SUBMIT_TIMEOUT);

      let savePrevented = true;
      this._$(button).on('xwiki:actions:save.realtime-saver', event => {
        savePrevented = event.isDefaultPrevented();
      });

      const restoreVersionSummary = this._maybeSetAutoSaveVersionSummary(form, isAutoSave);
      button.click();
      this._$(button).off('xwiki:actions:save.realtime-saver');
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
          if (this._$('#previewDiffModal').data('action') === 'cancel') {
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
      this._$(target).one(...args);
      removeListeners.push(() => this._$(target).off(...args));
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
        author: this.saver.getClientId()
      });
      return {version: newVersion};
    }

    dispose() {
      // Remove the event listeners and restore the action buttons behaviour.
      this._revertList.forEach(revert => revert());
    }
  }

  /**
   * An auto-saver that synchronizes the saver states using ChainPad and that saves the content by submitting the XWiki
   * edit form.
   */
  class ChainPadXWikiFormSaver {
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
        // Keep the target, because it is the one that knows which button performs the save we are asked for.
        saver => (this._target = new XWikiFormSaveTarget(config, saver))
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
      return this._saver.save({button: this._target.getSaveButton(continueEditing)});
    }

    stop() {
      return this._saver.stop();
    }
  }

  return ChainPadXWikiFormSaver;
});
