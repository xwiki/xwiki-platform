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

  // Declare the configuration namespace.
  CKEDITOR.config['xwiki-source'] = CKEDITOR.config['xwiki-source'] || {
    __namespace: true
  };

  CKEDITOR.plugins.xwikiSource = {};

  CKEDITOR.plugins.add('xwiki-source', {
    requires: 'sourcearea,notification,xwiki-loading,xwiki-localization',

    onLoad: function() {
      require(['textSelection'], function(textSelection) {
        CKEDITOR.plugins.xwikiSource.textSelection = textSelection;
      });
    },

    init: function(editor) {
      // The source command is not registered if the editor is loaded in-line.
      var sourceCommand = editor.getCommand('source');
      if (sourceCommand) {
        editor.on('beforeSetMode', jQuery.proxy(this.onBeforeSetMode, this));
        editor.on('beforeModeUnload', jQuery.proxy(this.onBeforeModeUnload, this));
        editor.on('mode', jQuery.proxy(this.onMode, this));

        // The default source command is not asynchronous so it becomes (re)enabled right after the editing mode is
        // changed. In our case switching between WYSIWYG and Source mode is asynchronous because we need to convert the
        // edited content on the server side. Thus we need to prevent the source command from being enabled while the
        // conversion takes place.
        // CKEDITOR-66: Switch to source corrupt page when connection lost or when connection is very slow
        var oldCheckAllowed = sourceCommand.checkAllowed;
        sourceCommand.checkAllowed = function() {
          return !this.running && oldCheckAllowed.apply(this, arguments);
        };
      }
    },

    onBeforeSetMode: function(event) {
      var newMode = event.data;
      var editor = event.editor;
      var currentModeFailed = editor.mode && (editor._.modes[editor.mode] || {}).failed;
      if (this.isModeSupported(newMode) && !currentModeFailed) {
        this.startLoading(editor);
      }
    },

    isModeSupported: function(mode) {
      return mode === 'wysiwyg' || mode === 'source';
    },

    onBeforeModeUnload: function(event) {
      var editor = event.editor;
      if (!this.isModeSupported(editor.mode)) {
        return;
      }
      var mode = editor._.modes[editor.mode];
      if (mode.failed) {
        mode.dirty = mode.failed = false;
        // Make sure we retry the conversion on the next mode switch.
        delete mode.data;
      } else {
        var oldData = mode.data;
        var newData = this.getFullData(editor);
        mode.dirty = oldData !== newData;
        mode.data = newData;
      }
    },

    getFullData: function(editor) {
      var isFullData = editor.config.fullData;
      editor.config.fullData = true;
      var fullData = editor.getData();
      editor.config.fullData = isFullData;
      return fullData;
    },

    onMode: function(event) {
      var editor = event.editor;
      var promise;
      if (editor.mode === 'wysiwyg' && editor._.previousMode === 'source') {
        // Convert from wiki syntax to HTML.
        promise = this.maybeConvertHTML(editor, true);
      } else if (editor.mode === 'source' && editor._.previousMode === 'wysiwyg') {
        // Convert from HTML to wiki syntax.
        promise = this.maybeConvertHTML(editor, false);
      } else if (this.isModeSupported(editor.mode)) {
        promise = jQuery.Deferred().resolve(editor);
      }
      if (promise) {
        promise.always(jQuery.proxy(this, 'endLoading'));
      }
    },

    maybeConvertHTML: function(editor, toHTML) {
      var oldMode = editor._.modes[editor._.previousMode];
      var newMode = editor._.modes[editor.mode];
      if (oldMode.dirty || typeof newMode.data !== 'string') {
        return this.convertHTML(editor, toHTML);
      } else {
        var deferred = jQuery.Deferred();
        editor.setData(newMode.data, {
          callback: jQuery.proxy(deferred, 'resolve', editor)
        });
        return deferred.promise();
      }
    },

    convertHTML: function(editor, toHTML) {
      var thisPlugin = this;
      var config = editor.config['xwiki-source'] || {};
      var deferred = jQuery.Deferred();
      jQuery.post(config.htmlConverter, {
        fromHTML: !toHTML,
        toHTML: toHTML,
        text: editor._.previousModeData
      }).done(function(data) {
        editor.setData(data, {
          callback: function() {
            // Take a snapshot after the data has been set, in order to be able to detect changes.
            editor._.modes[editor.mode].data = thisPlugin.getFullData(editor);
            deferred.resolve(editor);
          }
        });
      }).fail(function() {
        // Switch back to the previous edit mode without performing a conversion.
        editor._.modes[editor.mode].failed = true;
        editor.setMode(editor._.previousMode, function() {
          deferred.reject(editor);
          editor.showNotification(editor.localization.get('xwiki-source.conversionFailed'), 'warning');
        });
      });
      return deferred.promise();
    },

    startLoading: function(editor) {
      this.saveSelection(editor);
      editor.setLoading(true);
      // Prevent the source command from being enabled while the conversion takes place.
      var sourceCommand = editor.getCommand('source');
      // We have to set the flag before setting the command state in order to be taken into account.
      sourceCommand.running = true;
      sourceCommand.setState(CKEDITOR.TRISTATE_DISABLED);
      if (editor.mode === 'source') {
        // When switching from Source mode to WYSIWYG mode the wiki syntax is converted to HTML on the server side.
        // Before we receive the result the Source plugin sets the source (wiki syntax) as the data for the WYSIWYG
        // mode. This adds an entry (snapshot) in the undo history for the WYSIWYG mode. In order to prevent this we
        // lock the undo history until the conversion is done.
        // See CKEDITOR-58: Undo operation can replace the rich text content with wiki syntax
        editor.fire('lockSnapshot');
      }
      if (editor.editable()) {
        editor.container.findOne('.cke_button__source_icon').addClass('loading');
      }
      // A bug in Internet Explorer 11 prevents the user from typing into the Source text area if the WYSIWYG text
      // area is focused and the selection is collapsed before switching to Source mode. In order to avoid this
      // problem we have to either remove the focus from the WYSIWYG text area or to make sure the selection is not
      // collapsed before the switch. We didn't manage to remove the focus because we don't know what other focusable
      // elements are available on the page. Thus the solution we applied was to select all the content before the
      // switch so that the selection is not collapsed.
      // CKEDITOR-102: Unable to edit a page in Source mode on IE11
      // https://connect.microsoft.com/IE/feedback/details/1613994/ie-10-11-iframe-removal-causes-loss-of-the-ability-to-focus-input-elements
      // https://dev.ckeditor.com/ticket/7386
      if (editor.document && editor.document != CKEDITOR.document && CKEDITOR.env.ie && !CKEDITOR.env.edge) {
        // We apply the fix only if the WYSIWYG text area is using an iframe and if the browser is Internet Explorer
        // except Edge (that doesn't have the problem).
        editor.document.$.execCommand('SelectAll', false, null);
      }
    },

    endLoading: function(editor) {
      if (editor.editable()) {
        editor.container.findOne('.cke_button__source_icon').removeClass('loading');
      }
      if (editor.mode === 'wysiwyg') {
        // Unlock the undo history after the conversion is done and the WYSIWYG mode data is set.
        editor.fire('unlockSnapshot');
      }
      var sourceCommand = editor.getCommand('source');
      // We have to set the flag before setting the command state in order to be taken into account.
      sourceCommand.running = false;
      sourceCommand.setState(editor.mode !== 'source' ? CKEDITOR.TRISTATE_OFF : CKEDITOR.TRISTATE_ON);
      editor.setLoading(false);
      this.restoreSelection(editor);
    },

    saveSelection: function(editor) {
      var editable = editor.editable();
      if (editable) {
        editor._.textSelection = CKEDITOR.plugins.xwikiSource.textSelection.from(editable.$);
      } else {
        delete editor._.textSelection;
      }
    },

    restoreSelection: function(editor) {
      var editable = editor.editable();
      var textSelection = editor._.textSelection;
      if (editable && textSelection) {
        editor.focus();
        textSelection.applyTo(editable.$);
        editor.selectionChange(true);
        // The selection should be already scrolled into view (in most cases), but we do it again using the CKEditor API
        // to cover the cases when the selection start container height is larger than the editing area.
        var selection = editor.getSelection();
        if (selection) {
          selection.scrollIntoView();
        }
      }
    }
  });
})();

