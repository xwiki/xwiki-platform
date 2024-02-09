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

    init: function(editor) {
      // Make sure we restore the previous editing mode when the page is loaded from cache (back/forward/reload).
      var contentTypeField = this.getContentTypeField(editor);
      // Chrome doesn't cache the enabled/disabled state of form fields so we are caching it separately.
      if (editor._.xwikiCache[contentTypeField.attr('name')] === false || contentTypeField.prop('disabled')) {
        contentTypeField.prop('disabled', true);
        editor.config.startupMode = 'source';
      }

      this.editors.push(editor);
      editor.on('destroy', (function(event) {
        var index = this.editors.indexOf(editor);
        if (index >= 0) {
          this.editors.splice(index, 1);
        }
      }).bind(this));

      // Keyboard shortcuts for the edit form.
      this.addEditFormShortcutKey(editor, 'cancel', CKEDITOR.ALT + 67 /*C*/);
      this.addEditFormShortcutKey(editor, 'preview', CKEDITOR.ALT + 80 /*P*/);
      this.addEditFormShortcutKey(editor, 'saveAndContinue', CKEDITOR.ALT + CKEDITOR.SHIFT + 83 /*S*/);
      this.addEditFormShortcutKey(editor, 'save', CKEDITOR.ALT + 83 /*S*/);
    },

    onLoad: function() {
      // We need to update the form fields before the form is validated (for Preview, Save and Save & Continue).
      $(document).on('xwiki:actions:beforePreview xwiki:actions:beforeSave', (function(event, data) {
        if (!this.updateFormFields()) {
          event.preventDefault();
          // This is for older versions of XWiki (<10.8.1) where we had to stop the original event.
          if (data && data.originalEvent && typeof data.originalEvent.stop === 'function') {
            data.originalEvent.stop();
          }
        }
      }).bind(this));

      var submitInProgress = false;
      // Disable the leave confirmation when the form action buttons are used.
      $(document).on('xwiki:actions:cancel xwiki:actions:preview xwiki:actions:save xwiki:document:saved',
        (function(event, data) {
          // We reset the dirty field on 'xwiki:actions:save' only if it's not a Save & Continue. Otherwise we wait for
          // 'xwiki:document:saved' to be sure the document was saved.
          if (!data || !data['continue']) {
            submitInProgress = event.type === 'xwiki:actions:preview' || event.type === 'xwiki:actions:save';
            this.editors.forEach(function(editor) {
              // The editor (its focus manager to be precise) is blurred (loses focus) with a delay (200ms by default).
              // When the editor loses focus its content (HTML) can suffer changes (e.g. some placeholder text is added
              // or removed, the 'cke_widget_focused' and 'cke_widget_editable_focused' CSS classes are removed, etc.).
              // This means it's possible that the editor content is modified (becomes dirty) between the moment we
              // reset the dirty flag and the moment we leave the edit mode, which will trigger the leave confirmation,
              // unexpectedly. In order to overcome this we have to force the editor to lose focus immediately if the
              // focus manager has a blur timer started (meaning that a blur is scheduled).
              if (!editor.isDetached() && editor.focusManager?.hasFocus && editor.focusManager?._?.timer) {
                editor.focusManager.blur(true);
              }
              editor.resetDirty();
            });
          }
        }).bind(this));

      $(window).on('beforeunload', (function(event) {
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
      }).bind(this));
    },

    updateFormFields: function(fullData) {
      var success = true;
      fullData = fullData === true;
      this.editors.forEach(function(editor) {
        if (editor.elementMode === CKEDITOR.ELEMENT_MODE_REPLACE && !this.updateContent(editor, fullData)) {
          success = false;
        }
        this.updateContentType(editor);
      }, this);
      return success;
    },

    updateContent: function(editor, fullData) {
      var oldFullData;
      if (fullData) {
        oldFullData = editor.config.fullData;
        editor.config.fullData = true;
      }
      try {
        return editor.updateElement();
      } catch (e) {
        editor.showNotification(editor.localization.get('xwiki-save.failed'), 'warning');
        console.log(e);
        return false;
      } finally {
        if (fullData) {
          editor.config.fullData = oldFullData;
        }
      }
    },

    updateContentType: function(editor) {
      var contentTypeField = this.getContentTypeField(editor);
      var disabled = editor.mode === 'source';
      contentTypeField.prop('disabled', disabled);
      // Chrome doesn't cache the enabled/disabled state of form fields so we have to cache it separately.
      editor._.xwikiCache[contentTypeField.attr('name')] = !disabled;
      // Update also the source document syntax.
      contentTypeField.nextAll().filter(function() {
        return $(this).attr('name') === contentTypeField.val() + '_syntax';
      }).val(editor.config.sourceSyntax);
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
    },

    addEditFormShortcutKey: function(editor, action, shortcut) {
      var commandName = 'xwiki-' + action;
      editor.addCommand(commandName, {
        canUndo: false,
        context: false,
        contextSensitive: false,
        editorFocus: false,
        exec: function(editor) {
          var config = editor.config['xwiki-save'] || {};
          var actionButton = config[action + 'Button'] || ('input[name=action_' + action.toLowerCase() + ']');
          $(editor.container.$).closest('form, .form, body').find(actionButton).click();
        },
        modes: {
          wysiwyg: 1
        }
      });
      editor.setKeystroke(shortcut, commandName);
    }
  });
})();
