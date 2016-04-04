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
(function (){
  'use strict';
  var $ = jQuery;

  var modalTemplate = 
    '<div class="ckeditor-modal modal" tabindex="-1" role="dialog" data-backdrop="static">' +
      '<div class="modal-dialog" role="document">' +
        '<div class="modal-content">' +
          '<div class="modal-header">' +
            '<button type="button" class="close" data-dismiss="modal" aria-label="Close">' +
              '<span aria-hidden="true">&times;</span>' +
            '</button>' +
            '<h4 class="modal-title"></h4>' +
          '</div>' +
          '<div class="modal-body"></div>' +
          '<div class="modal-footer">' +
            '<button type="button" class="btn btn-default" data-dismiss="modal">Cancel</button>' +
            '<button type="button" class="btn btn-primary" disabled="disabled">OK</button>' +
          '</div>' +
        '</div>' +
      '</div>' +
    '</div>';

  var createModal = function(definition) {
    definition = $.extend({
      title: '',
      content: '',
      acceptLabel: 'OK',
      dismissLabel: 'Cancel'
    }, definition);
    var modal = $(modalTemplate).appendTo(document.body);
    modal.find('.modal-title').text(definition.title);
    modal.find('.modal-body').html('').append(definition.content);
    modal.find('.modal-footer .btn-primary').text(definition.acceptLabel);
    modal.find('.modal-footer .btn-default').text(definition.dismissLabel);
    return modal;
  };

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

    modal.one('shown.bs.modal', function(event) {
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

  var getNodeReference = function(node) {
    var separatorIndex = node.id.indexOf(':');
    var nodeType = node.id.substr(0, separatorIndex);
    var nodeStringReference = node.id.substr(separatorIndex + 1);
    return XWiki.Model.resolve(nodeStringReference, XWiki.EntityType.byName(nodeType));
  };

  var createEntityTreePicker = function(modal, handler) {
    var treePickerHandler = createTreePicker(modal, {
      canSelect: function(node) {
        return (node.data || {}).type === handler.entityType;
      },
      select: function(nodes) {
        handler.select(nodes.map(getNodeReference));
      }
    });
    handler.open = function(entityReference) {
      var openToNodeId = XWiki.EntityType.getName(entityReference.type) + ':' + XWiki.Model.serialize(entityReference);
      treePickerHandler.open(openToNodeId);
    };
    return handler;
  };

  var entityTypeToResourceType = ['wiki', 'space', 'doc', 'attach'];
  var convertEntityReferenceToResourceReference = function(entityReference) {
    var relativeReference = entityReference.relativeTo(XWiki.currentDocument.getDocumentReference());
    return {
      type: entityTypeToResourceType[entityReference.type],
      reference: XWiki.Model.serialize(relativeReference)
    };
  };

  var resourceTypeToEntityType = {
    doc: 'document',
    attach: 'attachment'
  };
  var convertResourceReferenceToEntityReference = function(resourceReference) {
    var entityType = XWiki.EntityType.byName(resourceTypeToEntityType[resourceReference.type]);
    return XWiki.Model.resolve(resourceReference.reference, entityType, XWiki.currentDocument.getDocumentReference());
  };

  var createResourcePicker = function(modal, handler) {
    var entityTreePickerHandler = createEntityTreePicker(modal, {
      entityType: resourceTypeToEntityType[handler.resourceType],
      select: function(entityReferences) {
        handler.select(entityReferences.map(convertEntityReferenceToResourceReference));
      }
    });
    handler.open = function(resourceReference) {
      entityTreePickerHandler.open(convertResourceReferenceToEntityReference(resourceReference));
    };
    return handler;
  };

  var registerResourcePicker = function(editor, resourceType, title) {
    if (editor._.resourcePickers[resourceType]) {
      // A resource picker has already been registered for the specified resource type.
      return;
    }

    var entityType = resourceTypeToEntityType[resourceType];
    var treeURL = (editor.config['xwiki-tree'] || {})[entityType + 'TreeURL'];
    if (typeof treeURL !== 'string') {
      return;
    }

    var modal = createModal({
      title: title,
      content: createTreeElement({
        'data-url': treeURL
      }),
      acceptLabel: 'Select'
    });

    var picker = createResourcePicker(modal, {resourceType: resourceType});
    editor._.resourcePickers[resourceType] = function(resourceReference) {
      var deferred = $.Deferred();
      picker.select = function(resourceReferences) {
        // We assume that only one resource can be selected.
        deferred.resolve(resourceReferences[0]);
      };
      picker.open(resourceReference);
      return deferred.promise();
    };
  };

  CKEDITOR.plugins.add('xwiki-tree', {
    requires: 'xwiki-resource',
    init: function(editor) {
      if (typeof $.fn.xtree === 'function') {
        // Register the tree pickers only if the tree is available.
        registerResourcePicker(editor, 'doc', 'Select Page');
        registerResourcePicker(editor, 'attach', 'Select Attachment');
      }
    }
  });
})();
