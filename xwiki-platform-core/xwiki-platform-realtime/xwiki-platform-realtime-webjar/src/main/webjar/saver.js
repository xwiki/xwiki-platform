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
  'chainpad-netflux',
  'json.sortify',
  'xwiki-realtime-crypto',
  'xwiki-meta',
  'xwiki-realtime-document',
  'xwiki-realtime-errorBox'
], function(
  /* jshint maxparams:false */
  $, ChainPadNetflux, jsonSortify, Crypto, xwikiMeta, doc, ErrorBox
) {
  'use strict';

  function warn(...args) {
    console.warn(...args);
  }

  function debug(...args) {
    console.debug(...args);
  }

  function verbose() {
    // Do nothing for now.
  }

  const SAVE_DOC_TIME = 60000;
  // How often to check if the document has been saved recently.
  const SAVE_DOC_CHECK_CYCLE = 20000;

  function now() {
    return new Date().getTime();
  }

  /**
   * Clients check for remote changes on random intervals. If another client has saved outside of the realtime session,
   * changes are merged on the server using XWiki's three-way merge algorithm. The changes are integrated into the local
   * textarea, which replicates across realtime sessions. If the resulting state does not match the last saved content,
   * then the contents are saved as a new version. Other members of the session are notified of the save, and the
   * resulting new version. They then update their local state to match. During this process, a series of checks are
   * made to reduce the number of unnecessary saves, as well as the number of unnecessary merges.
   */
  class Saver {
    constructor(config) {
      this._startInitializing();
      this._revertList = [];

      this._config = {
        formId: 'edit',
        safeCrash: warn
      };

      // Contains the realtime data.
      this._rtData = {};
      this._revertList.push(() => {
        this._rtData = {};
      });

      this._lastSaved = {
        content: '',
        time: now(),
        // http://jira.xwiki.org/browse/RTWIKI-37
        hasModifications: false,
        // For future tracking of 'edited since last save'. Only show the merge dialog to those who have edited.
        wasEditedLocally: false,
        receivedISAVE: false,
        shouldRedirect: false,
        isavedSignature: '',
        mergeMessage: () => {}
      };

      this._configure(config);

      this._realtimeInput = ChainPadNetflux.start(this._getRealtimeConfig());
      this._revertList.push(() => {
        this._realtimeInput?.stop();
        delete this._realtimeInput;
      });
    }

    _startInitializing() {
      this._initializing = new Promise(resolve => {
        this._notifyReady = () => {
          // Mark the Saver as ready right away (rather than using a promise callback which would be called on the next
          // tick), to be visible to the code executed right after _notifyReady is called.
          this._initializing = false;
          resolve();
        };
      });
    }

    async toBeReady() {
      if (this._initializing) {
        await this._initializing;
      }
      return this;
    }

    _configure(config) {
      $.extend(this._config, config);
      $.extend(this._lastSaved, {
        version: doc.version,
        time: doc.modified
      });
    }

    _interrupt() {
      if (this._lastSaved.receivedISAVE) {
        debug("Another client sent an ISAVED message.");
        debug("Aborting save action.");
        // Unset the flag, or else it will persist.
        this._lastSaved.receivedISAVE = false;
        // Return true such that calling functions know to abort.
        return true;
      }
      return false;
    }

    _update(content) {
      $.extend(this._lastSaved, {
        time: now(),
        content,
        wasEditedLocally: false
      });
    }

    destroyDialog(callback) {
      const $box = $('.xdialog-box.xdialog-box-confirmation'),
        $content = $box.find('.xdialog-content');
      if ($box.length) {
        $content.find('.button.cancel').click();
      }
      if (typeof callback === 'function') {
        callback(!!$box.length);
      }
    }

    /**
     * Realtime editors should call this on local edits.
     *
     * @param {boolean} wasEditedLocally {@code true} if the content was edited locally since the last save,
     *   {@code false} otherwise
     */
    setLocalEditFlag(wasEditedLocally) {
      this._lastSaved.wasEditedLocally = wasEditedLocally;
    }

    getLocalEditFlag() {
      return this._lastSaved.wasEditedLocally;
    }

    _bumpVersion(callback, versionData) {
      const success = (doc) => {
        debug('Triggering lastSaved refresh on remote clients.');
        this._lastSaved.version = doc.version;
        this._lastSaved.content = doc.content;
        /* jshint camelcase:false */
        const contentHash = this._chainpad.hex_sha256?.(doc.content) || '';
        this._saveMessage(this._lastSaved.version, contentHash);
        if (typeof callback === 'function') {
          callback(doc);
        }
      };
      if (versionData) {
        success(versionData);
      } else {
        doc.reload().then(success).catch(error => {
          const debugLog = {
            state: 'bumpVersion',
            lastSavedVersion: this._lastSaved.version,
            lastSavedContent: this._lastSaved.content,
            cUser: this._config.userName,
            cContent: this._config.getTextValue()
          };
          this._config.safeCrash('updateversion', JSON.stringify(debugLog));
          warn(error);
        });
      }
    }

    /**
     * Sends an ISAVED message.
     */
    _saveMessage(version, hash) {
      const newState = {
        version: version,
        by: this._config.userName,
        hash: hash,
        editorName: this._config.editorName
      };
      this._rtData[this._config.editorType] = newState;
      this._onLocal();

      this._chainpad.onSettle(() => {
        this._lastSaved.onReceiveOwnIsave?.();
      });
    }

    /**
     * Only used within Saver.create().
     */
    static _redirectToView() {
      window.location.href = window.XWiki.currentDocument.getURL('view');
    }

    _onMessage(data) {
      // Set a flag so any concurrent processes know to abort.
      this._lastSaved.receivedISAVE = true;

      // If the channel data is empty, do nothing (initial call in onReady).
      if (Object.keys(data).length === 0) {
        return;
      }
      for (let editor in data) {
        if (typeof data[editor] !== "object" || Object.keys(data[editor]).length !== 4) {
          // Corrupted data.
          continue;
        }
        if (this._rtData[editor] && jsonSortify(this._rtData[editor]) === jsonSortify(data[editor])) {
          // No change.
          continue;
        }
        this._onNewSave(editor, data[editor]);
        xwikiMeta.refreshVersion();
      }
      this._rtData = data;

      return false;
    }

    /**
     * Called when we receive an ISAVED message.
     *
     * Clients update lastSaved.version when they perform a save, then they send an ISAVED with the version. A single
     * user might have multiple windows open, for some reason, but might still have different save cycles. Checking
     * whether the received version matches the local version tells us whether the ISAVED was set by our *browser*. If
     * not, we should treat it as foreign.
     *
     * @param {string} type the editor type
     * @param {Object} message the received ISAVED message, holding information about the save
     */
    _onNewSave(editorType, message) {
      if (editorType === this._config.editorType) {
        if (this._lastSaved.version !== message.version) {
          this._displaySaverName(message, true);

          if (!this._initializing) {
            debug('A remote client saved and incremented the latest common ancestor.');
          }

          // Update lastSaved attributes.
          this._lastSaved.wasEditedLocally = false;

          // Update the local latest common ancestor version string.
          this._lastSaved.version = message.version;

          // Remember the state of the textArea when last saved so that we can avoid additional minor versions. There's
          // a *tiny* race condition here but it's probably not an issue.
          this._lastSaved.content = this._config.getTextValue();

          // Update the document meta in order to ensure proper merge on save (when using the form action buttons).
          doc.update({
            version: this._lastSaved.version,
            modified: now(),
            isNew: false
          });
        } else {
          this._lastSaved.onReceiveOwnIsave?.();
        }
        this._lastSaved.time = now();
      } else {
        this._displaySaverName(message, false);
      }
    }

    _displaySaverName(message, isMerged) {
      // A merge dialog might be open, if so, remove it and say as much.
      this.destroyDialog(dialogDestroyed => {
        if (dialogDestroyed) {
          // Tell the user about the merge resolution.
          this._lastSaved.mergeMessage('conflictResolved', [message.version]);
        } else if (!this._initializing) {
          let sender;
          // Otherwise say there was a remote save.
          // http://jira.xwiki.org/browse/RTWIKI-34
          if (this._config.userList) {
            sender = message.by.replace(/^.*-([^-]*)%2d\d*$/, function(all, one) {
              return decodeURIComponent(one);
            });
          }
          if (isMerged) {
            this._lastSaved.mergeMessage('savedRemote', [message.version, sender]);
          } else {
            this._lastSaved.mergeMessage('savedRemoteNoMerge', [message.version, sender, message.editorName]);
          }
        }
      });
    }

    /**
     * Stop the autosaver / merge when the user disallows realtime or when the WebSocket is disconnected.
     */
    stop() {
      this._revertList.forEach(revert => revert());
    }

    setLastSavedContent(content) {
      this._lastSaved.content = content;
    }

    _onRemote(info) {
      if (this._initializing) {
        return;
      }

      try {
        const data = JSON.parse(this._chainpad.getUserDoc());
        this._onMessage(data);
      } catch (e) {
        warn("Unable to parse realtime data from the saver.", e);
      }
    }

    _onReady(info) {
      this._chainpad = info.realtime;
      this._notifyReady();
      this._onRemote();
      this._onOpen();
    }

    _onLocal(info) {
      if (this._initializing) {
        return;
      }
      const sjson = jsonSortify(this._rtData);
      this._chainpad.contentUpdate(sjson);
      if (this._chainpad.getUserDoc() !== sjson) {
        warn("Saver: userDoc !== sjson");
      }
    }

    _getRealtimeConfig() {
      return {
        initialState: '{}',
        network: this._config.network,
        userName: this._config.userName || '',
        channel: this._config.channel,
        crypto: Crypto,
  
        onRemote: this._onRemote.bind(this),
        onReady: this._onReady.bind(this),
        onLocal: this._onLocal.bind(this),
        onAbort: this.stop.bind(this)
      };
    }

    _onOpen() {
      // There's a very small chance that the preview button might cause problems, so let's just get rid of it.
      $('[name="action_preview"]').hide();
      this._revertList.push(() => {
        $('[name="action_preview"]').show();
      });

      // Wait to get saved event.
      const saveHandler = this._onSavedHandler.bind(this);
      $(document).on('xwiki:document:saved.realtime-saver', saveHandler);
      this._revertList.push(() => {
        $(document).off('xwiki:document:saved.realtime-saver', saveHandler);
      });

      // Prevent the save buttons from reloading the page. Instead, reset the editor's content.
      const originalReloadEditor = XWiki.actionButtons.AjaxSaveAndContinue.prototype.reloadEditor;
      const reloadEditor = XWiki.actionButtons.AjaxSaveAndContinue.prototype.reloadEditor = () => {
        // TODO: Handle the page title.
        this._config.getTextAtCurrentRevision().then((data) => {
          this._config.setTextValue(data);
        });
      };
      this._revertList.push(() => {
        // Revert only if the reloadEditor method has not been overridden by another script.
        if (XWiki.actionButtons.AjaxSaveAndContinue.prototype.reloadEditor === reloadEditor) {
          XWiki.actionButtons.AjaxSaveAndContinue.prototype.reloadEditor = originalReloadEditor;
        }
      });

      this._check();
      this._revertList.push(() => {
        clearTimeout(this._autosaveTimeout);
      });
    }

    _onSavedHandler() {
      // This means your save has worked. Cache the last version.
      const lastVersion = this._lastSaved.version;
      const toSave = this._config.getTextValue();
      // Update your content.
      this._update(toSave);

      doc.reload().then(doc => {
        this._lastSaved.onReceiveOwnIsave = () => {
          // Once you get your ISAVED back, redirect.
          debug("lastSaved.shouldRedirect " + this._lastSaved.shouldRedirect);
          if (this._lastSaved.shouldRedirect) {
            debug('Saver.create.saveandview.receivedOwnIsaved');
            debug("redirecting!");
            Saver._redirectToView();
          } else {
            debug('Saver.create.saveandcontinue.receivedOwnIsaved');
          }
          // Clean up after yourself.
          delete this._lastSaved.onReceiveOwnIsave;
        };
        // Bump the version, fire your ISAVED.
        this._bumpVersion(doc => {
          if (doc.version === '1.1') {
            debug('Created document version 1.1');
          } else {
            debug(`Version bumped from ${lastVersion} to ${doc.version}.`);
          }
          this._lastSaved.mergeMessage('saved', [doc.version]);
        }, doc);
      }).catch(error => {
        warn(error);
        ErrorBox.show('save');
      });

      return true;
    }

    _check() {
      // Schedule the next check before doing anything else, otherwise we stop the autosave.
      clearTimeout(this._autosaveTimeout);
      verbose("Saver.create.check");
      const periodDuration = Math.random() * SAVE_DOC_CHECK_CYCLE;
      this._autosaveTimeout = setTimeout(this._check.bind(this), periodDuration);
      verbose(`Will attempt to save again in ${periodDuration} ms.`);

      let toSave;
      if (!this._lastSaved.wasEditedLocally || (toSave = this._config.getTextValue()) === this._lastSaved.content) {
        verbose("No changes made since last save. Avoiding unnecessary commits.");
        return;
      }

      if (toSave === null) {
        warn("Unable to get the edited content. Can't save.");
        return;
      }

      if (now() - this._lastSaved.time < SAVE_DOC_TIME) {
        verbose("Local changes detected, but not enough time has passed since the last check.");
        return;
      }

      // The merge conflict modal is displayed after clicking on the save button when there is a merge conflict.
      // Clicking the save button again would reopen the same modal and reset the fields the user did not submit yet.
      // We don't save if another user has saved since the last time we checked.
      if (!$('#previewDiffModal').is(':visible') && !this._interrupt()) {
        $(`#${this._config.formId} [name="action_saveandcontinue"]`).click();
      }
    }
  }

  return Saver;
});
