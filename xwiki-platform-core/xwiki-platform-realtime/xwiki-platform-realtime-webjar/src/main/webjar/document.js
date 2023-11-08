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

  const channelListAPI = {
    getByPath: function(path) {
      return this.filter(channel => JSON.stringify(channel.path) === JSON.stringify(path))[0];
    },
    getByPathPrefix: function(pathPrefix) {
      return this.filter(channel => channel.path.length >= pathPrefix.length &&
        JSON.stringify(channel.path.slice(0, pathPrefix.length)) === JSON.stringify(pathPrefix));
    }
  };

  const document = {
    // Initialize the document fields based on the meta information available on page load.
    documentReference: meta.documentReference,
    language: meta.locale,
    version: meta.version,
    isNew: meta.isNew,

    reload: function() {
      return $.getJSON(meta.restURL, {
        // Make sure the response is not retrieved from cache (IE11 doesn't obey the caching HTTP headers).
        timestamp: new Date().getTime()
      }).then(updatedDocument => {
        // Reload succeeded.
        // We were able to load the document so it's not new.
        this.isNew = false;
        return $.extend(this, updatedDocument, {
          // We need the real locale.
          language: updatedDocument.language || updatedDocument.translations['default']
        });
      }, jqXHR => {
        if (jqXHR.status === 404) {
          // The document doesn't exist anymore. Maybe it was deleted?
          return $.extend(this, {
            version: '1.1',
            modified: 0,
            content: '',
            isNew: true
          });
        } else {
          // Reload failed. Continue using the current data.
          return this;
        }
      }).then(this.update.bind(this));
    },

    update: function(data) {
      $.extend(this, data);
      if (this.documentReference === meta.documentReference && this.version !== meta.version) {
        // Update the meta and the hidden fields used by the edit form in order to ensure proper merge on save.
        meta.setVersion(this.version);
        $('#editingVersionDate').val(this.modified);
        $('#isNew').val(this.isNew);
      }
      return this;
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
      }, data), true)).then(this.reload.bind(this));
    },

    getChannels: function(params) {
      const url = new XWiki.Document(this.documentReference).getRestURL('channels');
      params = $.extend({
        // Make sure the response is not retrieved from cache (IE11 doesn't obey the caching HTTP headers).
        timestamp: new Date().getTime()
      }, params);
      return $.getJSON(url, $.param(params, true)).then(function(data) {
        if (!Array.isArray(data)) {
          console.error('Failed to retrieve the list of document channels.');
          return Promise.reject(data);
        } else {
          return $.extend(data, channelListAPI);
        }
      });
    }
  };

  // Extend with document fields coming from the real-time configuration.
  return $.extend(document, realtimeConfig.document);
});
