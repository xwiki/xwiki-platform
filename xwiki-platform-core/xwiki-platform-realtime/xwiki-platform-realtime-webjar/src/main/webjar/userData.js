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
      if (typeof onChange === 'function') {
        onChange(userData);
      }
    } catch (e) {
      console.log('Failed to parse user data: ' + textData);
      console.error(e);
    }
  }

  let module = {}, online, myId;
  function createConfig(network, key, configData) {
    let initializing = true;
    return {
      initialState: '{}',
      network,
      userName: configData.userName,
      channel: key,
      crypto: configData.crypto || null,
      // Operational Transformation
      patchTransformer: ChainPad.SmartJSONTransformer,

      onReady: function(info) {
        module.chainpad = info.realtime;
        updateUserData(module.chainpad.getUserDoc());
        initializing = false;
        this.onLocal();
      },

      onLocal: function() {
        if (!initializing && online) {
          const strHyperJSON = jsonSortify(userData);
          module.chainpad.contentUpdate(strHyperJSON);
          if (module.chainpad.getUserDoc() !== strHyperJSON) {
            console.warn('userDoc !== strHyperJSON');
          }
        }
      },

      onRemote: function(info) {
        if (!initializing) {
          updateUserData(module.chainpad.getUserDoc());
        }
      },

      onConnectionChange: function(info) {
        if (info.state) {
          myId = info.myId;
          online = true;
          module.chainpad.start();
          initializing = true;
        } else {
          module.chainpad.abort();
          online = false;
        }
      }
    };
  }

  function getMyUserData(configData, cursor) {
    const myUserData = {
      name: configData.userName
    };
    if (cursor) {
      myUserData['cursor_' + configData.editor] = cursor;
    }
    if (typeof configData.userAvatar === 'string') {
      myUserData.avatar = configData.userAvatar;
    }
    return myUserData;
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
    if (typeof cursor !== 'undefined') {
      intervalId = setInterval(function() {
        if (!online) {
          return;
        }
        const newCursor = cursor();
        if (oldCursor !== newCursor) {
          userData[myId] = userData[myId] || getMyUserData(configData);
          userData[myId]['cursor_' + configData.editor] = newCursor;
          oldCursor = newCursor;
          onChange(userData);
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

  module.start = function(network, key, configData) {
    configData = configData || {};
    myId = configData.myId;
    if (!myId || !configData.userName) {
      console.warn("myId and userName are required!");
      return;
    }

    online = true;
    onChange = configData.onChange;

    const config = createConfig(network, key, configData);
    userData = createUserData(configData, config);

    // We can't store the realtimeInput in the userData object because it's not serializable to JSON.
    module.realtimeInput = ChainPadNetflux.start(config);

    return userData;
  };

  return module;
});
