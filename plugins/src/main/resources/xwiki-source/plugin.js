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
(function() {
  'use strict';
  CKEDITOR.plugins.add('xwiki-source', {
    requires: 'sourcearea,notification',

    init: function(editor) {
      editor.on('beforeSetMode', jQuery.proxy(this.onBeforeSetMode, this));
      editor.on('beforeModeUnload', jQuery.proxy(this.onBeforeModeUnload, this));
      editor.on('mode', jQuery.proxy(this.onMode, this));
    },

    onBeforeSetMode: function(event) {
      if (this.isModeSupported(event.data)) {
        this.setLoading(event.editor, true);
      }
    },

    isModeSupported: function(mode) {
      return mode === 'wysiwyg' || mode === 'source';
    },

    onBeforeModeUnload: function(event) {
      var editor = event.editor;
      if (!this.isModeSupported(editor.mode)) {
        return;
      }
      var mode = editor._.modes[editor.mode];
      if (mode.failed) {
        mode.dirty = mode.failed = false;
        // Make sure we retry the conversion on the next mode switch.
        delete mode.data;
      } else {
        var oldData = mode.data;
        var newData = this.getFullData(editor);
        mode.dirty = oldData !== newData;
        mode.data = newData;
      }
    },

    getFullData: function(editor) {
      var isFullData = editor.config.fullData;
      editor.config.fullData = true;
      var fullData = editor.getData();
      editor.config.fullData = isFullData;
      return fullData;
    },

    onMode: function(event) {
      var editor = event.editor;
      if (editor.mode === 'wysiwyg' && editor._.previousMode === 'source') {
        // Convert from wiki syntax to HTML.
        this.maybeConvertHTML(editor, true);
      } else if (editor.mode === 'source' && editor._.previousMode === 'wysiwyg') {
        // Convert from HTML to wiki syntax.
        this.maybeConvertHTML(editor, false);
      } else if (this.isModeSupported(editor.mode)) {
        this.setLoading(editor, false);
      }
    },

    maybeConvertHTML: function(editor, toHTML) {
      var oldMode = editor._.modes[editor._.previousMode];
      var newMode = editor._.modes[editor.mode];
      if (oldMode.dirty || typeof newMode.data !== 'string') {
        this.convertHTML(editor, toHTML);
      } else {
        editor.setData(newMode.data, {
          callback: jQuery.proxy(this.setLoading, this, editor, false)
        });
      }
    },

    convertHTML: function(editor, toHTML) {
      var thisPlugin = this;
      var config = editor.config['xwiki-source'] || {};
      jQuery.post(config.htmlConverter, {
        convert: toHTML,
        text: editor._.previousModeData
      }).done(function(data) {
        editor.setData(data, {
          callback: function() {
            // Take a snapshot after the data has been set, in order to be able to detect changes.
            editor._.modes[editor.mode].data = thisPlugin.getFullData(editor);
            thisPlugin.setLoading(editor, false);
          }
        });
      }).fail(function() {
        // Switch back to the previous edit mode without performing a conversion.
        editor._.modes[editor.mode].failed = true;
        editor.setMode(editor._.previousMode, function() {
          thisPlugin.setLoading(editor, false);
          editor.showNotification('Failed to perform the conversion.', 'warning');
        });
      });
    },

    setLoading: function(editor, loading) {
      var sourceButton, editable = editor.editable();
      if (editable) {
        editor.setReadOnly(loading);
        sourceButton = editor.container.findOne('.cke_button__source_icon');
      }
      if (loading) {
        editor.ui.space('contents').setStyle('visibility', 'hidden');
        sourceButton && sourceButton.addClass('loading');
        // Disable the switch while the conversion takes place.
        setTimeout(function() {
          editor.getCommand('source').setState(CKEDITOR.TRISTATE_DISABLED);
        }, 0);
      } else {
        editor.ui.space('contents').removeStyle('visibility');
        sourceButton && sourceButton.removeClass('loading');
        editor.getCommand('source').setState(editor.mode !== 'source' ? CKEDITOR.TRISTATE_OFF : CKEDITOR.TRISTATE_ON);
      }
    }
  });
})();
