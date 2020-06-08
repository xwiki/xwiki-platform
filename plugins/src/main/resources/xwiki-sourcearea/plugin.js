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

  // The following code is partially taken (and adapted) from CKEditor's default sourcearea plugin.
  CKEDITOR.plugins.add('xwiki-sourcearea', {
    init: function(editor) {
      editor.addMode('source', function(callback) {
        var contentsSpace = editor.ui.space('contents');

        var textArea = contentsSpace.getDocument().createElement('textarea');
        textArea.setStyles(CKEDITOR.tools.extend({
          width: '100%',
          height: '100%',
          resize: 'none',
          outline: 'none',
          'text-align': 'left'
        }, CKEDITOR.tools.cssVendorPrefix('tab-size', editor.config.sourceAreaTabSize || 2)));
        // Make sure that source code is always displayed LTR, regardless of editor language.
        // See https://dev.ckeditor.com/ticket/10105
        textArea.setAttribute('dir', 'ltr');
        textArea.addClass('cke_source').addClass('cke_reset').addClass('cke_enable_context_menu');

        if (editor.elementMode === CKEDITOR.ELEMENT_MODE_INLINE) {
          jQuery(textArea.$).autoHeight();
        }

        contentsSpace.append(textArea);

        var editable = editor.editable(new SourceEditable(editor, textArea));
        // Fill the text area with the current editor data.
        editable.setData(editor.getData(1));

        editor.fire('ariaWidget', this);

        callback();
      });

      editor.addCommand('source', {
        modes: {wysiwyg: 1, source: 1},
        editorFocus: false,
        readOnly: 1,
        exec: function(editor) {
          if (editor.mode === 'wysiwyg') {
            editor.fire('saveSnapshot');
          }
          editor.getCommand('source').setState(CKEDITOR.TRISTATE_DISABLED);
          editor.setMode(editor.mode === 'source' ? 'wysiwyg' : 'source');
        },
        canUndo: false
      });

      if (editor.ui.addButton) {
        editor.ui.addButton('Source', {
          label: editor.lang.sourcearea.toolbar,
          command: 'source',
          toolbar: 'mode,10'
        });
      }

      // Create a fake contents space, if needed, before switching to source.
      editor.on('beforeSetMode', function(event) {
        var contentsSpace = editor.ui.space('contents');
        if (editor.elementMode === CKEDITOR.ELEMENT_MODE_INLINE && event.data === 'source' && !contentsSpace) {
          // The contents space is normally not available when editing in-place so we need to fake it.
          contentsSpace = editor.element.getDocument().createElement('div');
          contentsSpace.setAttributes({
            id: editor.ui.spaceId('contents'),
            'class': 'cke_contents cke_reset fake',
            role: 'presentation'
          });
          contentsSpace.insertAfter(editor.element);
        }
      });

      editor.on('mode', function() {
        editor.getCommand('source').setState(editor.mode === 'source' ? CKEDITOR.TRISTATE_ON : CKEDITOR.TRISTATE_OFF);

        if (editor.elementMode === CKEDITOR.ELEMENT_MODE_INLINE && editor.mode !== 'source') {
          // Remove the fake contents space we added above.
          var contentsSpace = editor.ui.space('contents');
          if (contentsSpace && contentsSpace.hasClass('fake')) {
            contentsSpace.remove();
          }
        }
      });

      editor.on('dataReady', function() {
        if (editor.mode === 'source' && editor.elementMode === CKEDITOR.ELEMENT_MODE_INLINE) {
          // Update the text area height.
          jQuery(editor.editable().$).trigger('input');
        }
      });
    }
  });

  var SourceEditable = CKEDITOR.tools.createClass({
    base: CKEDITOR.editable,
    proto: {
      setData: function(data) {
        this.setValue(data);
        this.status = 'ready';
        this.editor.fire('dataReady');
      },

      getData: function() {
        return this.getValue();
      },

      // Insertions are not supported in source editable.
      insertHtml: function() {},
      insertElement: function() {},
      insertText: function() {},

      // Read-only support for textarea.
      setReadOnly: function(isReadOnly) {
        this[(isReadOnly ? 'set' : 'remove') + 'Attribute']('readOnly', 'readonly');
      },

      detach: function() {
        SourceEditable.baseProto.detach.call(this);
        this.clearCustomData();
        this.remove();
      }
    }
  });

  // Credits: https://stackoverflow.com/a/25621277
  jQuery.fn.autoHeight = jQuery.fn.autoHeight || function() {
    var autoHeight = function(element) {
      return jQuery(element).css({
        'height': 'auto',
        'overflow-y': 'hidden'
      }).height(element.scrollHeight);
    };
    return this.each(function() {
      autoHeight(this).on('input', function() {
        autoHeight(this);
      });
    });
  };
})();
