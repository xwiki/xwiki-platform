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
 * Lets the user browse a document tree in order to feed the space suggestion input it is attached to. The tree follows
 * the selection mode of the input: with multiple selection the locations are checked and unchecked, with single
 * selection the selected node replaces the current location. The tree only adds and removes items, it does not hold a
 * state itself.
 *
 * The tree is displayed in a drop down so that the picker can be used inside a modal. The downside is that there is no
 * submit button: picking a node updates the input right away, which is why the tree has to be kept in sync with the
 * input. The tree is also not the best place to look at the selection, because a selected location can be hidden
 * under a collapsed node, so the suggestion input remains the reference for it.
 */
define('xwiki-compactLocationPicker', ['jquery', 'xwiki-suggestSpaces', 'xwiki-tree'], function($, suggestSpaces) {
  "use strict";

  const webHome = 'WebHome';

  // The prefix used by the document tree for the id of the nodes backing a page.
  const documentNodePrefix = 'document:';

  const getLabels = function(tree, node) {
    const labels = [node.text];
    // The parents are listed from the closest one to the root of the tree.
    node.parents.forEach(function(parentId) {
      if (parentId.indexOf(documentNodePrefix) === 0) {
        labels.unshift(tree.get_node(parentId).text);
      }
    });
    return labels;
  };

  const enhance = function(element) {
    const picker = $(element);
    if (picker.data('compactLocationPicker')) {
      // Already enhanced.
      return;
    }
    picker.data('compactLocationPicker', true);

    const select = picker.find('select.suggest-spaces');
    const multiple = select.prop('multiple');
    const dropDown = picker.children('.location-picker-browse');
    const toggle = dropDown.children('.dropdown-toggle');
    const menu = dropDown.children('.dropdown-menu');
    const treeElement = menu.find('.location-tree');
    // Set while we update the tree to match the input, so that we don't then update the input back.
    let updatingTree = false;

    const getSuggestInput = function() {
      return select[0]?.selectize;
    };

    const toLocation = function(tree, node) {
      if (node.id.indexOf(documentNodePrefix) !== 0) {
        return null;
      }
      const documentReference = XWiki.Model.resolve(node.id.substring(documentNodePrefix.length),
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

    /**
     * @return the suggestion matching the given tree node, in the format of the suggestion input,
     *   or null if the node is not a location
     */
    const toSuggestion = function(suggestInput, tree, node) {
      const location = toLocation(tree, node);
      return location && suggestSpaces.createSuggestion(suggestInput.settings, location.reference, location.labels);
    };

    const addLocation = function(tree, node) {
      const suggestInput = getSuggestInput();
      if (updatingTree || !suggestInput) {
        return;
      }
      const suggestion = toSuggestion(suggestInput, tree, node);
      if (suggestion) {
        suggestInput.addOption(suggestion);
        suggestInput.addItem(suggestion.value);
      }
    };

    const removeLocation = function(tree, node) {
      const suggestInput = getSuggestInput();
      if (updatingTree || !suggestInput) {
        return;
      }
      const suggestion = toSuggestion(suggestInput, tree, node);
      if (suggestion) {
        suggestInput.removeItem(suggestion.value);
      }
    };

    const setLocation = function(tree, node) {
      const suggestInput = getSuggestInput();
      if (updatingTree || !suggestInput) {
        return;
      }
      const suggestion = toSuggestion(suggestInput, tree, node);
      if (suggestion) {
        suggestInput.addOption(suggestion);
        suggestInput.setValue(suggestion.value);
        // There's nothing more to pick, so we give the room back to the form.
        toggle.dropdown('toggle');
      }
    };

    /**
     * Marks the nodes matching the selected locations and unmarks the others, so that the tree reflects the value of
     * the suggestion input, which can also be changed without the tree. The nodes are marked by checking them with
     * multiple selection and by selecting them with single selection.
     */
    const updateTree = function(tree) {
      const suggestInput = getSuggestInput();
      if (!suggestInput) {
        return;
      }
      updatingTree = true;
      try {
        if (!multiple) {
          tree.deselect_all(true);
        }
        // Only the nodes that have been loaded so far can be updated, which is enough: the others get their state from
        // the value of the suggestion input when they are loaded.
        tree.get_json('#', {'flat': true}).forEach(function(flatNode) {
          const node = tree.get_node(flatNode.id);
          const suggestion = toSuggestion(suggestInput, tree, node);
          const isSelected = suggestion && suggestInput.items.includes(suggestion.value);
          if (!multiple) {
            if (isSelected) {
              tree.select_node(node, true);
            }
          } else if (isSelected) {
            tree.check_node(node);
          } else if (suggestion) {
            tree.uncheck_node(node);
          }
        });
      } finally {
        updatingTree = false;
      }
    };

    /**
     * Hides the checkbox of the nodes that are not locations (e.g. the wiki nodes), so that it's clear what can be
     * picked.
     */
    const hideCheckboxOfNonLocations = function(tree) {
      tree.get_json('#', {'flat': true}).forEach(function(flatNode) {
        const node = tree.get_node(flatNode.id);
        if (!toLocation(tree, node)) {
          tree.hide_checkbox(node);
        }
      });
    };

    /**
     * The dropdown is placed right below the button by the style sheet, which is always correct because it is
     * positioned relative to the button. We only flip it above the button when there isn't enough room below, which
     * easily happens when the picker is displayed near the bottom of a dialog.
     */
    const flipIfNeeded = function() {
      dropDown.removeClass('dropup');
      const button = toggle[0].getBoundingClientRect();
      const roomBelow = document.documentElement.clientHeight - button.bottom;
      const roomAbove = button.top;
      if (roomBelow < menu[0].offsetHeight && roomAbove > roomBelow) {
        dropDown.addClass('dropup');
      }
    };

    const initTree = function() {
      // The tree can only be initialized once its element is visible, otherwise it can't measure itself.
      const treeEvents = treeElement.xtree({
        core: {
          multiple: multiple
        }
      }).on('ready.jstree refresh.jstree load_node.jstree', function(event, data) {
        if (multiple) {
          hideCheckboxOfNonLocations(data.instance);
        }
        updateTree(data.instance);
        // Loading nodes changes the height of the drop down, so it may not fit below the button any more.
        flipIfNeeded();
      });
      if (multiple) {
        treeEvents.on('check_node.jstree', function(event, data) {
          addLocation(data.instance, data.node);
        }).on('uncheck_node.jstree', function(event, data) {
          removeLocation(data.instance, data.node);
        });
      } else {
        treeEvents.on('select_node.jstree', function(event, data) {
          setLocation(data.instance, data.node);
        });
      }
    };

    dropDown.on('shown.bs.dropdown', function() {
      const tree = $.jstree.reference(treeElement);
      if (tree) {
        updateTree(tree);
      } else {
        initTree();
      }
      flipIfNeeded();
    });

    // Set while we close the drop down because the focus moved to another element, which must keep the focus.
    let closingOnFocusOut = false;

    dropDown.on('focusout', function(event) {
      // Bootstrap closes the drop down when the user clicks outside of it, but some widgets, like the suggestion
      // inputs, stop the propagation of their click events. So we also close the drop down as soon as the focus moves
      // to an element outside of it. Clicks on elements that can't be focused are still handled by Bootstrap.
      if (dropDown.hasClass('open') && event.relatedTarget && !dropDown[0].contains(event.relatedTarget)) {
        closingOnFocusOut = true;
        try {
          toggle.dropdown('toggle');
        } finally {
          closingOnFocusOut = false;
        }
      }
    });

    dropDown.on('hide.bs.dropdown', function() {
      // The drop down can be closed without the toggle button getting focus back which would otherwise drop
      // the focus to the body.
      if (!closingOnFocusOut) {
        toggle.trigger('focus');
      }
    });

    menu.on('click', function(event) {
      // Browsing the tree must not close the drop down.
      event.stopPropagation();
    });

    menu.on('keydown', function(event) {
      // Same for typing in the tree finder, but let the Escape key through so that the drop down can still be closed
      // from the keyboard.
      if (event.key !== 'Escape') {
        event.stopPropagation();
      }
    });
  };

  $.fn.compactLocationPicker = function() {
    return this.each(function() {
      enhance(this);
    });
  };
});

require(['jquery', 'xwiki-compactLocationPicker', 'xwiki-events-bridge'], function($) {
  const init = function(event, data) {
    const elements = $(data?.elements || document);
    elements.filter('.location-picker-compact').add(elements.find('.location-picker-compact')).compactLocationPicker();
  };

  $(document).on('xwiki:dom:loaded xwiki:dom:updated', init);
  $(init);
});
