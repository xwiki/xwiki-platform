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

  CKEDITOR.plugins.add('xwiki-wysiwygarea', {
    requires: 'wysiwygarea',

    init: function(editor) {
      // The plugins load additional CSS by calling editor.addContentsCss() which collects the style sheets in the
      // editor.config.contentsCss configuration property. Unfortunately this property is ignored when fullPage is on.
      // We need full page, because we want to load all the XWiki styles, and we also need the plugins styles so the
      // following code fixes this.
      if (editor.config.fullPage) {
        this.injectPluginStyles(editor);
      }

      // Add support for WYSIWYG mode while editing in-line.
      if (editor.elementMode === CKEDITOR.ELEMENT_MODE_INLINE) {
        this.overwriteWysiwygMode(editor);
      }
    },

    // Inject the plugins styles into the editing area.
    injectPluginStyles: function(editor) {
      // See https://ckeditor.com/docs/ckeditor4/latest/api/CKEDITOR_config.html#cfg-contentsCss
      // See https://ckeditor.com/docs/ckeditor4/latest/api/CKEDITOR_editor.html#event-toHtml
      editor.on('toHtml', function(event) {
        if (!CKEDITOR.tools.isArray(editor.config.contentsCss)) {
          // No styles to inject.
          return;
        }
        // CKEDITOR.htmlParser.fragment instance
        var documentFragment = event.data.dataValue;
        if (typeof documentFragment.find !== 'function') {
          // The root node doesn't have support for finding child elements.
          return;
        }
        var heads = documentFragment.find('head', true);
        if (heads.length === 0) {
          // No place to inject the styles.
          return;
        }
        var head = heads[0];
        editor.config.contentsCss.forEach(function(url) {
          // We want to inject only the styles required by the loaded plugins.
          if (url.indexOf('/plugins/') > 0) {
            head.children.push(new CKEDITOR.htmlParser.element('link', {
              rel: 'stylesheet',
              type: 'text/css',
              href: url
            }));
          }
        });
      }, null, null, 14);
    },

    overwriteWysiwygMode: function(editor) {
      editor.on('beforeModeUnload', function() {
        var contentsSpace;
        if (editor.mode === 'wysiwyg') {
          // Hide the content that is edited in-line when leaving the WYSIWYG mode and create a fake contents space
          // where the new mode (e.g. Source) will be injected (because the contents space is not available when editing
          // in-place).
          contentsSpace = editor.element.getDocument().createElement('div');
          // Copy the non-CKEditor related class names from the editor element (the content edited in-line) to ensure
          // consistent styles between WYSIWYG and the other editing modes (like Source).
          var classNames = (editor.element.getAttribute('class') || '').split(/\s+/).filter(function(className) {
            return className.substring(0, 4) !== 'cke_';
          }).join(' ');
          contentsSpace.setAttributes({
            id: editor.ui.spaceId('contents'),
            'class': 'cke_contents fake ' + classNames,
            role: 'presentation'
          });
          // Initialize with the height of the WYSIWYG mode in order to prevent UI flickering when switching modes.
          contentsSpace.setStyles({
            'min-height': editor.element.getSize('height', true) + 'px',
            // Reserve the space and show it only after the new mode is ready.
            'visibility': 'hidden'
          });
          // Minimize UI flickering by reducing the number of redraws that have to be done.
          editor.element.$.parentNode.replaceChild(contentsSpace.$, editor.element.$);
          editor.element.hide();
          editor.element.insertBefore(contentsSpace);
          // Set the outermost element. This is needed in order to compute correctly the position where the notification
          // messages are shown.
          editor.container = editor.ui.contentsElement = contentsSpace;
          // When an edit mode is unloaded its editable is detached and this:
          // * clears only the event listeners registered with attachListener() on the editable
          // * clears only some of the custom data set on the editable
          // * fires the contentDomUnload event
          // Unfortunately some of the event listeners are registered using on() and these are not removed when the
          // editable is detached. This is the case with the clipboard plugin (addPasteListenersToEditable) which
          // probably assumes the editable element is recreated each time the edit mode is loaded. It's not the case for
          // the in-line editor where we want to reuse the editable element. As a result we get duplicated paste
          // listeners that duplicate the content on paste.
          // At the same time, some of the custom data set on the editable element is not cleared. The focus manager
          // custom data is an example. If we don't clear it then the focus and blur listeners will misbehave when we
          // come back from Source mode.
          editor.once('contentDomUnload', function() {
            // This removes both the event listeners and the custom data set on the editable.
            editor.editable().clearCustomData();
          });
        } else {
          // Preserve the height of the contents space while switching modes in order to prevent UI flickering.
          contentsSpace = editor.ui.space('contents');
          contentsSpace.setStyle('min-height', contentsSpace.getSize('height', true) + 'px');
        }
      });

      editor.addMode('wysiwyg', function(callback) {
        // Replace the contents space used by the other edit modes with the content edited in-place.
        var contentsSpace = editor.ui.space('contents');
        // Use the height of the previous editing mode (e.g. while the conversion from Source to HTML takes place) in
        // order to reduce the UI flickering.
        editor.element.remove().setStyle('min-height', contentsSpace.getSize('height', true) + 'px').show();
        // Minimize UI flickering by reducing the number of redraws that have to be done.
        contentsSpace.$.parentNode.replaceChild(editor.element.$, contentsSpace.$);
        // Enable in-line editing.
        editor.editable(editor.element);
        // The editable itself is the outermost element. This is needed in order to compute correctly the position where
        // the notification messages are shown.
        editor.container = editor.ui.contentsElement = editor.element;
        // Notify the others that the editable is ready so they can register their event listeners. Note that the event
        // listeners are removed along with any custom data each time the edit mode is unloaded. This line ensures that
        // the editable is re-initialized whenever we switch (back) to WYSIWYG mode.
        editor.fire('contentDom');
        // Note that we trigger the callback without setting the editor data because this is done later by our source
        // plugin after converting the source wiki syntax to HTML.
        callback();
      });

      editor.on('modeReady', function() {
        // Remove the height constraint once the mode is ready (it prevented UI flickeing while switching modes).
        if (editor.mode === 'wysiwyg') {
          editor.element.removeStyle('min-height');
        } else {
          editor.ui.space('contents').setStyles({
            'min-height': '',
            'visibility': ''
          });
        }
      });

      // Show the content that is edited in-line when the editor is destroyed, in case the current mode is not WYSIWYG.
      editor.on('beforeDestroy', function() {
        editor.element.show();
      });
    }
  });
})();
