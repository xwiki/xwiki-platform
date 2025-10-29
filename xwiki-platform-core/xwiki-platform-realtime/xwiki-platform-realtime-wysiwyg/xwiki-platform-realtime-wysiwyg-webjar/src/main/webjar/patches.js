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
  'xwiki-realtime-wysiwyg-transformers',
  'hyper-json',
  'diff-dom',
  'json.sortify',
  'chainpad',
], function (
  /* jshint maxparams:false */
  Transformers, HyperJSON, DiffDOM, JSONSortify, ChainPad
) {
  'use strict';

  class Patches {
    // We can't use private fields currently because neither JSHit nor Closure Compiler support them.
    // See https://github.com/jshint/jshint/issues/3361
    // See https://github.com/google/closure-compiler/issues/2731

    /**
     * @param {Editor} editor the editor we want to patch
     */
    constructor(editor) {
      this._editor = editor;
      this._diffDOM = this._createDiffDOM();
      this._filters = [
        change => {
          // Reject any change made directly to the root node (i.e. the editor content wrapper) because it may break the
          // editor (e.g. it may remove attributes or listeners required by the editor). Only its descendants are
          // allowed to be modified.
          if (change.node === this._editor.getContentWrapper() &&
              // Allow actions where change.node is actually the parent node where the chaange takes place.
              !['addElement', 'addTextElement', 'relocateGroup'].includes(change.diff.action)) {
            return true;
          }
        },
        ...this._editor.getFilters()
      ];
    }

    _createDiffDOM() {
      const diffDOM = new DiffDOM.DiffDOM({
        // We need fine grained diff (at attribute level) even for large content, in order to be able to properly patch
        // widgets (images, rendering macro calls).
        maxChildCount: false,

        // Form fields can appear normally only in the output of a rendering macro, which is ignored when computing the
        // changes (because macro output can depend on the current user).
        valueDiffing: false,

        preDiffApply: (change) => {
          if (this._filters.some(filter => filter(change))) {
            return true;
          }

          // addAttribute, modifyAttribute, removeAttribute: node is the owner element
          // modifyTextElement, modifyComment: node is the modified text or comment node
          // relocateGroup: node is the parent of the moved nodes
          // replaceElement, removeElement, removeTextElement: node is going to be removed
          if (['addAttribute', 'modifyAttribute', 'removeAttribute', 'modifyTextElement', 'modifyComment',
              'relocateGroup', 'replaceElement', 'removeElement', 'removeTextElement'].includes(change.diff.action)) {
            diffDOM._updatedNodes.add(change.node);
          }
        },

        postDiffApply: (change) => {
          if (!change.newNode && ['addElement', 'replaceElement'].includes(change.diff.action)) {
            // Unfortunately DiffDOM doesn't set the new node when an element is added or replaced so we have to find it
            // ourselves.
            change.newNode = this._getFromRoute(change.diff.route);
          }

          if (['addTextElement', 'addElement', 'replaceElement'].includes(change.diff.action)) {
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
     * Retrieve a node from the edited content, by its route.
     *
     * @param {Array[Number]} route a path in the content tree, where each step represents the index of a child of the
     *   previous node
     * @returns the node at the given route in the content tree
     */
    _getFromRoute(route) {
      let node = this._editor.getContentWrapper();
      for (const index of route) {
        node = node.childNodes[index];
      }
      return node;
    }

    /**
     * @returns {string} the HyperJSON serialization of the synchronized editor content
     */
    getHyperJSON() {
      const html = this._editor.getOutputHTML();
      let contentWrapper = this._parseHTML(html);
      // HyperJSON doesn't support comments, so we have to convert them to custom HTML elements.
      contentWrapper = this._protectComments(contentWrapper);
      const hyperJSON = HyperJSON.fromDOM(contentWrapper);
      return JSONSortify(hyperJSON);
    }

    /**
     * Update the editor content without affecting its caret / selection.
     * 
     * @param {string} remoteHyperJSON the new content (usually the result of a remote change), serialized as HyperJSON
     * @param {boolean} propagate true when the new content should be propagated to coeditors
     */
    async setHyperJSON(remoteHyperJSON, propagate) {
      const localHyperJSON = this.getHyperJSON();
      // Avoid processing the remote HyperJSON, computing the DOM diff (which is not necessarily empty if the HyperJSON
      // is the same because you can always have some BR tag or some DOM attribute added by the browser and which are
      // not serialized in the HyperJSON), saving the selection, applying the DOM patch and restoring the selection, if
      // there is no actual change.
      if (remoteHyperJSON === localHyperJSON) {
        return;
      }

      let contentWrapper = this._hyperJSONToDOM(remoteHyperJSON);
      // HyperJSON doesn't support comments, so we had to convert them to custom HTML elements. Let's restore them.
      contentWrapper = this._restoreComments(contentWrapper);
      const inputHTML = contentWrapper.innerHTML;
      await this.setHTML(inputHTML, propagate);
    }

    /**
     * Update the editor content without affecting its caret / selection.
     * 
     * @param {string} html the new HTML content; we expect this to be the result of rendering the wiki syntax to HTML
     * @param {boolean} propagate true when the new content should be propagated to coeditors
     */
    async setHTML(html, propagate) {
      const contentWrapper = this._editor.parseInputHTML(html);
      // Content update is asynchronous because it requires server-side rendering sometimes (e.g. when macro parameters
      // have changed).
      await this._updateContent(contentWrapper, propagate);
    }

    /**
     * @param {string} html the HTML string to parse (we expected this to be the output of the editor)
     * @returns {Element} the body element of the HTML document created from the given HTML string
     */
    _parseHTML(html) {
      return new DOMParser().parseFromString(`<body>${html}</body>`, 'text/html').body;
    }

    /**
     * HyperJSON doesn't support comments, so we have to convert them to custom HTML elements.
     *
     * @param {Node} root the DOM subtree where to look for comment nodes to protect
     * @returns {Node} the given root node with the comments protected (replaced by a custom HTML element)
     */
    _protectComments(root) {
      const treeWalker = root.ownerDocument.createTreeWalker(root, NodeFilter.SHOW_COMMENT);
      const comments = [];
      while (treeWalker.nextNode()) {
        comments.push(treeWalker.currentNode);
      }
      comments.forEach(comment => {
        const commentElement = root.ownerDocument.createElement('xwiki-comment');
        commentElement.setAttribute('value', comment.data);
        comment.replaceWith(commentElement);
      });
      return root;
    }

    /**
     * HyperJSON doesn't support comments, so we have to convert the custom HTML elements back to comments.
     *
     * @param {Node} root the DOM subtree where to look for protected comments to restore
     * @returns {Node} the given root node with the protected comments restored (replaced by a actual comment node)
     */
    _restoreComments(root) {
      root.querySelectorAll('xwiki-comment').forEach(commentElement => {
        const comment = root.ownerDocument.createComment(commentElement.getAttribute('value'));
        commentElement.replaceWith(comment);
      });
      return root;
    }

    /**
     * @param {string} hyperJSON the HyperJSON string to convert to a DOM node
     * @returns {Node} the DOM subtree corresponding to the given HyperJSON string
     */
    _hyperJSONToDOM(hyperJSON) {
      const parsedHyperJSON = JSON.parse(hyperJSON);
      return HyperJSON.toDOM(parsedHyperJSON);
    }

    /**
     * Update the editor content without affecting its caret / selection.
     *
     * @param {Node} newContent the new content to set, as a DOM node
     * @param {boolean} propagate true when the new content should be propagated to coeditors
     */
    async _updateContent(newContent, propagate) {
      const selection = this._saveSelection();

      await this._editor.updateContent(oldContent => {
        // We have to call nodeToObj ourselves because the compared DOM elements are from different documents.
        const patch = this._diffDOM.diff(DiffDOM.nodeToObj(oldContent), DiffDOM.nodeToObj(newContent));
        this._diffDOM.apply(oldContent, patch, {
          // New (added) nodes must be created using the current DOM document, where they will be inserted.
          document: oldContent.ownerDocument
        });

        const updatedNodes = this._diffDOM._updatedNodes;
        this._maybeInvalidateSavedSelection(selection, updatedNodes);

        return updatedNodes;
      }, propagate);

      await this._restoreSelection(selection);
    }

    /**
     * Saves the current selection both as a text selection (to be used in case the selected nodes are replaced or
     * removed) and as an array of relative ranges (to be used in case the selected nodes are only moved around).
     *
     * @returns {Array[Object]} an array of objects (relative ranges) that could be used to restore the selection
     */
    _saveSelection() {
      if (this._editor.isReadOnly()) {
        // Let the code that put the editor in read-only mode handle the selection restore.
        return [];
      }

      // Save the selection as a text selection.
      this._editor.saveSelection();
      // Save the selection as an array of relative ranges.
      return this._editor.getSelection().map(range => {
        // Remember also the selection direction.
        const savedRange = {reversed: range.reversed};
        if (range.startContainer.childNodes.length) {
          // We can't simply store a reference to the DOM node before / after the selection boundary because when
          // applying a remote patch, diffDOM can reuse a DOM node for a different purpose (e.g. replacing its content
          // and its attributes). So the fact that a node is still attached to the DOM after the remote patch is applied
          // doesn't mean it has the same meaning as before. At the same time, the fact that a node has been modified by
          // a remote patch (e.g. by changing the value of an attribute) doesn't necessarily mean it's meaning has
          // changed. Saving a relative selection is important to avoid restoring the text selection which is slow
          // because it relies on diffing the text content. But we should restore the relative selection only if the
          // boundary nodes preserve their meaning afte the remote patch is applied. The ensure this we save the left
          // and right path from the root of the edited content to the selection boundary nodes, and invalidate the
          // saved selection if both change after the remote patch is applied.
          savedRange.startAfter = this._getNodePath(range.startContainer.childNodes[range.startOffset - 1]);
          savedRange.startBefore = this._getNodePath(range.startContainer.childNodes[range.startOffset]);
        } else {
          savedRange.startContainer = range.startContainer;
          savedRange.startOffset = range.startOffset;
        }
        if (range.endContainer.childNodes.length) {
          savedRange.endAfter = this._getNodePath(range.endContainer.childNodes[range.endOffset - 1]);
          savedRange.endBefore = this._getNodePath(range.endContainer.childNodes[range.endOffset]);
        } else {
          savedRange.endContainer = range.endContainer;
          savedRange.endOffset = range.endOffset;
        }
        return savedRange;
      });
    }

    /**
     * Compute the path of the given DOM node within the edited content.
     *
     * @param {Node} node the node for which to compute the path
     * @returns {Object} the path of the given node in the DOM tree, each path item representing the child index at that
     *   level
     */
    _getNodePath(node) {
      const path = {
        // The node the path was computed for.
        node,
        // The child index for this node and all its ancestors, counting from the left.
        left: [],
        // The child index for this node and all its ancestors, counting from the right.
        right: []
      };
      while (node?.parentNode && node !== this._editor.getContentWrapper()) {
        const nodeIndex = Array.from(node.parentNode.childNodes).indexOf(node);
        path.left.push(nodeIndex);
        path.right.push(node.parentNode.childNodes.length - nodeIndex);
        node = node.parentNode;
      }
      // We return the computed path only if the node is part of the edited content.
      if (node === this._editor.getContentWrapper()) {
        // Reverse the path because we built it starting from the node and going up to the content wrapper.
        path.left.reverse();
        path.right.reverse();
        path.left = path.left.join('/');
        path.right = path.right.join('/');
        return path;
      }
    }

    /**
     * Invalidates the saved selection if it is affected by the updated nodes.
     *
     * @param {Array[Object]} selection an array of objects (relative ranges) that can be used to restore the selection
     * @param {Set[Node]} updatedNodes the set of updated nodes (as a result of applying remote changes to the content)
     */
    _maybeInvalidateSavedSelection(selection, updatedNodes) {
      selection.forEach(range => {
        // The saved selection is relative if the original selection had its boundaries between DOM nodes. In that case
        // we remember the DOM node before and after the selection boundaries. This allows us to restore the selection
        // properly even when DOM nodes are added or removed, as long as either the node before or after remains in the
        // DOM. The saved selection in not relative if the original selection had its boundaries inside a text node. In
        // this case, we have to restore the text selection (diff-based) because we need to take into account how many
        // characters where added or removed before the selection boundary (offset).
        if (range.startContainer?.nodeType === Node.TEXT_NODE && updatedNodes.has(range.startContainer)) {
          delete range.startContainer;
        }
        if (range.endContainer?.nodeType === Node.TEXT_NODE && updatedNodes.has(range.endContainer)) {
          delete range.endContainer;
        }
        // Invalidate the selection boundaries for which both the left and the right path have changed.
        [range.startAfter, range.startBefore, range.endAfter, range.endBefore].forEach(oldPath => {
          const newPath = this._getNodePath(oldPath?.node);
          if (newPath?.left !== oldPath?.left && newPath?.right !== oldPath?.right) {
            delete oldPath.node;
          }
        });
      });
    }

    /**
     * Tries to restore the selection first using the relative ranges, if the selected nodes are still in the DOM, and
     * then using the text selection, if the selected nodes have been replaced or removed.
     *
     * @param {Array[Object]} selection an array of objects (relative ranges) that can be used to restore the selection
     */
    async _restoreSelection(selection) {
      const invalidRange = selection.find(savedRange =>
        [savedRange.startContainer, savedRange.startAfter?.node, savedRange.startBefore?.node]
          .every(node => !node?.isConnected) ||
        [savedRange.endContainer, savedRange.endAfter?.node, savedRange.endBefore?.node]
          .every(node => !node?.isConnected));
      if (invalidRange) {
        // Some of the selected nodes were removed from the DOM or the selection was in a text node that was modified.
        // Restore the text selection.
        await this._editor.restoreSelection();
      } else if (selection.length) {
        // The selected nodes are still in the DOM so we can restore the selection using the relative ranges.
        await this._editor.restoreSelection(selection.map(savedRange => {
          const range = this._editor.getContentWrapper().ownerDocument.createRange();
          range.reversed = savedRange.reversed;
          if (savedRange.startContainer?.isConnected) {
            range.setStart(savedRange.startContainer, savedRange.startOffset);
          } else if (savedRange.startBefore?.node?.isConnected) {
            range.setStartBefore(savedRange.startBefore.node);
          } else {
            range.setStartAfter(savedRange.startAfter.node);
          }
          if (savedRange.endContainer?.isConnected) {
            range.setEnd(savedRange.endContainer, savedRange.endOffset);
          } else if (savedRange.endAfter?.node?.isConnected) {
            range.setEndAfter(savedRange.endAfter.node);
          } else {
            range.setEndBefore(savedRange.endBefore.node);
          }
          return range;
        }));
      }
    }

    /**
     * Transform the local changes (from the current version) so that they can be rebased on top of the remote changes
     * (from the next version), producing the merged version.
     *
     * @param {string} previous the common ancestor between the current version and the next version of the content
     * @param {string} next the next version of the content, usually the previous version plus the remote changes
     * @param {string} current the current version of the content, usually the previous version plus the local changes
     * @returns {string} the merged version of the content, where local changes have been rebased on top of the remote
     *   changes
     */
    static merge(previous, next, current) {
      // Determine the local and remote changes (operations).
      const localOperations = ChainPad.Diff.diff(previous, current);
      const remoteOperations = ChainPad.Diff.diff(previous, next);
      // Transform the local operations so that we can apply them on top of the remote content (next).
      const updatedLocalOperations = Transformers.RebaseNaiveJSONTransformer(localOperations, remoteOperations,
        previous);
      // Apply the updated operations to the remote content in order to perform the 3-way merge (rebase).
      const merged = ChainPad.Operation.applyMulti(updatedLocalOperations, next);
      return merged;
    }
  }

  return Patches;
});
