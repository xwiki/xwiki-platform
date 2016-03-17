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
  // Empty plugin required for dependency management.
  CKEDITOR.plugins.add('xwiki-dialog', {});

  CKEDITOR.plugins.xwikiDialog = {
    getUIElementPath: function(elementId, elements) {
      // jshint maxdepth:4
      for (var i = 0; i < elements.length; i++) {
        var element = elements[i];
        if (element.id === elementId) {
          return [{element: element, position: i}];
        } else {
          var children = element.children || element.elements;
          if (children) {
            var path = this.getUIElementPath(elementId, children);
            if (path) {
              path.push({element: element, position: i});
              return path;
            }
          }
        }
      }
      return null;
    },

    replaceWith: function(dialogDefinition, existingElementId, newElementDefinition) {
      var path = this.getUIElementPath(existingElementId, dialogDefinition.contents);
      if (path && path.length > 1) {
        var existingElementPosition = path[0].position;
        var parentElement = path[1].element;
        (parentElement.children || parentElement.elements).splice(existingElementPosition, 1, newElementDefinition);
      }
    }
  };
})();
