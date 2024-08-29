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
#set ($l10nKeys = [
  'core.exporter.selectChildren',
  'core.exporter.unselectChildren'
])
#set ($l10n = {})
#foreach ($key in $l10nKeys)
  #set ($discard = $l10n.put($key, $services.localization.render($key)))
#end
#set ($iconNames = ['check', 'shape_square'])
#set ($icons = {})
#foreach ($iconName in $iconNames)
  #set ($discard = $icons.put($iconName, $services.icon.getMetaData($iconName)))
#end
#[[*/
// Start JavaScript-only code.
(function(l10n, icons) {
  "use strict";

/**
 * Export Tree
 */
define('xwiki-export-tree', ['jquery', 'xwiki-tree', 'xwiki-entityReference'], function($) {
  var selectChildNodes = function(tree, parentNode) {
    parentNode = parentNode || tree.get_node($.jstree.root);
    selectNodes(tree, parentNode.children);
  };

  var selectNodes = function(tree, nodeIds) {
    nodeIds.forEach(function(nodeId) {
      var node = tree.get_node(nodeId);
      // Select the nodes that are enabled or that are disabled but with children (in order to propagate the
      // selection to the descendants, because disabled nodes can have descendants that are enabled).
      if (!tree.is_disabled(nodeId) || node.children.length > 0) {
        tree.select_node(nodeId, false, true);
      } else if (tree.is_parent(nodeId)) {
        // Leave disabled nodes that are not loaded yet in the undetermined state.
        node.original.state.undetermined = true;
        node.state.undetermined = true;
      }
    });
  };

  var deselectChildNodes = function(tree, parentNode) {
    parentNode = parentNode || tree.get_node($.jstree.root);
    parentNode.children.forEach(function(childNodeId) {
      if (tree.is_disabled(childNodeId) && tree.is_undetermined(childNodeId)) {
        // Mark the child node as selected, otherwise deselect_node has no effect.
        tree.get_node(childNodeId).state.selected = true;
      }
      tree.deselect_node(childNodeId);
    });
  };

  var deselectDisabledNodes = function(tree, nodeIds) {
    nodeIds.forEach(function(nodeId) {
      if (tree.is_disabled(nodeId) && tree.is_selected(nodeId)) {
        // Deselect the node without propagating the change.
        var originalTrigger = tree.trigger;
        try {
          tree.trigger = function() {};
          // Leave disabled nodes that are not loaded yet in the undetermined state.
          if (!tree.is_loaded(nodeId) && tree.is_parent(nodeId)) {
            var node = tree.get_node(nodeId);
            node.original.state.undetermined = true;
            node.state.undetermined = true;
          }
          tree.deselect_node(nodeId);
        } finally {
          tree.trigger = originalTrigger;
        }
      }
    });
  };

  /**
   * This function is called recursively to process each node in the export tree in order to built properly the map of
   * pages to include and exclude from the export package.
   *
   * @param tree: the export tree instance
   * @param parentNode: the parent node to process; the parent node should be loaded and it must not be a leaf node
   * @param exportPages: a map of list, keys are pages to include, values are list of pages to exclude
   */
  var collectExportPages = function(tree, parentNode, exportPages) {
    var includedPages = [];
    var excludedPages = [];

    // First we need to put the parent node in the right list.
    var pageId = parentNode.data.type === 'document' && parentNode.data.id;
    if (pageId) {
      if (tree.is_checked(parentNode)) {
        includedPages.push(pageId);
      } else {
        excludedPages.push(pageId);
      }
    }

    // Then process its child nodes.
    var childNodes = parentNode.children.map(function(childId) {
      return tree.get_node(childId);
    });
    childNodes.filter(function(child) {
      // We're interested only in document child nodes.
      return child.data.type === 'document';
    }).forEach(function (child) {
      var childPageId = child.data.id;

      // If the child node doesn't have children (easy case)...
      if (tree.is_leaf(child)) {
        // ...add the child page reference to the right list.
        // Note that a leaf node can't have an undetermined state (it's either checked or unchecked).
        if (tree.is_checked(child)) {
          includedPages.push(childPageId);
        } else {
          excludedPages.push(childPageId);
        }

      // If the child node has its own children...
      } else {
        var childPageReference = XWiki.Model.resolve(childPageId, XWiki.EntityType.DOCUMENT);
        var childPageJoker = XWiki.Model.serialize(new XWiki.EntityReference('%', XWiki.EntityType.DOCUMENT,
          childPageReference.parent));

        if (tree.is_checked(child) && !tree.is_loaded(child)) {
          // Include the entire sub-tree of pages.
          includedPages.push(childPageJoker);
        } else {
          // The child node is either unchecked or undetermined or it is loaded, in which case we exclude the entire
          // sub-tree at this point because we will collect the export pages from this sub-tree later on recursively.
          // E.g. I'm in Foo.% I selected Foo.Bar but only some of its children: I need Foo.Bar.% to be excluded in the
          // request with Foo.% then another part of the request will select Foo.Bar.X, X being the selected children.
          excludedPages.push(childPageJoker);
        }
      }
    });

    // We can manage the export by specifying either the pages to include or the pages to exclude:
    // * if the parent node is not loaded and undetermined then we have to use excludes (export the child nodes without
    //   their parent)
    // * if the parent node has a child pagination node then we decide based on whether the pagination node is selected
    //    or not. If the pagination node is selected then we can't use includes because there are child pages we don't
    //    know yet. If the pagination node is deselected then we can't use excludes for the same reason: there are child
    //    pages we don't know yet.
    // * if there's no pagination child node then we decide based on whether the parent node is selected or not. If the
    //   parent node is selected then we use excludes (select the parent except for ...). Otherwise, we use includes
    //   (this has the effect that the child pages that don't appear in the tree, for any reason, are not included).
    var paginationNode = childNodes.find(childNode => childNode.data.type === 'pagination');
    var useExcludes = (!tree.is_loaded(parentNode) && tree.is_undetermined(parentNode)) ||
      (paginationNode && tree.is_checked(paginationNode)) ||
      (!paginationNode && tree.is_checked(parentNode));
    if (useExcludes) {
      var parentReference = XWiki.Model.resolve(parentNode.data.id, XWiki.EntityType.byName(parentNode.data.type));
      if (parentReference.type === XWiki.EntityType.DOCUMENT) {
        // Use the space reference.
        parentReference = parentReference.parent;
      } else if (parentReference.type === XWiki.EntityType.WIKI) {
        // Match any space from the specified wiki.
        parentReference = new XWiki.EntityReference('%', XWiki.EntityType.SPACE, parentReference);
      }
      parentReference = new XWiki.EntityReference('%', XWiki.EntityType.DOCUMENT, parentReference);
      var pageJoker = XWiki.Model.serialize(parentReference);
      exportPages[pageJoker] = excludedPages;
    } else {
      includedPages.forEach(function(includedPage) {
        exportPages[includedPage] = [];
      });
    }

    // Process the loaded & undetermined non-leaf children recursively.
    childNodes.forEach(function(child) {
      if (!tree.is_leaf(child) && (tree.is_loaded(child) || tree.is_undetermined(child))) {
        collectExportPages(tree, child, exportPages);
      }
    });
  };

  var exportTreeAPI = {
    getExportPages: function() {
      var exportPages = {};
      collectExportPages(this, this.get_node($.jstree.root), exportPages);
      return exportPages;
    },
    hasExportPages: function() {
      return this.get_checked().length > 0 || this.get_undetermined().length > 0;
    },
    isExportingAllPages: function() {
      var nodes = this.get_json(null, {
        flat: true,
        no_data: true,
        no_state: true,
        no_a_attr: true,
        no_li_attr: true
      });
      // Check if there are any unselected nodes.
      for (var i = 0; i < nodes.length; i++) {
        var nodeId = nodes[i].id;
        if (!this.is_selected(nodeId) && !this.is_undetermined(nodeId)) {
          return false;
        }
      }
      return true;
    },
    deselectChildNodes: function(node) {
      deselectChildNodes(this, this.get_node(node));
    }
  };

  var exportTreeSettings = {
    plugins: ['checkbox', 'contextmenu'],
    checkbox: {
      cascade: 'down+undetermined',
      // We don't want disabled nodes to be marked as selected when all their children are selected.
      three_state: false
    },
    contextmenu: {
      select_node: false,
      items: function (node) {
        var tree = $.jstree.reference(node);
        return {
          select_children: {
            label: l10n['core.exporter.selectChildren'],
            icon: icons.check.cssClass || icons.check.url,
            action: function () {
              selectChildNodes(tree, node);
            },
            _disabled: !tree.is_open(node)
          },
          unselect_children: {
            label: l10n['core.exporter.unselectChildren'],
            icon: icons.shape_square.cssClass || icons.shape_square.url,
            action: function () {
              deselectChildNodes(tree, node);
            },
            _disabled: !tree.is_open(node)
          }
        };
      }
    }
  };

  $.fn.exportTree = function(settings) {
    return this.on('select_node.jstree select_all.jstree', function(event, data) {
      // We don't use the list of selected nodes from the event data because the selection might have been changed by
      // the previous listeners (e.g. the checkbox plugin).
      deselectDisabledNodes(data.instance, data.instance.get_selected().slice());
    }).on('model.jstree', function(event, data) {
      // When new child nodes are loaded..
      var tree = data.instance;
      // Make sure the new nodes have an original state, even if empty, because some jsTree functions
      // (e.g. is_undetermined) fail otherwise.
      data.nodes.forEach(function(nodeId) {
        var node = tree.get_node(nodeId);
        node.original.state = node.original.state || {};
      });
      if (tree.is_selected(data.parent)) {
        // If the parent is selected then the checkbox plugin pre-selects the new child nodes.
        deselectDisabledNodes(tree, data.nodes);
      } else if (tree.is_disabled(data.parent) && tree.is_undetermined(data.parent)) {
        // Pre-select the child nodes by default when the parent selection is undetermined.
        selectNodes(tree, data.nodes);
      }
    }).on('click', '.jstree-anchor.jstree-disabled', function(event) {
      // Open / close disabled nodes when clicking on them, in order to let the users know that they can still interact
      // with these nodes.
      $(this).jstree('toggle_node', event.target);
    }).one('ready.jstree', function(event, data) {
      var tree = data.instance;
      // Extend the tree API.
      $.extend(tree, exportTreeAPI);
      // Select all the pages by default.
      tree.select_all();
      // Handle the Select All / Node actions.
      $(this).closest('.export-tree-container').find('.export-tree-action.selectAll').on('click', function(event) {
        event.preventDefault();
        tree.select_all();
      }).addBack().find('.export-tree-action.selectNone').on('click', function(event) {
        event.preventDefault();
        tree.deselect_all();
      });
    }).xtree($.extend(true, {}, exportTreeSettings, settings));
  }
});

/**
 * Export Tree Filter
 */
define('xwiki-export-tree-filter', ['jquery', 'bootstrap', 'xwiki-export-tree'], function($) {
  var filterRegex = /filters=(\w*)/;
  var getCurrentFilter = function(url) {
    var result = filterRegex.exec(url);
    return (result && result[1]) || '';
  };

  var saveFilterData = function(exportTree) {
    var dataURL = exportTree.attr('data-url');
    var currentFilter = getCurrentFilter(dataURL);
    var exportTreeFilterData = exportTree.data('exportTreeFilter');
    if (!exportTreeFilterData) {
      exportTreeFilterData = {};
      exportTree.data('exportTreeFilter', exportTreeFilterData);
    }
    var tree = $.jstree.reference(exportTree);
    exportTreeFilterData[currentFilter] = {
      // This doesn't include the root node data so we need to save that separately.
      children: tree.get_json(),
      data: tree.get_node($.jstree.root).data
    }
  };

  var getFilterData = function(exportTree) {
    var dataURL = exportTree.attr('data-url');
    var currentFilter = getCurrentFilter(dataURL);
    return exportTree.data('exportTreeFilter')[currentFilter];
  };

  var originalGetChildren;
  var getChildren = function(node, callback) {
    if (node.id === $.jstree.root) {
      // Use the stored tree data if available.
      var root = getFilterData(this.get_container());
      if (root) {
        node.data = root.data;
        return callback(root.children);
      }
    }
    return originalGetChildren.apply(this, arguments);
  };

  var applyFilter = function(exportTree, filter) {
    // Save the tree data associated with the current filter in case the current filter is applied again later.
    saveFilterData(exportTree);
    // Update the URL used to fetch the tree nodes.
    var dataURL = exportTree.attr('data-url');
    if (filterRegex.test(dataURL)) {
      dataURL = dataURL.replace(filterRegex, 'filters=' + encodeURIComponent(filter));
    } else {
      dataURL += '&filters=' + encodeURIComponent(filter);
    }
    exportTree.attr('data-url', dataURL);
    // Load the tree data associated with the new filter.
    var tree = $.jstree.reference(exportTree);
    if (tree.settings.core.data !== getChildren) {
      originalGetChildren = tree.settings.core.data;
      tree.settings.core.data = getChildren;
    }
    var skipLoading = !!exportTree.data('exportTreeFilter')[filter];
    if (skipLoading) {
      // Disable selection cascading while refreshing the tree if the new filter has a previous state.
      var originalCheckboxCascade = tree.settings.checkbox.cascade;
      tree.settings.checkbox.cascade = '';
      exportTree.one('refresh.jstree', function() {
        tree.settings.checkbox.cascade = originalCheckboxCascade;
      });
    }
    tree.refresh(skipLoading);
  };

  // Handle filter change.
  $(document).on('click', '.export-tree-filter a', function(event) {
    event.preventDefault();
    var li = $(this).closest('li');
    if (!li.hasClass('active')) {
      // Mark the active filter.
      var exportTreeFilter = li.closest('.export-tree-filter');
      exportTreeFilter.find('li.active').removeClass('active');
      li.addClass('active');
      var filter = $(this).data('filter') || '';
      exportTreeFilter.find('input[name="filter"]').val(filter);
      // Show the title of the active filter.
      exportTreeFilter.find('.active-filter-title').text($(this).find('.export-tree-filter-title').text());
      // Apply the selected filter.
      var exportTree = exportTreeFilter.closest('.export-tree-container').find('.export-tree');
      applyFilter(exportTree, filter);
    }
  });
});

require(['jquery'], function ($) {
  // Fill the form with the selected pages from the export tree.
  var createHiddenInputsFromExportTree = function(exportTree, container, filterHiddenPages) {
    var exportPages = exportTree.getExportPages();
    if (filterHiddenPages) {
      // Make sure that all the pages that were checked (selected explicitly) are submitted also using an exact match
      // because otherwise they might be left out by the hidden pages filter (in case they are hidden). Note that hidden
      // nested pages are sometimes displayed in the page tree, even if the current user has opted for not showing
      // hidden pages, because they have descendant pages that are not hidden.
      exportTree.get_checked()
        // Get the JSON for the corresponding tree node because we want to check the node type.
        .map(selectedNodeId => exportTree.get_node(selectedNodeId))
        .filter(selectedNode => {
          // Keep only the selected page nodes that don't have an entry already in exportedPages.
          return selectedNode.data.type === 'document' && !exportPages.hasOwnProperty(selectedNode.data.id);
        })
        // Map to the corresponding page reference.
        .map(selectedPageNode => selectedPageNode.data.id)
        // Add an explicit match to the exported pages.
        .forEach(selectedDocumentReference => {
          exportPages[selectedDocumentReference] = [];
        });
    }
    for (var pages in exportPages) {
      // Includes
      $('<input/>').attr({
        type: 'hidden',
        name: 'pages',
        value: pages
      }).appendTo(container);

      // Excludes
      $('<input/>').attr({
        type: 'hidden',
        name: 'excludes',
        value: aggregatePageNames(exportPages[pages])
      }).appendTo(container);
    }
  };

  //
  // Export Form
  //

  // Enable / disable the corresponding settings when the target XWiki version changes.
  $('#targetXWikiVersion').on('change', function() {
    // Disable all settings.
    $('#targetXWikiVersionSettings fieldset').prop('disabled', true);
    // Enable the settings that correspond to the selected value.
    $('#targetXWikiVersionSettings fieldset[data-version-range="' + $(this).val() + '"]').prop('disabled', false);
  });

  // Enable / disable the submit buttons whenever the selection changes in the tree.
  $('.export-tree').on('ready.jstree changed.jstree', function (event, data) {
    const tree = data.instance;
    const disabled = typeof tree.hasExportPages !== 'function' || !tree.hasExportPages();
    $(this).closest('form#export').find('input[type="submit"]').prop('disabled', disabled);
  });

  // Create the container for the hidden inputs used to submit the selected pages from the export tree.
  var hiddenContainer = $('<div class="hidden"></div>').insertAfter('.export-tree');

  $('form#export').on('submit', function() {
    var exportTree = $.jstree.reference($(this).find('.export-tree'));
    // We submit only the tree filter when all nodes are selected (in order to optimize the final database query).
    if (exportTree && !exportTree.isExportingAllPages()) {
      createHiddenInputsFromExportTree(exportTree, hiddenContainer.empty());
    }
  });

  //
  // Export Modal
  //

  const exportModal = $('#exportModal').on('show.bs.modal', function() {
    $(this).find('.xwiki-select.xwiki-export-formats').xwikiSelectWidget('clearSelection');
  });

  // Catch the event on the document in order to allow export formats to prevent the default behavior.
  $(document).on('xwiki:select:updated', '.xwiki-select.xwiki-export-formats', function() {
    const exportFormat = $(this).find('.xwiki-select-option-selected input[name=exportFormat]');
    if (exportFormat.length) {
      const exportURL = exportFormat.attr('data-url');
      if ($('#exportTreeModal').length && exportFormat.attr('data-multipage') === 'true') {
        // Switch to export tree modal to allow the user to select the pages to export.
        switchToExportTreeModal({
          id: exportFormat.val(),
          label: exportFormat.parent('.xwiki-select-option').find('label').text(),
          icon: exportFormat.parent('.xwiki-select-option').find('.xwiki-select-option-icon').children().clone(),
          url: exportURL,
          filterHiddenPages: !!exportFormat.data('filterHiddenPages'),
          excludeNestedPagesByDefault: !!exportFormat.data('excludeNestedPagesByDefault')
        });
      } else {
        // Export the current page.
        window.location.href = exportURL;
      }
    }
  });

  const switchToExportTreeModal = (config) => {
    // Show the export tree modal only after the export modal is completely hidden, otherwise the code that hides the
    // export modal can revert changes done by the code that shows the export tree modal (e.g. we loose the 'modal-open'
    // CSS class on the BODY element which is needed in order to hide the page scrollbars).
    exportModal.one('hidden.bs.modal', () => {
      // Enable the animation back for the next time the export modal is shown.
      exportModal.addClass('fade');
      openExportTreeModal(config);
    // Disable the animation when moving to the next step (export tree modal) in order to have a smooth transition.
    }).removeClass('fade').modal('hide');
  };

  const exportTreeModal = $('#exportTreeModal');
  const openExportTreeModal = (config) => {
    exportTreeModal.find('.modal-title-icon').empty().append(config.icon);
    exportTreeModal.find('.modal-title span.export-format').text(config.label);
    // Set the export tree modal configuration.
    exportTreeModal.data('config', config);
    // Disable the animation on show in order to have a smooth transition from the previous modal.
    exportTreeModal.removeClass('fade').modal();
  };

  //
  // Export Tree Modal
  //

  // Reload the export tree whenever the tree configuration changes (different export formats may require different
  // export tree configuration).
  exportTreeModal.on('show.bs.modal', () => {
    const treeElement = exportTreeModal.find('.export-tree').attr('data-ready', 'false');
    waitForExportTreeReady().then(maybeFilterHiddenPages).then(maybeExcludeNestedPagesByDefault).catch((e) => {
      console.log('Failed to update the export tree. Reason: ' + e);
    }).finally(() => {
      treeElement.attr('data-ready', 'true');
    });
  });

  const waitForExportTreeReady = () => {
    const treeElement = exportTreeModal.find('.export-tree');
    const tree = treeElement.jstree(true);
    if (typeof tree.getExportPages === 'function') {
      return Promise.resolve();
    } else {
      return new Promise((resolve, reject) => {
        treeElement.one('ready.jstree', resolve);
      });
    }
  };

  const maybeFilterHiddenPages = () => {
    const treeElement = exportTreeModal.find('.export-tree');
    const config = exportTreeModal.data('config');
    let treeURL = new URL(treeElement.attr('data-url'), window.location.href);
    const treeURLParams = new URLSearchParams(treeURL.search);
    const filterHiddenPages = treeURLParams.get('filterHiddenDocuments') === 'true';
    if (filterHiddenPages !== config.filterHiddenPages) {
      // We need to reload the tree with the new configuration.
      return new Promise((resolve, reject) => {
        treeURLParams.set('filterHiddenDocuments', config.filterHiddenPages);
        treeURL = new URL('?' + treeURLParams.toString(), treeURL);
        treeElement.attr('data-url', treeURL.toString());
        treeElement.one('refresh.jstree', resolve).jstree('refresh');
      });
    } else {
      return Promise.resolve();
    }
  };

  const maybeExcludeNestedPagesByDefault = () => {
    const config = exportTreeModal.data('config');
    if (config.excludeNestedPagesByDefault) {
      return new Promise((resolve, reject) => {
        const tree = exportTreeModal.find('.export-tree').jstree(true);
        const currentPageNodeId = 'document:' + XWiki.Model.serialize(XWiki.currentDocument.documentReference);
        tree.open_node(currentPageNodeId, () => {
          tree.select_node(currentPageNodeId);
          tree.deselectChildNodes(currentPageNodeId);
          resolve();
        });
      });
    } else {
      return Promise.resolve();
    }
  };

  // Enable / disable the submit buttons whenever the selection changes in the tree.
  exportTreeModal.find('.export-tree').on('ready.jstree changed.jstree refresh.jstree', function (event, data) {
    const tree = data.instance;
    const disabled = typeof tree.hasExportPages !== 'function' || !tree.hasExportPages();
    exportTreeModal.find('.modal-footer .btn-primary').prop('disabled', disabled);
  });

  // Useful to create quickly the right String given an array of page names.
  var aggregatePageNames = function (arrayOfNames) {
    return arrayOfNames.map(function (name) {
      return encodeURIComponent(name);
    }).join("&");
  };

  // Create the hidden form that we're going to use.
  var form = $('<form></form>').attr({
    id: 'export-modal-form',
    method: 'post'
  }).appendTo("body");

  // Export modal submit.
  exportTreeModal.find('.modal-footer .btn-primary').on('click', function (event) {
    var exportTree = exportTreeModal.find('.export-tree');
    // Make sure to remove any preselected page from the export URL since we're going to take the pages from the tree.
    const exportURL = exportTreeModal.data('config').url;
    form.empty().attr('action', exportURL.replace(/pages=.*?(&|$)/g, ''));
    // Fill the form and submit.
    const filterHiddenPages = exportTreeModal.data('config').filterHiddenPages;
    createHiddenInputsFromExportTree($.jstree.reference(exportTree), form, filterHiddenPages);
    exportTreeModal.find('input[type="hidden"][name="filter"]').clone().appendTo(form);
    form.submit();
  });

  //
  // Load the export tree.
  //

  require(['xwiki-export-tree', 'xwiki-export-tree-filter'], function () {
    $('.export-tree').exportTree();
  });
});

// End JavaScript-only code.
}).apply(']]#', $jsontool.serialize([$l10n, $icons]));
