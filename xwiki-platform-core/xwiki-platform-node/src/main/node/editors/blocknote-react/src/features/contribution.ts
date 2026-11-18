/**
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
import type {
  EditorBlockSchema,
  EditorInlineContentSchema,
  EditorStyleSchema,
} from "../blocknote";
import type { EditorInstance } from "../editorInstance";
import type {
  BlockNoteEditorOptions,
  ExtensionFactoryInstance,
} from "@blocknote/core";

/**
 * What an editing feature gets to contribute to an editor instance. The editor is created with the contributions, so
 * it doesn't exist yet when the features contribute.
 */
type FeatureContext = Pick<EditorInstance, "options" | "serviceContainer"> & {
  /**
   * @returns the editor instance, once its editor is created: call it from what the feature contributes (e.g. an
   *   event handler), not while contributing
   */
  getInstance(): EditorInstance;
};

/**
 * The BlockNote editor options that the editing features can set.
 */
type ContributedBlockNoteOptions = Partial<
  Pick<
    BlockNoteEditorOptions<
      EditorBlockSchema,
      EditorInlineContentSchema,
      EditorStyleSchema
    >,
    "uploadFile"
  >
>;

/**
 * What an editing feature adds to an editor instance.
 */
type FeatureContribution = {
  /**
   * The BlockNote extensions to register, e.g. to handle DOM events or to add ProseMirror plugins.
   */
  blockNoteExtensions?: ExtensionFactoryInstance[];

  /**
   * The BlockNote editor options to set. Two features can't set the same option.
   */
  blockNoteOptions?: ContributedBlockNoteOptions;
};

/**
 * Merges the contributions of the editing features. A BlockNote option set by several features is reported, and only
 * the first feature setting it is taken into account, so that the outcome doesn't depend on which feature comes last.
 *
 * @param contributions - the contributions of the editing features
 * @returns the merged contributions
 */
function mergeContributions(
  contributions: FeatureContribution[],
): Required<FeatureContribution> {
  const blockNoteOptions: Record<string, unknown> = {};
  for (const contribution of contributions) {
    for (const [key, value] of Object.entries(
      contribution.blockNoteOptions ?? {},
    )) {
      if (key in blockNoteOptions) {
        console.error(
          `The BlockNote option [${key}] is set by several editor features. Only the first one is taken into account.`,
        );
      } else {
        blockNoteOptions[key] = value;
      }
    }
  }
  return {
    blockNoteExtensions: contributions.flatMap(
      (contribution) => contribution.blockNoteExtensions ?? [],
    ),
    blockNoteOptions,
  };
}

export { mergeContributions };
export type { FeatureContext, FeatureContribution };
