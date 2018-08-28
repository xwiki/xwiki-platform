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

define('xwiki-suggestPages', ['jquery', 'xwiki-selectize'], function($, utils) {
  'use strict';

  var getSelectizeOptions = function(select) {
    return {
      create: true,
      load: function(text, callback) {
        loadPages(text).done(callback).fail(callback);
      }
    }
  };

  var loadPages = function(text) {
    // ${request.contextPath}/rest/wikis/query/
    var pages = $.getJSON(XWiki.currentDocument.getURL('get'), {
      'xpage': 'pagesuggest',
      'input': text,
      'limit': 10,
      'media': 'json'
    });

    return pages;
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
