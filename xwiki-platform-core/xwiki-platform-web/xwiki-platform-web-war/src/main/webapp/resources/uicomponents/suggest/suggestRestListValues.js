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

/**
 * Values listed by a REST resource, such as tags and wikis.
 */
define('xwiki-suggestRestListValues', ['jquery', 'xwiki-entityReference', 'xwiki-selectize'], function($, XWiki) {
  /**
   * Builds the selectize options for a REST resource that returns the list of values under the given response key,
   * each value having a name.
   */
  function getSelectizeOptions(restPath, responseKey) {
    const loadURL = [XWiki.contextPath, 'rest', ...restPath].join('/');
    async function load(text, callback) {
      try {
        const response = await $.getJSON(loadURL);
        const values = Array.isArray(response?.[responseKey]) ? response[responseKey] : [];
        callback(values.map(value => ({value: value.name})));
      } catch {
        callback();
      }
    }
    return {create: true, load, loadSelected: load};
  }

  $.fn.suggestRestListValues = function(restPath, responseKey) {
    return this.each(function() {
      $(this).xwikiSelectize(getSelectizeOptions(restPath, responseKey));
    });
  };
});

require(['jquery', 'xwiki-entityReference', 'xwiki-suggestRestListValues', 'xwiki-events-bridge'], function($, XWiki) {
  function init(event, data) {
    const container = $(data?.elements || document);
    container.find('.suggest-tags')
      .suggestRestListValues(['wikis', encodeURIComponent(XWiki.currentWiki), 'tags'], 'tags');
    container.find('.suggest-wikis').suggestRestListValues(['wikis'], 'wikis');
  }

  $(document).on('xwiki:dom:loaded xwiki:dom:updated', init);
  $(init);
});
