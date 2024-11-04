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
define('entityResourcePickerTranslationKeys', [], [
  'select',
  'doc.title',
  'attach.title'
]);

define('entityResourcePicker', [
  'jquery', 'resource', 'modal', 'l10n!entityResourcePicker', 'xwiki-meta', 'xwiki-tree'
], function($, $resource, $modal, translations, xm) {
  'use strict';

  var createdNodes = {};
  var createTreeElement = function(attributes) {
    return $(document.createElement('div')).attr($.extend({
      'class': 'ckeditor-tree jstree-no-links',
      'data-edges': true,
      'data-finder': true,
      'data-icons': true,
      'data-responsive': true
    }, attributes));
  };

  var createTreePicker = function(modal, handler) {
    var treeElement = modal.find('.ckeditor-tree');
    var selectButton = modal.find('.modal-footer .btn-primary');

    var validateSelection = function(tree) {
      // jshint camelcase:false
      var selectedNodes = tree.get_selected(true);
      for (var i = 0; i < selectedNodes.length; i++) {
        if (!handler.canSelect(selectedNodes[i])) {
          return false;
        }
      }
      return selectedNodes.length > 0;
    };

    modal.on('shown.bs.modal', function(event) {
      // Open to the specified node only once. Preserve the tree state otherwise.
      var openToNodeId = handler.openToNodeId;
      if (typeof openToNodeId === 'string' && openToNodeId !== modal.data('openTo')) {
        modal.data('openTo', openToNodeId);
      } else {
        openToNodeId = false;
      }
      var tree = $.jstree.reference(treeElement);
      if (!tree) {
        // Initialize the tree and hook the event listeners.
        tree = treeElement.xtree({
          core: {
            multiple: treeElement.data('multiple') === 'true'
          }
        }).one('ready.jstree', function(event, data) {
          if (openToNodeId) {
            data.instance.openTo(openToNodeId);
          }
        }).on('changed.jstree', function(event, data) {
          selectButton.prop('disabled', !validateSelection(data.instance));
        }).on('dblclick', '.jstree-anchor', function(event) {
          if (validateSelection($.jstree.reference(this))) {
            selectButton.click();
          }
        }).on('select_node.jstree', function (event, data) {
          if (data.node.data.type === 'addDocument') {
            let inst = data.instance;
            inst.edit(data.node, null, function (node) {
              // jshint camelcase:false
              $.jstree.reference(treeElement).deselect_all();
            });
          }
        }).on('xtree.runJob', function (event, promise, action, node, params) {
          if (action === 'create') {
            promise.then(function (promiseData) {
              if (createdNodes[params.id] === undefined) {
                createdNodes[params.id] = {};
              }
              if (promiseData instanceof Array) {
                let newNode = promiseData[0];
                createdNodes[params.id][newNode.id] = newNode;
              }
            });
          }
        }).on('refresh_node.jstree', function (event, data) {
          let parentNodeId = data.node.id;
          let nodesToCreate = createdNodes[parentNodeId];
          if (nodesToCreate) {
            let tree = $.jstree.reference(treeElement);
            // jshint camelcase:false
            let parentNode = tree.get_node(parentNodeId);
            for (let createdNodeId in nodesToCreate) {
              // Check that the node is not created already.
              let createdNode = nodesToCreate[createdNodeId];
              if (tree.get_node(createdNode.id) === false) {
                // jshint camelcase:false
                tree.create_node(parentNode, createdNode, 'last', null);
              }
            }
          }
        });
      } else if (openToNodeId) {
        // jshint camelcase:false
        tree.deselect_all();
        tree.close_all();
        tree.openTo(openToNodeId);
      }
    });

    selectButton.on('click', function() {
      modal.modal('hide');
      var tree = $.jstree.reference(treeElement);
      // jshint camelcase:false
      handler.select(tree.get_selected(true));
    });

    handler.open = function(openToNodeId) {
      this.openToNodeId = openToNodeId;
      modal.modal();
    };

    return handler;
  };

  var getEntityReference = function(node) {
    var separatorIndex = node.id.indexOf(':');
    var nodeType = node.id.substr(0, separatorIndex);
    var nodeStringReference = node.id.substr(separatorIndex + 1);
    return XWiki.Model.resolve(nodeStringReference, XWiki.EntityType.byName(nodeType));
  };

  var getEntity = function(node) {
    return {
      title: node.text,
      reference: getEntityReference(node)
    };
  };

  var getEntityNodeId = function(entityReference) {
    return XWiki.EntityType.getName(entityReference.type) + ':' + XWiki.Model.serialize(entityReference);
  };

  var resolveDocumentReference = function(entityReference) {
    var documentReference = entityReference.extractReference(XWiki.EntityType.DOCUMENT);
    if (!documentReference) {
      var spaceReference = entityReference.extractReference(XWiki.EntityType.SPACE);
      if (!spaceReference) {
        var wikiReference = entityReference.extractReference(XWiki.EntityType.WIKI) ||
          new XWiki.WikiReference(XWiki.currentWiki);
        spaceReference = new XWiki.EntityReference('Main', XWiki.EntityType.SPACE, wikiReference);
      }
      documentReference = new XWiki.EntityReference('WebHome', XWiki.EntityType.DOCUMENT, spaceReference);
    }
    return documentReference;
  };

  var getNodeToOpenTo = function(targetEntityType, entityReference) {
    var targetEntityReference = entityReference.extractReference(targetEntityType);
    if (targetEntityReference) {
      // The target entity is a parent of the specified entity.
      return getEntityNodeId(targetEntityReference);

    // The target entity might be a child of the specified entity.
    } else if (targetEntityType === XWiki.EntityType.DOCUMENT) {
      return getEntityNodeId(resolveDocumentReference(entityReference));
    } else if (targetEntityType === XWiki.EntityType.ATTACHMENT) {
      return 'attachments:' + XWiki.Model.serialize(resolveDocumentReference(entityReference));
    }

    // Otherwise just try to open to the specified entity.
    return getEntityNodeId(entityReference);
  };

  var createEntityTreePicker = function(modal, handler) {
    var treePickerHandler = createTreePicker(modal, {
      canSelect: function(node) {
        return (node.data || {}).type === handler.entityType;
      },
      select: function(nodes) {
        handler.select(nodes.map(getEntity));
      }
    });
    handler.open = function(entityReference) {
      treePickerHandler.open(getNodeToOpenTo(XWiki.EntityType.byName(handler.entityType), entityReference));
    };
    return handler;
  };

  var convertEntityToResource = function(entity, base) {
    return $.extend({}, entity, {
      reference: $resource.convertEntityReferenceToResourceReference(entity.reference, base),
      entityReference: entity.reference
    });
  };

  var createResourcePicker = function(modal, handler) {
    var entityTreePickerHandler = function(base) {
      return createEntityTreePicker(modal, {
        entityType: $resource.types[handler.resourceType].entityType,
        select: function(entities) {
          handler.select(entities.map(function (entity) {
            return convertEntityToResource(entity, base);
          }));
        }
      });
    };
    handler.open = function(resourceReference, base) {
      entityTreePickerHandler(base).open($resource.convertResourceReferenceToEntityReference(resourceReference, base));
    };
    return handler;
  };

  var treeURL = {
    doc: new XWiki.Document('DocumentTree', 'XWiki').getURL('get', $.param({
      outputSyntax: 'plain',
      language: $('html').attr('lang'),
      showAttachments: false,
      showTranslations: false,
      showWikis: true,
      showAddDocument: true,
      readOnly: false,
      // jshint camelcase:false
      form_token: xm.form_token
    })),
    attach: new XWiki.Document('DocumentTree', 'XWiki').getURL('get', $.param({
      outputSyntax: 'plain',
      language: $('html').attr('lang'),
      showTranslations: false,
      showWikis: true
    }))
  };

  var registerResourcePicker = function(resourceType, title) {
    var modal = $modal.createModal({
      'class': 'entity-resource-picker-modal',
      title: title,
      content: createTreeElement({
        'data-url': treeURL[resourceType],
        'data-hasContextMenu': true
      }),
      acceptLabel: translations.get('select')
    });

    var picker = createResourcePicker(modal, {resourceType: resourceType});
    $resource.pickers[resourceType] = function(resourceReference, base) {
      var deferred = $.Deferred();
      picker.select = function(resources) {
        // We assume that only one resource can be selected.
        deferred.resolve(resources[0]);
      };
      picker.open(resourceReference, base);
      return deferred.promise();
    };
  };

  if (typeof $.fn.xtree === 'function') {
    registerResourcePicker('doc', translations.get('doc.title'));
    registerResourcePicker('attach', translations.get('attach.title'));
  }
});
