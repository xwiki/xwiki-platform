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
    // See CKEDITOR-323: Insertion of &nbsp; in editor when deleting characters
    hasNonBreakingSpaceIssue: false,

    onLoad: function() {
      var thisPlugin = this;
      // Determine if a plain space is converted into a non-breaking space when the character before it is deleted.
      // See CKEDITOR-323: Insertion of &nbsp; in editor when deleting characters
      // Note that the editor must be visible in order to reproduce the issue. Make sure it doesn't appear on screen.
      var container = $('<div id="CKEDITOR-323"/>').css({
        position: 'absolute',
        top: '-1000px',
        left: '-1000px',
        width: '100px',
        height: '100px'
      }).appendTo('body');
      var textArea = $('<textarea/>').text('<p>ab c</p>').appendTo(container);
      // We need to create an editor instance, so better wait for all the plugins to be loaded first.
      setTimeout(function() {
        CKEDITOR.replace(textArea[0], {
          'xwiki-filter': {
            // Check if the problem reproduces without our fix.
            fixNonBreakingSpace: false
          }
        }).on('instanceReady', function(event) {
          var editor = event.editor;
          var textNode = editor.editable().getFirst().getFirst();
          // Split the text node after 'b' to simulate the CKEditor behavior when pressing the delete key.
          textNode.split(2);
          // Select the 'b' character.
          var range = editor.createRange();
          range.setStart(textNode, 1);
          range.setEnd(textNode, 2);
          editor.getSelection().selectRanges([range]);
          // Delete the selected text. We need to use the low level command because there's no corresponding high level
          // command in CKEditor.
          editor.document.$.execCommand('delete');
          // Destroy the editor after the other 'instanceReady' listeners are called.
          setTimeout(function() {
            editor.destroy();
            thisPlugin.hasNonBreakingSpaceIssue = textArea.val().indexOf('a&nbsp;c') >= 0;
            container.remove();
          }, 0);
        });
      }, 0);
    },

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

      var config = editor.config['xwiki-filter'] || {};
      if (config.fixNonBreakingSpace === undefined || config.fixNonBreakingSpace) {
        this.maybeFixNonBreakingSpace(editor);
      }
    },

    maybeFixNonBreakingSpace: function(editor) {
      var scheduleFix = function(textNode) {
        setTimeout(function() {
          var text = textNode.nodeValue;
          // Double check if the text node still needs to be fixed.
          if (text.charAt(0) === '\u00A0') {
            textNode.nodeValue = ' ' + text.substring(1);
          }
        }, 0);
      };

      var isSpaceConvertedToNonBreakingSpace = function(oldValue, newValue) {
        return oldValue.length > 0 && newValue.length > 0 && newValue.charAt(0) === '\u00A0' &&
          oldValue.charAt(0) === ' ' && newValue.substring(1) === oldValue.substring(1);
      };

      var isPrecededByText = function(node) {
        var previous = node.previousSibling;
        return previous && previous.nodeType === Node.TEXT_NODE && previous.nodeValue.length > 0 &&
          previous.nodeValue.charAt(previous.nodeValue.length - 1) !== ' ';
      };

      var thisPlugin = this;
      editor.once('instanceReady', function(event) {
        var mutationObserver = new MutationObserver(function(mutations, mutationObserver) {
          if (!thisPlugin.hasNonBreakingSpaceIssue) {
            mutationObserver.disconnect();
            return;
          }
          mutations.forEach(function(mutation) {
            if (mutation.target.nodeType === Node.TEXT_NODE &&
                isSpaceConvertedToNonBreakingSpace(mutation.oldValue, mutation.target.nodeValue) &&
                isPrecededByText(mutation.target)) {
              scheduleFix(mutation.target);
            }
          });
        });
        mutationObserver.observe(editor.editable().$, {
          characterData: true,
          characterDataOldValue: true,
          subtree: true
        });
      });
    }
  });
})();
