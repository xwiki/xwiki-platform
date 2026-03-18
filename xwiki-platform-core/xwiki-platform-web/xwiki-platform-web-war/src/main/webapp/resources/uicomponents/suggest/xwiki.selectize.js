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
define('xwiki-selectize-messages', {
  prefix: 'web.uicomponents.suggest.',
  keys: [
    'selectTypedText'
  ]
});

define('xwiki-selectize', [
  'module',
  'jquery',
  'tom-select',
  'xwiki-l10n!xwiki-selectize-messages',
  'xwiki-events-bridge'
], function(module, $, TomSelect, l10n) {
  // Load the CSS.
  module.config().css?.forEach(url => {
    $('<link>').attr({
      type: 'text/css',
      rel: 'stylesheet',
      href: url
    }).appendTo('head');
  });

  var optionTemplate = [
    '<div class="xwiki-selectize-option" data-value="">',
      '<span class="xwiki-selectize-option-icon"></span>',
      '<span class="xwiki-selectize-option-label"></span>',
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
      var anchor = $('<a class="xwiki-selectize-option-label"></a>').attr('href', url);
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
    output.find('.xwiki-selectize-option-icon').wrap('<span class="xwiki-selectize-option-icon-wrapper"></span>');
    var hint = option && option.hint;
    if (typeof hint === 'string' && hint !== '') {
      output.append($('<div class="xwiki-selectize-option-hint"></div>').text(hint));
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
      dropdown.classList.remove('active');
    },
    onDropdownOpen: function(dropdown) {
      dropdown.classList.add('active');
    },
    // We normally don't want to persist the custom values (free text) that the user selects but unfortunately Selectize
    // marks all loaded values (fetched from the server) as user values and so they get removed on delete / backspace if
    // we don't enable persistence. It's better to have some extra suggestions (coming from the free text selected by
    // the user) than to miss some suggestions because they were wrongly removed. We could try to patch Selectize to fix
    // the way it handles custom user suggestions but it's safer to simply enable persistence.
    persist: true,
    preload: true,
    render: {
      item: renderItem,
      option: renderOption,
      option_create: function(data, escapeHTML) {
        var label = escapeHTML(l10n.get('selectTypedText', '__typedText__')).replace('__typedText__', '<em></em>');
        // The 'option' class is needed starting with v0.12.5 in order to have proper styling.
        var output = $('<div class="create option"></div>').html(label);
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
    }
  };

  function loadSelectedValues(input) {
    const selectize = input.selectize;
    const wrapper = selectize.get$('wrapper');
    wrapper.addClass(selectize.settings.loadingClass);
    selectize.loading++;
    selectize.items.reduce((deferred, value) => {
      return deferred.then(() => loadSelectedValue(selectize, value));
    }, Promise.resolve()).finally(function() {
      selectize.loading = Math.max(selectize.loading - 1, 0);
      if (!selectize.loading) {
        wrapper.removeClass(selectize.settings.loadingClass);
      }
    });
  }

  var loadSelectedValue = function(selectize, value) {
    return new Promise((resolve, reject) => {
      var load;
      if (typeof selectize.settings.loadSelected === 'function') {
        load = selectize.settings.loadSelected;
      } else {
        load = selectize.settings.load;
      }
      if (value && typeof load === 'function') {
        load.call(selectize, value, function(options) {
          Array.isArray(options) && options.forEach(function(option) {
            var value = option[selectize.settings.valueField];
            if (selectize.options.hasOwnProperty(value)) {
              selectize.updateOption(value, option);
            } else {
              selectize.addOption(option);
            }
          });
          resolve();
        });
      } else {
        resolve();
      }
    });
  };

  function customize(input) {
    const tomSelect = input.selectize;
    const oldPositionDropdown = tomSelect.positionDropdown;
    tomSelect.positionDropdown = function(...args) {
      // We don't need to recompute the position if the dropdown is a child of the selectize widget.
      if (this.settings.dropdownParent === 'body') {
        oldPositionDropdown.apply(this, args);
      }
      // 'auto' means the dropdown width should be variable based on its content, but no less than the width of the
      // specified form field, otherwise the dropdown should have the same width as the form field.
      if (this.settings.dropdownWidth === 'auto') {
        this.get$('dropdown').css({
          width: 'auto',
          'min-width': this.get$('control')[0].getBoundingClientRect().width
        });
      }
    }

    const oldSetActiveOption = tomSelect.setActiveOption;
    tomSelect.setActiveOption = function(option, ...args) {
      this.liveRegion?.text($(option).text());
      return oldSetActiveOption.call(this, option, ...args);
    }

    // Create a live region to store the value of the currently active option.
    tomSelect.liveRegion = $('<span>');
    tomSelect.liveRegion.attr('aria-live', 'assertive');
    tomSelect.liveRegion.addClass('sr-only');
    tomSelect.get$('input').after(tomSelect.liveRegion);

    setDropDownAlignment(tomSelect);
    if (tomSelect.settings.takeInputWidth) {
      tomSelect.get$('control').width($(input).data('initialWidth'));
    }

    // Set the title of the input field.
    tomSelect.get$('control_input').attr('title', $(input).attr('title'));

    // Set the text alternative of the input field.
    tomSelect.get$('control_input').attr('aria-label', $(input).attr('aria-label'));
  }

  var setDropDownAlignment = function(selectize) {
    var dropdownAlignment = selectize.settings.dropdownAlignment;
    if (!dropdownAlignment && selectize.settings.dropdownParent !== 'body') {
      dropdownAlignment = 'left';
    }
    if (dropdownAlignment) {
      selectize.get$('dropdown').addClass('ts-dropdown-' + dropdownAlignment);
    }
  };

  // This started as a hack needed in order to be able to access internal selectize fields that are prefixed with $ when
  // the JavaScript is minified, because this code is parsed with Velocity. Otherwise, if we access these fields directly
  // by their name (e.g. selectize.$control) then we might get a Velocity syntax error on the minified JavaScript file.
  // We keept this after replacing selectize.js with Tom Select in order to keep backwards compatibility.
  TomSelect.prototype.get$ = function(key) {
    return $(this[key]);
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

  var isSelectizeValueUpToDate = function(input) {
    var expectedValues = $(input).val();
    if (expectedValues === null && $(input).attr('multiple')) {
      // Prior to jQuery 3.0 the returned value is null when the select input has multiple selection and there's no
      // value selected.
      expectedValues = [];
    }
    var actualValues = input.selectize.getValue();
    if (Array.isArray(expectedValues)) {
      return Array.isArray(actualValues) && actualValues.length === expectedValues.length &&
        actualValues.every((actualValue, index) => actualValue === expectedValues[index]);
    } else {
      return actualValues === expectedValues;
    }
  };

  var updateSelectizeValue = function(input) {
    var selectize = input.selectize;
    var values = $(input).val() || [];
    if (typeof values === 'string') {
      // In order to distinguish between no value (i.e. no constraint) and empty value (i.e. a constraint), both
      // represented by an empty string when the underlying field is a text input rather than a select, we make the
      // convension that a string consisting only in the configured delimiter indicates that empty evalue is selected.
      if (selectize.settings.maxItems === 1 && values !== selectize.settings.delimiter) {
        values = [values];
      } else {
        values = values.split(selectize.settings.delimiter);
      }
    }
    // Clear the selection without triggering a change event.
    selectize.clear(true);
    values.forEach(value => {
      if (selectize.options.hasOwnProperty(value)) {
        // Select a known value without triggering a change event.
        selectize.addItem(value, true);
      } else {
        // Select unknown value.
        selectize.createItem(value);
      }
    });
    loadSelectedValues(input);
  };

  function createTomSelect(input, settings) {
    // Save the width before the input is hidden.
    $(input).data('initialWidth', $(input).width());

    const tomSelect = new TomSelect(input, getSettings($(input), settings));

    // We expose the TomSelect instance through the 'selectize' property / data on the target input / select that is
    // being enhanced, in order to preserve backwards compatibility. We can do this because TomSelect is a fork of
    // Selectize, preserving its JavaScript API. On the long term we need to hide the underlying implementation and
    // expose our own API.
    input.selectize = tomSelect;
    $(input).data('selectize', tomSelect);

    $(input).on('change.xwikiSelectize', function(event) {
      // Check if the value of the selectize widget is synchronized with the value of the underlying input.
      if (isSelectizeValueUpToDate(input)) {
        // Update the live table if the widget is used as a live table filter.
        var liveTableId = $(input).closest('.xwiki-livetable-display-header-filter').closest('.xwiki-livetable')
          .attr('id');
        liveTableId && $(document).trigger("xwiki:livetable:" + liveTableId + ":filtersChanged");
      } else {
        // The input value has been changed outside of the selectize widget. Update the widget.
        updateSelectizeValue(input);
      }
    });

    tomSelect.on('destroy', () => {
      // Remove the custom region we added for accessibility.
      tomSelect.liveRegion.remove();

      // Delete the references to the TomSelect instance.
      delete input.selectize;
      $(input).removeData('selectize');

      $(input).off('change.xwikiSelectize');
    });

    customize(input);

    loadSelectedValues(input);
  }

  $.fn.xwikiSelectize = function(settings) {
    // Each input in the matched collection might have different in-line settings.
    return this.not('.tomselected, .ts-wrapper').each((index, input) => createTomSelect(input, settings));
  };

  if (!$.fn.selectize) {
    // We noticed there are contributed XWiki extensions that are requiring / importing the 'xwiki-selectize' module,
    // expecting to get Selectize as a transitive dependency. This is not the case anymore since we replaced Selectize
    // with TomSelect. Some of those extensions are calling $(...).selectize() instead of our own function. We make
    // $.fn.selectize a shortcut for our own function in order to avoid breaking those extensions.
    $.fn.selectize = $.fn.xwikiSelectize;
  }
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
