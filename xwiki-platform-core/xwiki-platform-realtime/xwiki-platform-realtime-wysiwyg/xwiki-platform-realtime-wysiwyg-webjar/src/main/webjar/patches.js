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
  'xwiki-realtime-transformers',
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

    _createDiffDOM(options) {
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

          this._updateSelection(options.selection, change);
        },

        postDiffApply: (change) => {
          if (!change.newNode && ['addElement', 'replaceElement'].includes(change.diff.action)) {
            // Unfortunately DiffDOM doesn't set the new node when an element is added or replaced so we have to find it
            // ourselves.
            change.newNode = this._getNodeByPath(change.diff.route);
          }

          if (['addTextElement', 'addElement', 'replaceElement'].includes(change.diff.action)) {
            diffDOM._updatedNodes.add(change.newNode);
          }
        },

        ...options
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

    _updateSelection(selection, change) {
      const validSelection = selection.every(savedRange => savedRange.start && savedRange.end);
      if (!validSelection) {
        // The selection has been invalidated by a previous change.
        return;
      }
      for (const range of selection) {
        range.start = this._updateRangeBoundary(range.start, change.diff);
        if (range.collapsed) {
          range.end = range.start;
        } else {
          range.end = this._updateRangeBoundary(range.end, change.diff);
        }
      }
    }

    _updateRangeBoundary(boundary, diff) {
      switch (diff.action) {
        case 'addElement':
        case 'addTextElement':
          return this._updateRangeBoundaryOnNodeAdded(boundary, diff);

        case 'removeElement':
        case 'removeTextElement':
          return this._updateRangeBoundaryOnNodeRemoved(boundary, diff);

        case 'modifyTextElement':
        case 'replaceElement':
          return this._updateRangeBoundaryOnNodeReplaced(boundary, diff);

        case 'relocateGroup':
          return this._updateRangeBoundaryOnGroupRelocated(boundary, diff);
      }
    }

    _updateRangeBoundaryOnNodeAdded(boundary, diff) {
      const boundaryParentPath = boundary.slice(0, -1).join('/');
      const changeParentPath = diff.route.slice(0, -1);
      // Check if the change parent is an ancestor of the boundary.
      if (boundaryParentPath.startsWith(changeParentPath.join('/')) &&
          // And if the node is added before the boundary.
          diff.route.at(-1) <= boundary[changeParentPath.length]) {
        // Increment the boundary offset to account for the added node.
        boundary[changeParentPath.length]++;
      }
      return boundary;
    }

    _updateRangeBoundaryOnNodeRemoved(boundary, diff) {
      const boundaryParentPath = boundary.slice(0, -1).join('/');
      const changeParentPath = diff.route.slice(0, -1);
      // Check if the change parent is an ancestor of the boundary.
      if (boundaryParentPath.startsWith(changeParentPath.join('/'))) {
        const changeIndex = diff.route.at(-1);
        const boundaryIndex = boundary[changeParentPath.length];
        if (changeIndex < boundaryIndex) {
          // Decrement the boundary offset to account for the removed node.
          boundary[changeParentPath.length]--;
        } else if (changeIndex === boundaryIndex) {
          // The boundary is inside the removed node, so we can't restore it anymore. We can't simply place the
          // boundary before the removed node because that might not be a valid caret position (even if we can
          // technically place the caret there, the browser might not display the caret and might ignore the typed
          // text until the user clicks somewhere else in the content).
          return null;
        }
      }
      return boundary;
    }

    _updateRangeBoundaryOnNodeReplaced(boundary, diff) {
      const boundaryParentPath = boundary.slice(0, -1).join('/');
      // Check if the change target is an ancestor of the boundary.
      if (boundaryParentPath.startsWith(diff.route.join('/'))) {
        if (diff.action === 'modifyTextElement') {
          // The boundary is inside the modified text node. We have to invalidate the selection if there are changes
          // before the boundary offset because those changes can be the result of splitting the text node (e.g. in
          // order to apply formatting to a part of the text), in which case we can't preserve the selection by using
          // the diff (the result wouldn't preserve the user intent).
          const ops = ChainPad.Diff.diff(diff.oldValue, diff.newValue);
          const minOffset = Math.min(...ops.map(op => op.offset));
          if (boundary.at(-1) > minOffset) {
            return null;
          }
        } else {
          // The boundary is inside the replaced node, so we invalidate the selection. We can't simply place the
          // boundary before the replaced node because that might not be a valid caret position (even if we can
          // technically place the caret there, the browser might not display the caret and might ignore the typed
          // text until the user clicks somewhere else in the content).
          return null;
        }
      }
      return boundary;
    }

    _updateRangeBoundaryOnGroupRelocated(boundary, diff) {
      const boundaryParentPath = boundary.slice(0, -1).join('/');
      const changePath = diff.route;
      // Check if the change target is an ancestor of the boundary.
      if (boundaryParentPath.startsWith(changePath.join('/'))) {
        const boundaryIndex = boundary[changePath.length];
        if (diff.from <= boundaryIndex && boundaryIndex < (diff.from + diff.groupLength)) {
          // The boundary is inside the relocated group, so we have to update its offset.
          if (diff.from < diff.to) {
            // Nodes are moved, along with the boundary, to the right, so we have to increment the boundary offset.
            boundary[changePath.length] += diff.to - (diff.from + diff.groupLength);
          } else {
            // Nodes are moved, along with the boundary, to the left, so we have to decrement the boundary offset.
            boundary[changePath.length] -= diff.from - diff.to;
          }
          boundary[changePath.length] += diff.to - (diff.from + diff.groupLength);
        } else if (diff.from < boundaryIndex && boundaryIndex < diff.to) {
          // Nodes are relocated from before the boundary to after it, so we have to decrement the boundary offset.
          boundary[changePath.length] -= diff.groupLength;
        } else if (diff.to <= boundaryIndex && boundaryIndex < diff.from) {
          // Nodes are relocated from after the boundary to before it, so we have to increment the boundary offset.
          boundary[changePath.length] += diff.groupLength;
        }
      }
      return boundary;
    }

    /**
     * Retrieve a node from the edited content, by its path.
     *
     * @param {Array[Number]} path a path in the content tree, where each step represents the index of a child of the
     *   previous node
     * @returns the node with the given path in the content tree
     */
    _getNodeByPath(path) {
      let node = this._editor.getContentWrapper();
      for (const index of path) {
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
      for (const comment of comments) {
        const commentElement = root.ownerDocument.createElement('xwiki-comment');
        commentElement.setAttribute('value', comment.data);
        comment.replaceWith(commentElement);
      }
      return root;
    }

    /**
     * HyperJSON doesn't support comments, so we have to convert the custom HTML elements back to comments.
     *
     * @param {Node} root the DOM subtree where to look for protected comments to restore
     * @returns {Node} the given root node with the protected comments restored (replaced by a actual comment node)
     */
    _restoreComments(root) {
      for (const commentElement of root.querySelectorAll('xwiki-comment')) {
        const comment = root.ownerDocument.createComment(commentElement.getAttribute('value'));
        commentElement.replaceWith(comment);
      }
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
      let selection = this._saveSelection();

      await this._editor.updateContent(oldContent => {
        selection = this._savePathBasedSelection(selection);

        const diffDOM = this._createDiffDOM({
          // New (added) nodes must be created using the current DOM document, where they will be inserted.
          document: oldContent.ownerDocument,
          // Update or invalidate the selection while applying the patch.
          selection
        });

        // We have to call nodeToObj ourselves because the compared DOM elements are from different documents.
        const patch = diffDOM.diff(
          DiffDOM.nodeToObj(oldContent, diffDOM.options),
          DiffDOM.nodeToObj(newContent, diffDOM.options)
        );

        diffDOM.apply(oldContent, patch);
        selection = this._restorePathBasedSelection(selection);

        return diffDOM._updatedNodes;
      }, propagate);

      this._restoreSelection(selection);
    }

    /**
     * Creates a non-live version of the current selection, that later can be used to save the path-based selection.
     *
     * @returns {Array[Object]} an array of objects (ranges) that represent the current selection in the edited content
     */
    _saveSelection() {
      if (this._editor.isReadOnly()) {
        // Let the code that put the editor in read-only mode handle the selection restore.
        return [];
      }

      // We also save the selection relative to the text content (ignoring the HTML structure), as a backup in case the
      // remote patch invalidates the saved selection paths.
      this._editor.saveSelection();

      // The DOM Selection / Range objects are live (they get updated when the DOM is modified, but the outcome is not
      // always what the user expects) so there's no point in keeping references to them. Instead we extract the
      // information we need.
      return this._editor.getSelection().map(range => ({
        collapsed: range.collapsed,
        reversed: range.reversed,
        startContainer: range.startContainer,
        startOffset: range.startOffset,
        startBefore: this._getNodeAfter(range.startContainer, range.startOffset),
        startAfter: this._getNodeBefore(range.startContainer, range.startOffset),
        endContainer: range.endContainer,
        endOffset: range.endOffset,
        endBefore: this._getNodeAfter(range.endContainer, range.endOffset),
        endAfter: this._getNodeBefore(range.endContainer, range.endOffset),
      }));
    }

    _getNodeBefore(container, offset) {
      // This returns null if the container is a text node.
      return container?.childNodes[offset - 1];
    }

    _getNodeAfter(container, offset) {
      // This returns null if the container is a text node.
      return container?.childNodes[offset];
    }

    /**
     * Saves the current selection, before applying a remote patch, in order to be able to restore it afterwards. We
     * can't save it as is because it relies on node references which may become stale after the remote patch is applied
     * (nodes can be removed or repurposed by DiffDOM). Instead we save the path from the root of the edited content to
     * the node holding the selection boundary, identifying each ancestor by its node index.
     *
     * @returns {Array[Object]} an array of objects (ranges) that could be used to restore the selection
     */
    _savePathBasedSelection(selection) {
      selection = selection.map(this._savePathBasedRange.bind(this));
      console.debug('Saved path-based selection: ', JSON.stringify(selection));
      return selection;
    }

    _savePathBasedRange(range) {
      const savedRange = {
        collapsed: range.collapsed,
        reversed: range.reversed,
        start: this._getRangeBoundaryPath(range.startContainer, range.startOffset, range.startBefore, range.startAfter)
      };
      if (range.collapsed) {
        savedRange.end = savedRange.start;
      } else {
        savedRange.end = this._getRangeBoundaryPath(range.endContainer, range.endOffset, range.endBefore,
          range.endAfter);
      }
      return savedRange;
    }

    _getRangeBoundaryPath(container, offset, beforeNode, afterNode) {
      if (container.isConnected) {
        // Fallthrough: the container is still in the DOM, we can use it to compute the path.
      } else if (beforeNode?.isConnected) {
        // The original container was removed but the node before the boundary is still in the DOM.
        container = beforeNode.parentNode;
        offset = this._getNodeIndex(beforeNode);
      } else if (afterNode?.isConnected) {
        // The original container and the node before the boundary were removed but the node after the boundary is still
        // available.
        container = afterNode.parentNode;
        offset = this._getNodeIndex(afterNode) + 1;
      } else {
        // The range boundary is not in the DOM anymore, so we can't compute its path.
        return null;
      }

      const path = this._getNodePath(container);
      return path && [...path, offset];
    }

    /**
     * Compute the path of the given DOM node within the edited content.
     *
     * @param {Node} node the node for which to compute the path
     * @returns {Array[Number]} the path of the given node in the DOM tree, each path item representing the node index
     *   at that level
     */
    _getNodePath(node) {
      const path = [];
      while (node?.parentNode && node !== this._editor.getContentWrapper()) {
        path.push(this._getNodeIndex(node));
        node = node.parentNode;
      }
      // We return the computed path only if the node is part of the edited content.
      if (node === this._editor.getContentWrapper()) {
        // Reverse the path because we built it starting from the node and going up to the content wrapper.
        return path.reverse();
      }
    }

    _getNodeIndex(node) {
      return Array.from(node.parentNode.childNodes).indexOf(node);
    }

    _restorePathBasedSelection(selection) {
      console.debug('Restoring path-based selection: ', JSON.stringify(selection));
      return selection.map(this._restorePathBasedRange.bind(this));
    }

    _restorePathBasedRange(savedRange) {
      const range = {reversed: savedRange.reversed};
      if (savedRange.start && savedRange.end) {
        range.collapsed = savedRange.start.join('/') === savedRange.end.join('/');
        range.startContainer = this._getNodeByPath(savedRange.start.slice(0, -1));
        range.startOffset = savedRange.start.at(-1);
        range.startBefore = this._getNodeAfter(range.startContainer, range.startOffset);
        range.startAfter = this._getNodeBefore(range.startContainer, range.startOffset);
        if (range.collapsed) {
          range.endContainer = range.startContainer;
          range.endOffset = range.startOffset;
          range.endBefore = range.startBefore;
          range.endAfter = range.startAfter;
        } else {
          range.endContainer = this._getNodeByPath(savedRange.end.slice(0, -1));
          range.endOffset = savedRange.end.at(-1);
          range.endBefore = this._getNodeAfter(range.endContainer, range.endOffset);
          range.endAfter = this._getNodeBefore(range.endContainer, range.endOffset);
        }
      }
      return range;
    }

    /**
     * Tries to restore the given selection, if it wasn't invalidated by the remote patch, or the text-based
     * selection otherwise.
     *
     * @param {Array[Object]} selection an array of objects (ranges) that can be used to restore the selection
     */
    _restoreSelection(selection) {
      if (!selection?.length) {
        // The selection was not saved (e.g. because the editor was read-only), so we don't try to restore it.
        return;
      }
      let ranges = selection.map(this._restoreRange.bind(this));
      if (ranges.includes(undefined)) {
        // Some of the selection ranges have been invalidated by the remote patch. We invalidate the entire selection
        // and restore the text-based selection instead.
        ranges = null;
        console.debug('Restoring the text-based selection.');
      }
      this._editor.restoreSelection(ranges);
    }

    _restoreRange(savedRange) {
      const start = this._getRangeBoundary(savedRange.startContainer, savedRange.startOffset, savedRange.startBefore,
        savedRange.startAfter);
      const end = savedRange.collapsed ? start : this._getRangeBoundary(savedRange.endContainer, savedRange.endOffset,
        savedRange.endBefore, savedRange.endAfter);
      if (start && end) {
        const range = this._editor.getContentWrapper().ownerDocument.createRange();
        range.reversed = savedRange.reversed;
        range.setStart(...start);
        range.setEnd(...end);
        return range;
      }
    }

    _getRangeBoundary(container, offset, beforeNode, afterNode) {
      if (container?.isConnected) {
        return [container, offset];
      } else if (beforeNode?.isConnected) {
        return [beforeNode.parentNode, this._getNodeIndex(beforeNode)];
      } else if (afterNode?.isConnected) {
        return [afterNode.parentNode, this._getNodeIndex(afterNode) + 1];
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