define('node-module', {
  load: function(name, req, onLoad, config) {
    window.module = window.module || {};
    req([name], function () {
      onLoad(window.module.exports);
    });
  }
});

define('textSelection', ['jquery', 'node-module!fast-diff'], function($, diff) {
  var isTextInput = function(element) {
    return typeof element.setSelectionRange === 'function';
  };

  var getTextSelection = function(element) {
    if (isTextInput(element)) {
      return {
        text: element.value,
        startOffset: element.selectionStart,
        endOffset: element.selectionEnd
      };
    } else {
      var selection = element.ownerDocument.defaultView.getSelection();
      if (selection && selection.rangeCount > 0) {
        var range = selection.getRangeAt(0);
        if (element.contains(range.commonAncestorContainer)) {
          return getTextSelectionFromRange(element, range);
        }
      }
      return {
        text: element.innerText,
        startOffset: 0,
        endOffset: 0
      };
    }
  };

  var getTextSelectionFromRange = function(root, range) {
    var beforeRange = root.ownerDocument.createRange();
    beforeRange.setStartBefore(root);
    beforeRange.setEnd(range.startContainer, range.startOffset);
    var startOffset = beforeRange.toString().length;
    return {
      text: root.innerText,
      startOffset: startOffset,
      endOffset: startOffset + range.toString().length
    };
  };

  var getRangeFromTextSelection = function(root, textSelection) {
    var start = findTextOffsetInDOM(root, textSelection.startOffset);
    var end = textSelection.endOffset === textSelection.startOffset ? start :
      findTextOffsetInDOM(root, textSelection.endOffset);
    var range = root.ownerDocument.createRange();
    range.setStart(start.node, start.offset);
    range.setEnd(end.node, end.offset);
    return range;
  };

  var findTextOffsetInDOM = function(root, offset) {
    var count = 0, node, iterator = root.ownerDocument.createNodeIterator(root, NodeFilter.SHOW_TEXT);
    do {
      node = iterator.nextNode();
      count += node ? node.nodeValue.length : 0;
    } while (node && count < offset);
    if (node) {
      return {
        node: node,
        offset: offset - (count - node.nodeValue.length)
      };
    } else {
      return {
        node: root,
        offset: offset > 0 ? root.childNodes.length : 0
      };
    }
  };

  var changeText = function(textSelection, newText) {
    var changes = diff(textSelection.text, newText);
    var startOffset = findTextOffsetInChanges(changes, textSelection.startOffset);
    var endOffset = textSelection.endOffset === textSelection.startOffset ? startOffset :
      findTextOffsetInChanges(changes, textSelection.endOffset);
    return {
      text: newText,
      startOffset: startOffset,
      endOffset: endOffset
    };
  };

  var findTextOffsetInChanges = function(changes, oldOffset) {
    var count = 0, newOffset = oldOffset;
    for (var i = 0; i < changes.length && count < oldOffset; i++) {
      var change = changes[i];
      if (change[0] < 0) {
        // Delete: shift the offset to the left.
        if (count + change[1].length > oldOffset) {
          // Shift the offset to the left with the number of deleted characters before the original offset.
          newOffset -= oldOffset - count;
        } else {
          // Shift the offset to the left with the number of deleted characters.
          newOffset -= change[1].length;
        }
        count += change[1].length;
      } else if (change[0] > 0) {
        // Insert: shift the offset to the right with the number of inserted characters.
        newOffset += change[1].length;
      } else {
        // Keep: don't change the offset.
        count += change[1].length;
      }
    }
    return newOffset;
  };

  var applySelection = function(element, range) {
    if (isTextInput(element)) {
      // Scroll the selection into view.
      // See https://bugs.chromium.org/p/chromium/issues/detail?id=331233
      var fullText = element.value;
      element.value = fullText.substring(0, range.endOffset);
      element.scrollTop = element.scrollHeight;
      element.value = fullText;
      // And then apply the selection.
      element.setSelectionRange(range.startOffset, range.endOffset);
    } else {
      // Scroll the selection into view.
      var scrollTarget = range.startContainer;
      if (scrollTarget.nodeType !== Node.ELEMENT_NODE) {
        scrollTarget = scrollTarget.parentNode;
      }
      scrollTarget.scrollIntoView();
      // And then apply the selection.
      var selection = element.ownerDocument.defaultView.getSelection();
      selection.removeAllRanges();
      selection.addRange(range);
    }
  };

  return {
    from: function(element) {
      return $.extend({}, this, getTextSelection(element));
    },
    applyTo: function(element) {
      var range;
      if (isTextInput(element)) {
        range = this.withText(element.value);
      } else {
        range = this.withText(element.innerText).asRange(element);
      }
      applySelection(element, range);
    },
    withText: function(text) {
      if (this.text === text) {
        return this;
      } else {
        return $.extend({}, this, changeText(this, text));
      }
    },
    asRange: function(root) {
      return getRangeFromTextSelection(root, this);
    }
  };
});
