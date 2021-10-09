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
define('xwiki-realtime-document', [
  'jquery',
  'xwiki-meta',
  'xwiki-realtime-config'
], function($, meta, realtimeConfig) {
  'use strict';

  var channelListAPI = {
    getByPath: function(path) {
      return this.filter(channel => JSON.stringify(channel.path) === JSON.stringify(path))[0];
    },
    getByPathPrefix: function(pathPrefix) {
      return this.filter(channel => channel.path.length >= pathPrefix.length &&
        JSON.stringify(channel.path.slice(0, pathPrefix.length)) === JSON.stringify(pathPrefix));
    }
  };

  var document = {
    // Initialize the document fields based on the meta information available on page load.
    documentReference: meta.documentReference,
    language: meta.locale,
    version: meta.version,
    isNew: meta.isNew,

    reload: function() {
      // TODO don't forget to set the real locale afterwards!
    },

    save: function(data) {
      return $.post(window.docsaveurl, $.param($.extend({
        /* jshint camelcase:false */
        form_token: meta.form_token,
        xredirect: '',
        language: this.language,
        xaction: ['save', 'saveandcontinue', 'preview', 'cancel'],
        action_saveandcontinue: 'Save',
        xeditaction: 'edit',
        previousVersion: this.version,
        isNew: this.isNew,
        editingVersionDate: this.modified,
        minorEdit: 1,
        ajax: true
      }, data), true)).then($.proxy(this, 'reload'));
    },

    getChannels: function(params) {
      var url = new XWiki.Document(this.documentReference).getRestURL('channels');
      return $.getJSON(url, $.param(params, true)).then(function(data) {
        if (!Array.isArray(data)) {
          console.error('Failed to retrieve the list of document channels.');
          return $.Deferred().reject(data).promise();
        } else {
          return $.extend(data, channelListAPI);
        }
      });
    }
  };

  // Extend with document fields coming from the real-time configuration.
  return $.extend(document, realtimeConfig.document);
});
