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
  BlockType,
  EditorBlockSchema,
  EditorInlineContentSchema,
  EditorStyleSchema,
} from "../blocknote";
import type { EditorInstance } from "../editorInstance";
import type {
  BlockNoteEditorOptions,
  ExtensionFactoryInstance,
} from "@blocknote/core";
import type {
  BlockTypeSelectItem,
  DefaultReactSuggestionItem,
} from "@blocknote/react";
import type { SyntaxConfig } from "@xwiki/platform-syntaxes-config";
import type { Container } from "inversify";
import type { ReactElement, ReactNode } from "react";

/**
 * The name under which the {@link EditorFeature}s are bound in the service container, in addition to the features
 * built into the editor.
 *
 * @since 18.10.0RC1
 * @beta
 */
const editorFeatureName = "EditorFeature";

/**
 * What an editing feature gets to contribute to an editor instance. The editor is created with the contributions, so
 * it doesn't exist yet when the features contribute.
 *
 * @since 18.10.0RC1
 * @beta
 */
type FeatureContext = Pick<EditorInstance, "options" | "serviceContainer"> & {
  /**
   * The configuration of the syntax of the edited content: a feature contributes only what the syntax can save.
   */
  syntax: SyntaxConfig;

  /**
   * Translates the given key, in the language of the editor UI.
   */
  t: (key: string) => string;

  /**
   * @returns the editor instance, once its editor is created: call it from what the feature contributes (e.g. an
   *   event handler), not while contributing
   */
  getInstance(): EditorInstance;
};

/**
 * The BlockNote editor options that the editing features can set.
 *
 * @since 18.10.0RC1
 * @beta
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
 * What an editing feature adds to the formatting toolbar.
 *
 * @since 18.10.0RC1
 * @beta
 */
type FormattingToolbarContribution = {
  /**
   * The toolbar items (e.g. buttons), shown after the text formatting items, in the order of the features. Each item
   * needs a key, and hides itself when it doesn't apply to the selection.
   */
  items?: ReactElement[];

  /**
   * The items added to the block type selector of the toolbar.
   */
  blockTypeSelectItems?: BlockTypeSelectItem[];

  /**
   * The toolbars replacing the formatting toolbar when a block of a given type is selected, keyed by block type.
   */
  blockToolbars?: Record<string, (block: BlockType) => ReactNode>;
};

/**
 * What an editing feature adds to an editor instance.
 *
 * @since 18.10.0RC1
 * @beta
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

  /**
   * @returns the items to add to the slash menu, which the editor filters with the query typed by the user
   */
  slashMenuItems?: () => DefaultReactSuggestionItem[];

  /**
   * What to add to the formatting toolbar.
   */
  formattingToolbar?: FormattingToolbarContribution;

  /**
   * The components to render with the editor, e.g. the controllers of the menus or the toolbars of the feature.
   */
  components?: ReactNode;
};

/**
 * Something the user can do while editing (e.g. create a link to a wiki page, upload a file, insert a macro call). The
 * editor doesn't know about the features: it applies what they contribute. Besides the features built into the
 * editor, any number of features can be bound in the service container (see {@link editorFeatureName}).
 *
 * @since 18.10.0RC1
 * @beta
 */
interface EditorFeature {
  /**
   * Identifies the feature, and its options in `EditorOptions.features`.
   */
  readonly id: string;

  /**
   * Called before the editor instance is created. Not called when the feature is disabled, i.e. when its options are
   * `false`.
   *
   * @param context - the editor instance the feature contributes to
   * @returns what the feature contributes, nothing when it doesn't apply (e.g. when the syntax can't save it)
   */
  contribute(context: FeatureContext): FeatureContribution | undefined;
}

/**
 * The contributions of all the editing features.
 */
type MergedContributions = {
  blockNoteExtensions: ExtensionFactoryInstance[];
  blockNoteOptions: ContributedBlockNoteOptions;
  slashMenuItems: () => DefaultReactSuggestionItem[];
  formattingToolbar: Required<FormattingToolbarContribution>;
  components: ReactNode[];
};

