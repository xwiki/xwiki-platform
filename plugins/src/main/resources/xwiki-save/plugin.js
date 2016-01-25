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
  CKEDITOR.plugins.add('xwiki-save', {
    editors: [],

    init: function(editor) {
      this.editors.push(editor);
      editor.on('destroy', $.proxy(function(event) {
        var index = this.editors.indexOf(editor);
        if (index >= 0) {
          this.editors.splice(index, 1);
        }
      }, this));

      editor.addCommand('xwiki-saveAndContinue', {
        canUndo: false,
        context: false,
        contextSensitive: false,
        editorFocus: false,
        exec: function(editor) {
          if (editor.checkDirty()) {
            var config = editor.config['xwiki-save'] || {};
            var saveAndContinueButton = config.saveAndContinueButton || 'input[name=action_saveandcontinue]';
            $(saveAndContinueButton).click();
          }
        },
        modes: {
          wysiwyg: 1
        }
      });

      editor.setKeystroke(CKEDITOR.ALT + CKEDITOR.SHIFT + 83 /*S*/, 'xwiki-saveAndContinue');
    },

    onLoad: function() {
      var thisPlugin = this;
      $(document).on('xwiki:actions:beforeSave', function(event) {
        thisPlugin.editors.forEach(function(editor) {
          editor.updateElement();
          thisPlugin.updateContentType(editor);
        });
      });
      $(document).on('xwiki:actions:cancel xwiki:actions:preview xwiki:actions:save xwiki:document:saved',
        function(event, data) {
          // We reset the dirty field on 'xwiki:actions:save' only if it's not a Save & Continue. Otherwise we wait for
          // 'xwiki:document:saved' to be sure the document was saved.
          if (!data || !data.continue) {
            thisPlugin.editors.forEach(function(editor) {
              editor.resetDirty();
            });
          }
        });
      $(window).on('beforeunload', function(event) {
        if (thisPlugin.checkDirty()) {
          event.returnValue = 'There are unsaved changes. Do you want to discard them?';
          return event.returnValue;
        }
      });
    },

    updateContentType: function(editor) {
      var fieldName = editor.element && editor.element.getNameAtt();
      if (typeof fieldName === 'string') {
        $('input[type=hidden][name=RequiresHTMLConversion]').filter(function() {
          return $(this).val() === fieldName;
        }).prop('disabled', editor.mode === 'source');
      }
    },

    checkDirty: function() {
      for (var i = 0; i < this.editors.length; i++) {
        var editor = this.editors[i];
        var config = editor.config['xwiki-save'] || {};
        if (config.leaveConfirmation && editor.checkDirty()) {
          return true;
        }
      }
      return false;
    }
  });
})();
