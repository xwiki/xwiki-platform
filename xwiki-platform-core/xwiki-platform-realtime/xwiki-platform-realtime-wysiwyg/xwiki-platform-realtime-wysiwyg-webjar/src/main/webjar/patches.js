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
      this._filters = new Filters();
    }

    _createDiffDOM() {
      const diffDOM = new DiffDOM.DiffDOM({
        preDiffApply: (change) => {
          // Reject any change made directly to the root node (i.e. the editor content wrapper) because it may break the
          // editor (e.g. it may remove attributes or listeners required by the editor). Only its descendants are
          // allowed to be modified. Note that the root node shouldn't have any changes normally because we restore it
          // in _restoreRootNode() before computing the changes, but we've seen cases where an (aria) attribute is added
          // just after.
          if (change.node === this._editor.getContentWrapper()) {
            return true;
          }

          if (['replaceElement', 'removeElement', 'removeTextElement'].includes(change.diff.action)) {
            diffDOM._updatedNodes.add(change.node.parentNode);
          } else if (['addAttribute', 'modifyAttribute', 'removeAttribute', 'modifyTextElement', 'modifyValue',
              'modifyComment', 'modifyChecked', 'modifySelected', 'relocateGroup'].includes(change.diff.action)) {
            diffDOM._updatedNodes.add(change.node);
          }
        },

        postDiffApply: (change) => {
          if (change.diff.action === 'addElement' && !change.newNode) {
            // Unfortunately DiffDOM doesn't set the new node when an element is added so we have to find it ourselves.
            change.newNode = this._getFromRoute(change.diff.route);
          }

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

      // The root node depends on the type of editor. For the classical iframe-based editor the root node is the BODY.
      // For the in-place editor the root node may be a DIV. We have to normalize the root node in order to be able to
      // synchronize the content between different types of editors. Basically the root node itself is not synchronized,
      // only its descendants are.
      hyperJSON[0] = 'xwiki-content';
      // Ignore all root attributes because they normally store user preferences that shouldn't be shared. Note that
      // when we apply a remote change we make sure to not overwrite (remove all) the root node attributes.
      hyperJSON[1] = {};

      return JSONSortify(hyperJSON);
    }

    /**
     * @param {boolean} raw whether to filter the editor content or not before serializing it to HyperJSON;
     *   {@code true} to serialize the raw (unfiltered) content, {@code false} to serialize the filtered (normalized)
     *   content
     * @returns {string} the serialization of the editor content as HyperJSON
     */
    getHyperJSON(raw) {
      if (raw) {
        // The raw content is used to determine the local changes that need to be rebased on top of the received remote
        // changes. Includes dynamic content, such as macro output.
        return this._stringifyNode(this._editor.getContentWrapper(), true);
      } else {
        // The filtered content is synchronized with the other editors. Includes only the content that is normally sent
        // to the server to be converted to wiki syntax.
        return this._convertHTMLToHyperJSON(this._editor.getOutputHTML());
      }
    }

    /**
     * Update the editor content without affecting its caret / selection.
     * 
     * @param {string} remoteFilteredHyperJSON the new normalized (filtered) content (usually the result of a remote
     *   change), serialized as HyperJSON
     *
     * @param {boolean} propagate true when the new content should be propagated to coeditors
     */
    async setHyperJSON(remoteFilteredHyperJSON, propagate) {
      let remoteHyperJSON;
      try {
        remoteHyperJSON = this._revertHyperJSONFilters(remoteFilteredHyperJSON);
      } catch (e) {
        console.warn('Failed to revert the HyperJSON filters.', {
          filteredHyperJSON: remoteFilteredHyperJSON,
          error: e
        });
        remoteHyperJSON = remoteFilteredHyperJSON;
      }

      let newContent;
      try {
        remoteHyperJSON = JSON.parse(remoteHyperJSON);
        newContent = HyperJSON.toDOM(this._restoreRootNode(remoteHyperJSON));
      } catch (e) {
        console.error('Failed to parse the HyperJSON string.', {
          filteredHyperJSON: remoteFilteredHyperJSON,
          rawHyperJSON: remoteHyperJSON,
          error: e
        });
        return;
      }

      // Content update is asynchronous because it requires server-side rendering sometimes (e.g. when macro parameters
      // have changed).
      await this._updateContent(newContent, propagate);
    }

    /**
     * Update the editor content without affecting its caret / selection.
     * 
     * @param {string} html the new HTML content; we expect this to be the result of rendering the wiki syntax to HTML
     * @param {boolean} propagate true when the new content should be propagated to coeditors
     */
    async setHTML(html, propagate) {
      // We convert to HyperJSON and set the HyperJSON so that we can use the same filters
      // as when receiving content from coeditors.
      const hjson = this._convertHTMLToHyperJSON(html);
      await this.setHyperJSON(hjson, propagate);
    }

    /**
     * Converts the given HTML (obtained by rendering the wiki syntax) to HyperJSON.
     *
     * @param {string} html the HTML content to convert to HyperJSON; we expect this to be the result of rendering the
     *   wiki syntax to HTML
     * @returns {string} the HyperJSON that corresponds to the given HTML
     */
    _convertHTMLToHyperJSON(html) {
      const contentWrapper = this._editor.parseInputHTML(html);
      return this._stringifyNode(contentWrapper, false);
    }

    /**
     * Update the editor content without affecting its caret / selection.
     * 
     * @param {Node} newContent the new content to set, as a DOM node
     * @param {boolean} propagate true when the new content should be propagated to coeditors
     */
    async _updateContent(newContent, propagate) {
      // Remember where the selection is, to be able to restore it in case the content update affects it.
      this._editor.saveSelection();
      let selection = this._editor.getSelection();
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

      await this._editor.contentUpdated(this._diffDOM._updatedNodes, propagate);

      // Restore the selection if the editor had a selection (i.e. if the selection was inside the editing area) before
      // the content update and it was affected by the content update. Note that the selection restore focuses the
      // editor. The editing area might have been reloaded so it's best to retrieve the selection again.
      selection = this._editor.getSelection();
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
      // Use the raw (unfiltered) content to determine the local changes only if the dynamic content (e.g. macro output)
      // is not affected by the remote change. Otherwise, ignore the local filtered content (e.g. macro output) when
      // deterining the local changes that need to be rebased on top of the received remote changes. The reason for this
      // is because we can't merge the macro output when the macro parameters are modified. We need to re-render the
      // macros (i.e. re-create the dynamic content).
      const localHyperJSON = this.getHyperJSON(!this._isDynamicContentModified(localFilteredHyperJSON,
        remoteFilteredHyperJSON));
      return Patches.merge(localFilteredHyperJSON, remoteFilteredHyperJSON, localHyperJSON);
    }

    /**
     * Compares the local content with the remote content in order to determine if the dynamic content needs to be
     * reloaded because some of its parameters have been modified.
     *
     * @param {string} localHyperJSON the local content
     * @param {string} remoteHyperJSON the remote content
     * @returns {boolean} {@code true} if the dynamic content needs to be reloaded because some of its parameters have
     *   been modified, {@code false} otherwise
     */
    _isDynamicContentModified(localHyperJSON, remoteHyperJSON) {
      const localMacroIterator = new MacroHyperJSONIterator(localHyperJSON);
      const remoteMacroIterator = new MacroHyperJSONIterator(remoteHyperJSON);
      let localMacro, remoteMacro;
      do {
        localMacro = localMacroIterator.next();
        remoteMacro = remoteMacroIterator.next();
        if (localMacro.done !== remoteMacro.done || localMacro.value !== remoteMacro.value) {
          return true;
        }
      } while (!localMacro.done && !remoteMacro.done);
      return false;
    }

    /**
     * Make sure the root node of the given HyperJSON matches the editor content wrapper. We have to do this, otherwise
     * we can't apply the given HyperJSON (the root nodes must match in order to be able to compute the changes and
     * apply the patch). Moreover, the root node itself is not synchronized, only its descendants are, so we shouldn't
     * lose or overwrite any of its attributes.
     *
     * @param {Array} hyperJSON the HyperJSON for which to restore the root node
     * @returns {Array} the modified HyperJSON, with the root node restored
     */
    _restoreRootNode(hyperJSON) {
      // Create a shallow clone of the content wrapper node and compute the corresponding HyperJSON.
      const rootClone = this._editor.getContentWrapper().cloneNode(false);
      const rootHyperJSON = HyperJSON.fromDOM(rootClone);
      // Preserve the descendants from the given HyperJSON.
      rootHyperJSON[2] = hyperJSON[2];
      return rootHyperJSON;
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

  class MacroHyperJSONIterator {
    /**
     * @param {string} hyperJSON the HyperJSON to iterate over
     */
    constructor(hyperJSON) {
      this._path = [{
        parent: ['', {}, [JSON.parse(hyperJSON)]],
        index: 0
      }];
    }

    next() {
      while (this._path.length) {
        const currentItem = this._path[this._path.length - 1];
        if (currentItem.index < currentItem.parent[2].length) {
          const nextParent = currentItem.parent[2][currentItem.index++];
          if (!Array.isArray(nextParent)) {
            // Skip text nodes.
            continue;
          }
          // Step inside the found element.
          this._path.push({
            parent: nextParent,
            index: 0
          });
          if (nextParent[1]['data-macro']) {
            return {
              done: false,
              value: nextParent[1]['data-macro']
            };
          }
        } else {
          this._path.pop();
        }
      }
      return {done: true};
    }
  }

  return Patches;
});
