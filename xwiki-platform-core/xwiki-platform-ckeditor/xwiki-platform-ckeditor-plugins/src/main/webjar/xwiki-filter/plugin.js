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
 *   <li>submits only the significant content</li>
 *   <li>converts the font tags into span tags</li>
 *   <li>unprotect allowed scripts</li>
 *   <li>escapes the content of style tags to avoid breaking the HTML parser</li>
 * </ul>
 *
 * @see http://docs.cksource.com/CKEditor_3.x/Developers_Guide/Data_Processor
 */
(function() {
  'use strict';
  var $ = jQuery;

  // Declare the configuration namespace.
  CKEDITOR.config['xwiki-filter'] = CKEDITOR.config['xwiki-filter'] || {
    __namespace: true
  };

  CKEDITOR.plugins.add('xwiki-filter', {
    init: function(editor) {
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

      var isScriptAllowed = function(script) {
        return script && script.name === 'script' && (
          script.attributes['data-wysiwyg'] === 'true' ||
          (typeof script.attributes.src === 'string' && script.attributes.src.indexOf('wysiwyg=true') > 0)
        );
      };

      var unprotectAllowedScripts = {
        comment: function(comment) {
          var prefix = '{cke_protected}%3Cscript%20';
          if (comment.substr(0, prefix.length) === prefix) {
            var fragment = CKEDITOR.htmlParser.fragment.fromHtml('<script ' +
              decodeURIComponent(comment.substr(prefix.length)));
            var script = fragment.children[0];
            if (isScriptAllowed(script)) {
              return script;
            }
          }
        }
      };

      // Filter the editor input.
      var dataFilter = editor.dataProcessor && editor.dataProcessor.dataFilter;
      if (dataFilter) {
        dataFilter.addRules(replaceEmptyLinesWithEmptyParagraphs, {priority: 5});
        if (editor.config.loadJavaScriptSkinExtensions) {
          dataFilter.addRules(unprotectAllowedScripts, {priority: 5});
        }
      }

      // Filter the editor output.
      var htmlFilter = editor.dataProcessor && editor.dataProcessor.htmlFilter;
      if (htmlFilter) {
        htmlFilter.addRules(replaceEmptyParagraphsWithEmptyLines, {priority: 14, applyToAll: true});
        htmlFilter.addRules(submitOnlySignificantContent, {priority: 5, applyToAll: true});
      }

      // We have to filter both the input HTML (toHtml) and the output HTML (toDataFormat) because the CKEditor HTML
      // parser is called in both cases. Priority 1 is needed to ensure our listener is called before the HTML parser
      // (which has priority 5).
      editor.on('toHtml', escapeStyleContent, null, null, 1);
      // Remove data-widget attributes on images, as otherwise they might be badly interpreted as another type of
      // widget, leading to CKEditor crashing.
      editor.on('toHtml', (event) => {
        event.data.dataValue.filter(new CKEDITOR.htmlParser.filter({
          elements: {
            img: function (element) {
              delete element.attributes['data-widget'];
            }
          }
        }));
        // We must use a priority below 8 to make sure our filter is executed before widgets are upcasted.
      }, null, null, 7);
      editor.on('toDataFormat', escapeStyleContent, null, null, 1);

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

      // CKEditor splits text nodes where the caret (selection) is when the Delete/Backspace key is pressed. This makes
      // Chrome convert a plain space at the start of the right side of the split into a non-breaking space, which in
      // turn breaks the text wrapping on small screens. What Chrome does makes a bit of sense because white space at
      // the start / end of a text is not visible in HTML unless we use non-breaking spaces. But Chrome should check if
      // the text node is alone. In any case, the root cause is the fact that CKEditor splits text nodes on delete.
      // Preventing the split is too dangerous, so we're trying to fix the non-breaking space after the Delete/Backspace
      // key is pressed (and thus after the text node split is done).
      // See https://github.com/ckeditor/ckeditor4/issues/3819 ([Chrome] &nbsp; is inserted instead of space)
      // See https://dev.ckeditor.com/ticket/11415 ([Chrome] &nbsp; is inserted instead of space)
      // See CKEDITOR-323: Insertion of &nbsp; in editor when deleting characters
      // See CKEDITOR-385: A non-breaking space is saved after adding and removing a space
      this.maybeFixNonBreakingSpaceOnDelete(editor);
    },

    nbspTimeout: null,

    maybeFixNonBreakingSpaceOnDelete: function(editor) {
      var thisPlugin = this;
      // Catch the Delete and Backspace key press.
      editor.on('key', function(event) {
        if (event.data.keyCode === /* Backspace */ 8 || event.data.keyCode === /* Delete */ 46) {
          // Unschedule the previous fix, in case it wasn't applied yet, because we're going to schedule a new one. This
          // is useful when the Delete / Backspace key is kept pressed and thus multiple key events are being fired one
          // after another. If we don't do this we may slow down the deletion of the characters on the screen.
          clearTimeout(thisPlugin.nbspTimeout);
          // Schedule the fix for the non-breaking space after the text node is split.
          thisPlugin.nbspTimeout = setTimeout(thisPlugin.maybeFixNonBreakingSpaceAfterDelete.bind(thisPlugin, editor),
            0);
        }
      });
    },

    maybeFixNonBreakingSpaceAfterDelete: function(editor) {
      var selection = editor.getSelection();
      if (selection && selection.isCollapsed()) {
        var range = selection.getRanges()[0];
        var newCaretPosition = this.maybeFixNonBreakingSpaceAt(range.startContainer, range.startOffset);
        if (newCaretPosition) {
          // Update the caret position after we fixed the non-breaking space.
          range.setEnd(newCaretPosition.node, newCaretPosition.offset);
          range.collapse();
          selection.selectRanges([range]);
        }
      }
    },

    maybeFixNonBreakingSpaceAt: function(node, offset) {
      // We want to check if the caret (node, offset) is between text nodes or at the start/end of a text node and if
      // these text nodes start/end with a non-breaking space that is not really needed. If that is the case then we
      // replace the non-breaking space with a plain space, also merging the sibling text nodes.

      if (node.type === CKEDITOR.NODE_ELEMENT) {
        // Check if the caret is between text nodes. Collect the sibling text nodes because we're going to merge them
        // after applying the fix. It's also easier to check if there is a non-breaking space to fix by looking at the
        // full text before / after the caret (no matter how many text nodes there are).
        var getSiblingTextNodes = function(node, direction) {
          var textNodes = [];
          while (node && node.type === CKEDITOR.NODE_TEXT) {
            textNodes.push(node);
            node = node[direction]();
          }
          return textNodes;
        };
        // When the direction is 'getPrevious', getSiblingTextNodes returns an array of DOM Nodes in reverse order.
        // The maybeFixNonBreakingSpace method expects both arrays to be in the actual DOM order.
        // This is why we need to reverse the textNodesBefore array.
        var textNodesBefore = getSiblingTextNodes(node.getChild(offset - 1), 'getPrevious').reverse();
        var textNodesAfter = getSiblingTextNodes(node.getChild(offset), 'getNext');
        return this.maybeFixNonBreakingSpace(textNodesBefore, textNodesAfter);
      } else if (node.type === CKEDITOR.NODE_TEXT) {
        if (offset === 0) {
          // The caret is at the start of the text node.
          return this.maybeFixNonBreakingSpaceAt(node.getParent(), node.getIndex());
        } else if (offset === node.getLength()) {
          // The caret is at the end of the text node.
          return this.maybeFixNonBreakingSpaceAt(node.getParent(), node.getIndex() + 1);
        }
      }
    },

    maybeFixNonBreakingSpace: function(textNodesBefore, textNodesAfter) {
      var leftText = textNodesBefore.map(function(textNode) {
        return textNode.getText();
      }).join('');
      var rightText = textNodesAfter.map(function(textNode) {
        return textNode.getText();
      }).join('');
      var needsFix = false;

      // Check if the text on the right starts with a non-breaking space followed by a non-space character (otherwise
      // the non-breaking space might be needed) and if the text on the left ends with a non-space character (otherwise
      // the non-breaking space from the right side might be needed). Basically we want to see if there is a
      // non-breaking space on the right side of the split and if it can be safely replaced with a plain space. Note
      // that the non-breaking space could have been inserted on purpose by the user but we cannot know this for sure so
      // we cannot preserve it. Anyway, testing shows that browsers don't preserve the non-breaking space when the user
      // deletes the text before/after it so our behavior is consistent.
      if (/\S$/.test(leftText) && /^\u00a0\S/.test(rightText)) {
        // Replace the non-breaking space with a plain space.
        rightText = ' ' + rightText.substring(1);
        needsFix = true;

      // Check if the text on the left ends with a non-breaking space preceded by a non-space character (otherwise the
      // non-breaking space might be needed) and if the text on the right starts with a non-space character (otherwise
      // the non-breaking space from the left side might be needed). Basically we want to see if there is a non-breaking
      // space on the left side of the split and if it can be safely replaced with a plain space.
      } else if (/\S\u00a0$/.test(leftText) && /^\S/.test(rightText)) {
        // Replace the non-breaking space with a plain space.
        leftText = leftText.substring(0, leftText.length - 1) + ' ';
        needsFix = true;
      }

      if (needsFix) {
        // Merge the sibling text nodes and return the new caret position.
        var textNode = textNodesBefore[0];
        while (textNode.hasNext() && textNode.getNext().type === CKEDITOR.NODE_TEXT) {
          textNode.getNext().remove();
        }
        textNode.setText(leftText + rightText);
        return {node: textNode, offset: leftText.length};
      }
    }
  });

  const domParser = new DOMParser();
  function escapeStyleContent(event) {
    let html = event.data.dataValue;
    // Depending on the editor type (inline or iframe-based) and configuration (fullPage true or false) the HTML
    // string can be a full HTML document or just a fragment (e.g. the content of the BODY tag).
    const isFragment = !html.trimEnd().endsWith('</html>');
    try {
      const doc = domParser.parseFromString(html, 'text/html');
      // We want to modify the input HTML string only if there is a style tag that need escaping.
      let modified = false;
      doc.querySelectorAll('style').forEach(style => {
        const styleContent = style.textContent;
        // Escaping the '<' (less than) character in the style content is normally not required but unfortunately
        // CKEditor's HTML parser relies heavily on regular expressions to match the start / end tags and considers
        // the '<' character as the start of a new tag, even when '<' is used inside CSS (e.g. with the content
        // property). See https://ckeditor.com/docs/ckeditor4/latest/api/CKEDITOR_htmlParser.html
        style.textContent = styleContent.replaceAll('<', '\\3C');
        modified = modified || (styleContent !== style.textContent);
      });
      if (modified) {
        event.data.dataValue = isFragment ? doc.body.innerHTML : doc.documentElement.outerHTML;
      }
    } catch (e) {
      console.warn('Failed to escape the style tags from the given HTML string: ' + html, e);
    }
  }
})();
