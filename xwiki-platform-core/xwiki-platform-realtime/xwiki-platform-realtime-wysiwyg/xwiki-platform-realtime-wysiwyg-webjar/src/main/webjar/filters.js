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

  class Filters {
    shouldSerializeNode(node) {
      return this.filters.every(filter => typeof filter.shouldSerializeNode !== 'function' ||
        filter.shouldSerializeNode(node));
    }

    filterHyperJSON(hjson) {
      return this.filters.reduce((hjson, filter) => {
        return typeof filter.filterHyperJSON === 'function' ? filter.filterHyperJSON(hjson) : hjson;
      }, hjson);
    }

    constructor() {
      //-------------------------------------//
      //---------- Generic filters ----------//
      //-------------------------------------//
      this.filters = [
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
            // Don't accept attributes that begin with 'on'. These are probably inline event listeners, and we don't
            // want to send scripts over the wire.
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
        }
      ];
    }
  }

  return Filters;
});