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
define('xwiki-realtime-toolbar', [
  'jquery',
  'xwiki-realtime-config',
  'xwiki-l10n!xwiki-realtime-messages'
], function($, Config, Messages) {
  'use strict';

  var uid = function() {
    return 'rt-uid-' + String(Math.random()).substring(2);
  };

  // The element that contains the user list, debug link and lag.
  var createRealtimeToolbar = function($container) {
    return $(
      '<div class="rt-toolbar">' +
        '<div class="rt-toolbar-leftside"/>' +
        '<div class="rt-toolbar-rightside"/>' +
      '</div>'
    ).attr('id', uid()).prependTo($container.first());
  };

  var createSpinner = function($container) {
    return $('<div class="rt-spinner"/>').attr('id', uid()).appendTo($container)[0];
  };

  var SPINNER_DISAPPEAR_TIME = 3000;
  var SPINNER = ['-', '\\', '|', '/'];
  var kickSpinner = function(spinnerElement, reversed) {
    var text = spinnerElement.textContent || '-';
    var delta = (reversed) ? -1 : 1;
    spinnerElement.textContent = SPINNER[(SPINNER.indexOf(text) + delta) % SPINNER.length];
    clearTimeout(spinnerElement.timeout);
    spinnerElement.timeout = setTimeout(function () {
      spinnerElement.textContent = '';
    }, SPINNER_DISAPPEAR_TIME);
  };

  var createUserList = function($container) {
    return $('<div class="rt-user-list"/>').attr('id', uid()).appendTo($container)[0];
  };

  var getOtherUsers = function(myUserId, userList, usersData) {
    var displayConfig = Config.toolbarUserlist;
    return userList.map(function(userId) {
      // Collect the user data.
      if (userId !== myUserId) {
        var userData = (usersData || {})[userId] || {};
        var userName = (userData.name || '').replace(/^.*-([^-]*)%2d[0-9]*$/, function(all, one) {
         return decodeURIComponent(one);
        });
        if (userName) {
          return {
            id: userId,
            name: userName,
            avatar: userData.avatar
          };
        }
      }
    }).filter(function(user) {
      // Filter out users without data.
      return user;
    }).map(function(user) {
      // Display the users.
      var userDisplay = $('<span/>').attr({
        'class': Config.marginAvatar === 1 ? 'rt-user-link' : '',
        'data-id': user.id
      });
      if (displayConfig === undefined || displayConfig === 'name' || displayConfig === 'both') {
        userDisplay.text(user.name);
      }
      if (displayConfig === 'avatar' || displayConfig === 'both') {
        if (user.avatar) {
          $('<img class="rt-user-avatar"/>').attr({
            src: user.avatar,
            title: user.name
          }).prependTo(userDisplay);
        } else if (user.avatar === '') {
          $('<span class="rt-user-fake-avatar"/>').attr('title', user.name).text(user.name.substring(0, 1))
            .prependTo(userDisplay);
        }
      }
      return userDisplay.html();
    }).join(displayConfig === 'avatar' ? ' ' : ', ');
  };

  var updateUserList = function(myUserName, listElement, userList, userData, onUsernameClick) {
    if (userList.indexOf(myUserName) < 0) {
      listElement.textContent = Messages.synchronizing;
      return;
    }
    if (userList.length === 1) {
      listElement.innerHTML = Messages.editingAlone;
    } else if (userList.length === 2) {
      listElement.innerHTML = Messages.editingWithOneOtherPerson + getOtherUsers(myUserName, userList, userData);
    } else {
      listElement.innerHTML = Messages.editingWith + ' ' + (userList.length - 1) + ' ' + Messages.otherPeople +
        getOtherUsers(myUserName, userList, userData);
    }
    $('.rt-user-link').off('click').on('click', function() {
      if (typeof onUsernameClick === 'function') {
        onUsernameClick($(this).attr('data-id'));
      } else if ($('iframe').length) {
        var baseHref = $('iframe')[0].contentWindow.location.href.split('#')[0] || '';
        $('iframe')[0].contentWindow.location.href = baseHref + '#rt-user-' + $(this).attr('data-id');
      }
    });
  };

  var createLagElement = function($container) {
    return $('<div class="rt-lag"/>').attr('id', uid()).appendTo($container)[0];
  };

  var checkLag = function(getLag, lagElement) {
    if (typeof getLag === 'function') {
      var lag = getLag();
      // Show the lag duration as a number of seconds.
      lagElement.textContent = Messages.lag + ' ' + (typeof lag === 'number' ? lag / 1000 : '??');
    }
  };

  var create = function({$container, myUserName, realtime, getLag, userList, config}) {
    var toolbar = createRealtimeToolbar($container);
    var userListElement = createUserList(toolbar.find('.rt-toolbar-leftside'));
    var spinner = createSpinner(toolbar.find('.rt-toolbar-rightside'));
    var lagElement = createLagElement(toolbar.find('.rt-toolbar-rightside'));
    var userData = config.userData;
    var changeNameID = config.changeNameID;
    var onUsernameClick = config.onUsernameClick;

    // Check if the user is allowed to change their name.
    if (changeNameID) {
      // Create the button and update the element containing the user list.
      userListElement = createChangeName($container, userListElement, changeNameID);
    }

    var connected = false;
    userList.change.push(function(newUserData) {
      var users = userList.users;
      if (users.indexOf(myUserName) !== -1) {
        connected = true;
      }
      if (!connected) {
        return;
      }
      if (newUserData) {
        // Someone has changed their name or color.
        userData = newUserData;
      }
      updateUserList(myUserName, userListElement, users, userData, onUsernameClick);
    });

    var ks = function() {
      if (connected) {
        kickSpinner(spinner, false);
      }
    };

    realtime.onPatch(ks);
    // Try to filter out non-patch messages, doesn't have to be perfect this is just the spinner.
    realtime.onMessage(function(msg) {
      if (msg.indexOf(':[2,') > -1) {
        ks();
      }
    });

    setInterval(function() {
      if (connected) {
        checkLag(getLag, lagElement);
      }
    }, 3000);

    return {
      toolbar,
      failed: function() {
        connected = false;
        userListElement.textContent = Messages.disconnected;
        lagElement.textContent = '';
      },
      reconnecting: function(userId) {
        connected = false;
        myUserName = userId;
        userListElement.textContent = Messages.reconnecting;
        lagElement.textContent = '';
      },
      connected: function() {
        connected = true;
      }
    };
  };

  return {create};
});
