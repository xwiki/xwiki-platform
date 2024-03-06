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
define('xwiki-realtime-wysiwyg-patches', [
  'xwiki-realtime-wysiwyg-filters',
  'xwiki-realtime-wysiwyg-transformers',
  'hyper-json',
  'diff-dom',
  'json.sortify',
  'chainpad',
], function (
  /* jshint maxparams:false */
  Filters, Transformers, HyperJSON, DiffDOM, JSONSortify, ChainPad
) {
  'use strict';

  // HTML block-level elements.
  const blocks = {"audio":1, "dd":1, "dt":1, "figcaption":1, "li":1, "video":1, "address":1, "article":1, "aside":1,
    "blockquote":1, "details":1, "div":1, "dl":1, "fieldset":1, "figure":1, "footer":1, "form":1, "h1":1, "h2":1,
    "h3":1, "h4":1, "h5":1, "h6":1, "header":1, "hgroup":1, "hr":1, "main":1, "menu":1, "nav":1, "ol":1, "p":1, "pre":1,
    "section":1, "table":1, "ul":1, "center":1, "dir":1, "noframes":1, "body": 1};

  // HTML block-level elements that can't be edited properly when empty, which is why we need to add a BR element.
  const emptyBlocksExpectingLineEnding = 'p,div,h1,h2,h3,h4,h5,h6,li,td,th,dt,dd'.split(',')
    .map(block => `${block}:empty`).join();

  class Patches {
    // We can't use private fields currently because neither JSHit nor Closure Compiler support them.
    // See https://github.com/jshint/jshint/issues/3361
    // See https://github.com/google/closure-compiler/issues/2731

    /**
     * @param {Editor} editor the editor we want to patch
     */
    constructor(editor) {
      this._editor = editor;
      this._diffDOM = Patches._createDiffDOM();
      this._filters = new Filters();
      this._filters.filters.push(...editor.getCustomFilters());
    }

    static _createDiffDOM() {
      const diffDOM = new DiffDOM.DiffDOM({
        preDiffApply: (change) => {
          if (['replaceElement', 'removeElement', 'removeTextElement'].includes(change.diff.action)) {
            diffDOM._updatedNodes.add(change.node.parentNode);
          } else if (['addAttribute', 'modifyAttribute', 'removeAttribute', 'modifyTextElement', 'modifyValue',
              'modifyComment', 'modifyChecked', 'modifySelected', 'relocateGroup'].includes(change.diff.action)) {
            diffDOM._updatedNodes.add(change.node);
          }
        },

        postDiffApply: (change) => {
          if (['addTextElement', 'addElement'].includes(change.diff.action)) {
            diffDOM._updatedNodes.add(change.newNode);
          }
        }
      });

      const originalApply = DiffDOM.DiffDOM.prototype.apply;
      diffDOM.apply = function (...args) {
        // Reset the list of updated nodes before applying a patch.
        this._updatedNodes = new Set();

        const result = originalApply.apply(this, args);

        // Remove null and undefined values from the list of updated nodes.
        this._updatedNodes.delete(null);
        this._updatedNodes.delete(undefined);

        return result;
      };

      return diffDOM;
    }

    /**
     * @param {Node} node the DOM node to serialize as HyperJSON
     * @param {boolean} raw whether to filter the editor content or not before serializing it to HyperJSON;
     *   {@code true} to serialize the raw (unfiltered) content, {@code false} to serialize the filtered (normalized)
     *   content
     * @returns {string} the serialization of the given DOM node as HyperJSON
     */
    _stringifyNode(node, raw) {
      const predicate = !raw && this._filters.shouldSerializeNode.bind(this._filters);
      const filter = !raw && this._filters.filterHyperJSON.bind(this._filters);
      const hyperJSON = HyperJSON.fromDOM(node, predicate, filter);
      if (!raw) {
        // The root node depends on the type of editor. For the classical iframe-based editor the root node is the BODY.
        // For the in-place editor the root node may be a DIV. We have to normalize the root node in order to be able to
        // synchronize the content between different types of editors.
        hyperJSON[0] = 'xwiki-content';
        // Ignore all root attributes because they normally store user preferences that shouldn't be shared.
        hyperJSON[1] = {};
      }
      return JSONSortify(hyperJSON);
    }

    /**
     * @param {boolean} raw whether to filter the editor content or not before serializing it to HyperJSON;
     *   {@code true} to serialize the raw (unfiltered) content, {@code false} to serialize the filtered (normalized)
     *   content
     * @returns {string} the serialization of the editor content as HyperJSON
     */
    getHyperJSON(raw) {
      return this._stringifyNode(this._normalizeContent(this._editor.getContentWrapper()), raw);
    }

    /**
     * Update the editor content without affecting its caret / selection.
     * 
     * @param {string} remoteFilteredHyperJSON the new normalized (filtered) content (usually the result of a remote
     *   change), serialized as HyperJSON
     *
     * @param {boolean} propagate true when the new content should be propagated to coeditors
     */
    setHyperJSON(remoteFilteredHyperJSON, propagate) {
      let remoteHyperJSON;
      try {
        remoteHyperJSON = this._revertHyperJSONFilters(remoteFilteredHyperJSON);
      } catch (e) {
        console.error('Failed to revert the HyperJSON filters.', {
          filteredHyperJSON: remoteFilteredHyperJSON,
          error: e
        });
        return;
      }

      let newContent;
      try {
        newContent = HyperJSON.toDOM(JSON.parse(remoteHyperJSON));
      } catch (e) {
        console.error('Failed to parse the HyperJSON string.', {
          filteredHyperJSON: remoteFilteredHyperJSON,
          rawHyperJSON: remoteHyperJSON,
          error: e
        });
        return;
      }

      this._updateContent(newContent, propagate);
    }

    /**
     * Update the editor content without affecting its caret / selection.
     * 
     * @param {string} html the new HTML content
     * @param {boolean} propagate true when the new content should be propagated to coeditors
     */
    setHTML(html, propagate) {
      const fixedHtml = this._editor.convertDataToHtml(html);

      let doc;
      try {
        doc = new DOMParser().parseFromString(fixedHtml, 'text/html');
      } catch (e) {
        console.error('Failed to parse the given HTML string: ' + html, e);
        return;
      }

      // We convert to HyperJSON and set the HyperJSON so that we can use the same filters
      // as when receiving content from coeditors.
      const hjson = this._stringifyNode(this._normalizeContent(doc.body), false);
      this.setHyperJSON(hjson, propagate);
    }

    /**
     * Update the editor content without affecting its caret / selection.
     * 
     * @param {Node} newContent the new content to set, as a DOM node
     * @param {boolean} propagate true when the new content should be propagated to coeditors
     */
    _updateContent(newContent, propagate) {
      // Remember where the selection is, to be able to restore it in case the content update affects it.
      this._editor.saveSelection();
      const selection = this._editor.getSelection();
      let rangeBefore = selection?.rangeCount && this._copyRangeBoundaryPoints(selection.getRangeAt(0));

      const oldContent = this._editor.getContentWrapper();
      if (!oldContent.contains(rangeBefore?.startContainer) && !oldContent.contains(rangeBefore?.endContainer)) {
        rangeBefore = false;
      }

      // We have to call nodeToObj ourselves because the compared DOM elements are from different documents.
      const patch = this._diffDOM.diff(DiffDOM.nodeToObj(oldContent), DiffDOM.nodeToObj(newContent));
      this._diffDOM.apply(oldContent, patch, {
        document: oldContent.ownerDocument
      });

      this._editor.contentUpdated(this._diffDOM._updatedNodes, propagate);

      // Restore the selection if the editor had a selection (i.e. if the selection was inside the editing area) before
      // the content update and it was affected by the content update. Note that the selection restore focuses the
      // editor.
      const rangeAfter = selection?.rangeCount && selection.getRangeAt(0);
      if (rangeBefore && (!rangeBefore.startContainer.isConnected || !rangeBefore.endContainer.isConnected ||
          !this._isSameRange(rangeBefore, rangeAfter))) {
        this._editor.restoreSelection();
      }
    }

    /**
     * DOM ranges are updated automatically when the DOM is modified which makes it hard to detect if a range has been
     * affected by a DOM change. Using {@code Range.cloneRange()} doesn't help either: the cloned range is still
     * updated automatically. The only option is to copy the range boundary points and check them later after the DOM is
     * modified.
     *
     * @param {Range} range the DOM range whose boundary points we want to copy
     * @returns an object holding the boundary points of the given range
     */
    _copyRangeBoundaryPoints(range) {
      return {
        startContainer: range?.startContainer,
        startOffset: range?.startOffset,
        endContainer: range?.endContainer,
        endOffset: range?.endOffset
      };
    }

    /**
     * Checks if two DOM ranges have the same boundary points.
     *
     * @param {Range} before the first range
     * @param {Range} after the second range
     * @returns {@code true} if the given ranges have the same boundary points, {@code false} otherwise
     */
    _isSameRange(before, after) {
      return before?.startContainer === after?.startContainer &&
        before?.startOffset === after?.startOffset &&
        before?.endContainer === after?.endContainer &&
        before?.endOffset === after?.endOffset;
    }

    _revertHyperJSONFilters(remoteFilteredHyperJSON) {
      const localFilteredHyperJSON = this.getHyperJSON();
      const localHyperJSON = this.getHyperJSON(true);
      // Determine the list of operations that can be used to add the filtered content back.
      const localOperations = ChainPad.Diff.diff(localFilteredHyperJSON, localHyperJSON);
      const remoteOperations = ChainPad.Diff.diff(localFilteredHyperJSON, remoteFilteredHyperJSON);
      // Transform the local operations so that we can apply them on top of the remote content.
      const updatedLocalOperations = Transformers.RebaseNaiveJSONTransformer(localOperations, remoteOperations,
        localFilteredHyperJSON);
      // Apply the updated operations to the remote content in order to perform the 3-way merge (rebase). This way we
      // integrate the local filtered content (user state, browser specific markup) into the remote content.
      const remoteHyperJSON = ChainPad.Operation.applyMulti(updatedLocalOperations, remoteFilteredHyperJSON);
      return remoteHyperJSON;
    }

    /**
     * The real-time sychronization works best when all users work with the same content (DOM). However, the edited
     * content (DOM) is not always the same in all browsers. For instance some browsers require a BR element to make
     * empty blocks editable, while others don't. Some browsers remove that BR element once you start typing, others
     * don't. The goal of this function is to remove some of these inconsistencies. Note that this functions modifies
     * directly the edited content (DOM), unlike the HyperJSON filters, so it doesn't remove user state (which is done
     * by the filters).
     *
     * @param {Node} content the DOM node to normalize
     * @returns {Node} the normalized DOM node
     */
    _normalizeContent(content) {
      // Remove empty text nodes and join adjacent text nodes.
      content.normalize();

      // Firefox needs a BR element at the end of each block otherwise space characters at the end of the block are
      // removed once you start typing. We need to add this BR ourselves for other browsers to ensure all users are
      // editing the same content (minus the user state).
      this._addLineEndings(content);

      return content;
    }

    /**
     * Adds a BR element to empty blocks and after each text node that is either followed by a block or is the last leaf
     * of the block element that contains it.
     *
     * @param {Node} node the node to which we want to add line endings
     */
    _addLineEndings(node) {
      // Add a BR element to empty blocks in order to make them editable.
      node.querySelectorAll(emptyBlocksExpectingLineEnding).forEach(emptyBlock => {
        emptyBlock.appendChild(emptyBlock.ownerDocument.createElement('br'));
      });

      // Add a BR element after text nodes that are followed by a block or that are the last leaf of a block.
      const textIterator = node.ownerDocument.createNodeIterator(node, NodeFilter.SHOW_TEXT,
        text => (
          // Look for text nodes that either are the last child node of their parent or are followed by a block...
          (!text.nextSibling || blocks[text.nextSibling.nodeName.toLowerCase()]) &&
          // ...and that are not empty (excluding whitespace, except for non-breaking space).
          (text.nodeValue.trim().length || text.nodeValue.includes('\u00A0'))
        ) ? NodeFilter.FILTER_ACCEPT : NodeFilter.FILTER_REJECT);

      let textNode;
      while ((textNode = textIterator.nextNode())) {
        let node = textNode;
        // Go up the DOM as long as there is no next sibling and we remain inside the current block.
        while (!node.nextSibling && node.parentNode && !blocks[node.parentNode.nodeName.toLowerCase()]) {
          node = node.parentNode;
        }
        // We either found the next node or we reached the block containing the text node.
        if (node.nextSibling && blocks[node.nextSibling.nodeName.toLowerCase()]) {
          // The text node is followed by a block, add a BR element before the block.
          node.parentNode.insertBefore(node.ownerDocument.createElement('br'), node.nextSibling);
        } else if (!node.nextSibling && node.parentNode) {
          // The text node is the last leaf of a block element. Append a BR at the end of that block.
          node.parentNode.appendChild(node.ownerDocument.createElement('br'));
        }
      }
    }
  }

  return Patches;
});
