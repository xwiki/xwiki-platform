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
      const isFullData = editor.config.fullData;
      editor.config.fullData = true;
      // The full data is sometimes used to restore the editor content and unfortunately CKEditor has a problem with
      // the content that it finds after the BODY tag (e.g. the user avatars when editing in realtime). The HTML is
      // "fixed" by moving the content inside the BODY tag. In order to avoid this we temporarily remove the content
      // after the BODY tag.
      const contentAfterBody = [];
      const editable = editor.editable()?.$;
      if (editable?.tagName.toUpperCase() === 'BODY') {
        while (editable.nextSibling) {
          contentAfterBody.push(editable.nextSibling);
          editable.nextSibling.remove();
        }
      }
      const fullData = editor.getData();
      // Restore the content after the BODY tag.
      editable?.after(...contentAfterBody);
      editor.config.fullData = isFullData;
      return fullData;
    },

    addModeChangeHandler: function(editor, handler, priority = 0) {
      editor._.modeChangeHandlers = editor._.modeChangeHandlers || [];
      editor._.modeChangeHandlers.push({handler, priority});
    },
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
      editor.on('beforeSetMode', this.onBeforeSetMode.bind(this));
      editor.on('beforeModeUnload', this.onBeforeModeUnload.bind(this));
      editor.on('mode', this.onMode.bind(this));
      CKEDITOR.plugins.xwikiSource.addModeChangeHandler(editor, this.onModeChanged.bind(this), 5);
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

    onMode: async function(event) {
      var editor = event.editor;
      if (this.isModeSupported(editor.mode)) {
        try {
          await this.callModeChangeHandlers(editor);
        } finally {
          await this.endLoading(editor);
          editor.fire('modeReady');
        }
      }
    },

    callModeChangeHandlers: async function(editor) {
      editor._.modeChangeHandlers.sort((a, b) => a.priority - b.priority);
      for (const {handler} of editor._.modeChangeHandlers) {
        await handler(editor, {
          previousMode: editor._.previousMode,
        });
      }
    },

    onModeChanged: async function(editor, {previousMode}) {
      if (editor.mode === 'wysiwyg' && previousMode === 'source') {
        // Convert from wiki syntax to HTML.
        await this.maybeConvertHTML(editor, true);
      } else if (editor.mode === 'source' && previousMode === 'wysiwyg') {
        // Convert from HTML to wiki syntax.
        await this.maybeConvertHTML(editor, false);
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

    endLoading: async function(editor) {
      if (editor.editable()) {
        $(editor.container.$).find('.cke_button__source_icon').first().removeClass('loading');
      }
      if (editor.mode === 'wysiwyg') {
        // Unlock the undo history after the conversion is done and the WYSIWYG mode data is set.
        editor.fire('unlockSnapshot');
      }
      await CKEDITOR.plugins.xwikiSelection.restoreSelection(editor, {
        beforeApply: () => editor.setLoading(false)
      });
    }
  });
})();
