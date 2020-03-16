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

  // Declare the configuration namespace.
  CKEDITOR.config['xwiki-source'] = CKEDITOR.config['xwiki-source'] || {
    __namespace: true
  };

  CKEDITOR.plugins.add('xwiki-source', {
    requires: 'sourcearea,notification,xwiki-loading,xwiki-localization',

    init: function(editor) {
      // The source command is not registered if the editor is loaded in-line.
      var sourceCommand = editor.getCommand('source');
      if (sourceCommand) {
        editor.on('beforeSetMode', jQuery.proxy(this.onBeforeSetMode, this));
        editor.on('beforeModeUnload', jQuery.proxy(this.onBeforeModeUnload, this));
        editor.on('mode', jQuery.proxy(this.onMode, this));

        // The default source command is not asynchronous so it becomes (re)enabled right after the editing mode is
        // changed. In our case switching between WYSIWYG and Source mode is asynchronous because we need to convert the
        // edited content on the server side. Thus we need to prevent the source command from being enabled while the
        // conversion takes place.
        // CKEDITOR-66: Switch to source corrupt page when connection lost or when connection is very slow
        var oldCheckAllowed = sourceCommand.checkAllowed;
        sourceCommand.checkAllowed = function() {
          return !this.running && oldCheckAllowed.apply(this, arguments);
        };
      }
    },

    onBeforeSetMode: function(event) {
      if (this.isModeSupported(event.data)) {
        this.startLoading(event.editor);
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
        this.endLoading(editor);
      }
    },

    maybeConvertHTML: function(editor, toHTML) {
      var oldMode = editor._.modes[editor._.previousMode];
      var newMode = editor._.modes[editor.mode];
      if (oldMode.dirty || typeof newMode.data !== 'string') {
        this.convertHTML(editor, toHTML);
      } else {
        editor.setData(newMode.data, {
          callback: jQuery.proxy(this.endLoading, this, editor)
        });
      }
    },

    convertHTML: function(editor, toHTML) {
      var thisPlugin = this;
      var config = editor.config['xwiki-source'] || {};
      jQuery.post(config.htmlConverter, {
        fromHTML: !toHTML,
        toHTML: toHTML,
        text: editor._.previousModeData
      }).done(function(data) {
        editor.setData(data, {
          callback: function() {
            // Take a snapshot after the data has been set, in order to be able to detect changes.
            editor._.modes[editor.mode].data = thisPlugin.getFullData(editor);
            thisPlugin.endLoading(editor);
          }
        });
      }).fail(function() {
        // Switch back to the previous edit mode without performing a conversion.
        editor._.modes[editor.mode].failed = true;
        editor.setMode(editor._.previousMode, function() {
          thisPlugin.endLoading(editor);
          editor.showNotification(editor.localization.get('xwiki-source.conversionFailed'), 'warning');
        });
      });
    },

    startLoading: function(editor) {
      editor.setLoading(true);
      // Prevent the source command from being enabled while the conversion takes place.
      var sourceCommand = editor.getCommand('source');
      // We have to set the flag before setting the command state in order to be taken into account.
      sourceCommand.running = true;
      sourceCommand.setState(CKEDITOR.TRISTATE_DISABLED);
      if (editor.mode === 'source') {
        // When switching from Source mode to WYSIWYG mode the wiki syntax is converted to HTML on the server side.
        // Before we receive the result the Source plugin sets the source (wiki syntax) as the data for the WYSIWYG
        // mode. This adds an entry (snapshot) in the undo history for the WYSIWYG mode. In order to prevent this we
        // lock the undo history until the conversion is done.
        // See CKEDITOR-58: Undo operation can replace the rich text content with wiki syntax
        editor.fire('lockSnapshot');
      }
      if (editor.editable()) {
        editor.container.findOne('.cke_button__source_icon').addClass('loading');
      }
      // A bug in Internet Explorer 11 prevents the user from typing into the Source text area if the WYSIWYG text
      // area is focused and the selection is collapsed before switching to Source mode. In order to avoid this
      // problem we have to either remove the focus from the WYSIWYG text area or to make sure the selection is not
      // collapsed before the switch. We didn't manage to remove the focus because we don't know what other focusable
      // elements are available on the page. Thus the solution we applied was to select all the content before the
      // switch so that the selection is not collapsed.
      // CKEDITOR-102: Unable to edit a page in Source mode on IE11
      // https://connect.microsoft.com/IE/feedback/details/1613994/ie-10-11-iframe-removal-causes-loss-of-the-ability-to-focus-input-elements
      // https://dev.ckeditor.com/ticket/7386
      if (editor.document && editor.document != CKEDITOR.document && CKEDITOR.env.ie && !CKEDITOR.env.edge) {
        // We apply the fix only if the WYSIWYG text area is using an iframe and if the browser is Internet Explorer
        // except Edge (that doesn't have the problem).
        editor.document.$.execCommand('SelectAll', false, null);
      }
    },

    endLoading: function(editor) {
      if (editor.editable()) {
        editor.container.findOne('.cke_button__source_icon').removeClass('loading');
      }
      if (editor.mode === 'wysiwyg') {
        // Unlock the undo history after the conversion is done and the WYSIWYG mode data is set.
        editor.fire('unlockSnapshot');
      }
      var sourceCommand = editor.getCommand('source');
      // We have to set the flag before setting the command state in order to be taken into account.
      sourceCommand.running = false;
      sourceCommand.setState(editor.mode !== 'source' ? CKEDITOR.TRISTATE_OFF : CKEDITOR.TRISTATE_ON);
      editor.setLoading(false);
    }
  });
})();
