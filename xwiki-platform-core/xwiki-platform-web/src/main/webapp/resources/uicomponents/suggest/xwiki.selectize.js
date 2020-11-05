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

define('xwiki-selectize', ['jquery', 'selectize', 'xwiki-events-bridge'], function($, Selectize) {
  'use strict';

  var optionTemplate = [
    '<div class="xwiki-selectize-option" data-value="">',
      '<span class="xwiki-selectize-option-icon" />',
      '<span class="xwiki-selectize-option-label" />',
    '</div>'
  ].join('');

  var renderCommon = function(option) {
    var output = $(optionTemplate);
    var value = (option && typeof option === 'object') ? option.value : option;
    output.attr('data-value', value);
    var icon = option && option.icon;
    if (typeof icon === 'object') {
      // Set the icon set type in case it was missing.
      if (!icon.iconSetType && icon.url) {
        icon.iconSetType = 'IMAGE';
      } else if (!icon.iconSetType && icon.cssClass) {
          icon.iconSetType = 'FONT';
      }
      // Render the icon depending on the icon set type.
      if (icon.iconSetType === 'IMAGE') {
        var image = $('<img class="xwiki-selectize-option-icon" alt="" />').attr('src', icon.url);
        output.find('.xwiki-selectize-option-icon').replaceWith(image);
      } else if (icon.iconSetType === 'FONT') {
        output.find('.xwiki-selectize-option-icon').addClass(icon.cssClass);
      } else {
        output.find('.xwiki-selectize-option-icon').remove();
      }
    } else {
      output.find('.xwiki-selectize-option-icon').remove();
    }
    var url = option && option.url;
    if (typeof url === 'string') {
      var anchor = $('<a class="xwiki-selectize-option-label" />').attr('href', url);
      output.find('.xwiki-selectize-option-label').replaceWith(anchor);
    }
    var label = (option && typeof option === 'object') ? (option.label || option.value) : option;
    output.find('.xwiki-selectize-option-label').text(label);
    return output;
  };

  var renderOption = function(option) {
    var output = renderCommon(option);
    // This class is needed starting with v0.12.5 in order to have proper styling.
    output.addClass('option');
    // We need a wrapper around the icon in order to center it because it looks better if the labels are aligned when
    // the suggestions are displayed on separate lines in the drop down list. We don't need to center the icon for the
    // selected suggestions because they are displayed in-line so the labels don't have to be aligned.
    output.find('.xwiki-selectize-option-icon').wrap('<span class="xwiki-selectize-option-icon-wrapper"/>');
    var hint = option && option.hint;
    if (typeof hint === 'string' && hint !== '') {
      output.append($('<div class="xwiki-selectize-option-hint"/>').text(hint));
    }
    return output;
  }

  var renderItem = function(option) {
    var output = renderCommon(option);
    var hint = option && option.hint;
    if (typeof hint === 'string' && hint !== '') {
      output.attr('title', hint);
    }
    return output;
  }

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
    onDropdownClose: function(dropdown) {
      dropdown.removeClass('active');
    },
    onDropdownOpen: function(dropdown) {
      dropdown.addClass('active');
    },
    persist: false,
    preload: 'focus',
    render: {
      item: renderItem,
      option: renderOption,
      option_create: function(data, escapeHTML) {
        var text = $jsontool.serialize($services.localization.render('web.uicomponents.suggest.selectTypedText',
          ['{0}']));
        // The 'option' class is needed starting with v0.12.5 in order to have proper styling.
        var output = $('<div class="create option"/>').html(escapeHTML(text).replace('{0}', '<em/>'));
        output.find('em').text(data.input);
        return output;
      }
    },
    searchField: ['value', 'label', 'hint'],
    onType: function(value) {
      // Mark the picker as loading if the suggestions are retrieved asynchronously and there's no cached result for the
      // given value.
      var loading = typeof this.settings.load === 'function' && !this.loadedSearches.hasOwnProperty(value);
      this.get$('wrapper').toggleClass(this.settings.loadingClass, loading);
    },
    onInitialize: function() {
      this.get$('control').on('click', 'a.xwiki-selectize-option-label', function(event) {
        // Clicking on the label should select the option not follow the link.
        event.preventDefault();
      });
    }
  };

  var loadSelectedValues = function() {
    var selectize = this.selectize;
    var wrapper = selectize.get$('wrapper');
    wrapper.addClass(selectize.settings.loadingClass);
    selectize.loading++;
    selectize.items.reduce(function(deferred, value) {
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
    var load;
    if (typeof selectize.settings.loadSelected === 'function') {
      load = selectize.settings.loadSelected;
    } else {
      load = selectize.settings.load;
    }
    if (value && typeof load === 'function') {
      load.call(selectize, value, function(options) {
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

  var customize = function() {
    var oldPositionDropdown = this.selectize.positionDropdown;
    this.selectize.positionDropdown = function() {
      // We don't need to recompute the position if the dropdown is a child of the selectize widget.
      if (this.settings.dropdownParent === 'body') {
        oldPositionDropdown.call(this);
      }
      // 'auto' means the dropdown width should be variable based on its content, but no less than the width of the
      // specified form field, otherwise the dropdown should have the same width as the form field.
      if (this.settings.dropdownWidth === 'auto') {
        this.get$('dropdown').css({
          width: '',
          'min-width': this.get$('control')[0].getBoundingClientRect().width
        });
      }
    }
    setDropDownAlignment(this.selectize);
    if (this.selectize.settings.takeInputWidth) {
      this.selectize.get$('control').width($(this).data('initialWidth'));
    }
    // Set the title of the input field.
    this.selectize.get$('control_input').attr('title', $(this).attr('title'));
  };

  var setDropDownAlignment = function(selectize) {
    var dropdownAlignment = selectize.settings.dropdownAlignment;
    if (!dropdownAlignment && selectize.settings.dropdownParent !== 'body') {
      dropdownAlignment = 'left';
    }
    if (dropdownAlignment) {
      selectize.get$('dropdown').addClass('selectize-dropdown-' + dropdownAlignment);
    }
  };

  // Hack needed in order to be able to access internal selectize fields that are prefixed with $ when the JavaScript is
  // minified, because this code is parsed with Velocity. Otherwise, if we access these fields directly by their name
  // (e.g. selectize.$control) then we might get a Velocity syntax error on the minified JavaScript file.
  Selectize.prototype.get$ = function(key) {
    return this[$jsontool.serialize($escapetool.d) + key];
  };

  var getDefaultSettings = function(input) {
    var defaultSettings = {};
    if (input.closest('.xform').length === 0) {
      defaultSettings.dropdownWidth = 'auto';
    }
    return defaultSettings;
  };

  var getSettings = function(input, settings) {
    return $.extend({}, defaultSettings, getDefaultSettings(input), settings, input.data('xwiki-selectize'));
  };

  $.fn.xwikiSelectize = function(settings) {
    // Save the width before the input is hidden.
    // Each input in the current collection might have a different initial width.
    this.each(function() {
      $(this).data('initialWidth', $(this).width());
    });
    return this.not('.selectized, .selectize-control')
      .on('initialize', customize)
      .on('change', function(event) {
        // Update the live table if the widget is used as a live table filter.
        var liveTableId = $(this).closest('.xwiki-livetable-display-header-filter')
          .closest('.xwiki-livetable').attr('id');
        liveTableId && $(document).trigger("xwiki:livetable:" + liveTableId + ":filtersChanged");
      })
      // Each input in the current collection might have different in-line settings.
      .each(function() {
        $(this).selectize(getSettings($(this), settings));
      })
      .trigger('initialize')
      .each(loadSelectedValues);
  };
});

require(['jquery', 'xwiki-selectize', 'xwiki-events-bridge'], function($) {
  // Make sure we don't initialize the selectize widgets twice because this file can be loaded twice (by RequireJS and as
  // a JSFX resource).
  if ($.fn.xwikiSelectize.initialized) {
    return;
  }
  $.fn.xwikiSelectize.initialized = true;

  var init = function(event, data) {
    var container = $((data && data.elements) || document);
    container.find('.xwiki-selectize').xwikiSelectize();
  };

  $(document).on('xwiki:dom:updated', init);
  $(init);
});
