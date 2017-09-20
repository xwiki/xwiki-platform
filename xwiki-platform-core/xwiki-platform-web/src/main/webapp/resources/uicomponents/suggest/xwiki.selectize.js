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
    'selectize': "$!services.webjars.url('org.webjars:selectize.js', 'js/standalone/selectize.min')"
  },
  shim: {
    'selectize': ['jquery']
  }
});

define('xwiki-selectize', ['jquery', 'selectize', 'xwiki-events-bridge'], function($) {
  'use strict';

  var optionTemplate = [
    '<div class="xwiki-selectize-option" data-value="">',
      '<span class="xwiki-selectize-option-icon" />',
      '<span class="xwiki-selectize-option-label" />',
    '</div>'
  ].join('');

  var renderOption = function(option) {
    var output = $(optionTemplate);
    var value = (option && typeof option === 'object') ? option.value : option;
    output.attr('data-value', value);
    var icon = option && option.icon;
    if (typeof icon === 'string') {
      if (icon.indexOf('/') >= 0 || icon.indexOf('.') >= 0) {
        // The icon is specified by its path.
        var image = $('<img class="xwiki-selectize-option-icon" alt="" />').attr('src', icon);
        output.find('.xwiki-selectize-option-icon').replaceWith(image);
      } else {
        // The icon is specified by its CSS class.
        output.find('.xwiki-selectize-option-icon').addClass(icon);
      }
    }
    var label = (option && typeof option === 'object') ? (option.label || option.value) : option;
    output.find('.xwiki-selectize-option-label').text(label);
    return output;
  };

  var defaultOptions = {
    highlight: false,
    loadThrottle: 500,
    persist: false,
    preload: 'focus',
    render: {
      item: renderOption,
      option: renderOption,
      option_create: function(data, escapeHTML) {
        // TODO: Use translation key here.
        var text = 'Select {0} ...';
        var output = $('<div class="create"/>').html(escapeHTML(text).replace('{0}', '<em/>'));
        output.find('em').text(data.input);
        return output;
      }
    },
    searchField: ['value', 'label']
  };

  var getSelectizeOptions = function(element) {
    return {
      maxItems: element.prop('multiple') ? null : 1,
      options: element.children('option').filter(function() {
        return $(this).val() && !$(this).prop('disabled');
      }).map(function() {
        return {
          value: $(this).val(),
          label: $(this).text(),
          icon: $(this).attr('data-icon')
        };
      }),
      placeholder: element.attr('placeholder')
    };
  };

  $.fn.xwikiSelectize = function(options) {
    return this.each(function() {
      $(this).selectize($.extend({}, defaultOptions, getSelectizeOptions($(this)), options));
      $(this).on('change', function(event) {
        // Update the live table if the widget is used as a live table filter.
        var liveTableId = $(this).closest('.xwiki-livetable-display-header-filter')
          .closest('.xwiki-livetable').attr('id');
        liveTableId && $(document).trigger("xwiki:livetable:" + liveTableId + ":filtersChanged");
      });
    });
  };
});
