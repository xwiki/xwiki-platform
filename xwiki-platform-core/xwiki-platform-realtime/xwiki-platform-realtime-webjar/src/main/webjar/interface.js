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
// This file defines functions which are used by the real-time editors (Wiki / WYSIWYG) and address components of the
// user interface.
define('xwiki-realtime-interface', ['jquery'], function($) {
  'use strict';

  var Interface = {};

  var uid = Interface.uid = function() {
    return 'realtime-uid-' + String(Math.random()).substring(2);
  };

  var LOCALSTORAGE_DISALLOW;
  var setLocalStorageDisallow = Interface.setLocalStorageDisallow = function(key) {
    LOCALSTORAGE_DISALLOW = key;
  };

  var allowed = false;
  var realtimeAllowed = Interface.realtimeAllowed = function(bool) {
    if (arguments.length) {
      // Change the value.
      allowed = !!bool;
      // The real-time edit doesn't support the standard auto-save feature (it provides instead its own auto-save).
      $('#autosaveControl').toggle(!allowed);
    }
    return allowed;
  };

  var createAllowRealtimeCheckbox = Interface.createAllowRealtimeCheckbox = function(id, checked, message) {
    var checkbox = $(
      '<div class="realtime-allow-outerdiv">' +
        '<label class="realtime-allow-label" for="">' +
          '<input type="checkbox" class="realtime-allow" id=""/>' +
        '</label>' +
      '</div>'
    ).appendTo('.buttons');
    checkbox.find('label').attr('for', id).append(document.createTextNode(message))
      .find('input').attr('id', id).prop('checked', !!checked);
  };

  var createMergeMessageElement = Interface.createMergeMessageElement = function(container, messages) {
    var $merges = $('<div class="realtime-merge"/>').attr('id', uid()).prependTo(container);

    var timeout;

    // Drop a method into the lastSaved object which handles messages.
    return function (messageKey, args) {
      // Keep multiple message sequences from fighting over resources.
      clearTimeout(timeout);

      var formattedMessage = messages[messageKey].replace(/\{(\d+)\}/g, function(all, token) {
        // If you pass an insufficient number of arguments it will return 'undefined'.
        return args[token];
      });

      console.log(formattedMessage);

      // Set the message, handle all types.
      $merges.text(formattedMessage);

      // Clear the message box in 10 seconds (1.5s message fadeout time).
      timeout = setTimeout(function() {
        $merges.fadeOut(1500, function() {
          $merges.empty().show();
        });
      }, 10000);
    };
  };

  return Interface;
});
