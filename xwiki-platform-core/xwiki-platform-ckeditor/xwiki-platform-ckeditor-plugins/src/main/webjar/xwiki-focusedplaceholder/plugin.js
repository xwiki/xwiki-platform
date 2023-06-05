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

      // Filter out placeholders when saving
      var filterPlaceholders = {
        elements: {
          '^': function (element) {
            if (element.attributes[ATTRIBUTE_NAME] !== undefined) {
              delete element.attributes[ATTRIBUTE_NAME];
            }
          },
        }
      };
      var htmlFilter = editor.dataProcessor && editor.dataProcessor.htmlFilter;
      htmlFilter.addRules(filterPlaceholders);


      // Create a TextWatcher instance to detect changes in the document
      var placeholderTextWatcher = new CKEDITOR.plugins.textWatcher(editor, updatePlaceholder);

      // The placeholder should update at every keystroke
      placeholderTextWatcher.ignoredKeys = [];

      editor.on('instanceReady', function () {
        // Attach the textWatcher to the editor
        placeholderTextWatcher.attach();

        // The placeholder should update when the content is changed via menus.
        // snapshots are created when that occurs
        editor.on("saveSnapshot", function () {
          placeholderTextWatcher.check(false);
        });

      });

      // Using contentDom event in case the DOM Tree gets recreated by CKEditor (e.g. mode change)
      editor.on("contentDom", function () {
        var editable = editor.editable();
        // The placeholder should update when the user clicks somewhere in the document.
        editable.attachListener(editable, "click",
          function () {
            placeholderTextWatcher.check(false);
          });

        // The placeholder should update when the editor gains focus.
        editable.attachListener(editable, "focus",
          function () {
            placeholderTextWatcher.check(false);
          });

        // The placeholder should disappear when the editor loses focus.
        // (The empty document placeholder might appear)
        editable.attachListener(editable, "blur",
          function () {
            removeAllPlaceholders(editor.editable().$);
          });

      });

      /**
       * Removes any placeholder present in the document
       *
       * @param {Object} document - the DOM Document
       */
      function removeAllPlaceholders(document) {
        document.querySelectorAll("[" + ATTRIBUTE_NAME + "]").forEach(function (match) {
          match.removeAttribute(ATTRIBUTE_NAME);
        });
      }

      /**
       * Checks if a node should be considered empty
       *
       * @param {Object} node - the DOM Node
       * @return {bool} - True when the node is considered empty, false otherwise
       */
      function isEmpty(node) {

        // Consider the length of a text node as its number of children
        if (node.nodeName === "#text") {
          return !Boolean(node.length);
        }

        var i;
        for (i = 0; i < node.childNodes.length; i++) {
          var child = node.childNodes[i];

          // Child is not considered empty if it is not ignored if empty
          if (!editor.config["xwiki-focusedplaceholder"].ignoreIfEmpty.includes(child.nodeName.toLowerCase())) {
            return false;
          }

          // Child is not considered empty if it is not empty
          if (!isEmpty(child)) {
            return false;
          }
        }

        return true;
      }

      /**
       * Finds the first configured ancestor of a DOM Node
       *
       * @param {Object} node
       * @return {Object} - The first configured ancestor, null if none were found
       */
      function getFirstConfiguredAncestor(node) {

        // No parents
        if (node === null) {
          return node;
        }
        if (node.parentNode === null) {
          return node.parentNode;
        }

        // Configured element
        if (node.tagName !== undefined &&
          editor.config["xwiki-focusedplaceholder"].placeholder[node.tagName.toLowerCase()] !== undefined) {
          return node;
        }

        return getFirstConfiguredAncestor(node.parentNode);

      }

      /**
       * Updates the placeholder when the caret moves
       *
       * @param {CKEDITOR.dom.range}
       */
      function updatePlaceholder(range) {
        // Clear previous placeholders
        removeAllPlaceholders(range.root.$);

        // No placeholder when a selection is occuring
        if (!range.collapsed) {
          return null;
        }

        // Find the matching ancestor
        var ancestor = getFirstConfiguredAncestor(range.getCommonAncestor().$);
        if (ancestor === null) {
          // No placeholder when there is no configured ancestor
          return;
        }

        // Add placeholder when ancestor is considered empty
        if (isEmpty(ancestor)) {
          ancestor.setAttribute(ATTRIBUTE_NAME, getPlaceholderContent(ancestor.tagName.toLowerCase()));
        }
      }

      /**
       * Returns the expected placeholder text for a given tagName
       *
       * @param {string} tag - Lowercase tagName
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
