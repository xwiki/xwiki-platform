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
CKEDITOR.plugins.add('wikiMacro', {
  requires: 'widget',
  init : function(editor) {
    // startMacroComment: CKEDITOR.htmlParser.comment
    var getMacroOutput = function(startMacroComment) {
      var output = [];
      var parent = startMacroComment.parent;
      for (var i = startMacroComment.getIndex() + 1; i < parent.children.length; i++) {
        var child = parent.children[i];
        if (child.type === CKEDITOR.NODE_COMMENT && child.value === 'stopmacro') {
          break;
        } else {
          output.push(child);
        }
      }
      return output;
    };

    // nodes: CKEDITOR.htmlParser.node[]
    var isInline = function(nodes) {
      for (var i = 0; i < nodes.length; i++) {
        var node = nodes[i];
        if (node.type === CKEDITOR.NODE_ELEMENT && !!CKEDITOR.dtd.$block[node.name]) {
          return false;
        }
      }
      return true;
    };

    // startMacroComment: CKEDITOR.htmlParser.comment
    var wrapMacroOutput = function(startMacroComment) {
      var output = getMacroOutput(startMacroComment);
      var wrapperName = isInline(output) ? 'span' : 'div';
      var wrapper = new CKEDITOR.htmlParser.element(wrapperName, {
        'class': 'macro',
        'data-macro': startMacroComment.value
      });
      for (var i = 0; i < output.length; i++) {
        output[i].remove();
        wrapper.add(output[i]);
      }
      startMacroComment.replaceWith(wrapper);
    };

    // comment: CKEDITOR.htmlParser.comment
    var maybeWrapMacroOutput = function(comment) {
      if (comment.value.substring(0, 11) === 'startmacro:') {
        wrapMacroOutput(comment);
      } else if (comment.value === 'stopmacro') {
        comment.remove();
      }
    };

    // macroOutputWrapper: CKEDITOR.htmlParser.element
    var unWrapMacroOutput = function(macroOutputWrapper) {
      var startMacroComment = new CKEDITOR.htmlParser.comment(macroOutputWrapper.attributes['data-macro']);
      var stopMacroComment = new CKEDITOR.htmlParser.comment('stopmacro');
      var macro = new CKEDITOR.htmlParser.fragment();
      macro.add(startMacroComment);
      macro.add(stopMacroComment);
      return macro;
    };

    // content: CKEDITOR.htmlParser.fragment
    var getMacroOutputComments = function(content) {
      var macroOutputMarkers = [];
      // Note that forEach is iterating a live list, meaning that the list is updated if we remove a node from the DOM.
      // That's why we have to collect the macro output markers first and then process them.
      content.forEach(function(comment) {
        if (comment.value.substring(0, 11) === 'startmacro:' || comment.value === 'stopmacro') {
          macroOutputMarkers.push(comment);
        }
      }, CKEDITOR.NODE_COMMENT, true);
      return macroOutputMarkers;
    };

    // We didn't use the editor.dataProcessor.dataFilter because it is executed with priority 10, so after the widgets
    // are upcasted (priority 8). Only element nodes can be upcasted and wiki macro output is marked with comment nodes
    // so we need to add the macro output wrapper before the upcast takes place.
    // See http://docs.ckeditor.com/#!/api/CKEDITOR.editor-event-toHtml
    // See http://docs.ckeditor.com/#!/api/CKEDITOR.htmlDataProcessor
    // See http://docs.ckeditor.com/#!/api/CKEDITOR.htmlParser.filter
    editor.on('toHtml', function(event) {
      // dataValue is a CKEDITOR.htmlParser.fragment instance.
      getMacroOutputComments(event.data.dataValue).forEach(maybeWrapMacroOutput);
    }, null, null, 7);

    // See http://docs.ckeditor.com/#!/api/CKEDITOR.plugins.widget.definition
    editor.widgets.add('wikiMacro', {
      requiredContent: 'div(macro)[data-macro]; span(macro)[data-macro]',
      upcast: function(element) {
        return (element.name == 'div' || element.name == 'span') &&
          element.hasClass('macro') && element.attributes['data-macro'];
      },
      downcast: unWrapMacroOutput,
      pathName: 'macro'
    });
  }
});
