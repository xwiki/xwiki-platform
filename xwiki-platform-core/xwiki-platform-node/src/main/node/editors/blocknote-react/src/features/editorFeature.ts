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
  BlockSpecs,
  CustomInlineContentConfig,
  ExtensionFactoryInstance,
  InlineContentSpec,
} from "@blocknote/core";
import type {
  BlockTypeSelectItem,
  DefaultReactSuggestionItem,
} from "@blocknote/react";
import type { SyntaxConfig } from "@xwiki/platform-syntaxes-config";
import type { Container } from "inversify";
import type { ReactElement, ReactNode } from "react";

/**
 * The name under which the {@link EditorFeature}s are bound in the service container. The editor has no feature of its
 * own: the generic features are bound by the `ComponentInit` of this package, which the integrations call.
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
 * An item of the slash menu.
 *
 * @since 18.10.0RC1
 * @beta
 */
type SlashMenuItem = DefaultReactSuggestionItem & {
  /**
   * The key of the BlockNote default slash menu item that this item replaces (e.g. "image"), taking its place in the
   * slash menu. BlockNote's default items are keyed by their entry in the `slash_menu` of the BlockNote dictionary.
   */
  key?: string;
};

/**
 * The block and inline content types that an editing feature adds to the schema of the editor. Their types are erased:
 * the typed API of the editor (e.g. `BlockOfType`) only knows the types of its own schema.
 *
 * @since 18.10.0RC1
 * @beta
 */
type SchemaContribution = {
  /**
   * The specifications of the block types to add, keyed by block type.
   */
  blockSpecs?: BlockSpecs;

  /**
   * The specifications of the inline content types to add, keyed by inline content type.
   */
  inlineContentSpecs?: Record<
    string,
    InlineContentSpec<CustomInlineContentConfig>
  >;
};

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
   * The block and inline content types to add to the schema of the editor. Two features can't add the same type.
   */
  schema?: SchemaContribution;

  /**
   * The BlockNote extensions to register, e.g. to handle DOM events or to add ProseMirror plugins.
   */
  blockNoteExtensions?: ExtensionFactoryInstance[];

  /**
   * The BlockNote editor options to set. Two features can't set the same option.
   */
  blockNoteOptions?: ContributedBlockNoteOptions;

  /**
   * @returns the items to add to the slash menu, which the editor filters with the query typed by the user; an item
   *   with the key of a BlockNote default item replaces it (two features can't replace the same item)
   */
  slashMenuItems?: () => SlashMenuItem[];

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
 * editor doesn't know about the features: it applies what they contribute. The features are bound in the service
 * container (see {@link editorFeatureName}), either for all the editor instances, or in the service container of an
 * editor instance, for that editor instance only. The features bound for the editor instance contribute first, then
 * the shared ones, each in the order they are bound in.
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
 * The contributions of all the editing features, merged into one, which the editor applies. Unlike a
 * {@link FeatureContribution}, where a feature leaves out what it doesn't contribute, every field is set, even when no
 * feature contributes to it (e.g. an empty list of extensions, a slash menu item getter returning no item), so that the
 * editor applies them without checking whether they are there.
 */
type MergedContributions = {
  schema: Required<SchemaContribution>;
  blockNoteExtensions: ExtensionFactoryInstance[];
  blockNoteOptions: ContributedBlockNoteOptions;
  slashMenuItems: () => SlashMenuItem[];
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
 * Keeps the first of the slash menu items replacing the same BlockNote default item, reporting the others, so that the
 * outcome doesn't depend on which feature comes last.
 */
function keepFirstReplacements(items: SlashMenuItem[]): SlashMenuItem[] {
  const keys = new Set<string>();
  return items.filter((item) => {
    if (!item.key) {
      return true;
    } else if (keys.has(item.key)) {
      console.error(
        `The slash menu item [${item.key}] is replaced by several editor features. Only the first one is taken into account.`,
      );
      return false;
    }
    keys.add(item.key);
    return true;
  });
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
  const blockSpecs: BlockSpecs = {};
  const inlineContentSpecs: Required<SchemaContribution>["inlineContentSpecs"] =
    {};
  const blockNoteOptions: Record<string, unknown> = {};
  const blockToolbars: Record<string, (block: BlockType) => ReactNode> = {};
  for (const contribution of contributions) {
    mergeRecord(blockSpecs, contribution.schema?.blockSpecs, "block type");
    mergeRecord(
      inlineContentSpecs,
      contribution.schema?.inlineContentSpecs,
      "inline content type",
    );
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
    schema: { blockSpecs, inlineContentSpecs },
    blockNoteExtensions: contributions.flatMap(
      (contribution) => contribution.blockNoteExtensions ?? [],
    ),
    blockNoteOptions,
    slashMenuItems: () =>
      keepFirstReplacements(
        contributions.flatMap(
          (contribution) => contribution.slashMenuItems?.() ?? [],
        ),
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
 * @returns the features bound in the given service container: the ones bound for the editor instance, then the ones
 *   bound for all the editor instances (in the parent containers)
 */
function getEditorFeatures(serviceContainer: Container): EditorFeature[] {
  return serviceContainer.isBound(editorFeatureName)
    ? serviceContainer.getAll<EditorFeature>(editorFeatureName, {
        chained: true,
      })
    : [];
}

/**
 * Asks the enabled editing features for their contributions to an editor instance, and merges them.
 *
 * @param context - the editor instance the features contribute to
 * @returns the merged contributions
 */
function contributeFeatures(context: FeatureContext): MergedContributions {
  const featureOptions = context.options.features as
    | Record<string, unknown>
    | undefined;
  const contributions: FeatureContribution[] = [];
  for (const feature of getEditorFeatures(context.serviceContainer)) {
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
  SchemaContribution,
  SlashMenuItem,
};
