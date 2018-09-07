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
            var hierarchy = element.hierarchy.elements;
            var label = hierarchy.pop();
            if (element.pageName === 'WebHome') {
              label = hierarchy.pop();
            }
            var hint = hierarchy.join(' / ');
            var doc = new XWiki.Document(XWiki.Model.resolve(element.id, XWiki.EntityType.DOCUMENT))
            var url = doc.getURL("view");
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
    var url = XWiki.Document.WikiSearchURLStub.replace("__wiki__", XWiki.Document.currentWiki);
    return $.getJSON(url, 'q=' + text + '&scope=name&scope=title&number=10&media=json');
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
