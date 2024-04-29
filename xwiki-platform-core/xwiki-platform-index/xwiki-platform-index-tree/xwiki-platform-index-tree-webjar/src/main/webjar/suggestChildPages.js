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

define('xwiki-suggestChildPages', ['jquery', 'jquery-ui', 'jquery-ui-touch-punch', 'xwiki-selectize'], function($) {
  webHome = webHome || 'WebHome';

  function getSelectizeOptions(select) {
    return {
      // TODO: Remove this once we add the remote source (REST API call).
      create: true,
      plugins: ['drag_drop'],
      // Where to look for child pages. The following is supported:
      // * "wiki:wikiName" look for top level pages in the specified wiki
      // * "space:spaceReference" look for child pages in the specified space
      parentReference: select.data('parent'),
      load: function(text, callback) {
        loadChildPages(text, this.settings).then(callback, callback);
      },
      loadSelected: function(text, callback) {
        loadChildPage(text, this.settings).then(callback, callback);
      }
    };
  }

  function processOptions(options) {
    // Resolve the parent reference.
    options.parentReference = XWiki.Model.resolve(options.parentReference || 'wiki:' + XWiki.currentWiki, null,
      XWiki.currentDocument.documentReference);
    return options;
  }

  function loadChildPages(text, options) {
    // TODO: Use the REST API to get the child pages.
    return Promise.resolve([]).then(processChildPages.bind(null, options));
  }

  function loadChildPage(value, options) {
    const isTerminal = options.parentReference.type === XWiki.EntityType.SPACE && !value.endsWith('/');
    const childPageName = decodeURIComponent(value.endsWith('/') ? value.substring(0, value.length - 1) : value);
    let childPageReference;
    if (isTerminal) {
      childPageReference = new XWiki.EntityReference(childPageName, XWiki.EntityType.DOCUMENT, options.parentReference);
    } else {
      childPageReference = new XWiki.EntityReference(webHome, XWiki.EntityType.DOCUMENT,
        new XWiki.EntityReference(childPageName, XWiki.EntityType.SPACE, options.parentReference));
    }
    const childPageURL = new XWiki.Document(childPageReference).getRestURL();
    return $.getJSON(childPageURL, $.param({
      prettyNames: true
    })).then(processChildPage.bind(null, options)).then(function(childPage) {
      // Preserve the value (i.e. don't normalize the value).
      childPage.value = value;
      // An array is expected in xwiki.selectize.js
      return [childPage];
    });
  }

  /**
   * Adapt the JSON returned by the REST call to the format expected by the Selectize widget.
   */
  function processChildPages(options, response) {
    if (Array.isArray(response.searchResults)) {
      return response.searchResults.map(processPage.bind(null, options));
    } else {
      return [];
    }
  }

  function processChildPage(options, childPage) {
    // Value
    const childPageReference = XWiki.Model.resolve(childPage.id, XWiki.EntityType.DOCUMENT);
    // We're going to add a slash at the end of the page name for nested child pages in order to distinguish them from
    // terminal child pages. This means we need to escape the slash that may appear in the page name. We chose to use a
    // partial URL escaping because it's easy to decode.
    let childPageName = childPageReference.name === webHome ? childPageReference.parent.name : childPageReference.name;
    childPageName = childPageName.replaceAll('%', '%25').replaceAll('/', '%2F');
    if (childPageReference.name === webHome) {
      childPageName += '/';
    }
    // Label
    const hierarchy = childPage.hierarchy.items;
    let childPageTitle = hierarchy.pop().label;
    if (childPageReference.name === webHome) {
      childPageTitle = hierarchy.pop().label;
    }
    return {
      value: childPageName,
      label: childPageTitle,
      icon: pageIcon,
      url: new XWiki.Document(childPageReference).getURL()
    };
  }

  $.fn.suggestChildPages = function(options) {
    return this.each(function() {
      const actualOptions = $.extend(getSelectizeOptions($(this)), options);
      $(this).xwikiSelectize(processOptions(actualOptions));
    });
  };
});

require(['jquery', 'xwiki-suggestChildPages', 'xwiki-events-bridge'], function($) {
  function init(event, data) {
    const container = $(data?.elements || document);
    container.find('.suggest-childPages').suggestChildPages();
  }

  $(document).on('xwiki:dom:loaded xwiki:dom:updated', init);
  $(init);
});

// End JavaScript-only code.
}).apply(']]#', $jsontool.serialize([$pageIcon, $webHome]));
