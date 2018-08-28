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

  var getSelectizeOptions = function(select) {
    return {
      create: true,
      load: function(text, callback) {
        loadPages(text).done(function(data) {
          var pages = [];
          data.searchResults.forEach(function (element) {
            var label = element.title;
            var queryString = "";
            // Separate spaces with " / " and unescape characters. E.g. A\.B.C\\D => A.B / C\D
            var hint = element.space.replace(/([^\\])\./g, "$1 / ").replace(/\\(.)/g, "$1");
            if (element.language) {
              queryString = "language=" + element.language;
              hint += " (" + element.language + ")";
            }
            var url = new XWiki.Document(XWiki.Model.resolve(element.id, XWiki.EntityType.DOCUMENT))
              .getURL("view", queryString);
            pages.push({
              'value': element.pageFullName,
              'label': label,
              'icon': pageIcon,
              'url': url,
              'hint': hint
            });
          });
          callback(pages);
        }).fail(callback);
      }
    }
  };

  var loadPages = function(text) {
    // We need to escape backslashes for Solr.
    text = text.replace(/\\/g, "\\\\");
    var response = $.getJSON("${request.contextPath}/rest/wikis/query/", {
      'q': "title:*" + text + "* or fullname:*" + text + "*",
      'limit': 10,
      'media': 'json'
    });

    return response;
  };

  $.fn.suggestPages = function() {
    return this.each(function() {
      $(this).xwikiSelectize(getSelectizeOptions($(this)));
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
