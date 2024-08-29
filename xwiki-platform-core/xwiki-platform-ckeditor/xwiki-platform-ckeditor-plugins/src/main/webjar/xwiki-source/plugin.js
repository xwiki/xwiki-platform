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
  var $ = jQuery;

  // Declare the configuration namespace.
  CKEDITOR.config['xwiki-source'] = CKEDITOR.config['xwiki-source'] || {
    __namespace: true
  };

  CKEDITOR.plugins.xwikiSource = {
    convertHTML: function(editor, params) {
      // Empty object that can be populated by listener of the 'xwiki:wysiwyg:convertHTML' event.
      // The values of this object are then used as parameters on the html conversion request.
      var extendedParams = {};

      $(document).trigger('xwiki:wysiwyg:convertHTML', extendedParams);
      var localParams = {
        // Make sure we use the syntax specified when the editor was loaded. This is especially important when the
        // edited document is new (unsaved) because we want the converter to use the syntax specified by the template
        // rather than the default wiki syntax.
        sourceSyntax: editor.config.sourceSyntax,
        // Indicate if the content is supposed to be executed in a restricted context
        wysiwygRestricted: editor.element.getAttribute('data-restricted') === 'true',
        // Don't wrap the returned HTML with the BODY tag and don't include the HEAD tag when the editor is used
        // in-line (because the returned HTML will be inserted directly into the main page).
        stripHTMLEnvelope: editor.elementMode === CKEDITOR.ELEMENT_MODE_INLINE
      };
      var postParams = $.extend(extendedParams, $.extend(localParams, params));
      return $.post(editor.config['xwiki-source'].htmlConverter, postParams);
    },

    getFullData: function(editor) {
      var isFullData = editor.config.fullData;
      editor.config.fullData = true;
      var fullData = editor.getData();
      editor.config.fullData = isFullData;
      return fullData;
    }
  };

  CKEDITOR.plugins.add('xwiki-source', {
    requires: 'notification,xwiki-loading,xwiki-localization,xwiki-selection,xwiki-sourcearea',

    beforeInit: function(editor) {
      // Fill missing configuration with default values.
      editor.config['xwiki-source'] = $.extend({
        // We need the source document to be the current document when the HTML conversion is performed in order to make
        // sure relative references within the edited content are properly resolved and serialized.
        htmlConverter: editor.config.sourceDocument.getURL('get', $.param({
          sheet: 'CKEditor.HTMLConverter',
          outputSyntax: 'plain',
          language: editor.getContentLocale(),
          formToken: document.documentElement.dataset.xwikiFormToken || ''
        }))
      }, editor.config['xwiki-source']);
    },

    afterInit: function(editor) {
      // The source command is not registered if the editor is loaded in-line.
      var sourceCommand = editor.getCommand('source');
      if (sourceCommand) {
        editor.on('beforeSetMode', this.onBeforeSetMode.bind(this));
        editor.on('beforeModeUnload', this.onBeforeModeUnload.bind(this));
        editor.on('mode', this.onMode.bind(this));

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
      var newMode = event.data;
      var editor = event.editor;
      var currentModeFailed = editor.mode && (editor._.modes[editor.mode] || {}).failed;
      if (this.isModeSupported(newMode) && !currentModeFailed) {
        this.startLoading(editor);
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
        var newData = CKEDITOR.plugins.xwikiSource.getFullData(editor);
        mode.dirty = oldData !== newData;
        mode.data = newData;
      }
    },

    onMode: function(event) {
      var editor = event.editor;
      var promise;
      if (editor.mode === 'wysiwyg' && editor._.previousMode === 'source') {
        // Convert from wiki syntax to HTML.
        promise = this.maybeConvertHTML(editor, true);
      } else if (editor.mode === 'source' && editor._.previousMode === 'wysiwyg') {
        // Convert from HTML to wiki syntax.
        promise = this.maybeConvertHTML(editor, false);
      } else if (this.isModeSupported(editor.mode)) {
        promise = $.Deferred().resolve(editor);
      }
      if (promise) {
        promise.always(this.endLoading.bind(this)).done(editor.fire.bind(editor, 'modeReady'));
      }
    },

    maybeConvertHTML: function(editor, toHTML) {
      var oldMode = editor._.modes[editor._.previousMode];
      var newMode = editor._.modes[editor.mode];
      if (oldMode.dirty || typeof newMode.data !== 'string') {
        return this.convertHTML(editor, toHTML);
      } else {
        var deferred = $.Deferred();
        editor.setData(newMode.data, {
          callback: deferred.resolve.bind(deferred, editor)
        });
        return deferred.promise();
      }
    },

    convertHTML: function(editor, toHTML) {
      var deferred = $.Deferred();
      CKEDITOR.plugins.xwikiSource.convertHTML(editor, {
        fromHTML: !toHTML,
        toHTML: toHTML,
        text: editor._.previousModeData
      }).done(function(data) {
        editor.setData(data, {
          callback: function() {
            // Take a snapshot after the data has been set, in order to be able to detect changes.
            editor._.modes[editor.mode].data = CKEDITOR.plugins.xwikiSource.getFullData(editor);
            deferred.resolve(editor);
          }
        });
      }).fail(function() {
        // Switch back to the previous edit mode without performing a conversion.
        editor._.modes[editor.mode].failed = true;
        editor.setMode(editor._.previousMode, function() {
          deferred.reject(editor);
          editor.showNotification(editor.localization.get('xwiki-source.conversionFailed'), 'warning');
        });
      });
      return deferred.promise();
    },

    startLoading: function(editor) {
      CKEDITOR.plugins.xwikiSelection.saveSelection(editor);
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
        $(editor.container.$).find('.cke_button__source_icon').first().addClass('loading');
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
        $(editor.container.$).find('.cke_button__source_icon').first().removeClass('loading');
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
      CKEDITOR.plugins.xwikiSelection.restoreSelection(editor);
    }
  });
})();
