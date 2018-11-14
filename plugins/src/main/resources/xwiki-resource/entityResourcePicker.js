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
  'jquery', 'resource', 'modal', 'l10n!entityResourcePicker', 'tree'
], function($, $resource, $modal, translations) {
  'use strict';

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
      for (var i = 0; i < selectedNodes.size(); i++) {
        if (!handler.canSelect(selectedNodes[i])) {
          return false;
        }
      }
      return selectedNodes.size() > 0;
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
        });
      } else if (openToNodeId) {
        // jshint camelcase:false
        tree.deselect_all();
        tree.close_all();
        tree.openTo(openToNodeId);
      }
    });

    selectButton.click(function() {
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
      var openToNodeId = XWiki.EntityType.getName(entityReference.type) + ':' + XWiki.Model.serialize(entityReference);
      treePickerHandler.open(openToNodeId);
    };
    return handler;
  };

  var convertEntityToResource = function(entity) {
    return $.extend({}, entity, {
      reference: $resource.convertEntityReferenceToResourceReference(entity.reference),
      entityReference: entity.reference
    });
  };

  var createResourcePicker = function(modal, handler) {
    var entityTreePickerHandler = createEntityTreePicker(modal, {
      entityType: $resource.types[handler.resourceType].entityType,
      select: function(entities) {
        handler.select(entities.map(convertEntityToResource));
      }
    });
    handler.open = function(resourceReference) {
      entityTreePickerHandler.open($resource.convertResourceReferenceToEntityReference(resourceReference));
    };
    return handler;
  };

  var treeURL = {
    doc: new XWiki.Document('DocumentTree', 'XWiki').getURL('get', $.param({
      outputSyntax: 'plain',
      language: $('html').attr('lang'),
      showAttachments: false,
      showTranslations: false,
      showWikis: true
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
        'data-url': treeURL[resourceType]
      }),
      acceptLabel: translations.get('select')
    });

    var picker = createResourcePicker(modal, {resourceType: resourceType});
    $resource.pickers[resourceType] = function(resourceReference) {
      var deferred = $.Deferred();
      picker.select = function(resources) {
        // We assume that only one resource can be selected.
        deferred.resolve(resources[0]);
      };
      picker.open(resourceReference);
      return deferred.promise();
    };
  };

  if (typeof $.fn.xtree === 'function') {
    registerResourcePicker('doc', translations.get('doc.title'));
    registerResourcePicker('attach', translations.get('attach.title'));
  }
});
