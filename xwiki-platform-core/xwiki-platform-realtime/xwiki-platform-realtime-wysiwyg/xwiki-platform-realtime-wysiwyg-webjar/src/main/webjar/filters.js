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
define('xwiki-realtime-wysiwygEditor-filters', [], function() {
  'use strict';

  const attributeActions = ['addAttribute', 'modifyAttribute', 'removeAttribute'];
  const forbiddenTags = ['SCRIPT', 'IFRAME', 'OBJECT', 'APPLET', 'VIDEO', 'AUDIO'];

  // Widget attributes that may have different values for each user (so they can't really be synchronized).
  const ignoredWidgetAttributes = ['data-cke-widget-upcasted', 'data-cke-widget-id', 'data-cke-filter'];

  // Reject the CKEditor drag and resize handlers (for widgets and images).
  const ignoredWidgetHelpers = [
    'cke_widget_drag_handler_container', 'cke_widget_drag_handler', 'cke_image_resizer'
  ];

  return {
    filters: [
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
        },
        shouldRejectChange: (change) => change.node?.tagName === 'BODY' && attributeActions.includes(change.diff.action)
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
        },
        shouldRejectChange: (change) => change.diff.name === 'type' &&
          (change.diff.value === '_moz' || change.diff.newValue === '_moz')
      },

      //
      // Widget filter.
      //
      {
        shouldSerializeNode: (node) => !(
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
        },
        shouldRejectChange: (change) =>
          ignoredWidgetHelpers.some(className => change.node?.classList?.contains(className)) ||
          // Reject the hidden (widget) selection.
          change.node?.hasAttribute?.('data-cke-hidden-sel') ||
          // Reject some widget attribute changes.
          ignoredWidgetAttributes.includes(change.diff.name) ||
          // Reject widget selection / focus changes.
          (change.diff.name === 'class' && change.diff.action === 'modifyAttribute' &&
            ['cke_widget_wrapper', 'cke_widget_editable'].some(className => change.node.classList.contains(className)))
      },

      //
      // Non-Realtime filter.
      //
      {
        // Reject elements with the 'rt-non-realtime' class (e.g. the magic line).
        shouldSerializeNode: (node) => !node.classList?.contains('rt-non-realtime'),
        shouldRejectChange: (change) => change.node?.classList?.contains('rt-non-realtime')
      },

      //
      // Translated labels filter.
      //
      {
        filterHyperJSON: (hjson) => {
          delete hjson[1]['aria-label'];
          return hjson;
        },
        // Don't change the aria-label attributes because they depend on the browser locale and they can create fights.
        shouldRejectChange: (change) => change.diff.name === 'aria-label' &&
          attributeActions.includes(change.diff.action)
      },

      //
      // Inline event listeners filter.
      //
      {
        filterHyperJSON: (hjson) => {
          Object.keys(hjson[1]).filter(name => name.substring(0, 2).toLowerCase() === 'on').forEach(name => {
            delete hjson[1][name];
          });
          return hjson;
        },
        // Don't accept attributes that begin with 'on'. These are probably inline event listeners, and we don't want
        // to send scripts over the wire.
        shouldRejectChange: (change) => {
          if (attributeActions.includes(change.diff.action) &&
              change.diff.name.substring(0, 2).toLowerCase() === 'on') {
            return `Rejecting forbidden attribute: ${change.diff.name}`;
          }
        }
      },

      //
      // Forbidden elements filter.
      //
      {
        shouldSerializeNode: (node) => {
          return !forbiddenTags.includes(node.nodeName);
        },
        shouldRejectChange: (change) => {
          if (['addElement', 'replaceElement'].includes(change.diff.action)) {
            const tagName = (change.diff.element || change.diff.newValue)?.nodeName;
            if (forbiddenTags.includes(tagName)) {
              return `Rejecting forbidden element: ${tagName}`;
            }
          }
        }
      }
    ],

    shouldSerializeNode: function(node) {
      return this.filters.every(filter => typeof filter.shouldSerializeNode !== 'function' ||
        filter.shouldSerializeNode(node));
    },

    filterHyperJSON: function(hjson) {
      return this.filters.reduce((hjson, filter) => {
        return typeof filter.filterHyperJSON === 'function' ? filter.filterHyperJSON(hjson) : hjson;
      }, hjson);
    },

    shouldRejectChange: function(change) {
      return this.filters.some(filter => {
        const result = typeof filter.shouldRejectChange === 'function' ? filter.shouldRejectChange(change) : false;
        if (typeof result === 'string') {
          // Log the reason why the change is rejected.
          console.log(result);
        }
        return !!result;
      });
    }
  };
});