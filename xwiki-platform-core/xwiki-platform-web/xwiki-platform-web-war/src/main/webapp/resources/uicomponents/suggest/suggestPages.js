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
/*!
#set ($pageIcon = $services.icon.getMetaData('page_white'))
#set ($webHome = $services.model.getEntityReference('DOCUMENT', 'default').name)
#[[*/
// Start JavaScript-only code.
(function(pageIcon, webHome) {
  "use strict";

define('xwiki-suggestPages', ['jquery', 'xwiki-selectize'], function($) {
  webHome = webHome || 'WebHome';

  var getSelectizeOptions = function(select) {
    return {
      create: true,
      // The document where the selected values are saved. Stored document references will be relative to this document.
      documentReference: select.data('documentReference'),
      // Where to look for pages. The following is supported:
      // * "wiki:wikiName" look for pages in the specified wiki
      // * "space:spaceReference" look for pages in the specified space
      searchScope: select.data('searchScope'),
      // We overwrite the list of search fields because we don't want to match the technical "WebHome" nested page name
      // that appears in the value.
      searchField: ['searchValue', 'label', 'hint'],
      load: function(text, callback) {
        loadPages(text, this.settings).then(callback, callback);
      },
      loadSelected: function(text, callback) {
        loadPage(text, this.settings).then(callback, callback);
      }
    }
  };

  var processOptions = function(options) {
    // Resolve the document reference relative to the current document reference.
    if (!options.documentReference || typeof options.documentReference === 'string') {
      options.documentReference = XWiki.Model.resolve(options.documentReference, XWiki.EntityType.DOCUMENT,
        XWiki.currentDocument.documentReference);
    }
    // Resolve the search scope.
    options.searchScope = resolveEntityReference(options.searchScope || 'wiki:' + XWiki.currentWiki);
    return options;
  };

  /**
   * Resolves an entity reference from a string representation of the form "entityType:entityReference".
   */
  var resolveEntityReference = function(typeAndReference) {
    if (typeof typeAndReference === 'string') {
      try {
        return XWiki.Model.resolve(typeAndReference, null, XWiki.currentDocument.documentReference);
      } catch (e) {
        return null;
      }
    }
    return typeAndReference;
  };

  var loadPages = function(text, options) {
    return $.getJSON(getRestSearchURL(options.searchScope), $.param({
      q: text,
      scope: ['name', 'title'],
      number: 10,
      localeAware: true,
      prettyNames: true
    }, true)).then(processPages.bind(null, options));
  };

  var loadPage = function(value, options) {
    var documentReference = XWiki.Model.resolve(value, XWiki.EntityType.DOCUMENT, options.documentReference);
    var documentRestURL = new XWiki.Document(documentReference).getRestURL();
    return $.getJSON(documentRestURL, $.param({
      prettyNames: true
    })).then(processPage.bind(null, options)).then(function(page) {
      // An array is expected in xwiki.selectize.js
      return [page];
    });
  };

  var getRestSearchURL = function(searchScope) {
    var spaces = searchScope.getReversedReferenceChain().filter(function(component) {
      return component.type === XWiki.EntityType.SPACE;
    }).map(function(component) {
      return component.name;
    });
    var wiki = searchScope.extractReferenceValue(XWiki.EntityType.WIKI);
    return XWiki.Document.getRestSearchURL('', spaces, wiki);
  };

  /**
   * Adapt the JSON returned by the REST call to the format expected by the Selectize widget.
   */
  var processPages = function(options, response) {
    if (Array.isArray(response.searchResults)) {
      return response.searchResults.map(processPage.bind(null, options));
    } else {
      return [];
    }
  };

  var processPage = function(options, page) {
    // Value (relative to the current wiki, where it is saved)
    var documentReference = XWiki.Model.resolve(page.id, XWiki.EntityType.DOCUMENT);
    var relativeReference = documentReference.relativeTo(options.documentReference.getRoot());
    var value = XWiki.Model.serialize(relativeReference);
    var searchValue = value;
    // Label
    var hierarchy = page.hierarchy.items;
    var label = hierarchy.pop().label;
    if (documentReference.name === webHome) {
      label = hierarchy.pop().label;
      // See XWIKI-16935: Page Picker shouldn't show results matching the technical "WebHome".
      searchValue = XWiki.Model.serialize(relativeReference.parent);
    }
    // Hint
    var hint = hierarchy.filter(function(item) {
      return item.type === 'space';
    }).map(function(item) {
      return item.label;
    }).join(' / ');
    return {
      value: value,
      searchValue: searchValue,
      label: label,
      hint: hint,
      icon: pageIcon,
      url: new XWiki.Document(documentReference).getURL()
    };
  };

  $.fn.suggestPages = function(options) {
    return this.each(function() {
      var actualOptions = $.extend(getSelectizeOptions($(this)), options);
      $(this).xwikiSelectize(processOptions(actualOptions));
    });
  };
});

require(['jquery', 'xwiki-suggestPages', 'xwiki-events-bridge'], function($) {
  var init = function(event, data) {
    var container = $((data && data.elements) || document);
    container.find('.suggest-pages').suggestPages();
  };

  $(document).on('xwiki:dom:loaded xwiki:dom:updated', init);
  $(init);
});

// End JavaScript-only code.
}).apply(']]#', $jsontool.serialize([$pageIcon, $webHome]));