/**
 * Copies the entries of the given record into the given target, reporting the keys that are already set, which keep the
 * value of the first feature setting them, so that the outcome doesn't depend on which feature comes last.
 */
function mergeRecord(
  target: Record<string, unknown>,
  record: Record<string, unknown> | undefined,
  what: string,
): void {
  for (const [key, value] of Object.entries(record ?? {})) {
    if (key in target) {
      console.error(
        `The ${what} [${key}] is set by several editor features. Only the first one is taken into account.`,
      );
    } else {
      target[key] = value;
    }
  }
}

/**
 * Merges the contributions of the editing features.
 *
 * @param contributions - the contributions of the editing features
 * @returns the merged contributions
 */
function mergeContributions(
  contributions: FeatureContribution[],
): MergedContributions {
  const blockNoteOptions: Record<string, unknown> = {};
  const blockToolbars: Record<string, (block: BlockType) => ReactNode> = {};
  for (const contribution of contributions) {
    mergeRecord(
      blockNoteOptions,
      contribution.blockNoteOptions,
      "BlockNote option",
    );
    mergeRecord(
      blockToolbars,
      contribution.formattingToolbar?.blockToolbars,
      "toolbar of the block type",
    );
  }
  return {
    blockNoteExtensions: contributions.flatMap(
      (contribution) => contribution.blockNoteExtensions ?? [],
    ),
    blockNoteOptions,
    slashMenuItems: () =>
      contributions.flatMap(
        (contribution) => contribution.slashMenuItems?.() ?? [],
      ),
    formattingToolbar: {
      items: contributions.flatMap(
        (contribution) => contribution.formattingToolbar?.items ?? [],
      ),
      blockTypeSelectItems: contributions.flatMap(
        (contribution) =>
          contribution.formattingToolbar?.blockTypeSelectItems ?? [],
      ),
      blockToolbars,
    },
    components: contributions.map((contribution) => contribution.components),
  };
}

/**
 * @param serviceContainer - the service container of the editor instance
 * @param builtInFeatures - the features built into the editor
 * @returns the features built into the editor, followed by the ones bound in the service container
 */
function getEditorFeatures(
  serviceContainer: Container,
  builtInFeatures: EditorFeature[],
): EditorFeature[] {
  return [
    ...builtInFeatures,
    // Chained, so that the features bound for all the editor instances are found, as well as the ones bound for this
    // editor instance.
    ...(serviceContainer.isBound(editorFeatureName)
      ? serviceContainer.getAll<EditorFeature>(editorFeatureName, {
          chained: true,
        })
      : []),
  ];
}

/**
 * Asks the enabled editing features for their contributions to an editor instance, and merges them.
 *
 * @param context - the editor instance the features contribute to
 * @param builtInFeatures - the features built into the editor
 * @returns the merged contributions
 */
function contributeFeatures(
  context: FeatureContext,
  builtInFeatures: EditorFeature[],
): MergedContributions {
  const featureOptions = context.options.features as
    | Record<string, unknown>
    | undefined;
  const contributions: FeatureContribution[] = [];
  for (const feature of getEditorFeatures(
    context.serviceContainer,
    builtInFeatures,
  )) {
    if (featureOptions?.[feature.id] === false) {
      continue;
    }
    try {
      contributions.push(feature.contribute(context) ?? {});
    } catch (error) {
      // A failing feature doesn't prevent the editor and the other features from working.
      console.error(`The editor feature [${feature.id}] failed.`, error);
    }
  }
  return mergeContributions(contributions);
}

export { contributeFeatures, editorFeatureName };
export type {
  ContributedBlockNoteOptions,
  EditorFeature,
  FeatureContext,
  FeatureContribution,
  FormattingToolbarContribution,
  MergedContributions,
};
