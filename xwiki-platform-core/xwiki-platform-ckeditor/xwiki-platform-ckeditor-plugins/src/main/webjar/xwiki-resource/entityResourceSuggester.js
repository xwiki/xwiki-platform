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
define('entityResourceSuggesterTranslationKeys', [], [
  'doc.placeholder',
  'attach.placeholder',
  'create.resolvedInSpace',
  'create.fullyResolvedInput'
]);

define('entityResourceSuggester', [
  'jquery', 'resource', 'l10n!entityResourceSuggester'
], function($, $resource, translations) {
  'use strict';

  var search = function(query, input, deferred, entityType, base) {  
    $.post(new XWiki.Document('SuggestSolrService', 'XWiki').getURL('get'), {
      outputSyntax: 'plain',
      language: $('html').attr('lang'),
      query: query.join('\n'),
      input: input,
      nb: 8
    }).done(function(response) {
      if (response.documentElement) {
        let results = response.getElementsByTagName('rs');
        let suggestions = [];
        let containsExactMatch = false;
        for (var i = 0; i < results.length; i++) {
          let result = convertSearchResultToResource(results.item(i), entityType, base);
          containsExactMatch = result.entityReference === input;
          suggestions.push(result);
        }
        if (!containsExactMatch) {
          suggestCreateDocument(input, base, suggestions, deferred);
        } else {
          deferred.resolve(suggestions);
        }
      } else {
        deferred.resolve([]);
      }
    }).fail(function() {
      deferred.resolve([]);
    });
  };

  var suggestCreateDocument = function(input, base, suggestions, deferred) {
    $.post(new XWiki.Document('LinkNameStrategyHelper', 'CKEditor').getURL('get'), {
      outputSyntax: 'plain',
      input: input,
      base: XWiki.Model.serialize(base)
    }).done(function(data) {
      data.forEach(function (item) {
        suggestions.push(createDocumentFromLinkNameStrategyHelperResult(item, base));
      });
      deferred.resolve(suggestions);
    }).fail(function(data) {
      console.error("Error while loading creation link response", data);
      deferred.resolve(suggestions);
    });
  };

  var createDocumentFromLinkNameStrategyHelperResult = function (item, base) {
    var entityReference = XWiki.Model.resolve(item.reference, XWiki.EntityType.DOCUMENT);
    return {
      reference: $resource.convertEntityReferenceToResourceReference(entityReference, base),
      entityReference: entityReference,
      title: translations.get('create.' + item.type),
      location: ''
    };
  };

  var convertSearchResultToResource = function(result, expectedEntityType, base) {
    var entityReference, location, title = result.childNodes[0].nodeValue;
    var entityType = result.getAttribute('type');
    var serializedEntityReference = result.getAttribute('id');
    entityReference = XWiki.Model.resolve(serializedEntityReference, XWiki.EntityType.byName(entityType));
    location = result.getAttribute('info');
    return {
      reference: $resource.convertEntityReferenceToResourceReference(entityReference, base),
      entityReference: entityReference,
      title: title,
      location: location
    };
  };

  var display = function(resource) {
    var suggestion = $(
      '<span class="resource-label"><span class="resource-icon"></span> </span>' +
      '<span class="resource-hint"></span>'
    );
    suggestion.find('.resource-icon').addClass($resource.types[resource.reference.type].icon);
    suggestion.first().append(document.createTextNode(resource.title)).next().html(resource.location);
    // Remove the home icon from the resource location displayed as hint because it distracts the user and it is
    // redundant. The home icon is useful to navigate to the home page but the resource suggestion hint is not used for
    // navigation so the home icon is not needed. We know that every path starts from home.
    suggestion.last().text(function(index, text) {
      return text.substr(0, 3) === ' / ' ? text.substr(3) : text;
    });
    return suggestion;
  };

  var advancedSearchPattern = /[+\-!(){}\[\]\^"~*?:\\]+/;

  $resource.types.doc.placeholder = translations.get('doc.placeholder');
  $resource.suggesters.doc = {
    retrieve: function(resourceReference, base) {
      var deferred = $.Deferred();
      var query = [
        'q=__INPUT__',
        'fq=type:DOCUMENT'
      ];
      var input = resourceReference.reference.trim();
      if (input) {
        query.push('qf=title^2 name');
        if (!advancedSearchPattern.test(input)) {
          query[0] = 'q=(__INPUT__)^2 __INPUT__*';
        }
      } else {
        // Recently modified pages.
        input = '*:*';
        query.push('sort=date desc');
      }
      search(query, input, deferred, XWiki.EntityType.DOCUMENT, base);
      return deferred.promise();
    },
    display: display
  };

  $resource.types.attach.placeholder = translations.get('attach.placeholder');
  $resource.suggesters.attach = {
    retrieve: function(resourceReference, base) {
      var deferred = $.Deferred();
      var query = [
        'q=__INPUT__',
        'fq=type:ATTACHMENT'
      ];
      var input = resourceReference.reference.trim();
      if (input) {
        query.push('qf=filename');
        if (!advancedSearchPattern.test(input)) {
          query[0] = 'q=(__INPUT__)^2 __INPUT__*';
        }
      } else {
        // Recently modified attachments.
        input = '*:*';
        query.push('sort=attdate_sort desc');
      }
      search(query, input, deferred, XWiki.EntityType.ATTACHMENT, base);
      return deferred.promise();
    },
    display: display
  };
});
