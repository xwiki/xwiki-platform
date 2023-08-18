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

  var getCacheInput = function(editor) {
    var fieldName = editor.element && editor.element.getNameAtt();
    if (fieldName) {
      return $(editor.element.$).closest('form').find('input.cache').filter(function() {
        return $(this).attr('name') === fieldName + '_cache';
      });
    } else {
      return $();
    }
  };

  var parseCache = function(cacheInput) {
    try {
      return JSON.parse(cacheInput.val() || '{}') || {};
    } catch (exception) {
      if (console) {
        console.log(exception);
      }
      return {};
    }
  };

  CKEDITOR.plugins.add('xwiki-cache', {
    beforeInit: function(editor) {
      // Initialize the cache before the editor is loaded.
      editor._.xwikiCache = parseCache(getCacheInput(editor));
      // Serialize the cache before we leave the page.
      $(window).on('beforeunload', function(event) {
        getCacheInput(editor).val(JSON.stringify(editor._.xwikiCache));
      });
    }
  });
})();
