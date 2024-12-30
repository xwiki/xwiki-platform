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
(function () {
  'use strict';

  // Declare the configuration namespace.
  CKEDITOR.config['xwiki-focusedplaceholder'] = CKEDITOR.config['xwiki-focusedplaceholder'] || {
    __namespace: true,
    placeholder: {
      __namespace: true
    }
  };

  var ATTRIBUTE_NAME = 'data-xwiki-focusedplaceholder';

  CKEDITOR.plugins.add('xwiki-focusedplaceholder', {

    init: function(editor) {
      // Add default configuration values.
      if (!editor.config["xwiki-focusedplaceholder"].ignoreIfEmpty) {
        //Empty inline tags (or nodeNames) that usually do not appear on screen should be ignored by default because
        // they might get propagated to new lines by CKEditor
        editor.config["xwiki-focusedplaceholder"].ignoreIfEmpty = [
          "#text",
          "a",
          "abbr",
          "b",
          "bdi",
          "bdo",
          "br",
          "cite",
          "data",
          "dfn",
          "em",
          "i",
          "mark",
          "s",
          "small",
          "span",
          "strong",
          "time",
          "u",
          "var",
          "wbr",
          "del",
          "ins",
        ];
      }
      // Add generic default placeholders for content sectioning, text and table HTML tags.
      [
        "body",
        "address",
        "aside",
        "footer",
        "header",
        "h1",
        "h2",
        "h3",
        "h4",
        "h5",
        "h6",
        "main",
        "nav",
        "section",
        "blockquote",
        "dl",
        "dt",
        "dd",
        "figcaption",
        "figure",
        "ol",
        "ul",
        "menu",
        "li",
        "p",
        "pre",
        "caption",
        "th",
        "td",
      ].forEach(function (tagName) {
        if (!editor.config["xwiki-focusedplaceholder"].placeholder[tagName]) {
          editor.config["xwiki-focusedplaceholder"].placeholder[tagName] =
            `xwiki-focusedplaceholder.placeholder.${tagName}`;
        }
      });

      // Filter out placeholders when saving or switching to Source.
      var filterPlaceholders = {
        attributes: {
          [ATTRIBUTE_NAME]: function() {
            return false;
          },
        }
      };
      var htmlFilter = editor.dataProcessor && editor.dataProcessor.htmlFilter;
      // The second parameter is important because it forces the filter to be applied also when the editor is read-only,
      // which happens when the editor is put in loading state (e.g. before switching to Source).
      htmlFilter.addRules(filterPlaceholders, {applyToAll: true});

      [
        // Update the placeholder when the edited content changes (e.g. the user types).
        'change',
        // Update the placeholder when the selection changes (e.g. the user moves the caret).
        'selectionChange',
        // Update (remove) the placeholder when the editor loses the focus. We need this also to avoid a conflict with
        // the empty content placeholder that is displayed when the editor doesn't have the focus (if empty).
        'blur'
      ].forEach((eventName) => {
        editor.on(eventName, (event) => {
          maybeUpdateFocusedPlaceholder(event.editor);
        });
      });

      function maybeUpdateFocusedPlaceholder(editor) {
        const editable = editor.editable();
        const oldFocusedEmptyBlock = editable?.findOne("[" + ATTRIBUTE_NAME + "]");
        const newFocusedEmptyBlock = getFocusedEmptyBlock(editor);
        if (newFocusedEmptyBlock !== oldFocusedEmptyBlock) {
          // The placeholder update shouldn't generate a separate entry in the editing history. Instead, we want to
          // update the previous or the next history entry. This way, undo should restore the state before the
          // placeholder update.
          editor.fire('lockSnapshot');
          oldFocusedEmptyBlock?.removeAttribute(ATTRIBUTE_NAME);
          let placeholderContent = editor.config.editorplaceholder;
          if (newFocusedEmptyBlock) {
            placeholderContent = getPlaceholderContent(newFocusedEmptyBlock.getName());
            newFocusedEmptyBlock.setAttribute(ATTRIBUTE_NAME, placeholderContent);
          }
          if (editable?.isInline()) {
            editable.setAttribute('aria-placeholder', placeholderContent);
          }
          editor.fire('unlockSnapshot');
        }
      }

      editor.on('beforeDestroy', function() {
        var editable = editor.editable();
        if (editable?.isInline()) {
          editable.removeAttributes([
            'aria-readonly',
            'aria-placeholder'
          ]);
        }
      });

      /**
       * Checks if the editor is focused and the element that has the caret is empty.
       *
       * @param {CKEDITOR.editor} editor - the editor instance
       * @return the element that holds the caret and is empty, or undefined if no such element is present
       */
      function getFocusedEmptyBlock(editor) {
        const selection = !editor.isDetached() && editor.getSelection();
        if (selection?.isCollapsed?.() && editor.focusManager.hasFocus) {
          const container = selection.getRanges()[0].startContainer;
          // Check first if the container itself is empty, in order to reduce the computations, since this method is
          // called whenever the user types (and most of the time the caret is inside a text node that is not empty).
          if (isEmpty(container)) {
            // Find the ancestor for which a placeholder needs to be shown.
            var ancestor = getFirstConfiguredAncestor(container);
            if (ancestor && isEmpty(ancestor)) {
              return ancestor;
            }
          }
        }
      }

      /**
       * Finds the first configured ancestor of a DOM Node
       *
       * @param {CKEDITOR.dom.node} node
       * @return {Object} - The first configured ancestor, null if none were found
       */
      function getFirstConfiguredAncestor(node) {
        while (node && (node.type !== CKEDITOR.NODE_ELEMENT ||
            !editor.config["xwiki-focusedplaceholder"].placeholder[node.getName()])) {
          node = node.getParent();
        }
        return node;
      }

      /**
       * Checks if a node should be considered empty
       *
       * @param {CKEDITOR.dom.node} node - the DOM node to check
       * @return {bool} - True when the node is considered empty, false otherwise
       */
      function isEmpty(node) {
        if (node.type === CKEDITOR.NODE_TEXT) {
          return node.isEmpty();
        }

        for (var child = node.getFirst(); child; child = child.getNext()) {
          // Child is not considered empty if it is not ignored if empty.
          if (!editor.config["xwiki-focusedplaceholder"].ignoreIfEmpty.includes(child.getName?.()) ||
              !isEmpty(child)) {
            return false;
          }
        }

        return true;
      }

      /**
       * Returns the expected placeholder text for a given tag name.
       *
       * @param {string} tag - Lowercase tag name
       * @return {string} - Placeholder text
       */
      function getPlaceholderContent(tag) {
        var translationKey = editor.config["xwiki-focusedplaceholder"].placeholder[tag];

        if (translationKey === undefined) {
          return "";
        }

        return editor.localization.get(translationKey);
      }
    },
  });
})();
