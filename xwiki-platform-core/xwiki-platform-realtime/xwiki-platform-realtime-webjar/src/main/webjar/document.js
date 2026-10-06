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
  'xwiki-realtime-config',
  'xwiki-document'
], function($, meta, realtimeConfig, documentAPI) {
  'use strict';

  // Destructured here rather than in the parameter list, because a non-simple parameter list would make the
  // 'use strict' directive above illegal.
  const {XWikiDocument} = documentAPI;

  const channelListAPI = {
    getByPath: function(path) {
      return this.find(channel => JSON.stringify(channel.path) === JSON.stringify(path));
    },
    getByPathPrefix: function(pathPrefix) {
      return this.filter(channel => channel.path.length >= pathPrefix.length &&
        JSON.stringify(channel.path.slice(0, pathPrefix.length)) === JSON.stringify(pathPrefix));
    }
  };

  // Adds the real-time channels API on top of the generic XWiki document API.
  class RealtimeXWikiDocument extends XWikiDocument {
    static currentDocument() {
      // Don't call super.currentDocument() here: the Closure Compiler, which minifies this code, compiles a super
      // call made from a static method into a plain call on the parent class (XWikiDocument.currentDocument()),
      // dropping the "this" binding that the parent factory needs in order to instantiate this class rather than the
      // parent one. The minified code would then return a document without the real-time channels API.
      const currentDocument = XWikiDocument.currentDocument.call(this, meta);
      const config = realtimeConfig.document || {};
      // The meta information doesn't expose the date of the last modification, which is needed to properly merge on
      // save. We keep it up to date on the edit form ourselves, see syncCurrentDocumentState().
      currentDocument.modified = Number(XWikiDocument.getFieldValue('editingVersionDate')) || config.modified;
      return currentDocument;
    }

    getChannels(params) {
      const url = this.getPageRestURL('channels', params);
      return this.getJSON(url).then(function(data) {
        if (Array.isArray(data)) {
          return Object.assign(data, channelListAPI);
        } else {
          throw new TypeError('Invalid response from the server when requesting the list of document channels.',
            {cause: data});
        }
      }, function(error) {
        throw new Error('Failed to retrieve the list of document channels.', {cause: error});
      });
    }
  }

  // The document currently displayed by the web page.
  const xwikiDocument = RealtimeXWikiDocument.currentDocument();

  // Update the document fields before and after the document is edited inplace (without reloading the web page).
  // We need jQuery here because these events are triggered with jQuery.
  $(document).on('xwiki:actions:edit xwiki:actions:view', function(event, data) {
    xwikiDocument.update(RealtimeXWikiDocument.currentDocument());
  });

  return xwikiDocument;
});
