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

  // Serialize the given parameters as a query string, supporting multi-value parameters (e.g. {path: ['a', 'b']}) and
  // ignoring the parameters that have no value. Parameters that are already serialized are returned as they are.
  function toQueryString(params) {
    if (typeof params !== 'object' || params === null) {
      return params;
    }
    const queryString = new URLSearchParams();
    for (const [name, value] of Object.entries(params)) {
      for (const item of Array.isArray(value) ? value : [value]) {
        if (item !== undefined && item !== null) {
          queryString.append(name, item);
        }
      }
    }
    return queryString.toString();
  }

  // Fetch JSON from the given URL. The error thrown when the request fails carries the response status.
  async function getJSON(url) {
    const response = await fetch(url, {
      // The XWiki REST API doesn't specify how its responses should be cached, so we ask for a fresh one.
      cache: 'no-store',
      headers: {
        // Without this the XWiki REST API answers with XML.
        'Accept': 'application/json',
        // Some server side code answers differently when the request is made from JavaScript.
        'X-Requested-With': 'XMLHttpRequest'
      }
    });
    if (!response.ok) {
      const error = new Error(`Failed to fetch [${url}]. Response status: ${response.status}`);
      error.status = response.status;
      throw error;
    }
    return response.json();
  }

  function getOldAPI(xwikiDocument) {
    return (xwikiDocument.documentReference && new XWiki.Document(xwikiDocument.documentReference)) ||
      XWiki.currentDocument;
  }

  function removeNullProperties(object) {
    return Object.fromEntries(Object.entries(object).filter(([key, value]) => value != null));
  }

  // The value of a hidden field of the edit form, when that field is present.
  function getFieldValue(id) {
    return document.getElementById(id)?.value;
  }

  // Update the value of a hidden field of the edit form, when that field is present.
  function setFieldValue(id, value) {
    const field = document.getElementById(id);
    if (field) {
      field.value = value;
    }
  }

  // Generic client-side API for an XWiki document. Nothing real-time specific should go here.
  class XWikiDocument {
    // The document currently displayed by the web page, with the fields exposed by the meta information.
    static currentDocument() {
      return new this({
        documentReference: meta.documentReference,
        language: meta.locale,
        version: meta.version,
        isNew: meta.isNew
      });
    }

    constructor(data) {
      Object.assign(this, data);
    }

    async reload() {
      try {
        const updatedDocument = await getJSON(this.getRestURL());
        return this.update({
          // The REST API response includes some properties with null values, that would otherwise overwrite the
          // properties of this document that have a value set.
          ...removeNullProperties(updatedDocument),
          // We were able to load the document so it's not new.
          isNew: false
        });
      } catch (error) {
        if (error.status === 404) {
          // The document doesn't exist anymore. Maybe it was deleted?
          return this.update({
            version: '1.1',
            modified: 0,
            content: '',
            isNew: true
          });
        }
        // Otherwise the reload failed and we continue using the current data.
        return this.update();
      }
    }

    update(data) {
      Object.assign(this, data);
      this.syncCurrentDocumentState();
      return this;
    }

    // Whether this document is the one currently displayed by the web page.
    isCurrentDocument() {
      return !!this.documentReference?.equals(meta.documentReference);
    }

    // Keep the meta and the hidden fields used by the edit form in sync, in order to ensure a proper merge on save.
    syncCurrentDocumentState() {
      if (this.isCurrentDocument() && this.version !== meta.version) {
        meta.setVersion(this.version);
        setFieldValue('editingVersionDate', this.modified);
        setFieldValue('isNew', this.isNew);
      }
    }

    // This document's real locale. It differs from its (raw) locale only for the original translation, whose raw
    // locale is empty.
    get realLocale() {
      const locale = this.language;
      if (typeof locale !== 'string' || locale === '') {
        return this.defaultLocale;
      }
      return locale;
    }

    // The locale of this document's original translation. Note that it is the empty string for a technical document,
    // whose default locale is the root locale.
    get defaultLocale() {
      return this.translations?.['default'];
    }

    getURL(action, params, fragment) {
      return getOldAPI(this).getURL(action, toQueryString(params), fragment);
    }

    // The REST URL of the wiki page this document belongs to, without taking its translation into account.
    getPageRestURL(entity, params) {
      return getOldAPI(this).getRestURL(entity, toQueryString(params));
    }

    // The REST URL of this document. Note that a document translation is exposed through a different REST URL than
    // the original translation.
    getRestURL(entity, params) {
      const translationEntity = this.language && ('translations/' + encodeURIComponent(this.language));
      return this.getPageRestURL([translationEntity, entity].filter(segment => segment).join('/'), params);
    }

    getRevision(version) {
      return getJSON(this.getRestURL('history/' + encodeURIComponent(version), {
        prettyNames: true
      }));
    }
  }

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
      const currentDocument = super.currentDocument();
      const config = realtimeConfig.document || {};
      if (!currentDocument.language) {
        // We know this is the original document translation, but the meta information doesn't expose its actual
        // (real) locale, so we take it from the real-time configuration.
        currentDocument.translations = {'default': config.realLocale};
      }
      // The meta information doesn't expose the date of the last modification, which is needed to properly merge on
      // save. We keep it up to date on the edit form ourselves, see syncCurrentDocumentState().
      currentDocument.modified = Number(getFieldValue('editingVersionDate')) || config.modified;
      return currentDocument;
    }

    getChannels(params) {
      const url = this.getPageRestURL('channels', params);
      return getJSON(url).then(function(data) {
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
