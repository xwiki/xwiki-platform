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
define('macroSelectorTranslationKeys', [], [
  'title',
  'filter.text.placeholder',
  'filter.category.all',
  'filter.category.other',
  'failedToRetrieveMacros',
  'select'
]);

define('macroSelector', ['jquery', 'modal', 'l10n!macroSelector'], function($, $modal, translations) {
  'use strict';
  var macrosBySyntax = {},
  allMacrosExcludedCategories = [],

  getMacros = function(syntaxId) {
    var deferred = $.Deferred();
    var macros = macrosBySyntax[syntaxId || ''];
    if (macros) {
      deferred.resolve(macros);
    } else {
      var url = new XWiki.Document('MacroService', 'CKEditor').getURL('get', $.param({
        outputSyntax: 'plain',
        language: $('html').attr('lang')
      }));
      $.get(url, {data: 'list', syntaxId: syntaxId}).done(function(macros) {
        // Bulletproofing: check if the returned data is json since it could some HTML representing an error
        if (typeof macros === 'object' && $.isArray(macros.list)) {
          macrosBySyntax[syntaxId || ''] = macros.list;
          allMacrosExcludedCategories = macros.options.allMacrosExcludedCategories;
          deferred.resolve(macros.list);
        } else {
          deferred.reject.apply(deferred, arguments);
        }
      }).fail(function() {
        deferred.reject.apply(deferred, arguments);
      });
    }
    return deferred.promise();
  },

  macroListTemplate = '<ul class="macro-list form-control" tabindex="0"></ul>',
  macroListItemTemplate =
    '<li data-macroCategory="" data-macroId="">' +
      '<div class="macro-name"></div>' +
      '<div class="macro-description"></div>' +
    '</li>',

  displayMacros = function(macros) {
    var list = $(macroListTemplate);
    var categories = {};
    macros.forEach(function(macro) {
      var macroCategory = macro.defaultCategory || '';
      categories[macroCategory] = 1;
      var macroListItem = $(macroListItemTemplate).attr({
        'data-macroId': macro.id.id,
        'data-macroCategory': macroCategory
      }).appendTo(list);
      macroListItem.find('.macro-name').text(macro.name);
      macroListItem.find('.macro-description').text(macro.description);
    });
    categories = Object.keys(categories).sort();
    var categoryFilter = createCategoryFilter(categories);
    var textFilter = $(document.createElement('input')).attr({
      'type': 'text',
      'class': 'macro-textFilter',
      'placeholder': translations.get('filter.text.placeholder')
    });
    var filters = $(document.createElement('div')).addClass('macro-filters input-group');
    filters.append(textFilter).append(categoryFilter);
    this.removeClass('loading').append(filters).append(list);
    // Filter the list of displayed macros to implement support for allMacrosExcludedCategories (i.e. when all macros
    // is selected, don't display macros in some given categories). More generally this makes sure that the filtering
    // is always done.
    filterMacros.call(this);
  },

  createCategoryFilter = function(categories) {
    var categoryFilter = $(
      '<div class="macro-categories input-group-btn">' +
        '<button type="button" class="btn btn-default dropdown-toggle" data-toggle="dropdown" ' +
          'aria-haspopup="true" aria-expanded="false"><span class="caret"/></button>' +
        '<ul class="dropdown-menu dropdown-menu-right">' +
          '<li><a href="#"/></li>' +
        '</ul>' +
      '</div>'
    );
    var allMacrosCategoryName = translations.get('filter.category.all');
    categoryFilter.find('.caret').before(document.createTextNode(allMacrosCategoryName + ' '));
    categoryFilter.find('a').text(allMacrosCategoryName);
    var separator = '<li role="separator" class="divider"></li>';
    var categoryList = categoryFilter.find('ul.dropdown-menu');
    if (categories.length > 0) {
      categoryList.append(separator);
    }
    var otherCategory;
    categories.forEach(function(category) {
      var item = $('<li><a href="#"></a></li>').attr('data-category', category);
      item.children('a').text(category || translations.get('filter.category.other'));
      if (category) {
        categoryList.append(item);
      } else {
        otherCategory = item;
      }
    });
    if (otherCategory) {
      if (categories.length > 1) {
        categoryList.append(separator);
      }
      categoryList.append(otherCategory);
    }
    return categoryFilter;
  },

  scrollIntoList = function(item) {
    var itemPositionTop = item.position().top;
    var list = item.parent();
    if (itemPositionTop < 0) {
      list.scrollTop(list.scrollTop() + itemPositionTop);
    } else {
      var delta = itemPositionTop + item.outerHeight() - list.height();
      if (delta > 0) {
        list.scrollTop(list.scrollTop() + delta);
      }
    }
  },

  filterMacros = function() {
    var text = $(this).find('.macro-textFilter').val().toLowerCase();
    var selectedCategory = $(this).find('.macro-categories .dropdown-toggle').attr('data-category');
    var macroSelector = $(this).closest('.macro-selector');
    macroSelector.find('.macro-list').scrollTop(0).children().each(function() {
      var name = $(this).find('.macro-name').text().toLowerCase();
      var description = $(this).find('.macro-description').text().toLowerCase();
      var category = $(this).attr('data-macroCategory');
      // We hide Macros located in some categories to exclude (e.g. internal and deprecated categories) so that they
      // are less visible to users, to provide a simpler user experience by not bloating the macro list with
      // macros that are less interesting to users.
      // Note that when "All Macros" is selected selectedCategory is undefined.
      var hide = (text && name.indexOf(text) < 0 && description.indexOf(text) < 0) ||
        (typeof selectedCategory === 'string' && category !== selectedCategory) ||
          (typeof selectedCategory !== 'string' && $.inArray(category, allMacrosExcludedCategories) !== -1);
      $(this).removeClass('selected').toggleClass('hidden', hide);
    }).not('.hidden').first().addClass('selected');
    macroSelector.trigger('change');
  },

  navigateMacroList = function(macroList, up) {
    var direction = up ? 'prev' : 'next';
    var selectedItem = macroList.children('.selected');
    if (selectedItem.size() > 0) {
      selectedItem = selectedItem[direction]();
    } else {
      selectedItem = macroList.children()[up ? 'last' : 'first']();
    }
    while(selectedItem.hasClass('hidden')) {
      selectedItem = selectedItem[direction]();
    }
    selectedItem.click();
  },

  maybeTriggerMacroSelection = function(macroSelector) {
    var selectedMacros = macroSelector.find('.macro-list .selected').map(function() {
      return $(this).attr('data-macroId');
    });
    if (selectedMacros.length > 0) {
      macroSelector.trigger('xwiki:macro:selected', selectedMacros);
    }
  },

  changeMacroCategory = function(event) {
    event.preventDefault();
    var selectedCategory = $(this).parent('li');
    var categoryFilter = $(this).closest('.macro-categories');
    var dropDownToggle = categoryFilter.find('.dropdown-toggle');
    var newCategoryId = selectedCategory.attr('data-category');
    var oldCategoryId = dropDownToggle.attr('data-category');
    if (newCategoryId !== oldCategoryId) {
      var caret = dropDownToggle.children('.caret').remove();
      dropDownToggle.text(selectedCategory.text() + ' ').append(caret);
      if (typeof newCategoryId === 'string') {
        dropDownToggle.attr('data-category', newCategoryId);
      } else {
        dropDownToggle.removeAttr('data-category');
      }
      categoryFilter.trigger('change', newCategoryId, oldCategoryId);
    }
    dropDownToggle.focus();
  },

  addMacroSelectorBehaviour = function(macroSelector) {
    macroSelector.on('click', '.macro-categories a', changeMacroCategory);
    macroSelector.on('change', '.macro-categories', $.proxy(filterMacros, macroSelector));

    var timeoutId;
    macroSelector.on('input', '.macro-textFilter', function() {
      clearTimeout(timeoutId);
      timeoutId = setTimeout($.proxy(filterMacros, macroSelector), 500);
    });

    macroSelector.on('click', '.macro-list > li', function() {
      var item = $(this);
      item.addClass('selected').siblings().removeClass('selected');
      scrollIntoList(item);
      macroSelector.trigger('change');
    });

    macroSelector.on('keydown', '.macro-textFilter, .macro-list', function(event) {
      if (event.which === 38 || event.which === 40) {
        navigateMacroList(macroSelector.find('.macro-list'), event.which === 38);
        event.preventDefault();
      } else if (event.which === 13) {
        maybeTriggerMacroSelection(macroSelector);
      }
    });

    macroSelector.on('dblclick', '.macro-list', function() {
      maybeTriggerMacroSelection(macroSelector);
    });
  },

  maybeDisplayMacros = function(requestNumber, macros) {
    // Check if the list of macros corresponds to the last request.
    if (this.prop('requestNumber') === requestNumber) {
      displayMacros.call(this, macros);
      this.trigger('ready');
    }
  },

  maybeShowError = function(requestNumber) {
    // Check if the error corresponds to the last request.
    if (this.prop('requestNumber') === requestNumber) {
      var errorMessage = $('<div class="box errormessage"/>').text(translations.get('failedToRetrieveMacros'));
      this.removeClass('loading').append(errorMessage);
    }
  },

  createMacroSelectorAPI = function(macroSelector) {
    return {
      filter: function(text, category) {
        macroSelector.find('.macro-textFilter').val(text);
        macroSelector.find('.macro-categories .dropdown-toggle').attr('data-category', category);
        filterMacros.call(macroSelector[0]);
      },
      getSelectedMacro: function() {
        return macroSelector.find('.macro-list > li.selected').attr('data-macroId');
      },
      reset: function(macroId) {
        this.filter('');
        this.select(macroId);
      },
      select: function(macroId) {
        macroSelector.find('.macro-list > li').filter(function() {
          return $(this).attr('data-macroId') === macroId;
        }).click();
      },
      update: function(syntaxId) {
        syntaxId = syntaxId || macroSelector.attr('data-syntaxId');
        var requestNumber = (macroSelector.prop('requestNumber') || 0) + 1;
        macroSelector.empty().addClass('loading')
          .attr('data-syntaxId', syntaxId)
          .prop('requestNumber', requestNumber);
        getMacros(syntaxId).done($.proxy(maybeDisplayMacros, macroSelector, requestNumber))
          .fail($.proxy(maybeShowError, macroSelector, requestNumber));
      }
    };
  },

  selectMacro = $modal.createModalStep({
    'class': 'macro-selector-modal',
    title: translations.get('title'),
    content: '<div class="macro-selector loading"/>',
    acceptLabel: translations.get('select'),
    onLoad: function() {
      var modal = this;
      var selectButton = modal.find('.modal-footer .btn-primary');
      modal.on('shown.bs.modal', function(event) {
        var input = modal.data('input') || {};
        var macroSelector = modal.find('.macro-selector');
        var macroSelectorAPI = macroSelector.data('macroSelectorAPI');
        if (!macroSelectorAPI) {
          // Create the macro selector.
          macroSelector.on('ready', function() {
            macroSelectorAPI.select(input.macroId);
            macroSelector.find('.macro-textFilter').focus();
          }).on('change', function() {
            selectButton.prop('disabled', !macroSelectorAPI.getSelectedMacro());
          }).on('xwiki:macro:selected', function(event, macroIds) {
            selectButton.click();
          }).attr('data-syntaxId', input.syntaxId);
          macroSelectorAPI = macroSelector.xwikiMacroSelector();
        } else if (macroSelector.attr('data-syntaxId') !== input.syntaxId) {
          // Update the list of macros.
          macroSelectorAPI.update(input.syntaxId);
        } else {
          macroSelectorAPI.reset(input.macroId);
          macroSelector.find('.macro-textFilter').focus();
        }
      });
      selectButton.click(function() {
        var macroSelectorAPI = modal.find('.macro-selector').xwikiMacroSelector();
        var output = modal.data('input') || {};
        output.macroId = macroSelectorAPI.getSelectedMacro();
        modal.data('output', output).modal('hide');
      });
    }
  });

  $.fn.xwikiMacroSelector = function() {
    this.each(function() {
      var macroSelector = $(this);
      if (!macroSelector.data('macroSelectorAPI')) {
        var macroSelectorAPI = createMacroSelectorAPI(macroSelector);
        macroSelector.data('macroSelectorAPI', macroSelectorAPI);
        addMacroSelectorBehaviour(macroSelector);
        if (macroSelector.hasClass('loading')) {
          macroSelectorAPI.update();
        } else {
          macroSelector.trigger('ready');
        }
      }
    });
    return this.data('macroSelectorAPI');
  };

  return selectMacro;
});
