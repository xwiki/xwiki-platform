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

  const {SaveTransport, Saver, XWikiFormSaveTarget} = AutoSave;

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
        saver => (this._target = new XWikiFormSaveTarget(saver, {
          document: xwikiDocument,
          formId: config.formId,
          autoSaveVersionSummary: Messages.autoSaveSummary,
          onCreateVersion: config.onCreateVersion
        }))
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
