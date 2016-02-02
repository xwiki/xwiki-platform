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
  CKEDITOR.plugins.add('xwiki-image', {
    requires: 'uploadimage,xwiki-marker',

    init: function(editor) {
      editor.plugins['xwiki-marker'].addMarkerHandler(editor, 'image', {
        // comment: CKEDITOR.htmlParser.comment
        toHtml: function(comment) {
          if (comment.next && comment.next.name === 'img') {
            var reference = comment.value.substring('startimage:'.length);
            comment.next.attributes['data-reference'] = CKEDITOR.tools.unescapeComment(reference);
          }
        },
        // element: CKEDITOR.htmlParser.element
        isMarked: function(element) {
          return element.name === 'img' && element.attributes['data-reference'];
        },
        // image: CKEDITOR.htmlParser.element
        toDataFormat: function(image) {
          var reference = CKEDITOR.tools.escapeComment(image.attributes['data-reference']);
          var startImageComment = new CKEDITOR.htmlParser.comment('startimage:' + reference);
          var stopImageComment = new CKEDITOR.htmlParser.comment('stopimage');
          startImageComment.insertBefore(image);
          stopImageComment.insertAfter(image);
          delete image.attributes['data-reference'];
        }
      });
    },

    afterInit: function(editor) {
      // Override the 'uploadimage' widget definition to include the image markers.
      var originalOnUploaded = editor.widgets.registered.uploadimage.onUploaded;
      editor.widgets.registered.uploadimage.onUploaded = function(upload) {
        this.parts.img.setAttribute('data-reference', 'false|-|attach|-|' + upload.fileName);
        originalOnUploaded.call(this, upload);
      };

      var originalReplaceWith = editor.widgets.registered.uploadimage.replaceWith;
      editor.widgets.registered.uploadimage.replaceWith = function(data, mode) {
        var reference = this.parts.img.getAttribute('data-reference');
        if (typeof reference === 'string') {
          var startImageMarker = '<!--startimage:' + CKEDITOR.tools.escapeComment(reference) + '-->';
          var stopImageMarker = '<!--stopimage-->';
          data = startImageMarker + data + stopImageMarker;
        }
        originalReplaceWith.call(this, data, mode);
      };
    }
  });
})();
