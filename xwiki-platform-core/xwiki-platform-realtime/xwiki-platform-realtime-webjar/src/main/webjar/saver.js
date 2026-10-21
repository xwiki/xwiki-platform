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

  // How long to wait for the server to acknowledge the local saver state, in milliseconds.
  const SETTLE_TIMEOUT = 2000;

  // The auto-save modules report what they do through the Logger role of the component API. This WebJar has no
  // component container to resolve the default implementation from, so it passes this console backed one.
  const logger = {
    module: 'Saver',

    setModule(module) {
      this.module = module;
    },

    debug(...args) {
      this._log('debug', ...args);
    },

    info(...args) {
      this._log('info', ...args);
    },

    warn(...args) {
      this._log('warn', ...args);
    },

    error(...args) {
      this._log('error', ...args);
    },

    _log(level, ...args) {
      console[level](this.module + ':', ...args);
    }
  };

  /**
   * Synchronizes the saver states using ChainPad.
   */
  class ChainPadSaveTransport extends SaveTransport {
    constructor(config, saver) {
      super(saver);

      this._config = config;
      this._revertList = [];
      // The callbacks of the callers waiting for the local state to reach the server, see whenSettled.
      this._settleWaiters = new Set();

      this._initializing = new Promise((resolve, reject) => {
        this._notifyReady = () => {
          // Mark the transport as ready right away (rather than using a promise callback which would be called on the
          // next tick), to be visible to the code executed right after _notifyReady is called.
          this._initializing = false;
          resolve();
        };
        this._notifyError = reject;
      });
      // The failure is reported to the caller of toBeReady, which may not be waiting yet when we fail to join the
      // channel, so mark it as handled to avoid an unhandled promise rejection.
      this._initializing.catch(() => {});

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

    /**
     * ChainPad settles when the server acknowledges our pending changes, but this acknowledgement may never come:
     * ChainPad doesn't time out the messages that are waiting for it and Netflux drops them (without rejecting them)
     * when the WebSocket connection is closed. So we stop waiting when the connection is lost, when the transport is
     * disposed, or when waiting has taken too long, because the saver waits for this before leaving the realtime
     * session and a settlement that never comes must not block the editor.
     *
     * @returns a promise that resolves when the local state has been received by the server, or when waiting for it
     *   doesn't make sense anymore
     */
    whenSettled() {
      if (!this._chainpad) {
        // We're not connected (yet, or anymore) so there's nothing to wait for.
        return Promise.resolve();
      }

      return new Promise(resolve => {
        const settle = () => {
          clearTimeout(timeout);
          this._settleWaiters.delete(settle);
          resolve();
        };
        const timeout = setTimeout(settle, SETTLE_TIMEOUT);
        this._settleWaiters.add(settle);
        this._chainpad.onSettle(settle);
      });
    }

    _stopWaitingForSettlement() {
      // Copy the set because each waiter removes itself from it.
      [...this._settleWaiters].forEach(settle => settle());
    }

    _onConnectionLost() {
      // Stop ChainPad from trying again and again to send our pending changes over a connection that is closed.
      this._chainpad?.abort();
      // We can only keep the state locally from now on (see updateLocalState).
      delete this._chainpad;
      this._stopWaitingForSettlement();
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
        // Called when the WebSocket connection is closed.
        onAbort: () => {
          this._onConnectionLost();
          this.saver.stop();
        },
        // Called when we fail to join the channel.
        onError: error => {
          logger.warn('Failed to join the saver channel.', error);
          if (this._initializing) {
            this._notifyError(error);
          }
        }
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
      if (this._initializing || !this._chainpad) {
        return;
      }

      const remoteStates = this._chainpad.getUserDoc();
      logger.debug('Received remote states: ', remoteStates);

      try {
        this._states = JSON.parse(remoteStates);
        // The remote document may not include our own state yet, in which case we keep the state we have until the
        // next push re-inserts it in the map.
        this.state = this._states[this.getClientId()] || this.state;
        this.saver.onRemoteStatesChanged();
      } catch (e) {
        logger.warn('Unable to parse remote states.', e);
      }
    }

    _onLocal() {
      if (this._initializing || !this._chainpad) {
        return;
      }
      const localStates = jsonSortify(this._states);
      logger.debug('Push local states: ', localStates);
      this._chainpad.contentUpdate(localStates);
      const remoteStates = this._chainpad.getUserDoc();
      if (remoteStates !== localStates) {
        logger.warn('Unexpected remote states after synchronization: ', {
          expected: localStates,
          actual: remoteStates
        });
      }
    }

    async dispose() {
      delete this._chainpad;
      this._stopWaitingForSettlement();

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
      this._saver = new Saver({logger},
        saver => new ChainPadSaveTransport(config, saver),
        saver => new XWikiFormSaveTarget(saver, {
          document: xwikiDocument,
          formId: config.formId,
          autoSaveVersionSummary: Messages.autoSaveSummary,
          onCreateVersion: config.onCreateVersion,
          logger
        })
      );
      // The saver states are delivered asynchronously, so no save status is reported before we start listening.
      this._saver.addLocalStatusListener(config.onLocalStatusChange);
      this._saver.addStatusListener(config.onStatusChange);
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
      return this._saver.save({action: continueEditing ? 'saveAndContinue' : 'saveAndView'});
    }

    stop() {
      return this._saver.stop();
    }
  }

  return ChainPadXWikiFormSaver;
});
