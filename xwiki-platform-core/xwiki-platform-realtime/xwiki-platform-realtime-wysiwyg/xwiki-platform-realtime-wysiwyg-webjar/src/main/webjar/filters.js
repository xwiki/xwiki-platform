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
define('xwiki-realtime-wysiwyg-filters', [], function () {
  'use strict';

  const forbiddenTags = ['SCRIPT', 'IFRAME', 'OBJECT', 'APPLET', 'VIDEO', 'AUDIO'];

  const ignoredAttributes = [
    // Ignore the aria-label attribute because it depends on the current user locale and thus it can create fights.
    'aria-label',
    // Ignore the focused block placeholder attribute because the focus is different for each user and also because the
    // placeholder text depends on the current user locale.
    'data-xwiki-focusedplaceholder',
  ];

  // Widget attributes that may have different values for each user (so they can't really be synchronized).
  const ignoredWidgetAttributes = ['data-cke-widget-upcasted', 'data-cke-widget-id', 'data-cke-filter'];

  // Reject the CKEditor drag and resize handlers (for widgets and images).
  const ignoredWidgetHelpers = [
    'cke_widget_drag_handler_container', 'cke_widget_drag_handler', 'cke_image_resizer'
  ];

  return {
    shouldSerializeNode: function (node) {
      return this.filters.every(filter => typeof filter.shouldSerializeNode !== 'function' ||
        filter.shouldSerializeNode(node));
    },

    filterHyperJSON: function (hjson) {
      return this.filters.reduce((hjson, filter) => {
        return typeof filter.filterHyperJSON === 'function' ? filter.filterHyperJSON(hjson) : hjson;
      }, hjson);
    },

    filters: [
      //-------------------------------------//
      //---------- Generic filters ----------//
      //-------------------------------------//

      //
      // Body filter.
      //
      {
        // Ignore all BODY attributes because they normally store user preferences that shouldn't be shared.
        filterHyperJSON: (hjson) => {
          if (hjson[0] === 'BODY') {
            hjson[1] = {};
          }
          return hjson;
        }
      },

      //
      // BR filter.
      //
      {
        // Catch `type="_moz"` before it goes over the wire.
        filterHyperJSON: (hjson) => {
          if (hjson[1].type === '_moz') {
            delete hjson[1].type;
          }
          return hjson;
        }
      },

      //
      // Ignored attributes filter.
      //
      {
        filterHyperJSON: (hjson) => {
          ignoredAttributes.forEach(attributeName => {
            delete hjson[1][attributeName];
          });
          return hjson;
        }
      },

      //
      // Inline event listeners filter.
      //
      {
        filterHyperJSON: (hjson) => {
          // Don't accept attributes that begin with 'on'. These are probably inline event listeners, and we don't want
          // to send scripts over the wire.
          Object.keys(hjson[1]).filter(name => name.substring(0, 2).toLowerCase() === 'on').forEach(name => {
            delete hjson[1][name];
          });
          return hjson;
        }
      },

      //
      // Forbidden elements filter.
      //
      {
        shouldSerializeNode: (node) => {
          return !forbiddenTags.includes(node.nodeName);
        }
      },

      //
      // Non-Realtime filter.
      //
      {
        // Reject elements with the 'rt-non-realtime' class (e.g. the magic line).
        shouldSerializeNode: (node) => !node.classList?.contains('rt-non-realtime')
      },

      //-----------------------------------------------//
      //---------- CKEditor specific filters ----------//
      //-----------------------------------------------//

      //
      // Widget filter.
      //
      {
        shouldSerializeNode: (node) => !(
          // Reject the hidden (widget) selection and some widget helpers.
          node.nodeType === Node.ELEMENT_NODE &&
          (node.hasAttribute('data-cke-hidden-sel') ||
            ignoredWidgetHelpers.some(className => node.classList.contains(className)))
        ),
        filterHyperJSON: (hjson) => {
          ignoredWidgetAttributes.forEach(attributeName => {
            delete hjson[1][attributeName];
          });
          // Each user may have a different widget selected and/or focused, we don't want to synchronize that.
          if (hjson[1].class) {
            hjson[1].class = hjson[1].class.split(/\s+/).filter(className => ![
              'cke_widget_selected', 'cke_widget_focused', 'cke_widget_editable_focused'
            ].includes(className)).join(' ');
          }
          return hjson;
        }
      },

      //
      // Filling character sequence filter.
      // See https://ckeditor.com/docs/ckeditor4/latest/api/CKEDITOR_dom_selection.html#property-FILLING_CHAR_SEQUENCE
      // See https://bugs.webkit.org/show_bug.cgi?id=15256 (Impossible to place an editable selection inside empty
      // elements)
      //
      {
        // Both shouldSerializeNode and filterHyperJSON are currently called only for elements so in order to filter
        // text nodes we need to filter the direct text child nodes of the element passed to filterHyperJSON.
        filterHyperJSON: (hjson) => {
          const oldChildNodes = hjson[2];
          const newChildNodes = [];
          oldChildNodes.forEach(childNode => {
            if (typeof childNode === 'string') {
              // Remove the filling character sequence from text nodes.
              childNode = childNode.replace(CKEDITOR.dom.selection.FILLING_CHAR_SEQUENCE, '');
              // Ignore text nodes that are used only to allow the user to place the caret inside empty elements.
              if (childNode !== '') {
                newChildNodes.push(childNode);
              }
            } else {
              newChildNodes.push(childNode);
            }
          });
          hjson[2] = newChildNodes;
          return hjson;
        }
      },
    ]
  };
});