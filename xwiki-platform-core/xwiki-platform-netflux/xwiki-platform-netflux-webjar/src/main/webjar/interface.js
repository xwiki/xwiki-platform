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
// This file defines functions which are used in RTWiki/RTWysiwyg, and address components of the user interface.
define(['jquery'], function ($) {
  'use strict';

  var Interface = {};

  var debug = function(x) {console.log(x);};

  var uid = Interface.uid = function() {
    return 'realtime-uid-' + String(Math.random()).substring(2);
  };

  var setStyle = Interface.setStyle = function() {
    $('head').append([
      '<style>',
      '.realtime-merge {',
      '  float: left',
      '}',
      '#secret-merge {',
      '  opacity: 0;',
      '}',
      '#secret-merge:hover {',
      '  opacity: 1;',
      '}',
      '</style>'
     ].join(''));
  };

  var LOCALSTORAGE_DISALLOW;
  var setLocalStorageDisallow = Interface.setLocalStorageDisallow = function(key) {
    LOCALSTORAGE_DISALLOW = key;
  };

  //This hides a DIFFERENT autosave, not the one included in the realtime. This is a checkbox which is off by default.
  // We hide it so that it can't be turned on, because that would cause some problems.
  var setAutosaveHiddenState = function(hidden) {
    var elem = $('#autosaveControl');
    if (hidden) {
      elem.hide();
    } else {
      elem.show();
    }
  };
  // Stub for old versions
  Interface.setAutosaveHiddenState = function() {};

  var allowed = false;
  var realtimeAllowed = Interface.realtimeAllowed = function(bool) {
    if (typeof bool === 'undefined') {
      return allowed;
    } else {
      allowed = bool;
      setAutosaveHiddenState(bool);
      return bool;
    }
  };

  var createAllowRealtimeCheckbox = Interface.createAllowRealtimeCheckbox = function(id, checked, message) {
    $('head').append([
      '<style>',
      '.realtime-allow-outerdiv {',
      '  display: inline;',
      '  white-space: nowrap;',
      '}',
      '</style>'
     ].join(''));
    $('.buttons').append(
      '<div class="realtime-allow-outerdiv">' +
        '<label class="realtime-allow-label" for="' + id + '">' +
          '<input type="checkbox" class="realtime-allow" id="' + id + '" ' +
            checked + '" />' +
          ' ' + message +
        '</label>' +
      '</div>'
    );
  };

  // TODO: move into Interface (after factoring out more arguments). Maybe this should go in autosaver instead?
  var createMergeMessageElement = Interface.createMergeMessageElement = function(container, messages) {
    setStyle();
    var id = uid();
    $(container).prepend( '<div class="realtime-merge" id="'+id+'"></div>');
    var $merges = $('#'+id);

    var timeout;

    // drop a method into the lastSaved object which handles messages
    return function (msgType, args) {
      // keep multiple message sequences from fighting over resources
      clearTimeout(timeout);

      var formattedMessage = messages[msgType].replace(/\{(\d+)\}/g, function(all, token) {
        // if you pass an insufficient number of arguments
        // it will return 'undefined'
        return args[token];
      });

      debug(formattedMessage);

      // set the message, handle all types
      $merges.text(formattedMessage);

      // clear the message box in five seconds
      // 1.5s message fadeout time
      timeout = setTimeout(function() {
        $merges.fadeOut(1500, function() {
          $merges.text('');
          $merges.show();
        });
      },10000);
    };
  };

  return Interface;
});
