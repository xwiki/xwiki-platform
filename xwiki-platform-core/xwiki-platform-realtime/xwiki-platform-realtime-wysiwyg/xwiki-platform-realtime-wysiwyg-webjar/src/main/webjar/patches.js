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
  'xwiki-realtime-wysiwyg-hyperJSONSelectionSerializer',
  'xwiki-realtime-wysiwyg-hyperJSONSelectionResolver',
  'xwiki-realtime-wysiwyg-domSelectionSerializer',
  'xwiki-realtime-wysiwyg-domSelectionResolver',
  'hyper-json',
  'diff-dom',
  'chainpad',
], function (
  /* jshint maxparams:false */
  Transformers, serializeHyperJSONSelection, resolveHyperJSONSelection, serializeDOMSelection, resolveDOMSelection,
  HyperJSON, DiffDOM, ChainPad
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
     * @returns {object} the editor data, including the HyperJSON serialization of the synchronized editor content and
     *   the editor text-based selection (relative to the returned HyperJSON content)
     */
    getData() {
      const html = this._getOutputHTMLWithSelectionMarkers();
      let contentWrapper = this._parseHTML(html);
      // HyperJSON doesn't support comments, so we have to convert them to custom HTML elements.
      contentWrapper = this._protectComments(contentWrapper);
      return serializeHyperJSONSelection(contentWrapper);
    }

    /**
     * Inserts special HTML comments to mark the selection boundaries, retrieves the editor HTML output and
     * finally cleans up the previously inserted comments.
     *
     * @returns {string} the editor HTML output, with special markers (HTML comments) inserted to indicate where the
     *   selection starts and ends
     */
    _getOutputHTMLWithSelectionMarkers() {
      let markers = new Set();
      try {
        // Normalize the edited content first to reduce conflicts when applying remote changes (e.g. merge adjacent text
        // nodes that result after removing inline style).
        this._editor.getContentWrapper().normalize();
        markers = serializeDOMSelection(this._editor.getSelection());
        return this._editor.getOutputHTML();
      } finally {
        markers.forEach(marker => {
          if (marker.parentNode) {
            marker.remove();
          }
        });
      }
    }

    /**
     * Update the editor content and selection.
     * 
     * @param {object} data the new content (usually the result of a remote change), serialized as HyperJSON, and the
     *   new selection
     */
    async setData(data) {
      let contentWrapper = HyperJSON.toDOM(this._parseHyperJSON(data));
      // HyperJSON doesn't support comments, so we had to convert them to custom HTML elements. Let's restore them.
      contentWrapper = this._restoreComments(contentWrapper);
      this._editor.filterInputContent(contentWrapper);
      const inputHTML = contentWrapper.innerHTML;
      await this.setHTML(inputHTML);
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
     * Parse the HyperJSON string into a HyperJSON object with selection markers.
     *
     * @param {object} data an object holding the HyperJSON string to parse and the selection boundaries
     * @returns {HyperJSON} the parsed HyperJSON object (including the selection markers)
     */
    _parseHyperJSON(data) {
      const hyperJSON = JSON.parse(data.content);
      if (data.selection?.length) {
        // Inject the selection markers in the HyperJSON content.
        resolveHyperJSONSelection(hyperJSON, data.selection);
      }
      return hyperJSON;
    }

    /**
     * Update the editor content without affecting its caret / selection.
     *
     * @param {Node} newContent the new content to set, as a DOM node
     * @param {boolean} propagate true when the new content should be propagated to coeditors
     */
    async _updateContent(newContent, propagate) {
      await this._editor.updateContent({
        patchContent: oldContent => this._patchContent(oldContent, newContent),
        restoreSelection: resolveDOMSelection
      }, propagate);
    }

    _patchContent(oldContent, newContent) {
      // We have to call nodeToObj ourselves because the compared DOM elements are from different documents.
      const patch = this._diffDOM.diff(DiffDOM.nodeToObj(oldContent), DiffDOM.nodeToObj(newContent));
      this._diffDOM.apply(oldContent, patch, {
        // New (added) nodes must be created using the current DOM document, where they will be inserted.
        document: oldContent.ownerDocument
      });
      return this._diffDOM._updatedNodes;
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
