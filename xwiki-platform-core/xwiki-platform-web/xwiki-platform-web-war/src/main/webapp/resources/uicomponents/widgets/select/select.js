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
require(['jquery'], function($) {
  'use strict';

  /**
   * Represents an XWiki Select Widget (for internal use only). External scripts must use the jQuery plugin defined
   * below instead.
   */
  var XWikiSelectWidget = function(domElement) {
    // ----------------------
    // Init fields
    // ----------------------
    var self = this;
    self.selectWidget = $(domElement);

    /**
     * Send an event to say that the selection have changed
     */
    self.triggerSelectionChange = function () {
      self.selectWidget.trigger('xwiki:select:updated', {'elements': self.selectWidget[0]});
    };

    /**
     * Callback used when an option from the select widget is clicked
     */
    self.onOptionClicked = function () {
      var option = $(this);
      var input = option.find('input');
      if (input.prop('checked')) {
        // The input is already selected, so we have nothing to do, and we do not trigger any event.
        // Note that if the user clicks on the <label> element of the widget, the "click" event is triggered twice:
        // once because of this listener, and then because of the input's state change.
        return;
      }
      input.prop('checked', true);
      self.selectWidget.find('.xwiki-select-option-selected').removeClass('xwiki-select-option-selected');
      option.addClass('xwiki-select-option-selected');
      self.triggerSelectionChange();
    };

    /**
     * When the user types some text to filter the options, we hide/show each options according to their matching with
     * the filter.
     */
    self.onFilterChange = function () {
      // The filter is the DOM input.
      var filter = $(this);
      // The value is an array of words to match
      var filterValues = filter.val().split(' ');
      // We are going to count how many categories have matching options.
      var matchingCategoriesCount = 0;
      // For each category.
      var categories = self.selectWidget.find('.xwiki-select-category');
      for (var i = 0; i < categories.length; ++i) {
        var category = $(categories[i]);
        // For each option of that category.
        var options = category.find('.xwiki-select-option');
        var matchingOptionsCount = 0;
        for (var j = 0; j < options.length; ++j) {
          var option = $(options[j]);
          // We look both in the label and in the hint of the option
          var label = option.find('label').text().toLowerCase();
          var hint  = option.find('.xHint').text().toLowerCase();
          var optionText = label + ' ' + hint;
          // We look if the label match all the values of the filer.
          var optionShouldBeVisible = true;
          for (var k = 0; k < filterValues.length; ++k) {
            var filterValue = filterValues[k].toLowerCase();
            // If one of the filter values is missing from the label of this option, we must hide it.
            if (optionText.indexOf(filterValue) == -1) {
              optionShouldBeVisible = false;
              break;
            }
          }
          // Hide/show the option according to the result of the previous loop.
          if (optionShouldBeVisible) {
            // We must call show() because the option could have been hidden by a previous filter.
            // It has no effect if the option is already visible.
            option.show();
            matchingOptionsCount++;
          } else {
            option.hide();
          }
        }

        // Now, we update the count of matching items in the heading of the category.
        category.find('.xwiki-select-category-count').text(matchingOptionsCount);

        // We hide the category if it has no matching options.
        if (matchingOptionsCount == 0) {
          category.hide();
        } else {
          category.show();
          matchingCategoriesCount++;
        }
      }

      // If there is no matching categories at all, we display a nice "No result" message instead of having a blank
      // panel.
      if (matchingCategoriesCount == 0) {
        // Append the "no result" message only if it is not already displayed
        if (self.selectWidget.find('.xwiki-select-no-results').length == 0) {
          self.selectWidget.find('.xwiki-select-options').append('<p class="xwiki-select-no-results">$escapetool.javascript($escapetool.xml($services.localization.render("web.widgets.select.filter.noResults")))</p>');
        }
      } else {
        // We remove the "no results" message, just in case it has been displayed by a previous filter.
        self.selectWidget.find('.xwiki-select-no-results').remove();
      }
    };

    /**
    /* Initialization
     */
    self.init = function () {
      self.selectWidget.find('.xwiki-select-option').on('click', self.onOptionClicked);
      self.selectWidget.find('input.xwiki-select-filter').on('change', self.onFilterChange)
        .on('keyup', self.onFilterChange);
    };

    /**
     * Clear selection
     */
    self.clearSelection = function () {
      self.selectWidget.find('.xwiki-select-option-selected').removeClass('xwiki-select-option-selected');
      self.selectWidget.find('input[type="radio"]:checked').prop('checked', false);
      self.triggerSelectionChange();
    };

    /**
     * Return the current selected option
     */
    self.getValue = function () {
      return self.selectWidget.find('input[type="radio"]:checked').val();
    };

    self.init();
  };

  /**
   * Define a jQuery plugin about the select widget.
   * @since 7.4.1
   */
  $.fn.xwikiSelectWidget = function(action) {
    //--------------------
    // Handle actions
    //--------------------
    if (!action || action == 'init') {
      // Handle each object separately
      for (var i = 0; i < this.length; ++i) {
        var domElement = this[i];
        // This widget might have already been created
        if ($(domElement).data('xwikiSelectWidget')) {
          continue;
        }
        // Previously, we stored all the XWikiSelectWidgets in a Map where the DOM elements were the keys, but IE10 does
        // not support the Map object so we had to find a workaround.
        $(domElement).data('xwikiSelectWidget', new XWikiSelectWidget(domElement)).attr('data-ready', true);
      }
    } else if (action == 'clearSelection') {
      // Handle each object separately
      for (var i = 0; i < this.length; ++i) {
        $(this[i]).data('xwikiSelectWidget').clearSelection();
      }
    } else if (action == 'getValue') {
      // In such a case, there is no possible chaining
      return $(this[0]).data('xwikiSelectWidget').getValue();
    }

    // Enable chaining
    return this;
  };

  /**
   * Initializer called when the DOM is ready
   */
  $(function() {
    $('.xwiki-select').xwikiSelectWidget();
  });
});
