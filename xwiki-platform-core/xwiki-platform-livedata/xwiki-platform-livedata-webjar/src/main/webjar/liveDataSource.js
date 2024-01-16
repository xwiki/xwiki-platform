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

define('xwiki-livedata-source', ['module', 'jquery'], function(module, $) {
  'use strict';

  function init() {
    var baseURL = module.config().contextPath + '/rest/liveData/sources/';

    // Hold the most recent request to update the Live Data entries. It can be aborted. For instance, if some criteria 
    // (e.g., filtering or sorting) are changed before the previous request is completed. 
    let entriesRequest;

    var getEntries = function(liveDataQuery) {
      var entriesURL = getEntriesURL(liveDataQuery.source);

      var parameters = {
        properties: liveDataQuery.properties,
        offset: liveDataQuery.offset,
        limit: liveDataQuery.limit
      };
      // Add filters.
      parameters.matchAll = [];
      liveDataQuery.filters.forEach(filter => {
        if (filter.matchAll) {
          parameters.matchAll.push(filter.property);
        }
        parameters['filters.' + filter.property] = filter.constraints
          .filter(constraint => constraint.value !== undefined)
          .map(constraint => {
            if (constraint.operator === undefined) {
              constraint.operator = "";
            }
            return constraint;
          })
          .map(constraint => constraint.operator + ':' + constraint.value);
      });
      // Add sort.
      parameters.sort = liveDataQuery.sort.map(sort => sort.property);
      parameters.descending = liveDataQuery.sort.map(sort => sort.descending);

      // We abort previous requests to avoid a race condition. It can happen that getEntries is called twice in a short
      // time (when the user is typing in a filter field for instance, quickly changing sorting, or just if the network 
      // is slow) and that the first request succeeds after the second request, and its results would replace the
      // "fresher" state.
      entriesRequest?.abort();
      entriesRequest = $.getJSON(entriesURL, $.param(parameters, true));

      return Promise.resolve(entriesRequest.then(toLiveData))
        .finally(cleanupRequest.bind(null, entriesRequest));
    };

    function cleanupRequest(requestToClean) {
      // We reset the request object to null for two reasons:
      // - avoid keeping an object we don't need anymore in memory, preventing it from being GC'd
      // - make sure we don't attempt to abort a request that already terminated.
      //
      // We only nullify the request if it is the request we just handled.
      // Otherwise, this means that a fresher request is in flight. In which case
      // we need to be able to abort this fresher one if yet another request is
      // fired before it succeeds.
      if (requestToClean === entriesRequest) {
        entriesRequest = null;
      }
    }

    var getEntriesURL = function(source) {
      var entriesURL = baseURL + encodeURIComponent(source.id) + '/entries';
      return addSourcePathParameters(source, entriesURL);
    };

    function addSourcePathParameters(source, url) {
      var parameters = {
        // Make sure the response is not retrieved from cache (IE11 doesn't obey the caching HTTP headers).
        timestamp: new Date().getTime(),
        namespace: `wiki:${XWiki.currentWiki}`
      };
      addSourceParameters(parameters, source);
      return `${url}?${$.param(parameters, true)}`;
    }

    function getEntryPropertyURL(source, entryId, propertyId) {
      const encodedSourceId = encodeURIComponent(source.id);
      const encodedEntryId = encodeURIComponent(entryId);
      const encodedPropertyId = encodeURIComponent(propertyId);
      const url = `${baseURL}${encodedSourceId}/entries/${encodedEntryId}/properties/${encodedPropertyId}`
      return addSourcePathParameters(source, url);
    }

    function getEntryURL(source, entryId) {
      const encodedSourceId = encodeURIComponent(source.id);
      const encodedEntryId = encodeURIComponent(entryId);
      const url = `${baseURL}${encodedSourceId}/entries/${encodedEntryId}`
      return addSourcePathParameters(source, url);
    }

    var addSourceParameters = function(parameters, source) {
      $.each(source, (key, value) => {
        if (key !== 'id') {
          parameters['sourceParams.' + key] = value;
        }
      });
    };

    var toLiveData = function(data) {
      return {
        count: data.count,
        entries: data.entries.map(entry => entry.values)
      };
    };

    var addEntry = function(source, entry) {
      return Promise.resolve($.post(getEntriesURL(source), entry).then(e => e.values));
    };

    function updateEntry(source, entryId, values) {
      return Promise.resolve($.ajax({
        type: 'PUT',
        url: getEntryURL(source, entryId),
        contentType: 'application/json',
        data: JSON.stringify({values})
      }));
    }

    var getTranslations = function(locale, prefix, keys) {
      const translationsURL = `${module.config().contextPath}/rest/wikis/${XWiki.currentWiki}/localization/translations`;
      return Promise.resolve($.getJSON(translationsURL, $.param({
        locale: locale,
        prefix: prefix,
        key: keys
      }, true)).then(toTranslationsMap));
    };

    var toTranslationsMap = function(responseJSON) {
      var translationsMap = {};
      responseJSON.translations?.forEach(translation => translationsMap[translation.key] = translation.rawSource);
      return translationsMap;
    };

    function updateEntryProperty(source, entryId, propertyId, propertyValue) {
      return Promise.resolve($.ajax({
        type: 'PUT',
        url: getEntryPropertyURL(source, entryId, propertyId),
        contentType: 'text/plain',
        data: `${propertyValue}`
      }))
    }

    return {
      getEntries,
      addEntry,
      updateEntry,
      updateEntryProperty,
      getTranslations
    };
  }

  return {init};
});
