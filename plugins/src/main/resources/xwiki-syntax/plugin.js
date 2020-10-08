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

  // Reload the CKEditor instances when the document syntax is changed because the Advanced Content Filter configuration
  // depends on the content syntax.
  $(document).on('xwiki:document:syntaxChange.ckeditor', function(event, data) {
    $.each(CKEDITOR.instances, function(key, editor) {
      maybeChangeSyntax(editor, event, data);
    });
  });

  var maybeChangeSyntax = function(editor, event, data) {
    if (matchesSyntaxChangeEvent(editor, event, data)) {
      data.promise = data.promise.then(function() {
        var notification = new XWiki.widgets.Notification(editor.localization.get('xwiki-syntax.update.inProgress'),
          'inprogress');
        return maybeConvertSyntaxAndReload(editor, data).done(function() {
          notification.replace(new XWiki.widgets.Notification(editor.localization.get('xwiki-syntax.update.done'),
            'done'));
        }).fail(function() {
          notification.replace(new XWiki.widgets.Notification(editor.localization.get('xwiki-syntax.update.failed'),
            'error'));
        });
      });
    }
  };

  var matchesSyntaxChangeEvent = function(editor, event, data) {
    var sourceDocumentReference = XWiki.Model.resolve(editor.element.getAttribute('data-sourceDocumentReference'),
      XWiki.EntityType.DOCUMENT, XWiki.currentDocument.documentReference);
    var form = $(event.target).closest('form, .form');
    // Check if the syntax change event targets the edited document (the source document).
    return sourceDocumentReference.equals(data.documentReference) &&
      // Check if the syntax plugin is enabled for this editor instance.
      editor.plugins['xwiki-syntax'] &&
      // Check if the syntax change is the result of canceling the form that holds the CKEditor instance.
      (!data.reverting || !$.contains(form[0], editor.element.$));
  };

  var maybeConvertSyntaxAndReload = function(editor, data) {
    editor.element.setAttribute('data-sourceDocumentSyntax', data.syntax.id);
    return maybeConvertSyntax(editor, data).then($.proxy(reloadEditor, null, editor));
  };

  var maybeConvertSyntax = function(editor, data) {
    if (editor.mode === 'wysiwyg' && data.convertSyntax) {
      // We need to convert the annotated XHTML to wiki syntax, then convert the wiki syntax and finally render the
      // converted wiki syntax as annotated XHTML.

      // 1. Annotated XHTML to wiki syntax
      return CKEDITOR.plugins.xwikiSource.convertHTML(editor, {
        fromHTML: true,
        toHTML: false,
        sourceSyntax: data.previousSyntax.id,
        text: editor.getData()

      // 2. Syntax conversion
      }).then($.proxy(data.syntaxConverter, 'convert', data.syntax, data.previousSyntax))

      // 3. Wiki syntax to annotated XHTML
      .then(function(source) {
        return CKEDITOR.plugins.xwikiSource.convertHTML(editor, {
          fromHTML: false,
          toHTML: true,
          sourceSyntax: data.syntax.id,
          text: source
        });
      });
    } else if (editor.mode === 'source' && data.convertSyntax) {
      // Easy, just convert the source syntax.
      return data.syntaxConverter.convert(data.syntax, data.previousSyntax, editor.getData());
    } else {
      // Return the current content as is (including the styles, if the content is HTML).
      return $.Deferred().resolve(CKEDITOR.plugins.xwikiSource.getFullData(editor)).promise();
    }
  };

  var reloadEditor = function(editor, content) {
    var deferred = $.Deferred();
    var data = {promise: deferred.promise()};
    // Save the current edit mode in order to restore it after the editor is reloaded.
    var mode = editor.mode;
    // Notify the owner of the editor that we're about to destroy it and that it needs to be reloaded.
    editor.fireOnce('reload', data);
    // Destroy the editor without updating the content / value of the underlying element because we're going to use the
    // given content.
    editor.destroy(/* noUpdate: */ true);
    // Restore the previous edit mode and set the updated content after the editor is reloaded.
    data.promise = data.promise.then($.proxy(maybeSetEditMode, null, mode, content))
      .then($.proxy(setEditedContent, null, content));
    // Trigger the actual reload of the editor, passing custom editor configuration. Note that the statup mode is not
    // taken into account when the editor is loaded in-line. This is why we still need to set the edit mode above after
    // the editor is re-loaded.
    deferred.resolve({startupMode: mode});
    return data.promise;
  };

  var maybeSetEditMode = function(mode, content, editor) {
    var deferred = $.Deferred();
    // We need this check because CKEditor doesn't call the callback if the given mode is already set.
    if (mode === editor.mode) {
      deferred.resolve(editor);
    } else {
      // Avoid any conversion when switching modes.
      var previousMode = editor._.modes[editor.mode];
      previousMode.data = CKEDITOR.plugins.xwikiSource.getFullData(editor);
      editor._.modes[mode].data = content;
      editor.setMode(mode, function() {
        // Force a conversion on the next mode switch.
        delete previousMode.data;
        deferred.resolve(editor);
      });
    }
    return deferred.promise();
  };

  var setEditedContent = function(content, editor) {
    var deferred = $.Deferred();
    editor.setData(content, {
      callback: function() {
        deferred.resolve(editor);
      }
    });
    return deferred.promise();
  };

  // An empty plugin that can be used to enable / disable the syntax change handling for a particular CKEditor instance.
  CKEDITOR.plugins.add('xwiki-syntax', {
    requires: 'xwiki-localization'
  });
})();
