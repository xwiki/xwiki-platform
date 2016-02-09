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
  var wikiLinkClassPattern =  /\bwiki(\w*)link\b/i;
  var resourceTypeToLinkType = {
    attach: 'attachment',
    mailto: 'external',
    unc: 'external',
    url: 'external'
  };

  CKEDITOR.plugins.add('xwiki-link', {
    requires: 'xwiki-marker,xwiki-resource',

    init: function(editor) {
      editor.plugins['xwiki-marker'].addMarkerHandler(editor, 'wikilink', {
        // startLinkComment: CKEDITOR.htmlParser.comment
        // content: CKEDITOR.htmlParser.node[]
        toHtml: function(startLinkComment, content) {
          if (content.length === 1 && content[0].type === CKEDITOR.NODE_ELEMENT && content[0].children.length === 1 &&
              content[0].children[0].name === 'a') {
            // Remove the link wrapper while editing, but keep the link type in order to preserve the link styles.
            var linkWrapper = content[0];
            var link = linkWrapper.children[0];
            linkWrapper.replaceWithChildren();
            // We store the link type in a data attribute because we don't want the user to see it in the link dialog.
            var linkType = (wikiLinkClassPattern.exec(linkWrapper.attributes['class']) || ['', ''])[1];
            link.attributes['data-linktype'] = linkType;
            // Store the link reference.
            var reference = startLinkComment.value.substring('startwikilink:'.length);
            link.attributes['data-reference'] = CKEDITOR.tools.unescapeComment(reference);
            // Handle the auto-generated link content.
            if (link.children.length === 1 && link.children[0].type === CKEDITOR.NODE_ELEMENT &&
                link.children[0].hasClass('wikigeneratedlinkcontent')) {
              // Store the initial (generated) link content in order to be able to detect changes.
              link.attributes['data-wikigeneratedlinkcontent'] = link.children[0].getHtml();
              link.children[0].replaceWithChildren();
            }
            // Handle free-standing links.
            if (link.hasClass('wikimodel-freestanding')) {
              link.attributes['data-freestanding'] = true;
              // This is an internal class that should not be visible when editing the link.
              link.removeClass('wikimodel-freestanding');
            }
          } else {
            // Unexpected HTML structure inside link markers. Keep the markers.
            return false;
          }
        },
        // element: CKEDITOR.htmlParser.element
        isMarked: function(element) {
          return element.name === 'a' && element.attributes['data-reference'];
        },
        // link: CKEDITOR.htmlParser.element
        toDataFormat: function(link) {
          // Add the start/stop link markers.
          var reference = CKEDITOR.tools.escapeComment(link.attributes['data-reference']);
          var startLinkComment = new CKEDITOR.htmlParser.comment('startwikilink:' + reference);
          var stopLinkComment = new CKEDITOR.htmlParser.comment('stopwikilink');
          startLinkComment.insertBefore(link);
          stopLinkComment.insertAfter(link);
          delete link.attributes['data-reference'];
          // Wrap the link.
          link.wrapWith(new CKEDITOR.htmlParser.element('span', {
            'class': 'wiki' + (link.attributes['data-linktype'] || '') + 'link'
          }));
          delete link.attributes['data-linktype'];
          // Handle the auto-generated link content.
          if (link.attributes['data-wikigeneratedlinkcontent'] === link.getHtml()) {
            // The link content was generated and hasn't been modified. Wrap the link content.
            link.add(new CKEDITOR.htmlParser.element('span', {'class': 'wikigeneratedlinkcontent'}), 0);
            var linkContent = link.children[0];
            while (link.children.length > 1) {
              var child = link.children[1];
              child.remove();
              linkContent.add(child);
            }
            // Only the links with auto-generated content can be free-standing.
            if (link.attributes['data-freestanding'] === 'true') {
              link.addClass('wikimodel-freestanding');
            }
          }
          delete link.attributes['data-wikigeneratedlinkcontent'];
          delete link.attributes['data-freestanding'];
        }
      });
    },

    onLoad: function() {
      overrideLinkPlugin();
    }
  });

  var overrideLinkPlugin = function() {
    var linkPlugin = CKEDITOR.plugins.link;
    if (!linkPlugin) {
      return;
    }
    if (typeof linkPlugin.parseLinkAttributes === 'function') {
      var oldParseLinkAttributes = linkPlugin.parseLinkAttributes;
      linkPlugin.parseLinkAttributes = function(editor, element) {
        var data = oldParseLinkAttributes.call(linkPlugin, editor, element);
        var serializedResourceReference = element && element.getAttribute('data-reference');
        if (serializedResourceReference) {
          data.resourceReference = CKEDITOR.plugins.get('xwiki-resource')
            .parseResourceReference(serializedResourceReference);
        }
        return data;
      };
    }
    if (typeof linkPlugin.getLinkAttributes === 'function') {
      var oldGetLinkAttributes = linkPlugin.getLinkAttributes;
      linkPlugin.getLinkAttributes = function(editor, data) {
        var attributes = oldGetLinkAttributes.call(linkPlugin, editor, data);
        var resourceReference = data.resourceReference;
        if (resourceReference) {
          attributes.set['data-reference'] = CKEDITOR.plugins.get('xwiki-resource')
            .serializeResourceReference(resourceReference);
          attributes.set['data-linktype'] = resourceTypeToLinkType[resourceReference.type] || '';
        }
        return attributes;
      };
    }
  };

  CKEDITOR.on('dialogDefinition', function(event) {
    // Make sure we affect only the editors that load this plugin.
    if (!event.editor.plugins['xwiki-link']) {
      return;
    }

    // Take the dialog window name and its definition from the event data.
    var dialogName = event.data.name;
    var dialogDefinition = event.data.definition;
    if (dialogName === 'link') {
      CKEDITOR.plugins.get('xwiki-resource').replaceWithResourcePicker(dialogDefinition, 'url', {
        resourceTypes: (event.editor.config['xwiki-link'] || {}).resourceTypes || ['doc', 'attach', 'url', 'mailto'],
        setup: function(data) {
          this.setValue(data.resourceReference);
        },
        commit: function(data) {
          data.resourceReference = this.getValue();
          data.resourceReference.typed = data.resourceReference.type !== 'doc' &&
            (data.resourceReference.type !== 'url' || data.resourceReference.reference.indexOf('://') < 0);
        }
      });
      CKEDITOR.plugins.get('xwiki-resource').updateResourcePickerOnFileBrowserSelect(dialogDefinition,
        ['info', 'resourceReference'], ['upload', 'uploadButton']);
    }
  });
})();
