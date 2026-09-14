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
/*!
#set ($webHome = $services.model.getEntityReference('DOCUMENT', 'default').name)
#[[*/
// Start JavaScript-only code.
(function(webHome) {
  "use strict";

/**
 * Lets the user browse a document tree in order to feed the space suggestion input it is attached to. The suggestion
 * input remains the single source of truth for the value: the tree only adds and removes items, it doesn't hold any
 * value of its own.
 */
define('xwiki-multiLocationPicker', ['jquery', 'xwiki-suggestSpaces', 'xwiki-tree'], function($, suggestSpaces) {
  webHome = webHome || 'WebHome';

  // The prefix used by the document tree for the id of the nodes backing a page.
  var documentNodePrefix = 'document:';

  var enhance = function(element) {
    var picker = $(element);
    if (picker.data('locationPickerMulti')) {
      // Already enhanced.
      return;
    }
    picker.data('locationPickerMulti', true);

    var select = picker.find('select.suggest-spaces');
    var browser = picker.children('.location-picker-browse');
    var toggle = browser.children('.dropdown-toggle');
    var menu = browser.children('.dropdown-menu');
    var treeElement = menu.find('.location-tree');
    // Set while we update the tree to match the input, so that we don't then update the input back.
    var updatingTree = false;

    /**
     * The suggestion input is enhanced asynchronously, so we can only look for the widget when the user actually
     * interacts with the tree.
     */
    var getSuggestInput = function() {
      return select[0] && select[0].selectize;
    };

    var toLocation = function(tree, node) {
      if (node.id.indexOf(documentNodePrefix) !== 0) {
        return null;
      }
      var documentReference = XWiki.Model.resolve(node.id.substring(documentNodePrefix.length),
        XWiki.EntityType.DOCUMENT);
      if (documentReference.name !== webHome) {
        // Only the pages backing a space can be picked as a location. The tree is configured to hide the terminal pages
        // but let's not rely on it.
        return null;
      }
      return {
        reference: documentReference.parent,
        // The tree already knows the pretty name of each ancestor, so we get a proper hierarchy hint for free.
        labels: getLabels(tree, node)
      };
    };

    var getLabels = function(tree, node) {
      var labels = [node.text];
      // The parents are listed from the closest one to the root of the tree.
      node.parents.forEach(function(parentId) {
        if (parentId.indexOf(documentNodePrefix) === 0) {
          labels.unshift(tree.get_node(parentId).text);
        }
      });
      return labels;
    };

    /**
     * @return the suggestion matching the given tree node, in the exact format the suggestion input uses for the
     *   locations it suggests itself, or null if the node is not a location
     */
    var toSuggestion = function(suggestInput, tree, node) {
      var location = toLocation(tree, node);
      return location && suggestSpaces.createSuggestion(suggestInput.settings, location.reference, location.labels);
    };

    var addLocation = function(tree, node) {
      var suggestInput = getSuggestInput();
      if (updatingTree || !suggestInput) {
        return;
      }
      var suggestion = toSuggestion(suggestInput, tree, node);
      if (suggestion) {
        suggestInput.addOption(suggestion);
        suggestInput.addItem(suggestion.value);
      }
    };

    var removeLocation = function(tree, node) {
      var suggestInput = getSuggestInput();
      if (updatingTree || !suggestInput) {
        return;
      }
      var suggestion = toSuggestion(suggestInput, tree, node);
      if (suggestion) {
        suggestInput.removeItem(suggestion.value);
      }
    };

    /**
     * Checks the nodes matching the selected locations and unchecks the others, so that the tree reflects the value of
     * the suggestion input, which can also be changed without the tree.
     */
    var updateTree = function(tree) {
      var suggestInput = getSuggestInput();
      if (!suggestInput) {
        return;
      }
      updatingTree = true;
      try {
        // Only the nodes that have been loaded so far can be updated, which is enough: the others get their state from
        // the value of the suggestion input when they are loaded.
        tree.get_json('#', {'flat': true}).forEach(function(flatNode) {
          var node = tree.get_node(flatNode.id);
          var suggestion = toSuggestion(suggestInput, tree, node);
          if (suggestion) {
            if (suggestInput.items.indexOf(suggestion.value) < 0) {
              tree.uncheck_node(node);
            } else {
              tree.check_node(node);
            }
          }
        });
      } finally {
        updatingTree = false;
      }
    };

    /**
     * Hides the check box of the nodes that are not locations (e.g. the wiki nodes), so that it's clear what can be
     * picked.
     */
    var hideCheckboxOfNonLocations = function(tree) {
      tree.get_json('#', {'flat': true}).forEach(function(flatNode) {
        var node = tree.get_node(flatNode.id);
        if (!toLocation(tree, node)) {
          tree.hide_checkbox(node);
        }
      });
    };

    /**
     * The drop down is placed right below the button by the style sheet, which is always correct because it is
     * positioned relative to the button. We only flip it above the button when there isn't enough room below, which
     * easily happens when the picker is displayed near the bottom of a dialog.
     *
     * Note that we deliberately toggle a class instead of computing the position of the drop down ourselves: any
     * ancestor can establish a containing block (the dialog of the macro editor, for instance, is translated, which
     * makes both absolute and fixed positioning resolve against it rather than against the viewport) and no amount of
     * coordinate arithmetic is reliable in the face of that.
     */
    var flipIfNeeded = function() {
      browser.removeClass('dropup');
      var button = toggle[0].getBoundingClientRect();
      var roomBelow = document.documentElement.clientHeight - button.bottom;
      var roomAbove = button.top;
      if (roomBelow < menu[0].offsetHeight && roomAbove > roomBelow) {
        browser.addClass('dropup');
      }
    };

    browser.on('shown.bs.dropdown', function() {
      var tree = $.jstree.reference(treeElement);
      if (tree) {
        updateTree(tree);
      } else {
        // The tree can only be initialized once its element is visible, otherwise it can't measure itself.
        treeElement.xtree().on('ready.jstree refresh.jstree load_node.jstree', function(event, data) {
          hideCheckboxOfNonLocations(data.instance);
          updateTree(data.instance);
          // Loading nodes changes the height of the drop down, so it may not fit below the button any more.
          flipIfNeeded();
        }).on('check_node.jstree', function(event, data) {
          addLocation(data.instance, data.node);
        }).on('uncheck_node.jstree', function(event, data) {
          removeLocation(data.instance, data.node);
        });
      }
      flipIfNeeded();
    });

    menu.on('click', function(event) {
      // Browsing the tree must not close the drop down.
      event.stopPropagation();
    });

    menu.on('keydown', function(event) {
      // Same for typing in the tree finder, but let the Escape key through so that the drop down can still be closed
      // from the keyboard.
      if (event.which !== 27) {
        event.stopPropagation();
      }
    });
  };

  $.fn.multiLocationPicker = function() {
    return this.each(function() {
      enhance(this);
    });
  };
});

require(['jquery', 'xwiki-multiLocationPicker', 'xwiki-events-bridge'], function($) {
  var init = function(event, data) {
    var elements = $((data && data.elements) || document);
    elements.filter('.location-picker-multi').multiLocationPicker();
    elements.find('.location-picker-multi').multiLocationPicker();
  };

  $(document).on('xwiki:dom:loaded xwiki:dom:updated', init);
  $(init);
});

// End JavaScript-only code.
}).apply(']]#', $jsontool.serialize([$webHome]));
