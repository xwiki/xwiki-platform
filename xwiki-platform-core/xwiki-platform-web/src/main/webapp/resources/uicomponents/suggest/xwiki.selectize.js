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
    } else {
      output.find('.xwiki-selectize-option-icon').remove();
    }
    var url = option && option.url;
    if (typeof url === 'string') {
      var anchor = $('<a class="xwiki-selectize-option-label" />').attr('href', url).click(function(event) {
        // Clicking on the label should select the option not follow the link.
        event.preventDefault();
      });
      output.find('.xwiki-selectize-option-label').replaceWith(anchor);
    }
    var label = (option && typeof option === 'object') ? (option.label || option.value) : option;
    output.find('.xwiki-selectize-option-label').text(label);
    return output;
  };

  var defaultSettings = {
    // Copying the CSS classes from the form field to the dropdown can have unexpected side effects if those classes are
    // not meant to be applied on the dropdown (e.g. live table active filter).
    copyClassesToDropdown: false,
    // Append the drop down list to the BODY element as otherwise we may get scroll bars if the parent of the selectize
    // widget has limited width or height (e.g. when you change the filter value on an empty live table).
    dropdownParent: 'body',
    // Disable the highlighting because it is buggy.
    // See for instance https://github.com/selectize/selectize.js/issues/1149 .
    highlight: false,
    labelField: 'label',
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

  var loadSelectedValues = function() {
    var values = $(this).val();
    if (!$.isArray(values)) {
      values = [values]
    }
    var selectize = this.selectize;
    var wrapper = selectize.$wrapper;
    wrapper.addClass(selectize.settings.loadingClass);
    selectize.loading++;
    values.reduce(function(deferred, value) {
      return deferred.then(function() {
        return loadSelectedValue(selectize, value);
      });
    }, $.Deferred().resolve()).always(function() {
      selectize.loading = Math.max(selectize.loading - 1, 0);
      if (!selectize.loading) {
        wrapper.removeClass(selectize.settings.loadingClass);
      }
    });
  };

  var loadSelectedValue = function(selectize, value) {
    var deferred = $.Deferred();
    if (value && typeof selectize.settings.load === 'function') {
      selectize.settings.load(value, function(options) {
        $.isArray(options) && options.forEach(function(option) {
          var value = option[selectize.settings.valueField];
          if (selectize.options.hasOwnProperty(value)) {
            selectize.updateOption(value, option);
          } else {
            selectize.addOption(option);
          }
        });
        deferred.resolve();
      });
    } else {
      deferred.resolve();
    }
    return deferred.promise();
  };

  var handleDropdownWidth = function() {
    if (getDropdownWidthSetting(this) === 'auto') {
      var oldPositionDropdown = this.selectize.positionDropdown;
      this.selectize.positionDropdown = function() {
        oldPositionDropdown.call(this);
        var dropdown = this.$dropdown;
        dropdown.css({
          width: '',
          'min-width': dropdown.css('width')
        });
      };
    }
  };

  /**
   * @param input the form field that was enhanced with the Selectize widget
   * @return 'auto' if the dropdown width should be variable based on its content, but no less than the width of the
   *         specified form field, otherwise the dropdown should have the same width as the form field
   */
  var getDropdownWidthSetting = function(input) {
    if (input.selectize.settings.hasOwnProperty('dropdownWidth')) {
      return this.selectize.settings.dropdownWidth;
    } else if ($(input).closest('.xform').length === 0) {
      return 'auto';
    }
  };

  $.fn.xwikiSelectize = function(settings) {
    var $select = this.selectize($.extend({}, defaultSettings, settings))
      .each(loadSelectedValues).each(handleDropdownWidth)
      .on('change', function(event) {
        // Update the live table if the widget is used as a live table filter.
        var liveTableId = $(this).closest('.xwiki-livetable-display-header-filter')
          .closest('.xwiki-livetable').attr('id');
        liveTableId && $(document).trigger("xwiki:livetable:" + liveTableId + ":filtersChanged");
      });
    var selectize = $select[0].selectize;
    var load = selectize.settings.load;
    var addLoading = function() {
      selectize.$control_input.addClass("loading");
    }
    var removeLoading = function() {
      selectize.$control_input.removeClass("loading");
    }
    var timer = -1;
    var clearTimer = function() {
      clearTimeout(timer);
      timer = -1;
    }
    selectize.settings.load = function(text, callback) {
      clearTimer();
      addLoading();
      load(text, callback);
    }
    selectize.on("type", function() {
      addLoading();
      clearTimeout(timer);
      timer = setTimeout(removeLoading, 1000);
    });
    selectize.on("load", function() {
      if (timer == -1) {
        removeLoading();
      }
    });

    return $select;
  };
});
