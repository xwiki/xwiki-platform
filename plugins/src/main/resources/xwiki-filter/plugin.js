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

/**
 * The following transformations (specific to the XWiki Rendering) are performed:
 *
 * <ul>
 *   <li>converts empty lines to empty paragraphs and back</li>
 * </ul>
 *
 * @see http://docs.cksource.com/CKEditor_3.x/Developers_Guide/Data_Processor
 */
(function() {
  'use strict';
  CKEDITOR.plugins.add('xwiki-filter', {
    init : function(editor) {
      var replaceEmptyLinesWithEmptyParagraphs = {
        elements: {
          div: function(element) {
            if (element.attributes['class'] === 'wikimodel-emptyline') {
              element.name = 'p';
              delete element.attributes['class'];
              // Skip the subsequent rules as we changed the element name.
              return element;
            }
          }
        }
      };

      var replaceEmptyParagraphsWithEmptyLines = {
        elements: {
          p: function(element) {
            var index = element.getIndex();
            // Empty lines are used to separate blocks of content so normally they are not the first or the last child.
            // See CKEDITOR-87: Table copy-pasted from a Word file into CKEditor does not display properly on page view.
            if (index > 0 && index < element.parent.children.length - 1 && isEmptyParagraph(element)) {
              element.name = 'div';
              element.attributes['class'] = 'wikimodel-emptyline';
              // Skip the subsequent rules as we changed the element name.
              return element;
            }
          },
          // We need a separate rule to clean the empty line after the block filter rule adds the space char.
          div: function(element) {
            if (element.attributes['class'] === 'wikimodel-emptyline') {
              while (element.children.length > 0) {
                element.children[0].remove();
              }
            }
          }
        }
      };

      var isEmptyParagraph = function(paragraph) {
        var children = paragraph.children;
        for (var i = 0; i < children.length; i++) {
          var child = children[i];
          // Ignore the BR element if it's the last child.
          if ((child.type === CKEDITOR.NODE_ELEMENT && !(child.name === 'br' && i === children.length - 1)) ||
              // Ignore white-space in text nodes. It seems the text node value is HTML encoded..
              (child.type === CKEDITOR.NODE_TEXT && CKEDITOR.tools.htmlDecode(child.value).trim() !== '')) {
            return false;
          }
        }
        return true;
      };

      // Discard the HTML markup that is not needed in the wiki syntax conversion. This way we prevent unexpected
      // conversion errors caused by such markup.
      var submitOnlySignificantContent = {
        elements: {
          html: function(element) {
            if (!editor.config.fullData) {
              // Discard everything outside the BODY element. Note that we keep the HTML and BODY elements because they
              // allow us to convert the HTML to wiki syntax without needing to perform server-side HTML cleaning (to
              // add the missing tags).
              // See CKEDITOR-47: Styles are included in the content when using Firebug during page save
              // See CKEDITOR-117: The WYSIWYG saves crap when Grammarly browser plugin is enabled
              var body = element.getFirst('body');
              element.children = body ? [body] : [];
            }
          },
          body: function(element) {
            if (!editor.config.fullData) {
              // Discard the attributes of the BODY element.
              element.attributes = {};
            }
          },
          script: function(element) {
            // Browser extensions can inject script tags into the editing area. Remove them when saving the content.
            // Note that we cannot rely on the Advanced Content Filter for this because it handles only the content that
            // is added by editor features or through copy pasting. The browser extensions can bypass it.
            // See CKEDITOR-133: Use of Greasemonkey in Firefox can interfere with CKEditor content.
            if (!editor.config.fullData) {
              return false;
            }
          }
        }
      };

      // Filter the editor input.
      var dataFilter = editor.dataProcessor && editor.dataProcessor.dataFilter;
      if (dataFilter) {
        dataFilter.addRules(replaceEmptyLinesWithEmptyParagraphs, {priority: 5});
      }

      // Filter the editor output.
      var htmlFilter = editor.dataProcessor && editor.dataProcessor.htmlFilter;
      if (htmlFilter) {
        htmlFilter.addRules(replaceEmptyParagraphsWithEmptyLines, {priority: 14, applyToAll: true});
        htmlFilter.addRules(submitOnlySignificantContent, {priority: 5, applyToAll: true});
      }

      // Transform <font color="..." face="..."> into <span style="color: ...; font-family: ...">.
      // See https://ckeditor.com/old//comment/125305#comment-125305
      editor.filter.addTransformations([
        [
          {
            element: 'font',
            left: function(element) {
              return element.attributes.color || element.attributes.face;
            },
            right: function(element) {
              element.styles = element.styles || {};
              if (element.attributes.color) {
                element.styles.color = element.attributes.color;
                delete element.attributes.color;
              }
              if (element.attributes.face) {
                element.styles['font-family'] = element.attributes.face;
                delete element.attributes.face;
              }
              // Drop the size attribute because it's to complex to convert to CSS.
              // See https://developer.mozilla.org/en-US/docs/Web/HTML/Element/font
              delete element.attributes.size;
              element.name = 'span';
              return element;
            }
          }
        ]
      ]);
    }
  });
})();
