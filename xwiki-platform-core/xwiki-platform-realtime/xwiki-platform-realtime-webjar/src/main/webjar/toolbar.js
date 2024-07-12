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

  function uid() {
    return 'rt-uid-' + String(Math.random()).substring(2);
  }

  // The element that contains the user list, debug link and lag.
  function createRealtimeToolbar($container) {
    return $(
      '<div class="rt-toolbar">' +
        '<div class="rt-toolbar-leftside"></div>' +
        '<div class="rt-toolbar-rightside"></div>' +
      '</div>'
    ).attr('id', uid()).prependTo($container.first());
  }

  function createSpinner($container) {
    return $('<div class="rt-spinner"></div>').attr('id', uid()).appendTo($container)[0];
  }

  const SPINNER_DISAPPEAR_TIME = 3000;
  const SPINNER = ['-', '\\', '|', '/'];
  function kickSpinner(spinnerElement, reversed) {
    const text = spinnerElement.textContent || '-';
    const delta = reversed ? -1 : 1;
    spinnerElement.textContent = SPINNER[(SPINNER.indexOf(text) + delta) % SPINNER.length];
    clearTimeout(spinnerElement.timeout);
    spinnerElement.timeout = setTimeout(function () {
      spinnerElement.textContent = '';
    }, SPINNER_DISAPPEAR_TIME);
  }

  function createUserList($container) {
    return $('<div class="rt-user-list"></div>').attr('id', uid()).appendTo($container)[0];
  }

  const userDisplayMode = Config.toolbarUserlist || 'both';
  function getOtherUsers(myUserId, userList, usersData) {
    return userList.map(function(userId) {
      // Collect the user data.
      const userData = usersData?.[userId];
      if (userData && userId !== myUserId) {
        const userJSON = userData.name?.replace(
          // <userReference>-encoded(<userName>)%2d<randomNumber>
          /^(.*)-([^-]*)%2d\d*$/,
          function(all, userReference, userName) {
            return JSON.stringify({
              reference: userReference,
              name: decodeURIComponent(userName)
            });
          }
        );
        if (userJSON !== userData.name) {
          return $.extend(JSON.parse(userJSON), {
            id: userId,
            avatar: userData.avatar
          });
        }
      }
    }).filter(function(user) {
      // Filter out users without data.
      return user;
    // Display each user. We don't need to separate the users with comma if the avatars are displayed.
    }).map(displayUser).join((userDisplayMode === 'avatar' || userDisplayMode === 'both') ? '' : ', ');
  }

  function displayUser(user) {
    const userDisplay = $('<a></a>').attr({
      'class': Config.marginAvatar === 1 ? 'rt-user-link' : '',
      'href': new XWiki.Document(XWiki.Model.resolve(user.reference, XWiki.EntityType.DOCUMENT)).getURL(),
      'data-id': user.id,
      'data-reference': user.reference
    });
    if (userDisplayMode === 'name' || userDisplayMode === 'both') {
      userDisplay.text(user.name);
    }
    if (userDisplayMode === 'avatar' || userDisplayMode === 'both') {
      if (user.avatar) {
        $('<img class="rt-user-avatar"/>').attr({
          src: user.avatar,
          title: user.name
        }).prependTo(userDisplay);
      } else if (user.avatar === '') {
        $('<span class="rt-user-fake-avatar"></span>').attr('title', user.name).text(user.name.substring(0, 1))
          .prependTo(userDisplay);
      }
    }
    return userDisplay.prop('outerHTML');
  }

  function updateUserList(myUserName, listElement, userList, userData, onUserNameClick) {
    // Update the current user id on the tool bar (used by automated functional tests).
    listElement.closest('.rt-toolbar').dataset.userId = myUserName;
    if (userList.indexOf(myUserName) < 0) {
      listElement.textContent = Messages.synchronizing;
      return;
    }
    if (userList.length === 1) {
      listElement.innerHTML = Messages.editingAlone;
    } else {
      listElement.innerHTML = Messages.editingWith + ' ' + getOtherUsers(myUserName, userList, userData);
    }
    $('.rt-user-link').off('click').on('click', function(event) {
      event.preventDefault();
      if (typeof onUserNameClick === 'function') {
        onUserNameClick($(this).attr('data-id'));
      } else if ($('iframe').length) {
        const baseHref = $('iframe')[0].contentWindow.location.href.split('#')[0] || '';
        $('iframe')[0].contentWindow.location.href = baseHref + '#rt-user-' + $(this).attr('data-id');
      }
    });
  }

  function createLagElement($container) {
    return $('<div class="rt-lag"></div>').attr('id', uid()).appendTo($container)[0];
  }

  function checkLag(getLag, lagElement) {
    if (typeof getLag === 'function') {
      const lag = getLag();
      // Show the lag duration as a number of seconds.
      lagElement.textContent = Messages.lag + ' ' + (typeof lag === 'number' ? lag / 1000 : '??');
    }
  }

  function create({$container, myUserName, realtime, getLag, userList, config}) {
    // Save the current user id on the tool bar to be used by automated functional tests.
    const toolbar = createRealtimeToolbar($container).attr('data-user-id', myUserName);
    let userListElement = createUserList(toolbar.find('.rt-toolbar-leftside'));
    const spinner = createSpinner(toolbar.find('.rt-toolbar-rightside'));
    const lagElement = createLagElement(toolbar.find('.rt-toolbar-rightside'));
    let userData = config.userData;

    let connected = false;
    userList.change.push(function(newUserData) {
      const users = userList.users;
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
      updateUserList(myUserName, userListElement, users, userData, config.onUsernameClick);
    });

    function ks() {
      if (connected) {
        kickSpinner(spinner);
      }
    }

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
  }

  return {create};
});
