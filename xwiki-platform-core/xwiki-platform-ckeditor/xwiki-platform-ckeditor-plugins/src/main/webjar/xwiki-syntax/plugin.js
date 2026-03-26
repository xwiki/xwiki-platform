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
  const $ = jQuery;

  // Reload the CKEditor instances when the document syntax is changed because the Advanced Content Filter configuration
  // depends on the content syntax.
  $(document).on('xwiki:document:syntaxChange.ckeditor', function(event, data) {
    $.each(CKEDITOR.instances, function(key, editor) {
      maybeChangeSyntax(editor, event, data);
    });
  });

  function maybeChangeSyntax(editor, event, data) {
    if (matchesSyntaxChangeEvent(editor, event, data)) {
      data.promise = data.promise.then(async () => {
        const notification = new XWiki.widgets.Notification(editor.localization.get('xwiki-syntax.update.inProgress'),
          'inprogress');
        try {
          editor = await maybeConvertSyntaxAndReload(editor, data);
          notification.replace(new XWiki.widgets.Notification(editor.localization.get('xwiki-syntax.update.done'),
            'done'));
          return editor;
        } catch (error) {
          notification.replace(new XWiki.widgets.Notification(editor.localization.get('xwiki-syntax.update.failed'),
            'error'));
          // Re-throw the error so that the next (async) syntax change handlers are not executed.
          throw error;
        }
      });
    }
  }

  function matchesSyntaxChangeEvent(editor, event, data) {
    const form = $(event.target).closest('form, .form');
    // Check if the syntax change event targets the edited document (the source document).
    return editor.config.sourceDocument.documentReference.equals(data.documentReference) &&
      // Check if the syntax plugin is enabled for this editor instance.
      editor.plugins['xwiki-syntax'] &&
      // Check if the syntax change is the result of canceling the form that holds the CKEditor instance.
      (!data.reverting || !$.contains(form[0], editor.element.$));
  }

  async function maybeConvertSyntaxAndReload(editor, data) {
    editor.element.setAttribute('data-syntax', data.syntax.id);
    editor.config.sourceSyntax = data.syntax.id;
    const content = await maybeConvertSyntax(editor, data);
    return await reloadEditor(editor, data.syntax, content);
  }

  async function maybeConvertSyntax(editor, data) {
    if (editor.mode === 'wysiwyg' && data.convertSyntax && data.syntax.parser) {
      // We need to convert the annotated XHTML to wiki syntax, then convert the wiki syntax and finally render the
      // converted wiki syntax as annotated XHTML.

      // 1. Annotated XHTML to wiki syntax
      let source = await CKEDITOR.plugins.xwikiSource.convertHTML(editor, {
        fromHTML: true,
        toHTML: false,
        sourceSyntax: data.previousSyntax.id,
        text: editor.getData()
      });

      // 2. Syntax conversion
      source = await data.syntaxConverter.convert(data.syntax, data.previousSyntax, source);

      // 3. Wiki syntax to annotated XHTML
      return await CKEDITOR.plugins.xwikiSource.convertHTML(editor, {
        fromHTML: false,
        toHTML: true,
        sourceSyntax: data.syntax.id,
        text: source
      });
    } else if (editor.mode === 'source' && data.convertSyntax) {
      // Easy, just convert the source syntax.
      return await data.syntaxConverter.convert(data.syntax, data.previousSyntax, editor.getData());
    } else if (data.syntax.renderer || editor.mode === 'source') {
      // Return the current content as is (including the styles, if the content is HTML).
      return CKEDITOR.plugins.xwikiSource.getFullData(editor);
    } else {
      // The new syntax doesn't have a renderer so it means it doesn't support the WYSIWYG edit mode. We need to force
      // the source mode. We convert the current Annotated XHTML to the previous syntax, letting the user do the manual
      // conversion to the new syntax.
      return await CKEDITOR.plugins.xwikiSource.convertHTML(editor, {
        fromHTML: true,
        toHTML: false,
        sourceSyntax: data.previousSyntax.id,
        text: editor.getData()
      });
    }
  }

  async function reloadEditor(editor, syntax, content) {
    // We want to fire the reload event before the editor is destroyed, in order to collect the reload async handlers,
    // then destroy the editor and finally call the reload async handlers.
    let callReloadAsyncHandlers;
    const data = {
      promise: new Promise(resolve => callReloadAsyncHandlers = resolve)
    };
    // Save the current edit mode in order to restore it after the editor is reloaded. If the new syntax doesn't support
    // the WYSIWYG edit mode then we need to force the source mode instead.
    var mode = syntax.renderer ? editor.mode : 'source';
    // Notify the owner of the editor that we're about to destroy it and that it needs to be reloaded.
    editor.fireOnce('reload', data);
    // Destroy the editor without updating the content / value of the underlying element because we're going to use the
    // given content. Wait for the async destroy handlers to be executed.
    await editor.destroy(/* noUpdate: */ true);
    // Trigger the actual reload of the editor, passing custom editor configuration. Note that the statup mode is not
    // taken into account when the editor is loaded in-line. This is why we still need to set the edit mode below after
    // the editor is re-loaded.
    callReloadAsyncHandlers({startupMode: mode});
    // Wait for the editor to be reloaded, i.e. for the new editor instance to be ready.
    editor = await data.promise;
    // Restore the previous edit mode and set the updated content after the editor is reloaded.
    await maybeSetEditMode(editor, mode, content);
    await setEditedContent(editor, content);
    if (!syntax.renderer) {
      // Disable the edit mode switch if the new syntax doesn't support the WYSIWYG edit mode..
      editor.getCommand('source').disable();
      // ..and warn the user about it.
      editor.showNotification(editor.localization.get('xwiki-syntax.wysiwygModeUnsupported', syntax.label), 'warning');
    }
    // Return the new editor instance.
    return editor;
  }

  function maybeSetEditMode(editor, mode, content) {
    return new Promise(resolve => {
      // We need this check because CKEditor doesn't call the callback if the given mode is already set.
      if (mode === editor.mode) {
        resolve(editor);
      } else {
        // Avoid any conversion when switching modes.
        const previousMode = editor._.modes[editor.mode];
        previousMode.data = CKEDITOR.plugins.xwikiSource.getFullData(editor);
        editor._.modes[mode].data = content;
        editor.setMode(mode, () => {
          // Force a conversion on the next mode switch.
          delete previousMode.data;
          resolve(editor);
        });
      }
    });
  }

  function setEditedContent(editor, content) {
    return new Promise(resolve => {
      editor.setData(content, {
        callback: () => resolve(editor)
      });
    });
  }

  // An empty plugin that can be used to enable / disable the syntax change handling for a particular CKEditor instance.
  CKEDITOR.plugins.add('xwiki-syntax', {
    requires: 'notification,xwiki-localization,xwiki-source'
  });
})();
