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

    // We didn't use the editor.dataProcessor.dataFilter because it is executed with priority 10, so after the widgets
    // are upcasted (priority 8). Only element nodes can be upcasted and wiki macro output is marked with comment nodes
    // so we need to add the macro output wrapper before the upcast takes place.
    // See http://docs.ckeditor.com/#!/api/CKEDITOR.editor-event-toHtml
    // See http://docs.ckeditor.com/#!/api/CKEDITOR.htmlDataProcessor
    // See http://docs.ckeditor.com/#!/api/CKEDITOR.htmlParser.filter
    editor.on('toHtml', function(event) {
      // dataValue is a CKEDITOR.htmlParser.fragment instance.
      event.data.dataValue.forEach(maybeWrapMacroOutput, CKEDITOR.NODE_COMMENT, true);
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
