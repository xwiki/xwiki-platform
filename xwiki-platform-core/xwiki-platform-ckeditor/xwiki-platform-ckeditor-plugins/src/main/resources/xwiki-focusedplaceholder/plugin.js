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

    var ATTRIBUTE_NAME = 'data-xwiki-focusedplaceholder';

    CKEDITOR.plugins.add('xwiki-focusedplaceholder', {

        /**
         * Styles that would be applied to the editor by the placeholder text when visible.
         *
         * @property {String}
         */
        placeholderStyle: '[' + ATTRIBUTE_NAME + ']::before {' +
            'position: absolute;' +
            'opacity: .8;' +
            'color: #aaa;' +
            'content: attr( ' + ATTRIBUTE_NAME + ' );' +
            '}' +
            '.cke_wysiwyg_div[' + ATTRIBUTE_NAME + ']::before {' +
            'margin-top: 1em;' +
            '}',

        onLoad: function() {
            // Placeholder style
            CKEDITOR.addCss(this.placeholderStyle);
        },

        beforeInit: function(editor) {
            // Default plugin configuration, to be overriden by other plugins
            editor.config.focusedplaceholder = {
                // Default placeholder for content sectioning, text and table HTML tags
                placeholder: {
                    _fallback: "",
                    body: "xwiki-focusedplaceholder.placeholder.body", // Body
                    address: "xwiki-focusedplaceholder.placeholder.address", // Address
                    aside: "xwiki-focusedplaceholder.placeholder.aside", // Aside
                    footer: "xwiki-focusedplaceholder.placeholder.footer", // Footer
                    header: "xwiki-focusedplaceholder.placeholder.header", // Header
                    h1: "xwiki-focusedplaceholder.placeholder.h1", // Heading 1
                    h2: "xwiki-focusedplaceholder.placeholder.h2", // Heading 2
                    h3: "xwiki-focusedplaceholder.placeholder.h3", // Heading 3
                    h4: "xwiki-focusedplaceholder.placeholder.h4", // Heading 4
                    h5: "xwiki-focusedplaceholder.placeholder.h5", // Heading 5
                    h6: "xwiki-focusedplaceholder.placeholder.h6", // Heading 6
                    main: "xwiki-focusedplaceholder.placeholder.main", // Main content
                    nav: "xwiki-focusedplaceholder.placeholder.nav", // Navigation
                    section: "xwiki-focusedplaceholder.placeholder.section", // Section
                    blockquote: "xwiki-focusedplaceholder.placeholder.blockquote", // Quote
                    dl: "xwiki-focusedplaceholder.placeholder.dl", // Definition list
                    dt: "xwiki-focusedplaceholder.placeholder.dt", // Definition entry
                    dd: "xwiki-focusedplaceholder.placeholder.dd", // Description
                    figcaption: "xwiki-focusedplaceholder.placeholder.figcaption", // Caption
                    figure: "xwiki-focusedplaceholder.placeholder.figure", // Figure
                    ol: "xwiki-focusedplaceholder.placeholder.ol", // Ordered list
                    ul: "xwiki-focusedplaceholder.placeholder.ul", // Unordered list
                    menu: "xwiki-focusedplaceholder.placeholder.menu", // Menu (unordered list)
                    li: "xwiki-focusedplaceholder.placeholder.li", // List item
                    p: "xwiki-focusedplaceholder.placeholder.p", // Paragraph
                    pre: "xwiki-focusedplaceholder.placeholder.pre", // Preformated
                    caption: "xwiki-focusedplaceholder.placeholder.caption", // Title
                    th: "xwiki-focusedplaceholder.placeholder.th", // Header
                    td: "xwiki-focusedplaceholder.placeholder.td", // Cell
                },
                /* Empty inline tags (or nodeNames) that usually do not appear on screen should be ignored by default
                 * because they might get propagated to new lines by CKEditor*/
                ignoreIfEmpty: [
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
                ],
            };
        },

        init: function(editor) {

            var focusedplaceholder = editor.plugins["xwiki-focusedplaceholder"];
            focusedplaceholder.editor = editor;

            // Create a TextWatcher instance to detect changes in the document
            focusedplaceholder.placeholderTextWatcher = new CKEDITOR.plugins.textWatcher(editor,
                function(range) {
                    focusedplaceholder.updatePlaceholder(range);
                });

            // The placeholder should update at every keystroke
            focusedplaceholder.placeholderTextWatcher.ignoredKeys = [];

            editor.on('instanceReady', function() {
                // Attach the placeholder to the editor
                focusedplaceholder.placeholderTextWatcher.attach();

                /* The placeholder should update when the content is changed via menus
                 * snapshots are created when that occurs*/
                editor.on("saveSnapshot", function() {
                    focusedplaceholder.placeholderTextWatcher.check(false);
                });
            });


            /* The placeholder should update when the user clicks somewhere in the document.
             * Note: using contentDom event in case the DOM Tree gets recreated by CKEditor (e.g. mode change)*/
            editor.on("contentDom", function() {
                editor.document.$.addEventListener("click",
                    function() {
                        focusedplaceholder.placeholderTextWatcher.check(false);
                    });
            });
        },

        /**
         * Removes any placeholder present in the document
         *
         * @param {Object} document - the DOM Document
         */
        removeAllPlaceholders: function(document) {
            document.querySelectorAll("[" + ATTRIBUTE_NAME + "]").forEach(function(match) {
                match.removeAttribute(ATTRIBUTE_NAME);
            });
        },

        /**
         * Checks if an element should be considered empty
         *
         * @param {Object} element - the DOM Element
         * @return {bool}
         */
        isEmpty: function(element) {

            // Consider the length of a text node as its number of children
            if (element.nodeName === "#text") {
                return !Boolean(element.length);
            }
            var i;
            for (i = 0; i < element.childNodes.length; i++) {
                var child = element.childNodes[i];
                // Child is not empty if it doesn't have a nodeName
                if (child.nodeName === undefined) {
                    return false;
                }

                // Child is not considered empty if it is not ignored if empty
                if (!this.editor.config.focusedplaceholder.ignoreIfEmpty.includes(child.nodeName.toLowerCase())) {
                    return false;
                }

                // Child is not considered empty if it is not empty
                if (!this.isEmpty(child)) {
                    return false;
                }
            }

            return true;
        },

        /**
         * Finds the first configured ancestor of a DOM element
         *
         * @param {Object} element
         * @return {Object} - The first configured ancestor
         */
        getFirstConfiguredAncestor: function(element) {

            // No parents
            if (element === null) {
                return element;
            }
            if (element.parentNode === null) {
                return element.parentNode;
            }

            // Configured element
            if (element.tagName !== undefined &&
                this.editor.config.focusedplaceholder.placeholder[element.tagName.toLowerCase()] !== undefined) {
                return element;
            }

            return this.getFirstConfiguredAncestor(element.parentNode);

        },

        /**
         * Updates the placeholder when the caret moves
         *
         * @param {CKEDITOR.dom.range}
         */
        updatePlaceholder: function(range) {
            // Clear previous placeholders
            this.removeAllPlaceholders(range.document.$);

            // No placeholder when a selection is occuring
            if (!range.collapsed) {
                return null;
            }

            // Find the matching ancestor
            var ancestor = this.getFirstConfiguredAncestor(range.getCommonAncestor().$);
            if (ancestor === null) {
                ancestor = range.getCommonAncestor().$;
            }

            // Add placeholder when ancestor is considered empty
            if (this.isEmpty(ancestor)) {
                ancestor.setAttribute(ATTRIBUTE_NAME, this.getPlaceholderContent(ancestor.tagName.toLowerCase()));
            }
        },

        /**
         * Returns the expected placeholder text for a given element
         *
         * @param {string} tag - Lowercase tagName
         * @return {string}
         */
        getPlaceholderContent: function(tag) {

            var translationKey = this.editor.config.focusedplaceholder.placeholder[tag];

            if (translationKey === null) {
                translationKey = this.editor.config.focusedplaceholder.placeholder._fallback;
            }

            if (translationKey === undefined) {
                return "";
            }

            return this.editor.localization.get(translationKey);
        }

    });
})();