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
  'xwiki-realtime-document'
], function(
  /* jshint maxparams:false */
  $, ChainPad, ChainPadNetflux, jsonSortify, Crypto, xwikiDocument
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

  function now() {
    return new Date().getTime();
  }

  /**
   * Generic auto-saver that keeps track of local update count and schedules saves when the content is modified.
   */
  class GenericSaver {
    constructor() {
      // The state of this saver instance, that gets pushed to the other clients.
      this._state = {
        // The number of local changes since this saver was created. This is used to determine if there are unsaved
        // local changes.
        updateCount: 0,

        // The number of changes for each client that were last saved by this saver.
        savedUpdateCount: {},

        // Whether there are unsaved local changes. This is deterimned by comparing the local update count with the
        // saved updated count of all clients.
        dirty: false,

        // Whether this saver is currently saving the content. A value greater than 0 means the saver is currently
        // attempting to save with that priority. Depending on the saver implementation, manual save may have for
        // instance higher priority than autosave.
        saving: 0,
      };
    }

    /**
     * Called each time the edited content is modified locally.
     */
    contentModifiedLocally() {
      this._state.updateCount++;
      this._updateState(true);
      this._scheduleSave();
    }

    isDirty() {
      return this._state.dirty;
    }

    _scheduleSave() {
      // Cancel the previous scheduled save.
      clearTimeout(this._saveTimer);
      if (!this._dirtyTimestamp || now() - this._dirtyTimestamp < SAVE_INTERVAL) {
        this._saveTimer = setTimeout(this._maybeSave.bind(this), SAVE_INTERVAL);
      } else {
        // Save right away because too much time has passed since the last time the content became dirty.
        this._maybeSave();
      }
    }

    _updateState(push, immediate) {
      const wasDirty = this._state.dirty;
      this._state.dirty =
        // The content can't be dirty if there are no local changes.
        this._state.updateCount > 0 &&
        // Check if there is a client that has saved all our local changes.
        !this._someState(state => state.savedUpdateCount[this._getClientId()] >= this._state.updateCount);
      if (wasDirty !== this._state.dirty) {
        // Dirty state changed.
        if (wasDirty) {
          // Notify immediately that the content is clean, otherwise, if the user saving the content is not the one that
          // made the changes, the save status will remain dirty after the save success notification.
          push = immediate = true;
        } else {
          // Remember the last time when the content became dirty in order to be able to save immediately when the save
          // interval is reached (even if the user is still making changes).
          this._dirtyTimestamp = now();
        }
      }
      if (push) {
        // Push the state of this saver to the other clients.
        this._pushState(immediate);
      }
    }

    _getClientId() {
      // Must be implemented by subclasses.
      return '';
    }

    _pushState(immediate) {
      // Must be implemented by subclasses.
    }

    _maybeSave() {
      if (!this._isSomeoneSaving() && this._isSomeoneDirty()) {
        this._save();
      }
    }

    _isSomeoneSaving() {
      return this._someState(state => state.saving && this._isConnected(state));
    }

    _isSomeoneDirty() {
      return this._someState(state => state.dirty && this._isConnected(state));
    }

    _someState(predicate) {
      return Object.values(this._getStates()).some(predicate);
    }

    _getStates() {
      // Must be implemented by subclasses.
      return {};
    }

    _getConnectedStates() {
      return Object.fromEntries(Object.entries(this._getStates())
        .filter(([clientId, state]) => this._isConnected(state)));
    }

    _isConnected(state) {
      // Must be overridden by subclasses to indicate if the user associated with the given state is connected to the
      // realtime editing session.
      return true;
    }

    async _save(options) {
      options = options || {};

      this._state.saving = this._getSavePriority(options);
      // Let the others know immediately that we are saving, in order to reduce concurrent saves.
      this._updateState(true, true);

      const savingClientId = await this._getSavingClientId();
      if (savingClientId === this._getClientId()) {
        const savedUpdateCount = this._getUpdateCounts();
        debug("Saving ", savedUpdateCount);

        try {
          await this._submit(options);
          this._state.savedUpdateCount = savedUpdateCount;
        } catch (error) {
          warn("Failed to save.", error);
        }
      }

      this._state.saving = 0;
      // Propagate the state immediately after a successful save because the user may leave the edit mode and this will
      // close the WebSocket connection.
      this._updateState(true, true);
    }

    _getSavePriority() {
      // By default all clients have the same priority when saving. Subclasses may override this method to give higher
      // priority to manual saves, for instance (i.e. when the user clicks on the same button).
      return 1;
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
      for (const [clientId, state] of Object.entries(this._getStates())) {
        updateCounts[clientId] = state.updateCount || 0;
      }
      return updateCounts;
    }

    async _submit(options) {
      // Must be implemented by subclasses.
    }
  }

  /**
   * Autosaver that synchronizes the states of the clients using ChainPad.
   */
  class ChainPadSaver extends GenericSaver {
    constructor(config) {
      super();

      this._revertList = [];

      this._initializing = new Promise(resolve => {
        this._notifyReady = () => {
          // Mark the Saver as ready right away (rather than using a promise callback which would be called on the next
          // tick), to be visible to the code executed right after _notifyReady is called.
          this._initializing = false;
          resolve();
        };
      });

      this._config = {...config};
      // The cached states of all the clients.
      this._states = {
        [this._getClientId()]: this._state
      };

      this._realtimeInput = ChainPadNetflux.start(this._getRealtimeConfig());
      this._revertList.push(() => {
        this._realtimeInput?.stop();
        delete this._realtimeInput;
      });
    }

    _getClientId() {
      return this._config.userName;
    }

    _getStates() {
      return this._states;
    }

    _pushState(immediate) {
      this._state.id = this._myId;
      this._getStates()[this._getClientId()] = this._state;
      this._onLocal();
      if (immediate) {
        this._chainpad.sync();
      }
    }

    _isConnected(state) {
      return this._userList.users.includes(state.id);
    }

    async toBeReady() {
      if (this._initializing) {
        await this._initializing;
      }
      return this;
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
        onAbort: this.stop.bind(this)
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
        this._state = this._getStates()[this._getClientId()] || this._state;
        this._updateState();
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

    /**
     * Stop the autosave when the user disallows realtime or when the WebSocket is disconnected.
     */
    stop() {
      // Cancel the scheduled save.
      clearTimeout(this._saveTimer);

      // Disconnect from the realtime channel and revert the changes made by this saver (i.e. remove event listeners,
      // restore action buttons behaviour).
      this._revertList.forEach(revert => revert());
    }
  }

  /**
   * A ChainPadSaver implementation specific to XWiki.
   */
  class XWikiSaver extends ChainPadSaver {
    constructor(config) {
      super({
        formId: 'edit',
        onStatusChange: () => {},
        onCreateVersion: () => {},
        ...config
      });
    }

    _onReady(info) {
      super._onReady(info);

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
        if (!this._state.saving) {
          event.preventDefault();
          event.stopImmediatePropagation();
          this._save({button: event.target});
        }
      };
      $(form).on('xwiki:actions:beforeSave.realtime-saver', beforeSaveHandler);
      this._revertList.push(() => {
        $(form).off('xwiki:actions:beforeSave.realtime-saver', beforeSaveHandler);
      });
    }

    _overwriteAjaxSaveAndContinue(form) {
      const self = this;
      const originalAjaxSaveAndContinue = $.extend({}, XWiki.actionButtons.AjaxSaveAndContinue.prototype);
      const newAjaxSaveAndContinue = {
        // Prevent the save buttons from reloading the page. Instead, reset the editor's content.
        // FIXME: The in-place editor is also overriding reloadEditor, before this code is executed, so here we're
        // actually overwritting in-place editor's behavior.
        reloadEditor: () => {
          xwikiDocument.reload();
          // HACK: Replicate the behavior from the in-place editor.
          setTimeout(() => {
            $(form).trigger('xwiki:actions:reload');
          }, 0);
        },
        // Redirect only after we have confirmation that the saver state has been propagated to all clients.
        maybeRedirect: function(continueEditing) {
          if (!continueEditing) {
            self._chainpad.onSettle(() => {
              originalAjaxSaveAndContinue.maybeRedirect.apply(this, arguments);
            });
            return true;
          } else {
            return originalAjaxSaveAndContinue.maybeRedirect.apply(this, arguments);
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

    _updateState(push, immediate) {
      super._updateState(push, immediate);

      this._notifyStatusChange();

      let latestVersion = '0.0';
      let savedBy;
      for (const [clientId, state] of Object.entries(this._getStates())) {
        if (this._compareVersions(state.version || '0.0', latestVersion) > 0) {
          latestVersion = state.version;
          savedBy = clientId;
        }
      }
      if (this._compareVersions(latestVersion, xwikiDocument.version) > 0) {
        xwikiDocument.update({
          version: latestVersion,
          modified: now(),
          isNew: false
        });
        if (savedBy !== this._getClientId()) {
          this._config.onCreateVersion({
            number: latestVersion,
            date: xwikiDocument.modified,
            author: savedBy
          });
        }
      }
    }

    _notifyStatusChange() {
      const status = (this._isSomeoneSaving() && 1) || (this._isSomeoneDirty() ? 0 : 2);
      if (this._previousStatus !== status) {
        this._previousStatus = status;
        this._config.onStatusChange(status);
      }
    }

    async toBeReady() {
      const result = await super.toBeReady();
      this._notifyStatusChange();
      if (!xwikiDocument.isNew) {
        // Retrieve information about the initial version, when joining the editing session, but without blocking the
        // saver ready state.
        xwikiDocument.getRevision(xwikiDocument.version).then(revision => {
          this._config.onCreateVersion({
            number: revision.version,
            date: new Date(revision.modified).getTime(),
            author: {
              reference: this._getAbsoluteUserReference(revision.modifier),
              name: revision.modifierName
            }
          });
        }).catch(error => {
          console.debug('Failed to retrieve information about the initial version.', error);
        });
      }
      return result;
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

    _getSavePriority({button}) {
      // Give higher priority to manual saves (when the user clicks on the save button). Also give higher priority to
      // Save & View over Save & Continue. The former leaves the edit mode so we want to make sure we don't lose unsaved
      // changes, while the latter keeps the user in the edit mode where we have autosave.
      if (button) {
        // Manual save
        return button.getAttribute('name') === 'action_save' ? 3 : 2;
      } else {
        // Autosave
        return super._getSavePriority();
      }
    }

    async _submit({button}) {
      // The merge conflict modal is already displayed (from a previous save attempt). Clicking the save button again
      // would reopen the same modal and reset the fields the user did not submit yet. We don't want that.
      if ($('#previewDiffModal').is(':visible')) {
        throw new Error('Merge conflict prevents save.');
      }

      button = button || this._getSaveButton(true);
      if (!$(button).is(':enabled')) {
        throw new Error('The save button is disabled or missing.');
      }

      const form = document.getElementById(this._config.formId);
      const removeListeners = [];
      const submitResultPromise = this._getSubmitResult(form, removeListeners);

      let savePrevented = true;
      $(button).on('xwiki:actions:save.realtime-saver', event => {
        savePrevented = event.isDefaultPrevented();
      });
      $(button).click();
      $(button).off('xwiki:actions:save.realtime-saver');
      if (savePrevented) {
        // The save is prevented if the form has invalid data (e.g. missing mandatory title). In this case the
        // xwiki:document:saved and xwiki:document:saveFailed events are not triggered, so we need to remove the
        // corresponding event listeners and reject the save.
        removeListeners.forEach(removeListener => removeListener());
        throw new Error('Save prevented. Verify that the form has valid data.');
      }

      this._afterSave(await submitResultPromise);
    }

    _getSaveButton(continueEditing) {
      const form = document.getElementById(this._config.formId);
      return form.querySelector('input[name="action_save' + (continueEditing ? 'andcontinue' : '') + '"]');
    }

    _getSubmitResult(form, removeListeners) {
      return new Promise((resolve, reject) => {
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
            reject('Failed to save.');
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
          reject('Discarding local changes by reloading the editor.');
        });
        // ... or for the merge conflict modal to be closed without resolving the conflict.
        this._once(document, removeListeners, 'hide.bs.modal.realtime-saver', '#previewDiffModal', () => {
          if ($('#previewDiffModal').data('action') === 'cancel') {
            reject('Save canceled.');
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
      const originalHandler = args[args.length - 1];
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
      if (newVersion === '1.1') {
        debug('Created document version 1.1');
      } else {
        debug(`Version bumped from ${xwikiDocument.version} to ${newVersion}.`);
      }
      this._state.version = newVersion;
      this._config.onCreateVersion({
        number: newVersion,
        date: now(),
        author: this._getClientId()
      });
    }

    save(continueEditing) {
      return this._save({button: this._getSaveButton(continueEditing)});
    }
  }

  return XWikiSaver;
});
