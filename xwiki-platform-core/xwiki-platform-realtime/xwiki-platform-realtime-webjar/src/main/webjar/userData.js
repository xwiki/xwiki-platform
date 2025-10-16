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
define('xwiki-realtime-userData', [
  'chainpad',
  'chainpad-netflux',
  'json.sortify'
], function(ChainPad, ChainPadNetflux, jsonSortify) {
  'use strict';

  let userData, onChange;
  function updateUserData(textData) {
    try {
      const json = JSON.parse(textData);
      for (let key in json) {
        userData[key] = json[key];
      }
    } catch (error) {
      console.error('Failed to parse user data.', {userData: textData, error});
    }
  }

  let module = {}, online, myId;

  function startInitializing() {
    module._initializing = new Promise(resolve => {
      module._notifyReady = () => {
        // Mark the UserData as ready right away (rather than using a promise callback which would be called on the next
        // tick), to be visible to the code executed right after _notifyReady is called.
        module._initializing = false;
        resolve();
      };
    });
  }

  function createConfig(network, key, configData) {
    return {
      initialState: '{}',
      network,
      userName: configData.user.sessionId,
      channel: key,
      crypto: configData.crypto || null,
      // Operational Transformation
      patchTransformer: ChainPad.SmartJSONTransformer,

      onReady: function(info) {
        module.chainpad = info.realtime;
        module._notifyReady();
        updateUserData(module.chainpad.getUserDoc());
        this.onLocal();
      },

      onLocal: function() {
        if (!module._initializing && online) {
          onChange(userData);
          const strHyperJSON = jsonSortify(userData);
          module.chainpad.contentUpdate(strHyperJSON);
          if (module.chainpad.getUserDoc() !== strHyperJSON) {
            console.warn('userDoc !== strHyperJSON');
          }
        }
      },

      onRemote: function(info) {
        if (!module._initializing) {
          updateUserData(module.chainpad.getUserDoc());
          onChange(userData);
        }
      },

      onConnectionChange: function(info) {
        if (info.state) {
          myId = info.myId;
          online = true;
          module.chainpad.start();
          startInitializing();
        } else {
          module.chainpad.abort();
          online = false;
        }
      }
    };
  }

  function getMyUserData(configData, cursor) {
    return {
      ['cursor_' + configData.editor]: cursor,
      ...configData.user,
    };
  }

  function createUserData(configData, config) {
    let cursor, oldCursor;
    if (configData.editor && typeof configData.getCursor === 'function') {
      cursor = configData.getCursor;
      oldCursor = cursor();
    }

    const userData = {};
    userData[myId] = getMyUserData(configData, oldCursor);

    let intervalId;
    if (cursor !== 'undefined') {
      intervalId = setInterval(function() {
        if (!online) {
          return;
        }
        const newCursor = cursor();
        if (oldCursor !== newCursor) {
          userData[myId] = userData[myId] || getMyUserData(configData);
          userData[myId]['cursor_' + configData.editor] = newCursor;
          oldCursor = newCursor;
          config.onLocal();
        }
      }, 3000);
    }

    userData.stop = function() {
      clearInterval(intervalId);
      module.realtimeInput?.stop();
      delete module.realtimeInput;
    };

    return userData;
  }

  module.start = async function(network, key, configData = {}) {
    startInitializing();

    myId = configData.myId;
    if (!myId || !configData.user.sessionId) {
      console.error("myId and sessionId are required!");
      return;
    }

    online = true;
    onChange = configData.onChange || (() => {});

    const config = createConfig(network, key, configData);
    userData = createUserData(configData, config);

    // We can't store the realtimeInput in the userData object because it's not serializable to JSON.
    module.realtimeInput = ChainPadNetflux.start(config);

    await module._initializing;

    return userData;
  };

  return module;
});
