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
define('xwiki-realtime-transformers', ['chainpad'], function(ChainPad) {
  'use strict';

  /**
   * The given text has been modified both remotely, by the specified {@code remoteOperations}, and locally, by the
   * specified {@code localOperations}. This function transforms (rebases / three-way merge) the local operations
   * against the remote operations, so that they can be applied to the given text on top of the remote operations.
   * 
   * Unlike {@link NaiveJSONTransformer}, this transformer ignores local operations that fall inside a remote delete
   * operation (i.e. local modifications to content that is deleted remotely are ignored).
   * 
   * @param {Array<Operation>} localOperations your own local operations
   * @param {Array<Operation>} remoteOperations the incoming remote operations (performed by other users)
   * @param {string} text the mutual common ancestor (the text before the local and remote operations were performed)
   * @param {boolean} [allowEmptyOperations=false] whether to allow empty operations in the result
   * @returns {Array<Operation>} the transformed (rebased) local operations
   */
  function RebaseNaiveJSONTransformer(localOperations, remoteOperations, text, allowEmptyOperations = false) {
    const rebasedLocalOperations = RebaseTextTransformer(localOperations, remoteOperations, text, allowEmptyOperations);
    const textAfterRemoteOperations = ChainPad.Operation.applyMulti(remoteOperations, text);
    const textAfterRebase = ChainPad.Operation.applyMulti(rebasedLocalOperations, textAfterRemoteOperations);
    // Verify that the rebased local operations produce valid JSON, otherwise we fail the rebase.
    JSON.parse(textAfterRebase);
    return rebasedLocalOperations;
  }

  /**
   * The given text has been modified both remotely, by the specified {@code remoteOperations}, and locally, by the
   * specified {@code localOperations}. This function transforms (rebases / three-way merge) the local operations
   * against the remote operations, so that they can be applied to the given text on top of the remote operations.
   * 
   * Unlike {@link TextTransformer}, this transformer ignores local operations that fall inside a remote delete
   * operation (i.e. local modifications to content that is deleted remotely are ignored).
   * 
   * @param {Array<Operation>} localOperations your own local operations
   * @param {Array<Operation>} remoteOperations the incoming remote operations (performed by other users)
   * @param {string} text the mutual common ancestor (the text before the local and remote operations were performed)
   * @param {boolean} [allowEmptyOperations=false] whether to allow empty operations in the result
   * @returns {Array<Operation>} the transformed (rebased) local operations
   */
  function RebaseTextTransformer(localOperations, remoteOperations, text, allowEmptyOperations = false) {
    let textAfterRemoteOperations = ChainPad.Operation.applyMulti(remoteOperations, text);

    let rebasedLocalOperations = [];
    for (let i = localOperations.length - 1; i >= 0; i--) {
        let rebasedLocalOperation = localOperations[i];
        for (let j = remoteOperations.length - 1; j >= 0; j--) {
            try {
              rebasedLocalOperation = rebaseOperationSafely(rebasedLocalOperation, remoteOperations[j],
                allowEmptyOperations);
            } catch (e) {
                console.error("The pluggable transform function threw an error, " +
                  "failing operational transformation", e);
                return [];
            }
            if (!rebasedLocalOperation) {
                break;
            }
        }
        if (rebasedLocalOperation) {
            if (ChainPad.Common.PARANOIA) {
              ChainPad.Operation.check(rebasedLocalOperation, textAfterRemoteOperations.length);
            }
            rebasedLocalOperations.unshift(rebasedLocalOperation);
        }
    }

    return rebasedLocalOperations;
  }

  /**
   * Transforms the given {@code localOperation} so that it can be applied on top of the given {@code remoteOperation}.
   *
   * @param {Operation} localOperation your own local operation
   * @param {Operation} remoteOperation the incoming remote operation (performed by another user)
   * @param {boolean} [allowEmptyOperations=false] whether to allow empty operations in the result
   * @returns {Operation} the transformed (rebased) local operation, or {@code null} if the local operation should be
   *   ignored
   */
  function rebaseOperationSafely(localOperation, remoteOperation, allowEmptyOperations = false) {
    if (ChainPad.Common.PARANOIA) {
      ChainPad.Operation.check(localOperation);
      ChainPad.Operation.check(remoteOperation);
    }
    let rebasedLocalOperation = rebaseOperation(localOperation, remoteOperation);
    if (!allowEmptyOperations && !rebasedLocalOperation?.toRemove && !rebasedLocalOperation?.toInsert?.length) {
      // Discard the rebased local operation because it doesn't do anything.
      rebasedLocalOperation = null;
    } else if (ChainPad.Common.PARANOIA) {
      ChainPad.Operation.check(rebasedLocalOperation);
    }
    return rebasedLocalOperation;
  }

  /**
   * Transforms the given {@code localOperation} so that it can be applied on top of the given {@code remoteOperation}.
   *
   * @param {Operation} localOperation your own local operation
   * @param {Operation} remoteOperation the incoming remote operation (performed by another user)
   * @returns {Operation} the transformed (rebased) local operation, or {@code null} if the local operation should be
   *   ignored
   */
  function rebaseOperation(localOperation, remoteOperation) {
    let rebasedLocalOperation;
    if (localOperation.offset > remoteOperation.offset) {
      if (localOperation.offset >= remoteOperation.offset + remoteOperation.toRemove) {
        // Simple rebase (the local operation inserts or replaces content after the remote operation).
        rebasedLocalOperation = ChainPad.Operation.create(
          localOperation.offset - remoteOperation.toRemove + remoteOperation.toInsert.length,
          localOperation.toRemove,
          localOperation.toInsert
        );
      } else if (localOperation.offset + localOperation.toRemove <= remoteOperation.offset + remoteOperation.toRemove) {
        // Discard the local operation because it modifies content that has been removed by the remote operation.
        rebasedLocalOperation = null;
      } else {
        // We have to truncate the content that the local operation removes because some part of it was already removed
        // by the remote operation.
        rebasedLocalOperation = ChainPad.Operation.create(
          remoteOperation.offset + remoteOperation.toInsert.length,
          localOperation.toRemove - (remoteOperation.offset + remoteOperation.toRemove - localOperation.offset),
          localOperation.toInsert
        );
      }
    } else if (localOperation.offset + localOperation.toRemove <= remoteOperation.offset) {
      if (localOperation.offset === remoteOperation.offset && !remoteOperation.toRemove) {
        // Both operations insert content at the same position, without removing anything. We can't know the logical
        // order of the insertions: the local content can be inserted either before or after the remote content. We
        // choose to insert the local content after the remote content because the local insertion is rebased on top of
        // the remote insertion.
        rebasedLocalOperation = ChainPad.Operation.create(
          remoteOperation.offset + remoteOperation.toInsert.length,
          0,
          localOperation.toInsert
        );
      } else {
        // The local operation is not affected by the remote operation.
        rebasedLocalOperation = localOperation;
      }
    } else if (localOperation.offset + localOperation.toRemove > remoteOperation.offset + remoteOperation.toRemove) {
      // The local operation removes the content inserted by the remote operation.
      rebasedLocalOperation = ChainPad.Operation.create(
        localOperation.offset,
        localOperation.toRemove - remoteOperation.toRemove + remoteOperation.toInsert.length,
        localOperation.toInsert
      );
    } else if (localOperation.offset === remoteOperation.offset) {
      // Discard the local operation because it modifies content that has been removed by the remote operation.
      rebasedLocalOperation = null;
    } else {
      // We have to truncate the content that the local operation removes because some part of it was already removed by
      // the remote operation.
      rebasedLocalOperation = ChainPad.Operation.create(
        localOperation.offset,
        remoteOperation.offset - localOperation.offset,
        localOperation.toInsert
      );
    }

    return rebasedLocalOperation;
  }

  return {
    RebaseNaiveJSONTransformer,
    RebaseTextTransformer
  };
});