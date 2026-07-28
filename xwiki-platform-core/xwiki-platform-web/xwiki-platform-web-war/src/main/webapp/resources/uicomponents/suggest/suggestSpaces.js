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
#set ($spaceIcon = $services.icon.getMetaData('folder'))
#set ($webHome = $services.model.getEntityReference('DOCUMENT', 'default').name)
#[[*/
// Start JavaScript-only code.
(function(spaceIcon, webHome) {
  "use strict";

define('xwiki-suggestSpaces', ['jquery', 'xwiki-selectize'], function($) {
  webHome = webHome || 'WebHome';

  // How many suggestions we show at most.
  var limit = 10;

  var getSelectizeOptions = function(select) {
    return {
      create: true,
      // The document where the selected values are saved. Stored space references will be relative to the wiki of this
      // document.
      documentReference: select.data('documentReference'),
      // Where to look for spaces. The following is supported:
      // * "wiki:wikiName" look for spaces in the specified wiki
      // * "space:spaceReference" look for nested spaces of the specified space
      searchScope: select.data('searchScope'),
      // The value is a space reference so, unlike for pages, there's no technical "WebHome" name to hide from the
      // matching. We can match on the value directly.
      searchField: ['value', 'label', 'hint'],
      load: function(text, callback) {
        loadSpaces(text, this.settings).then(callback, callback);
      },
      loadSelected: function(value, callback) {
        loadSpace(value, this.settings).then(callback, callback);
      }
    };
  };

  var processOptions = function(options) {
    // Resolve the document reference relative to the current document reference.
    if (!options.documentReference || typeof options.documentReference === 'string') {
      options.documentReference = XWiki.Model.resolve(options.documentReference, XWiki.EntityType.DOCUMENT,
        XWiki.currentDocument.documentReference);
    }
    // Resolve the search scope. Fall back to the current wiki if the given scope can't be resolved.
    options.searchScope = resolveEntityReference(options.searchScope || 'wiki:' + XWiki.currentWiki) ||
      resolveEntityReference('wiki:' + XWiki.currentWiki);
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

  /**
   * Looks for spaces matching the given text using two complementary search sources, because neither of them is enough
   * on its own:
   * <ul>
   *   <li>searching for pages matches the title of the space home page (i.e. the name the space is displayed with) but
   *     not the technical space name, because the name of the page backing a space is always "WebHome";</li>
   *   <li>searching for spaces matches the technical space name but returns neither the pretty names nor the
   *     hierarchy.</li>
   * </ul>
   */
  var loadSpaces = function(text, options) {
    return $.when(loadSpacesFromPages(text, options), loadSpacesFromSpaces(text, options))
      .then(function(spacesFromPages, spacesFromSpaces) {
        return removeDuplicates(spacesFromPages.concat(spacesFromSpaces)).slice(0, limit);
      });
  };

  var loadSpacesFromPages = function(text, options) {
    return $.getJSON(getRestSearchURL(options.searchScope), $.param({
      q: text,
      scope: ['name', 'title'],
      // The search doesn't know about spaces so we have to filter out the terminal pages ourselves, which means we need
      // to ask for more results than we display.
      number: limit * 4,
      localeAware: true,
      prettyNames: true
    }, true)).then(function(response) {
      var pages = Array.isArray(response.searchResults) ? response.searchResults : [];
      // Only the non-terminal pages, i.e. the pages backing a space, are of interest here.
      return pages.filter(function(page) {
        return page.pageName === webHome;
      }).map(processPage.bind(null, options));
    }, function() {
      return [];
    });
  };

  var loadSpacesFromSpaces = function(text, options) {
    return $.getJSON(getRestSearchURL(options.searchScope), $.param({
      q: text,
      scope: 'spaces',
      number: limit * 2
    })).then(function(response) {
      var spaces = Array.isArray(response.searchResults) ? response.searchResults : [];
      return spaces.map(function(space) {
        return createSuggestion(options, resolveSpaceReference(space.space, space.wiki));
      });
    }, function() {
      return [];
    });
  };

  /**
   * Loads a space that is already selected, in order to display it with its pretty name and hierarchy.
   */
  var loadSpace = function(value, options) {
    var spaceReference = XWiki.Model.resolve(value, XWiki.EntityType.SPACE, options.documentReference);
    var homeReference = new XWiki.EntityReference(webHome, XWiki.EntityType.DOCUMENT, spaceReference);
    return $.getJSON(new XWiki.Document(homeReference).getRestURL(), $.param({
      prettyNames: true
    })).then(function(page) {
      // An array is expected in xwiki.selectize.js
      return [processPage(options, page)];
    }, function() {
      // The home page of the space may not exist, or may not be viewable. Fall back on the reference itself.
      return [createSuggestion(options, spaceReference)];
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

  var resolveSpaceReference = function(localSpaceReference, wiki) {
    return XWiki.Model.resolve(localSpaceReference, XWiki.EntityType.SPACE, [wiki]);
  };

  /**
   * Adapts a page returned by the REST search or by the page resource to the format expected by the Selectize widget.
   * The page is expected to be the home page of a space.
   */
  var processPage = function(options, page) {
    var spaceReference = resolveSpaceReference(page.space, page.wiki);
    var hierarchy = (page.hierarchy && page.hierarchy.items) || [];
    var labels = hierarchy.filter(function(item) {
      return item.type === 'space';
    }).map(function(item) {
      return item.label;
    });
    return createSuggestion(options, spaceReference, labels);
  };

  /**
   * Adapts a space reference to the format expected by the Selectize widget. Exposed by this module so that the other
   * ways of picking a space (e.g. browsing a document tree) can produce suggestions that look exactly like the ones
   * suggested here.
   *
   * @param options the Selectize settings, used to know the document the value is saved in
   * @param spaceReference the reference of the space to suggest
   * @param labels the labels of the spaces in the hierarchy, the last one being the label of the given space; when not
   *   specified the names from the space reference are used instead
   */
  var createSuggestion = function(options, spaceReference, labels) {
    if (!labels || !labels.length) {
      labels = spaceReference.getReversedReferenceChain().filter(function(component) {
        return component.type === XWiki.EntityType.SPACE;
      }).map(function(component) {
        return component.name;
      });
    }
    var relativeReference = spaceReference.relativeTo(options.documentReference.getRoot());
    var homeReference = new XWiki.EntityReference(webHome, XWiki.EntityType.DOCUMENT, spaceReference);
    return {
      value: XWiki.Model.serialize(relativeReference),
      label: labels[labels.length - 1],
      // The hierarchy of the space, without the space itself.
      hint: labels.slice(0, -1).join(' / '),
      icon: spaceIcon,
      url: new XWiki.Document(homeReference).getURL()
    };
  };

  var removeDuplicates = function(suggestions) {
    var seen = {};
    return suggestions.filter(function(suggestion) {
      if (Object.hasOwn(seen, suggestion.value)) {
        return false;
      }
      seen[suggestion.value] = true;
      return true;
    });
  };

  $.fn.suggestSpaces = function(options) {
    return this.each(function() {
      var actualOptions = $.extend(getSelectizeOptions($(this)), options);
      $(this).xwikiSelectize(processOptions(actualOptions));
    });
  };

  return {
    createSuggestion: createSuggestion
  };
});

require(['jquery', 'xwiki-suggestSpaces', 'xwiki-events-bridge'], function($) {
  var init = function(event, data) {
    var container = $((data && data.elements) || document);
    container.find('.suggest-spaces').suggestSpaces();
  };

  $(document).on('xwiki:dom:loaded xwiki:dom:updated', init);
  $(init);
});

// End JavaScript-only code.
}).apply(']]#', $jsontool.serialize([$spaceIcon, $webHome]));
