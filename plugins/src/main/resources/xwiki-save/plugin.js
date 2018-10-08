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

  // Declare the configuration namespace.
  CKEDITOR.config['xwiki-save'] = CKEDITOR.config['xwiki-save'] || {
    __namespace: true
  };

  CKEDITOR.plugins.add('xwiki-save', {
    requires: 'notification,xwiki-cache,xwiki-localization',
    editors: [],

    beforeInit: function(editor) {
      // Make sure we restore the previous editing mode when the page is loaded from cache (back/forward/reload).
      var contentTypeField = this.getContentTypeField(editor);
      // Chrome doesn't cache the enabled/disabled state of form fields so we are caching it separately.
      if (editor._.xwikiCache[contentTypeField.attr('name')] === false || contentTypeField.prop('disabled')) {
        contentTypeField.prop('disabled', true);
        editor.config.startupMode = 'source';
      }
    },

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
      // We need to update the form fields before the form is validated (for Preview, Save and Save & Continue).
      $(document).on('xwiki:actions:beforePreview xwiki:actions:beforeSave', $.proxy(function(event, data) {
        if (!this.updateFormFields()) {
          event.preventDefault();
          // This is for older versions of XWiki (<10.8.1) where we had to stop the original event.
          if (data && data.originalEvent && typeof data.originalEvent.stop === 'function') {
            data.originalEvent.stop();
          }
        }
      }, this));

      var submitInProgress = false;
      // Disable the leave confirmation when the form action buttons are used.
      $(document).on('xwiki:actions:cancel xwiki:actions:preview xwiki:actions:save xwiki:document:saved',
        $.proxy(function(event, data) {
          // We reset the dirty field on 'xwiki:actions:save' only if it's not a Save & Continue. Otherwise we wait for
          // 'xwiki:document:saved' to be sure the document was saved.
          if (!data || !data['continue']) {
            submitInProgress = event.type === 'xwiki:actions:preview' || event.type === 'xwiki:actions:save';
            this.editors.forEach(function(editor) {
              editor.resetDirty();
            });
          }
        }, this));

      $(window).on('beforeunload', $.proxy(function(event) {
        // Update the form fields before the page is unloaded in order to allow the browser to cache their values
        // (Back-Forward and Soft Reload cache). The form fields have already been updated (for validation) if a submit
        // is currently in progress.
        if (!submitInProgress) {
          // Cache the full data (including wiki macro output).
          this.updateFormFields(true);
        } else {
          submitInProgress = false;
        }
        // Display the leave confirmation if there are unsaved changes.
        var dirtyEditor = this.checkDirty();
        if (dirtyEditor) {
          event.returnValue = dirtyEditor.localization.get('xwiki-save.leaveConfirmationMessage');
          return event.returnValue;
        }
      }, this));
    },

    updateFormFields: function(fullData) {
      var success = true;
      fullData = fullData === true;
      this.editors.forEach(function(editor) {
        var oldFullData;
        if (fullData) {
          oldFullData = editor.config.fullData;
          editor.config.fullData = true;
        }
        try {
          editor.updateElement();
        } catch (e) {
          success = false;
          editor.showNotification(editor.localization.get('xwiki-save.failed'), 'warning');
          console.log(e);
        }
        if (fullData) {
          editor.config.fullData = oldFullData;
        }
        this.updateContentType(editor);
      }, this);
      return success;
    },

    updateContentType: function(editor) {
      var contentTypeField = this.getContentTypeField(editor);
      var disabled = editor.mode === 'source';
      contentTypeField.prop('disabled', disabled);
      // Chrome doesn't cache the enabled/disabled state of form fields so we have to cache it separately.
      editor._.xwikiCache[contentTypeField.attr('name')] = !disabled;
    },

    getContentTypeField: function(editor) {
      var fieldName = editor.element && editor.element.getNameAtt();
      return $('input[name=RequiresHTMLConversion]').filter(function() {
        return $(this).val() === fieldName;
      });
    },

    checkDirty: function() {
      for (var i = 0; i < this.editors.length; i++) {
        var editor = this.editors[i];
        var config = editor.config['xwiki-save'] || {};
        if (config.leaveConfirmation && editor.checkDirty()) {
          return editor;
        }
      }
      return false;
    }
  });
})();
