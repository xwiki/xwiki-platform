define(['jquery', 'JobRunner', 'jsTree', 'tree-finder'], function($, JobRunner) {
  'use strict';

  // jsTree uses the underscore notation for its API, instead of camel case.
  // jshint camelcase:false,maxstatements:false

  // Fix the regular expression used by jsTree to escape special characters in CSS selectors. It is mainly used to be
  // able to find a tree node by its id using Element#querySelector. We overwrite the default value used by jsTree in
  // order to add the following special characters: ?`.
  // TODO: Drop this after https://github.com/vakata/jstree/pull/914/ is merged.
  $.jstree.idregex = /[\\:&!^|()\[\]<>@*'+~#";.,=\- \/${}%?`]/g;

  var formToken = $('meta[name=form_token]').attr('content');

  var getNodeTypes = function(nodes) {
    var typesMap = {};
    $.each(nodes, function() {
      if (this.data && typeof this.data.type === 'string') {
        typesMap[this.data.type] = true;
      }
    });
    var types = [];
    for (var type in typesMap) {
      if (typesMap.hasOwnProperty(type)) {
        types.push(type);
      }
    }
    return types;
  };

  var getChildren = function(node, callback, parameters) {
    // 'this' is the tree instance.
    callback = $.proxy(callback, this);
    if (node.id === '#' && !node.data) {
      // If the root node doesn't have any data then infer it from its children.
      var nestedCallback = callback;
      callback = function(children) {
        var validChildren = getNodeTypes(children);
        if (validChildren.length > 0) {
          node.data = {validChildren: validChildren};
        }
        nestedCallback(children);
      };
    }
    var childrenURL = node.data && node.data.childrenURL;
    parameters = parameters || {};
    if (!childrenURL) {
      childrenURL = this.element.attr('data-url');
      parameters = $.extend({
        data: 'children',
        id: node.id
      }, parameters);
    }
    if (childrenURL) {
      $.get(childrenURL, parameters)
        .done(callback)
        .fail(function() {
          callback([]);
        });
    } else {
      callback([]);
    }
  };

  var canAcceptChild = function(parent, child) {
    return !parent.data || !parent.data.validChildren ||
      (child.data && parent.data.validChildren.indexOf(child.data.type) >= 0);
  };

  var canPerformBatchOperation = function(nodes, action) {
    for (var i = 0; i < nodes.length; i++) {
      var node = nodes[i];
      if (!node.data || !node.data['can' + action]) {
        return false;
      }
    }
    return true;
  };

  var canCopyNodes = function(nodes) {return canPerformBatchOperation(nodes, 'Copy');};
  var canCutNodes = function(nodes) {return canPerformBatchOperation(nodes, 'Move');};
  var canRemoveNodes = function(nodes) {return canPerformBatchOperation(nodes, 'Delete');};

  var validateOperation = function (operation, node, parent, position, more) {
    // The operation can be 'create_node', 'rename_node', 'delete_node', 'move_node' or 'copy_node'.
    // In case the operation is 'rename_node' the position is filled with the new node name.
    return (operation === 'create_node' && canAcceptChild(parent, node)) ||
      (operation === 'rename_node' && node.data && node.data.canRename) ||
      (operation === 'delete_node' && node.data && node.data.canDelete) ||
      (operation === 'move_node' && node.data && node.data.canMove && canAcceptChild(parent, node)) ||
      (operation === 'copy_node' && node.data && node.data.canCopy && canAcceptChild(parent, node));
  };

  var areDraggable = function(nodes) {
    for (var i = 0; i < nodes.length; i++) {
      var node = nodes[i];
      if (!node.data || !node.data.draggable) {
        return false;
      }
    }
    return true;
  };

  var addMoreChildren = function(tree, paginationNode) {
    // Mark the pagination node as loading to prevent multiple pagination requests for the same offset.
    var paginationElement = tree.get_node(paginationNode.id, true);
    if (paginationElement.hasClass('jstree-loading')) return;
    paginationElement.addClass('jstree-loading');
    // Replace the pagination node with the nodes from the next page.
    var parent = tree.get_node(paginationNode.parent);
    getChildren.call(tree, parent, function(children) {
      var position = paginationElement.parent().children().index(paginationElement[0]);
      tree.delete_node(paginationNode);
      $.each(children, function(index) {
        // Create the node only if it doesn't exist (the node may have been loaded by a call to openTo).
        // Note that deleting or moving the existing node may not be allowed by #validateOperation().
        if (!tree.get_node(this)) {
          tree.create_node(parent, this, position + index, index === 0 && function(firstChild) {
            tree.select_node(firstChild);
          });
        }
      });
    }, {offset: paginationNode.data.offset});
  };

  var disableNodeBeforeLoading = function(tree, node) {
    tree.get_node(node, true).addClass('jstree-loading');
    tree.disable_node(node);
  };

  var enableNodeAfterLoading = function(tree, node) {
    tree.get_node(node, true).removeClass('jstree-loading');
    tree.enable_node(node);
  };

  var createJobRunner = function(treeElement) {
    var jobServiceURL = $(treeElement).attr('data-url');
    return new JobRunner({
      createStatusRequest: function(jobId) {
        return {
          url: jobServiceURL,
          data: {
            id: jobId,
            data: 'jobStatus'
          }
        };
      },
      createAnswerRequest: function(jobId, data) {
        return {
          url: jobServiceURL,
          data: $.extend({}, data, {
            id: jobId,
            action: 'answer',
            form_token: formToken
          })
        };
      }
    });
  };

  var createEntity = function(tree, node) {
    var params = {name: node.text};
    if (node.data && node.data.type) {
      params.type = node.data.type;
    }
    return tree.execute('create', tree.get_node(node.parent), params);
  };

  var deleteEntity = function(tree, node) {
    return tree.execute('delete', node);
  };

  var moveEntity = function(tree, node) {
    return tree.execute('move', node, {
      parent: node.parent,
      name: node.text
    });
  };

  var copyEntity = function(tree, node, newParent) {
    return tree.execute('copy', node, {parent: newParent});
  };

  var getContextMenuItems = function(node, callback) {
    if (!node.data || !node.data.hasContextMenu) return;

    var tree = this;
    var callbackWrapper = function(menu) {
      // This is useful if you want to disable some menu items before the menu is shown.
      tree.element.trigger('xtree.openContextMenu', {
        tree: tree,
        node: node,
        menu: menu
      });
      callback.call(tree, menu);
    };

    if (node.data.contextMenuURL) {
      tree.contextMenuByURL = tree.contextMenuByURL || {};
      var menu = tree.contextMenuByURL[node.data.contextMenuURL];
      if (menu) {
        callbackWrapper(menu);
      } else {
        var menuURL = node.data.contextMenuURL;
        $.get(menuURL, function(menu) {
          tree.contextMenuByURL[menuURL] = fixContextMenuActions(menu);
          callbackWrapper(menu);
        });
      }
    } else if (tree.contextMenuByNodeType) {
      callbackWrapper(tree.contextMenuByNodeType[node.data.type]);
    } else {
      var nodeType = node.data.type;
      $.get(tree.element.attr('data-url'), {data: 'contextMenu'}, function(contextMenuByNodeType) {
        tree.contextMenuByNodeType = fixContextMenusActions(contextMenuByNodeType);
        callbackWrapper(tree.contextMenuByNodeType[nodeType]);
      });
    }
  };

  var fixContextMenusActions = function(menus) {
    for (var type in menus) {
      if (menus.hasOwnProperty(type)) {
        fixContextMenuActions(menus[type]);
      }
    }
    return menus;
  };

  var fixContextMenuActions = function(menu) {
    for (var key in menu) {
      if (menu.hasOwnProperty(key)) {
        var item = menu[key];
        var actionName = item.action || key;
        item.action = createContextMenuAction(actionName, item.parameters);
      }
    }
    return menu;
  };

  var createContextMenuAction = function(action, parameters) {
    return function (data) {
      var tree = $.jstree.reference(data.reference);
      // Make sure the parameters are not modified by the event listeners.
      data.parameters = $.extend(true, {}, parameters || {});
      tree.element.trigger('xtree.contextMenu.' + action, data);
    };
  };

  var prepareNodeTemplate = function(parent, template) {
    var defaultTemplate = {
      text: 'New Child',
      children: false,
      data: {
        // Make sure the specified parent can accept the new child node.
        type: parent.data && parent.data.validChildren && parent.data.validChildren[0],
        // Make sure the created node can be renamed and deleted.
        canRename: true,
        canDelete: true
      }
    };
    return $.extend(true, defaultTemplate, template || {});
  };

  var getPath = function(nodeId, callback) {
    // 'this' is the tree instance.
    callback = $.proxy(callback, this);
    if (this.get_node(nodeId)) {
      // The specified node is already loaded in the tree.
      callback(this.get_path(nodeId, false, true));
    } else {
      // We need to retrieve the node path from the server.
      var url = this.element.attr('data-url');
      if (url) {
        $.get(url, {data: 'path', 'id': nodeId})
          .done(callback)
          .fail(function() {
            callback([]);
          });
      } else {
        callback([]);
      }
    }
  };

  var openPath = function(parent, path, callback) {
    // 'this' is the tree instance.
    if (path && path.length > 0) {
      var child = this.get_node(path[0]);
      var children = this.get_children_dom(parent);
      if (!child && canAcceptChild(parent, path[0]) && children.length > 0 &&
          this.get_node(children.last()).data.type === 'pagination') {
        // The child node is probably not displayed because of the pagination. It's expensive to retrieve all the rest
        // of the children (i.e. all the next pages) until we find the node from the path so we simply add the child to
        // the parent. Don't worry, the node won't be duplicated when the pagination is triggered.
        child = this.get_node(this.create_node(parent, path[0], children.length - 1));
      }
      if (child) {
        // The node is present in the tree. Open it.
        return this.open_node(child, function() {
          openPath.call(this, child, path.slice(1), callback);
        });
      }
    }
    // If we get here then it means we cannot advance any more.
    if (callback) {
      callback(parent);
    }
  };

  var extendQueryString = function(url, parameters) {
    url += url.indexOf('?') < 0 ? '?': '&';
    return url + $.param(parameters);
  };

  var getDefaultParams = function(element) {
    var baseParams = {
      core: {
        // The node label is plain text by default. Otherwise we have to do HTML escaping in lots of places.
        force_text: true,
        // Turn off animations by default.
        animation: false,
        themes: {
          name: 'xwiki',
          responsive: element.attr('data-responsive') !== 'false',
          icons: element.attr('data-icons') !== 'false',
          dots: element.attr('data-edges') !== 'false'
        }
      }
    };
    var plugins = [];
    if (element.attr('data-checkboxes') === 'true') {
      plugins.push('checkbox');
    }
    if (element.attr('data-url')) {
      if (element.attr('data-dragAndDrop') === 'true') {
        plugins.push('dnd');
      }
      if (element.attr('data-contextMenu') === 'true') {
        plugins.push('contextmenu');
      }
      if (element.attr('data-finder') === 'true') {
        plugins.push('finder');
      }
      return $.extend(true, baseParams, {
        core: {
          data: getChildren,
          check_callback: validateOperation
        },
        plugins: plugins,
        dnd: {
          is_draggable: areDraggable
        },
        contextmenu: {
          items: getContextMenuItems
        },
        finder: {
          url: extendQueryString(element.attr('data-url'), {
            data: 'suggestions'
          })
        }
      });
    } else {
      // The tree structure is in-line.
      baseParams.plugins = plugins;
      return baseParams;
    }
  };

  var customTreeAPI = {
    openTo: function(nodeId, callback) {
      // Select the node if no callback is provided.
      callback = callback || $.proxy(this, 'select_node');
      // We need to open all the ancestors of the specified node.
      getPath.call(this, nodeId, function(path) {
        var root = this.get_node('#');
        openPath.call(this, root, path, callback);
      });
    },
    refreshNode: function(node) {
      if (node === '#') {
        // jsTree doesn't want to refresh the root node so we refresh the entire tree.
        this.refresh();
      } else {
        this.refresh_node(node);
      }
    },
    execute: function(action, node, params) {
      var url = node.data && node.data[action + 'URL'];
      params = params || {};
      if (!url) {
        url = this.element.attr('data-url');
        params.action = action;
        params.id = node.id;
      }
      params.form_token = formToken;
      var promise = this.jobRunner.run(url, params);
      this.element.trigger('xtree.runJob', promise);
      return promise;
    }
  };

  $.fn.xtree = function(params) {
    return this.on('select_node.jstree', function(event, data) {
      var tree = data.instance;
      var selectedNode = data.node;
      if (selectedNode.data && selectedNode.data.type === 'pagination') {
        addMoreChildren(tree, selectedNode);
      } else if (data.event && !$(data.event.target).hasClass('jstree-no-link') &&
          $(data.event.target).closest('.jstree-no-links').length === 0) {
        // The node selection was triggered by an user event and links are enabled.
        window.location.href = selectedNode.a_attr.href;
      }

    }).on('open_node.jstree', function(event, data) {
      var originalNode = data.node.original;
      if (originalNode && originalNode.iconOpened) {
        data.instance.set_icon(data.node, originalNode.iconOpened);
      }

    }).on('close_node.jstree', function(event, data) {
      var originalNode = data.node.original;
      if (originalNode && originalNode.iconOpened) {
        data.instance.set_icon(data.node, originalNode.icon);
      }

    //
    // Catch events triggered when the tree structure is modified.
    //

    }).on('create_node.jstree', function(event, data) {
      // We don't create the node right now because we want the user to specify the node name. The node will be created
      // when the user 'renames' the node that has been created with the default name.

    }).on('rename_node.jstree', function(event, data) {
      var entityId = data.node.data && data.node.data.id;
      if (entityId) {
        // Rename a node that has a corresponding entity.
        if (data.old != data.text) {
          disableNodeBeforeLoading(data.instance, data.node);
          moveEntity(data.instance, data.node).always(function() {
            data.instance.refreshNode(data.node.parent);
          });
        }
      } else {
        // Create a new entity.
        disableNodeBeforeLoading(data.instance, data.node);
        createEntity(data.instance, data.node)
          .done(function() {
            data.instance.refreshNode(data.node.parent);
          })
          .fail(function() {
            data.instance.delete_node(data.node);
          });
      }

    }).on('delete_node.jstree', function(event, data) {
      // Make sure the deleted tree node has an associated entity.
      var entityId = data.node.data && data.node.data.id;
      if (entityId) {
        deleteEntity(data.instance, data.node).fail(function() {
          data.instance.refreshNode(data.parent);
        });
      }

    }).on('move_node.jstree', function(event, data) {
      var entityId = data.node.data && data.node.data.id;
      // Don't trigger the server-side move unless the tree node has an entity associated.
      if (!entityId || data.parent === data.old_parent) {
        return;
      }
      disableNodeBeforeLoading(data.instance, data.node);
      moveEntity(data.instance, data.node)
        .done(function() {
          data.instance.refreshNode(data.parent);
        })
        .fail(function(response) {
          // Undo the move.
          // Disconnect the node from the associated entity to prevent moving the entity.
          data.node.data.id = null;
          data.instance.move_node(data.node, data.old_parent, data.old_position);
          // Reconnect the tree node to the entity as soon as possible.
          setTimeout(function() {
            data.node.data.id = entityId;
            enableNodeAfterLoading(data.instance, data.node);
          }, 0);
        });

    }).on('copy_node.jstree', function(event, data) {
      var entityId = data.original.data && data.original.data.id;
      // Don't trigger the server-side copy unless the tree node has an entity associated.
      if (!entityId) {
        return;
      }
      disableNodeBeforeLoading(data.instance, data.node);
      // Copy the original node meta data, without the id, to be able to undo the copy in case of failure.
      data.node.data = $.extend(true, {}, data.original.data);
      delete data.node.data.id;
      copyEntity(data.instance, data.original, data.parent)
        .done(function() {
          data.instance.refreshNode(data.parent);
        })
        .fail(function(response) {
          // Undo the copy.
          data.instance.delete_node(data.node);
        });

    //
    // Catch events triggered by the context menu.
    //

    }).on('xtree.contextMenu.refresh', function(event, data) {
      var tree = $.jstree.reference(data.reference);
      var node = tree.get_node(data.reference);
      tree.refreshNode(node);

    }).on('xtree.contextMenu.create', function(event, data) {
      var tree = $.jstree.reference(data.reference);
      var parent = tree.get_node(data.reference);
      var template = prepareNodeTemplate(parent, data.parameters.template);
      tree.create_node(parent, template, 'first', function(newNode) {
        setTimeout(function() {
          tree.edit(newNode);
        }, 0);
      });

    }).on('xtree.contextMenu.cut', function(event, data) {
      var tree = $.jstree.reference(data.reference);
      tree.cut(tree.get_selected());

    }).on('xtree.contextMenu.copy', function(event, data) {
      var tree = $.jstree.reference(data.reference);
      tree.copy(tree.get_selected());

    }).on('xtree.contextMenu.paste', function(event, data) {
      var tree = $.jstree.reference(data.reference);
      var node = tree.get_node(data.reference);
      tree.paste(node);

    }).on('xtree.contextMenu.rename', function(event, data) {
      var tree = $.jstree.reference(data.reference);
      var node = tree.get_node(data.reference);
      setTimeout(function() {tree.edit(node);}, 0);

    }).on('xtree.contextMenu.remove', function(event, data) {
      var skipConfirmation = data.parameters.confirmationMessage === false;
      var confirmationMessage = data.parameters.confirmationMessage ||
        'Are you sure you want to delete the selected nodes?';
      // Display the confirmation after the context menu closes.
      setTimeout(function() {
        if (skipConfirmation || window.confirm(confirmationMessage)) {
          var tree = $.jstree.reference(data.reference);
          tree.delete_node(tree.get_selected());
        }
      }, 0);

    }).on('xtree.contextMenu.openLink', function(event, data) {
      var tree = $.jstree.reference(data.reference);
      if (data.parameters.urlProperty) {
        var node = tree.get_node(data.reference);
        window.location = node.data[data.parameters.urlProperty];
      } else {
        var nodeElement = tree.get_node(data.reference, true);
        window.location = nodeElement.children('a.jstree-anchor').prop('href');
      }

    }).on('xtree.contextMenu.openLinkInNewTab', function(event, data) {
      var tree = $.jstree.reference(data.reference);
      var node = tree.get_node(data.reference, true);
      window.open(node.children('a.jstree-anchor').prop('href'));

    //
    // Enable/Disable context menu items before the context menu is shown.
    //

    }).on('xtree.openContextMenu', function(event, data) {
      var selectedNodes = data.tree.get_selected(true);
      if (data.menu.copy) {
        data.menu.copy._disabled = !canCopyNodes(selectedNodes);
      }
      if (data.menu.cut) {
        data.menu.cut._disabled = !canCutNodes(selectedNodes);
      }
      if (data.menu.paste) {
        data.menu.paste._disabled = !data.tree.can_paste();
      }
      if (data.menu.rename) {
        data.menu.rename._disabled = !data.node.data || !data.node.data.canRename;
      }
      if (data.menu.remove) {
        data.menu.remove._disabled = !canRemoveNodes(selectedNodes);
      }

    //
    // Un-wrap the links generated from wiki syntax so that they are taken into account by jsTree.
    //

    }).find('li > span[class^="wiki"] > a').unwrap().addBack()

    //
    // Create the tree and extend its API.
    //

    .each(function() {
      $(this).jstree($.extend(true, getDefaultParams($(this)), params || {}));
      $.extend($.jstree.reference(this), customTreeAPI, {jobRunner: createJobRunner(this)});
    });
  };

  return $;
});
