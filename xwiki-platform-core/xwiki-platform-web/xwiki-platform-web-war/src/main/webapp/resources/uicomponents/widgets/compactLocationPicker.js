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

  /**
   * @return the location backed by the given tree node, or null if the node is not a location
   */
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
   * @return the nodes that have been loaded so far in the given tree
   */
  const getLoadedNodes = function(tree) {
    return tree.get_json('#', {'flat': true}).map(flatNode => tree.get_node(flatNode.id));
  };

  const getSuggestInput = function(picker) {
    return picker.select[0]?.selectize;
  };

  /**
   * @return the suggestion matching the given tree node, in the format of the suggestion input,
   *   or null if the node is not a location
   */
  const toSuggestion = function(suggestInput, tree, node) {
    const location = toLocation(tree, node);
    return location && suggestSpaces.createSuggestion(suggestInput.settings, location.reference, location.labels);
  };

  //
  // Updating the suggestion input from the tree.
  //

  /**
   * @return the suggestion to apply to the suggestion input after a change of the given tree node, or null when there
   *   is nothing to apply: the node is not a location, the suggestion input is not ready, or the change comes from
   *   updating the tree to match the suggestion input
   */
  const getSuggestionToApply = function(picker, tree, node) {
    const suggestInput = getSuggestInput(picker);
    if (picker.updatingTree || !suggestInput) {
      return null;
    }
    return toSuggestion(suggestInput, tree, node);
  };

  const addLocation = function(picker, tree, node) {
    const suggestion = getSuggestionToApply(picker, tree, node);
    if (suggestion) {
      const suggestInput = getSuggestInput(picker);
      suggestInput.addOption(suggestion);
      suggestInput.addItem(suggestion.value);
    }
  };

  const removeLocation = function(picker, tree, node) {
    const suggestion = getSuggestionToApply(picker, tree, node);
    if (suggestion) {
      getSuggestInput(picker).removeItem(suggestion.value);
    }
  };

  const setLocation = function(picker, tree, node) {
    const suggestion = getSuggestionToApply(picker, tree, node);
    if (suggestion) {
      const suggestInput = getSuggestInput(picker);
      suggestInput.addOption(suggestion);
      suggestInput.setValue(suggestion.value);
      // There's nothing more to pick, so we give the room back to the form.
      picker.toggle.dropdown('toggle');
    }
  };

  //
  // Updating the tree from the suggestion input.
  //

  /**
   * Marks the given node if it matches a selected location, and unmarks it otherwise. The nodes are marked by checking
   * them with multiple selection and by selecting them with single selection.
   */
  const markNode = function(picker, suggestInput, tree, node) {
    const suggestion = toSuggestion(suggestInput, tree, node);
    const isSelected = suggestion && suggestInput.items.includes(suggestion.value);
    if (!picker.multiple) {
      if (isSelected) {
        tree.select_node(node, true);
      }
    } else if (isSelected) {
      tree.check_node(node);
    } else if (suggestion) {
      tree.uncheck_node(node);
    }
  };

  /**
   * Marks the nodes matching the selected locations and unmarks the others, so that the tree reflects the value of
   * the suggestion input, which can also be changed without the tree.
   */
  const updateTree = function(picker, tree) {
    const suggestInput = getSuggestInput(picker);
    if (!suggestInput) {
      return;
    }
    picker.updatingTree = true;
    try {
      if (!picker.multiple) {
        tree.deselect_all(true);
      }
      // Only the nodes that have been loaded so far can be updated, which is enough: the others get their state from
      // the value of the suggestion input when they are loaded.
      getLoadedNodes(tree).forEach(node => markNode(picker, suggestInput, tree, node));
    } finally {
      picker.updatingTree = false;
    }
  };

  /**
   * Hides the checkbox of the nodes that are not locations, like the wiki nodes, so that it's clear what can be
   * picked.
   */
  const hideCheckboxOfNonLocations = function(tree) {
    getLoadedNodes(tree).filter(node => !toLocation(tree, node)).forEach(node => tree.hide_checkbox(node));
  };

  const initTree = function(picker) {
    // The tree can only be initialized once its element is visible, otherwise it can't measure itself.
    const treeEvents = picker.treeElement.xtree({
      core: {
        multiple: picker.multiple
      }
    }).on('ready.jstree refresh.jstree load_node.jstree', (event, data) => {
      if (picker.multiple) {
        hideCheckboxOfNonLocations(data.instance);
      }
      updateTree(picker, data.instance);
      // Loading nodes changes the height of the drop down, so it may not fit below the button any more.
      flipIfNeeded(picker);
    });
    if (picker.multiple) {
      treeEvents.on('check_node.jstree', (event, data) => addLocation(picker, data.instance, data.node))
        .on('uncheck_node.jstree', (event, data) => removeLocation(picker, data.instance, data.node));
    } else {
      treeEvents.on('select_node.jstree', (event, data) => setLocation(picker, data.instance, data.node));
    }
  };

  //
  // Handling the drop down holding the tree.
  //

  /**
   * The dropdown is placed right below the button by the style sheet, which is always correct because it is
   * positioned relative to the button. We only flip it above the button when there isn't enough room below, which
   * easily happens when the picker is displayed near the bottom of a dialog.
   */
  const flipIfNeeded = function(picker) {
    picker.dropDown.removeClass('dropup');
    const button = picker.toggle[0].getBoundingClientRect();
    const roomBelow = document.documentElement.clientHeight - button.bottom;
    const roomAbove = button.top;
    if (roomBelow < picker.menu[0].offsetHeight && roomAbove > roomBelow) {
      picker.dropDown.addClass('dropup');
    }
  };

  const bindDropDownEvents = function(picker) {
    picker.dropDown.on('shown.bs.dropdown', () => {
      const tree = $.jstree.reference(picker.treeElement);
      if (tree) {
        updateTree(picker, tree);
      } else {
        initTree(picker);
      }
      flipIfNeeded(picker);
    });

    picker.dropDown.on('focusout', event => {
      // Bootstrap closes the drop down when the user clicks outside of it, but some widgets, like the suggestion
      // inputs, stop the propagation of their click events. So we also close the drop down as soon as the focus moves
      // to an element outside of it. Clicks on elements that can't be focused are still handled by Bootstrap.
      if (picker.dropDown.hasClass('open') && event.relatedTarget &&
          !picker.dropDown[0].contains(event.relatedTarget)) {
        picker.closingOnFocusOut = true;
        try {
          picker.toggle.dropdown('toggle');
        } finally {
          picker.closingOnFocusOut = false;
        }
      }
    });

    picker.dropDown.on('hide.bs.dropdown', () => {
      // The drop down can be closed without the toggle button getting focus back which would otherwise drop
      // the focus to the body.
      if (!picker.closingOnFocusOut) {
        picker.toggle.trigger('focus');
      }
    });

    picker.menu.on('click', event => {
      // Browsing the tree must not close the drop down.
      event.stopPropagation();
    });

    picker.menu.on('keydown', event => {
      // Same for typing in the tree finder, but let the Escape key through so that the drop down can still be closed
      // from the keyboard.
      if (event.key !== 'Escape') {
        event.stopPropagation();
      }
    });
  };

  const enhance = function(element) {
    const container = $(element);
    if (container.data('compactLocationPicker')) {
      // Already enhanced.
      return;
    }
    container.data('compactLocationPicker', true);

    const select = container.find('select.suggest-spaces');
    const dropDown = container.children('.location-picker-browse');
    const menu = dropDown.children('.dropdown-menu');
    const picker = {
      select,
      multiple: select.prop('multiple'),
      dropDown,
      toggle: dropDown.children('.dropdown-toggle'),
      menu,
      treeElement: menu.find('.location-tree'),
      // Set while we update the tree to match the input, so that we don't then update the input back.
      updatingTree: false,
      // Set while we close the drop down because the focus moved to another element, which must keep the focus.
      closingOnFocusOut: false
    };
    bindDropDownEvents(picker);
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
