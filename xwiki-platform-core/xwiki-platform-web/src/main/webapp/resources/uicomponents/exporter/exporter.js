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
 * Export Tree
 */
define('export-tree', ['jquery', 'tree'], function($) {
  'use strict';

  var selectChildNodes = function(tree, parentNode) {
    parentNode = parentNode || tree.get_node($.jstree.root);
    selectNodes(tree, parentNode.children);
  };

  var selectNodes = function(tree, nodeIds) {
    nodeIds.forEach(function(nodeId) {
      var node = tree.get_node(nodeId);
      // Select the nodes that are enabled or that are disabled but with children (in order to propagate the
      // selection to the descendants, because disabled nodes can have descendants that are enabled).
      if (!node.state.disabled || node.children.length > 0) {
        tree.select_node(node, false, true);
      } else if (node.state.disabled && tree.is_parent(node)) {
        // Leave disabled nodes that are not loaded yet in the undetermined state.
        node.original.state.undetermined = true;
        node.state.undetermined = true;
      }
    });
  };

  var deselectChildNodes = function(tree, parentNode) {
    parentNode = parentNode || tree.get_node($.jstree.root);
    parentNode.children.forEach(function(childNodeId) {
      var childNode = tree.get_node(childNodeId);
      if (childNode.state.disabled && tree.is_undetermined(childNode)) {
        // Mark the child node as selected, otherwise deselect_node has no effect.
        childNode.state.selected = true;
      }
      tree.deselect_node(childNode);
    });
  };

  var deselectDisabledNodes = function(tree, nodeIds) {
    nodeIds.forEach(function(nodeId) {
      var node = tree.get_node(nodeId);
      if (node.state.disabled && node.state.selected) {
        // Deselect the node without propagating the change.
        var originalTrigger = tree.trigger;
        try {
          tree.trigger = function() {};
          // Leave disabled nodes that are not loaded yet in the undetermined state.
          if (!tree.is_loaded(node) && tree.is_parent(node)) {
            node.original.state.undetermined = true;
            node.state.undetermined = true;
          }
          tree.deselect_node(node);
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
   * @param parentNode: the parent node to process
   * @param exportPages: a map of list, keys are pages to include, values are list of pages to exclude
   */
  var collectExportPages = function(tree, parentNode, exportPages) {
    // Set default value for pagination.
    var isPaginationExisting = false;
    var isPaginationChecked = false;

    var nodeId = parentNode.data.id;

    var pageReference = XWiki.Model.resolve(nodeId, XWiki.EntityType.DOCUMENT);
    var pageJoker = XWiki.Model.serialize(new XWiki.EntityReference('%', XWiki.EntityType.DOCUMENT, pageReference.parent));

    var nodeWithChildren = [];
    var includedPages = [];
    var excludedPages = [];

    // First we need to put the parent node in the right list.
    if (!tree.is_checked(parentNode)) {
      excludedPages.push(XWiki.Model.serialize(pageReference));
    } else {
      includedPages.push(XWiki.Model.serialize(pageReference));
    }

    // Then process its children.
    parentNode.children.forEach(function (nodeDom) {

      var node = tree.get_node(nodeDom);
      var subNodeId = node.data.id;
      var subPageReference;

      // if the node does not have children (easy case)
      if (tree.is_leaf(node)) {

        // pagination node is actually a leaf node
        if (node.data.type === "pagination") {
          isPaginationExisting = true;
          isPaginationChecked = tree.is_checked(node);

        // standard document node
        } else {
          subPageReference = XWiki.Model.serialize(XWiki.Model.resolve(subNodeId, XWiki.EntityType.DOCUMENT));

          // the page ref in the right list
          if (tree.is_checked(node)) {
            includedPages.push(subPageReference);
          } else {
            excludedPages.push(subPageReference);
          }
        }

      // if the node has children
      } else {
        subPageReference = XWiki.Model.resolve(subNodeId, XWiki.EntityType.DOCUMENT);
        var subPageJoker = XWiki.Model.serialize(new XWiki.EntityReference('%', XWiki.EntityType.DOCUMENT, subPageReference.parent));

        // if it's not loaded, we'll manage the entire space
        if (!tree.is_loaded(node)) {

          // put the space in the right list
          if (tree.is_checked(node)) {
            includedPages.push(subPageJoker);
          } else {
            excludedPages.push(subPageJoker);
          }
        } else {
          // in that case the page will be managed within its own space, so we need to exclude it
          // in the current context to avoid selecting it in the request
          // e.g. I'm in Foo.% I selected Foo.Bar but only some of its children:
          // I need Foo.Bar.% to be excluded in the request with Foo.%
          // then another part of the request will select Foo.Bar.X, X being the selected children.
          excludedPages.push(subPageJoker);
          nodeWithChildren.push(node);
        }
      }
    });

    // if we don't have a pagination node, or we have one and it's checked:
    // we manage the export with exporting the space and excluding stuff that needs to be
    if (!isPaginationExisting || (isPaginationExisting && isPaginationChecked)) {
      exportPages[pageJoker] = excludedPages;

    // a pagination exists but it's not checked:
    // we need to manage the export by specifying exactly what we include, we don't need to specify exclude
    } else {
      includedPages.forEach(function (elem) {
          exportPages[elem] = [];
        }
      );
    }

    // we call the same function for the children
    nodeWithChildren.forEach(function (node) {
      collectExportPages(tree, node, exportPages);
    });
  };

  var exportTreeAPI = {
    getExportPages: function() {
      var exportPages = {};
      this.get_node($.jstree.root).children.forEach(function(nodeId) {
        collectExportPages(this, this.get_node(nodeId), exportPages);
      }, this);
      return exportPages;
    }
  };

  var icons = {
    check: $jsontool.serialize($services.icon.getMetaData('check')),
    square: $jsontool.serialize($services.icon.getMetaData('shape_square'))
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
            label: $jsontool.serialize($services.localization.render('core.exporter.selectChildren')),
            icon: icons.check.cssClass || icons.check.url,
            action: function () {
              selectChildNodes(tree, node);
            },
            _disabled: !node.state.opened
          },
          unselect_children: {
            label: $jsontool.serialize($services.localization.render('core.exporter.unselectChildren')),
            icon: icons.square.cssClass || icons.square.url,
            action: function () {
              deselectChildNodes(tree, node);
            },
            _disabled: !node.state.opened
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
    }).one('ready.jstree', function(event, data) {
      var tree = data.instance;
      // Extend the tree API.
      $.extend(tree, exportTreeAPI);
      // Select all the pages by default.
      tree.select_all();
      // Handle the Select All / Node actions.
      $(this).closest('.export-tree-container').find('.export-tree-action.selectAll').click(function(event) {
        event.preventDefault();
        tree.select_all();
      }).addBack().find('.export-tree-action.selectNone').click(function(event) {
        event.preventDefault();
        tree.deselect_all();
      });
    }).xtree($.extend(true, {}, exportTreeSettings, settings));
  }
});

/**
 * Export Tree Filter
 */
define('export-tree-filter', ['jquery', 'bootstrap', 'export-tree'], function($) {
  'use strict';

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
    exportTreeFilterData[currentFilter] = tree.get_json();
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
      var children = getFilterData(this.element);
      if (children) {
        return callback(children);
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
    tree.refresh();
  };

  // Handle filter change.
  $('.export-tree-filter a').click(function(event) {
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

require([
  'jquery',
  "$!services.webjars.url('org.xwiki.platform:xwiki-platform-tree-webjar', 'require-config.min.js', {'evaluate': true})"
], function ($) {
  'use strict';

  // Fill the form with the selected pages from the export tree.
  var createHiddenInputsFromExportTree = function(exportTree, container) {
    var exportPages = $.jstree.reference(exportTree).getExportPages();
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

  // Enable the submit buttons when the export tree is ready.
  $('.export-tree').on('ready.jstree', function () {
    $(this).closest('form#export').find('input[type="submit"]').prop('disabled', false);
  })

  // Create the container for the hidden inputs used to submit the selected pages from the export tree.
  var hiddenContainer = $('<div class="hidden"/>').insertAfter('.export-tree');

  $('form#export').submit(function() {
    createHiddenInputsFromExportTree($(this).find('.export-tree'), hiddenContainer.empty());
  });

  //
  // Export Modal
  //

  // Enable the submit buttons when the export tree is ready.
  $('.export-tree').on('ready.jstree', function () {
    $(this).closest('#exportModalOtherCollapse').find('a.btn.disabled').removeClass('disabled');
  })

  // Useful to create quickly the right String given an array of page names.
  var aggregatePageNames = function (arrayOfNames) {
    return arrayOfNames.map(function (name) {
      return encodeURIComponent(name);
    }).join("&");
  };

  // Create the hidden form that we're going to use.
  var form = $('<form/>').attr({
    id: 'export-modal-form',
    method: 'post'
  }).appendTo("body");

  // Export modal submit.
  $('#exportModalOtherCollapse a.btn-primary').click(function (event) {
    var exportTree = $(this).closest('#exportModalOtherCollapse').find('.export-tree');
    if (exportTree.length > 0) {
      event.preventDefault();
      if (!$(this).hasClass('disabled')) {
        // Make sure to remove any preselected page from the submit URL since we're going to take the pages from the tree.
        form.empty().attr('action', $(this).attr('href').replace(/pages=.*?(&|$)/g, ''));
        // Fill the form and submit.
        createHiddenInputsFromExportTree(exportTree, form);
        $(this).closest('#exportModalOtherCollapse').find('input[type="hidden"][name="filter"]').clone().appendTo(form);
        form.submit();
      }
    }
  });

  //
  // Load the export tree.
  //

  require(['export-tree', 'export-tree-filter'], function () {
    $('.export-tree').exportTree();
  });
});
