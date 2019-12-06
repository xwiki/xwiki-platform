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
require.config({
  paths: {
    'xwiki-selectize': "$xwiki.getSkinFile('uicomponents/suggest/xwiki.selectize.js', true)" +
      "?v=$escapetool.url($xwiki.version)"
  }
});

define('xwiki-suggestPages', ['jquery', 'xwiki-selectize'], function($) {
  'use strict';

  var pageIcon = $jsontool.serialize($services.icon.getMetaData('page_white'));
  var webHome = "$!services.model.getEntityReference('DOCUMENT', 'default').name" || 'WebHome';

  var getSelectizeOptions = function(select) {
    return {
      create: true,
      // The document where the selected values are saved. Stored document references will be relative to this document.
      documentReference: select.data('documentReference'),
      // Where to look for pages. The following is supported:
      // * "wiki:wikiName" look for pages in the specified wiki
      // * "space:spaceReference": look for pages in the specified space
      searchScope: select.data('searchScope'),
      load: function(text, callback) {
        loadPages(text, this.settings).done(callback).fail(callback);
      },
      loadSelected: function(text, callback) {
        loadPage(text, this.settings).done(callback).fail(callback);
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
    var scopes = ['name', 'title'];
    return $.getJSON(getRestSearchURL(options.searchScope), $.param({
      'q': text,
      'scope': scopes,
      'number': 10,
      'localeAware': true,
      'media': 'json'
    }, true)).then($.proxy(processPages, null, options));
  };

  var loadPage = function(value, options) {
    var documentReference = XWiki.Model.resolve(value, XWiki.EntityType.DOCUMENT, options.documentReference);
    var localValue = XWiki.Model.serialize(documentReference.relativeTo(options.searchScope.getRoot()));
    // TODO: Call the document REST URL instead, when it will include the page hierarchy.
    return loadPages(localValue, options);
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
    if ($.isArray(response.searchResults)) {
      return response.searchResults.map($.proxy(processPage, null, options));
    } else {
      return [];
    }
  };

  var processPage = function(options, page) {
    // Value (relative to the current wiki, where it is saved)
    var documentReference = XWiki.Model.resolve(page.id, XWiki.EntityType.DOCUMENT);
    var value = XWiki.Model.serialize(documentReference.relativeTo(options.documentReference.getRoot()));
    // Label
    var hierarchy = page.hierarchy.items;
    var label = hierarchy.pop().label;
    if (page.pageName === webHome) {
      label = hierarchy.pop().label;
    }
    // Hint
    var hint = hierarchy.filter(function(item) {
      return item.type === 'space';
    }).map(function(item) {
      return item.label;
    }).join(' / ');
    return {
      'value': value,
      'label': label,
      'hint': hint,
      'icon': pageIcon,
      'url': new XWiki.Document(documentReference).getURL()
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
  XWiki.domIsLoaded && init();
});
