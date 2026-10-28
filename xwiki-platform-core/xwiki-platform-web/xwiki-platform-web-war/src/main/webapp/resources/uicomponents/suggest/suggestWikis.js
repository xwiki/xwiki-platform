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
 * Wikis
 */
define('xwiki-suggestWikis', ['jquery', 'xwiki-entityReference', 'xwiki-selectize'], function($, XWiki) {
  function getSelectizeOptions() {
    const loadURL = [XWiki.contextPath, 'rest', 'wikis'].join('/');
    // The wikis resource can't filter or paginate, but a farm holds few wikis, so all of them are listed and the
    // picker filters them on the typed text.
    async function load(text, callback) {
      try {
        const response = await $.getJSON(loadURL);
        const wikis = Array.isArray(response?.wikis) ? response.wikis : [];
        callback(wikis.map(wiki => ({value: wiki.name})));
      } catch {
        callback();
      }
    }
    return {create: true, load, loadSelected: load};
  }

  $.fn.suggestWikis = function() {
    return this.each(function() {
      $(this).xwikiSelectize(getSelectizeOptions());
    });
  };
});

require(['jquery', 'xwiki-suggestWikis', 'xwiki-events-bridge'], function($) {
  function init(event, data) {
    const container = $(data?.elements || document);
    container.find('.suggest-wikis').suggestWikis();
  }

  $(document).on('xwiki:dom:loaded xwiki:dom:updated', init);
  $(init);
});
