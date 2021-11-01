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
], function(ChainPad, chainpadNetflux, jsonSortify) {
  'use strict';
  var userData, onChange;
  var updateUserData = function(textData) {
    try {
      var json = JSON.parse(textData);
      for (var key in json) {
        userData[key] = json[key];
      }
      if (typeof onChange === 'function') {
        onChange(userData);
      }
    } catch (e) {
      console.log('Failed to parse user data: ' + textData);
      console.error(e);
    }
  };

  var module = {}, online, myId;
  var createConfig = function(network, key, configData) {
    var initializing = true;
    return {
      initialState: '{}',
      network,
      userName: configData.userName,
      channel: key,
      crypto: configData.crypto || null,
      // Operational Transformation
      patchTransformer: ChainPad.SmartJSONTransformer,

      onReady: function(info) {
        module.leave = info.leave;
        module.chainpad = info.realtime;
        updateUserData(module.chainpad.getUserDoc());
        initializing = false;
        this.onLocal();
      },

      onLocal: function() {
        if (!initializing && online) {
          var strHyperJSON = jsonSortify(userData);
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
  };

  var getMyUserData = function(configData, cursor) {
    var myUserData = {
      name: configData.userName
    };
    if (cursor) {
      myUserData['cursor_' + configData.editor] = cursor;
    }
    if (typeof configData.userAvatar === 'string') {
      myUserData.avatar = configData.userAvatar;
    }
    return myUserData;
  };

  var createUserData = function(configData, config) {
    var cursor, oldCursor;
    if (configData.editor && typeof configData.getCursor === 'function') {
      cursor = configData.getCursor;
      oldCursor = cursor();
    }

    var userData = {};
    userData[myId] = getMyUserData(configData, oldCursor);

    var intervalId;
    if (typeof cursor !== 'undefined') {
      intervalId = setInterval(function() {
        if (!online) {
          return;
        }
        var newCursor = cursor();
        if (oldCursor !== newCursor) {
          userData[myId] = userData[myId] || getMyUserData(configData);
          userData[myId]['cursor_' + configData.editor] = newCursor;
          oldCursor = newCursor;
          onChange(userData);
          config.onLocal();
        }
      }, 3000);
    }

    userData.leave = function() {
      clearInterval(intervalId);
      try {
        // Don't throw error if the channel is already removed.
        module.leave();
      } catch (e) {
        console.error(e);
      }
    };

    return userData;
  };

  module.start = function(network, key, configData) {
    configData = configData || {};
    myId = configData.myId;
    if (!myId || !configData.userName) {
      console.warn("myId and userName are required!");
      return;
    }

    online = true;
    onChange = configData.onChange;

    var config = createConfig(network, key, configData);
    userData = createUserData(configData, config);

    chainpadNetflux.start(config);

    return userData;
  };

  return module;
});
