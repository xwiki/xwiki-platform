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
define('xwiki-suggestPropertyValues', ['jquery', 'xwiki-selectize'], function($) {
  var getSelectizeOptions = function(select) {
    var loadURL = [
      XWiki.contextPath, 'rest',
      'wikis', encodeURIComponent(XWiki.currentWiki),
      'classes', encodeURIComponent(select.attr('data-className')),
      'properties', encodeURIComponent(select.attr('data-propertyName')),
      'values'
    ].join('/');

    var getLoad = function(getOptions) {
      return function(text, callback) {
        $.getJSON(loadURL, getOptions(text)).then(function(response) {
          if (response && Array.isArray(response.propertyValues)) {
            return response.propertyValues.map(getSuggestion);
          } else {
            return [];
          }
        }).then(callback, callback);
      };
    }

    var options = {
      create: true,
      load: getLoad(function(text) {
        return { 'fp': text, 'limit': 10 }
      }),
      loadSelected: getLoad(function(text) {
        return { 'fp': text, 'exactMatch': true }
      })
    };

    var freeText = select.attr('data-freeText');
    if (freeText) {
      freeText = freeText.toLowerCase();
      if (freeText === 'allowed') {
        options.createOnBlur = true;
      } else if (freeText === 'forbidden') {
        options.create = false;
      }
    }

    return options;
  };

  var getSuggestion = function(propertyValue) {
    var metaData = propertyValue.metaData || {};
    return {
      value: propertyValue.value,
      label: metaData.label,
      icon: metaData.icon,
      url: metaData.url,
      hint: metaData.hint
    };
  };

  $.fn.suggestPropertyValues = function() {
    return this.each(function() {
      $(this).xwikiSelectize(getSelectizeOptions($(this)));
    });
  };
});

require(['jquery', 'xwiki-suggestPropertyValues', 'xwiki-events-bridge'], function($) {
  var init = function(event, data) {
    var container = $((data && data.elements) || document);
    container.find('.suggest-propertyValues').suggestPropertyValues();
  };

  $(document).on('xwiki:dom:updated', init);
  $(init);
});
