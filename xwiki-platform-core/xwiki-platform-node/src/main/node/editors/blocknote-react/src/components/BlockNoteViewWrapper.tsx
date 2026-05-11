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

import { CustomFormattingToolbar } from "./CustomFormattingToolbar";
import { FilePanel } from "./files/FilePanel";
import { CustomLinkToolbar } from "./links/CustomLinkToolbar";
import {
  createBlockNoteSchema,
  createDictionary,
  querySuggestionsMenuItems,
} from "../blocknote";
import "@blocknote/core/fonts/inter.css";
import { adaptMacroForBlockNote } from "../blocknote/utils";
import { blocksToYXmlFragment } from "@blocknote/core/yjs";
import { BlockNoteView } from "@blocknote/mantine";
import "@blocknote/mantine/style.css";
import {
  FilePanelController,
  FormattingToolbar,
  FormattingToolbarController,
  LinkToolbarController,
  SuggestionMenuController,
  useCreateBlockNote,
} from "@blocknote/react";
import { MacrosAstToReactJsxConverter } from "@xwiki/platform-macros-ast-react-jsx";
import { useEffect } from "react";
import type {
  BlockType,
  EditorBlockSchema,
  EditorInlineContentSchema,
  EditorLanguage,
  EditorSchema,
  EditorStyleSchema,
  EditorType,
} from "../blocknote";
import type {
  BlockNoteConcreteMacro,
  ContextForMacros,
} from "../blocknote/utils";
import type { LinkEditionContext } from "../misc/linkSuggest";
import type { ImageEditionOverrideFn } from "./images/CustomImageToolbar";
import type { BlockNoteEditorOptions } from "@blocknote/core";
import type { Collaboration } from "@xwiki/platform-collaboration-api";
import type { MacroWithUnknownParamsType } from "@xwiki/platform-macros-api";

/**
 * Default options for the BlockNote editor
 *
 * @since 0.16
 * @beta
 */
type DefaultBlockNoteEditorOptions = BlockNoteEditorOptions<
  EditorBlockSchema,
  EditorInlineContentSchema,
  EditorStyleSchema
>;

/**
 * Properties for the BlockNote editor component.
 *
 * @since 0.16
 * @beta
 */
type BlockNoteViewWrapperProps = {
  /**
   * Options to forward to the BlockNote editor
   */
  blockNoteOptions?: Partial<
    Omit<DefaultBlockNoteEditorOptions, "schema" | "collaboration">
  >;

  /**
   * The name of the editor instance, usually the name of the form field it is attached to. When realtime collaboration
   * is enabled, this is used to identify the Yjs document fragment that the editor is bound to (because a document can
   * have multiple fields that are being edited in realtime time).
   *
   * @since 18.3.0RC1
   */
  name?: string;

  /**
   * The display theme to use
   */
  theme?: "light" | "dark";

  /**
   * The editor's language
   */
  lang: EditorLanguage;

  /**
   * The editor's label. Used for accessibility.
   * @since 18.3.0RC1
   */
  label: string;

  /**
   * The editor's initial content
   * If realtime is enabled, this content may be replaced by the other users' own editor content
   */
  content: BlockType[];

  /**
   * Macros to show in the editor
   *
   * @since 18.0.0RC1
   * @beta
   */
  macros:
    | {
        /**
         * List of buildable macros
         *
         * @since 18.0.0RC1
         * @beta
         */
        list: MacroWithUnknownParamsType[];

        /**
         * Context for macros
         *
         * @since 18.0.0RC1
         * @beta
         */
        ctx: ContextForMacros;
      }
    | false;

  /**
   * The collaboration session.
   */
  collaboration?: Collaboration;

  /**
   * Run a function when the document's content change
   * WARN: this function may be fired at a rapid rate if the user types rapidly. Debouncing may be required on your end.
   */
  onChange?: (editor: EditorType) => void;

  /**
   * Link edition utilities
   */
  linkEditionCtx: LinkEditionContext;

  /**
   * Overrides for default behavior
   *
   * @since 0.26
   */
  overrides?: {
    /**
     * Intercept image edition mechanism (i.e. clicking on the edition icon in images' toolbar)
     */
    imageEdition?: ImageEditionOverrideFn;
  };

  /**
   * Make the wrapper forward some data through references
   */
  refs?: {
    setEditor?: (editor: EditorType) => void;
  };
};

/**
 * BlockNote editor wrapper
 */
// eslint-disable-next-line max-statements
const BlockNoteViewWrapper: React.FC<BlockNoteViewWrapperProps> = ({
  blockNoteOptions,
  name = "content",
  theme,
  content,
  macros,
  collaboration,
  onChange,
  lang,
  linkEditionCtx,
  overrides,
  label,
  refs: { setEditor } = {},
}: BlockNoteViewWrapperProps) => {
  const builtMacros: BlockNoteConcreteMacro[] = [];

  if (macros) {
    const macroAstToReactJsxConverter = new MacrosAstToReactJsxConverter(
      linkEditionCtx.remoteURLParser,
      linkEditionCtx.remoteURLSerializer,
    );

    for (const macro of macros.list) {
      builtMacros.push(
        adaptMacroForBlockNote(macro, macros.ctx, macroAstToReactJsxConverter),
      );
    }
  }

  const schema = createBlockNoteSchema(builtMacros);

  // When realtime collaboration is enabled, the initial content is set through the shared document. Moreover, BlockNote
  // doesn't support empty content, as in an empty array of blocks, so instead of passing an empty array we don't pass
  // the initial content at all and let it initialize the content with whatever makes sense (e.g. an empty paragraph).
  const initialContent = collaboration || !content.length ? undefined : content;

  const getCollaborationOptions = ():
    | BlockNoteEditorOptions<never, never, never>["collaboration"]
    | undefined => {
    if (!collaboration) {
      return undefined;
    }

    const collaborator = collaboration.collaborator;
    return {
      provider: collaboration.provider,
      fragment: collaboration.doc.getXmlFragment(name),
      user: {
        name: collaborator.user.username!,
        color: collaborator.color,
      },
    };
  };

  // Create the BlockNote editor instance.
  const editor = useCreateBlockNote({
    ...blockNoteOptions,
    initialContent,
    collaboration: getCollaborationOptions(),
    // Editor's schema, with custom blocks definition
    schema,
    // Use the provided language for the dictionary
    dictionary: createDictionary(lang),
    // The default drop cursor only shows up above and below blocks - we replace
    // it with the multi-column one that also shows up on the sides of blocks.
    tables: {
      headers: true,
    },
    domAttributes: {
      editor: {
        "aria-label": label,
      },
    },
  });

  // Allow the parent component to access the editor instance.
  useEffect(() => {
    setEditor?.(editor);
  }, [setEditor, editor]);

  // When realtime collaboration is enabled, the first user joining the session must set the initial content. The rest
  // of the participants will retrieve the editor content from the realtime server. We know who is the first user
  // joining the session by checking for the absence of an initialContentLoaded key in the document's configuration map
  // (shared across all session participants).
  if (collaboration) {
    const initialContentLoaded = collaboration.doc
      .getMap("configuration")
      .get("initialContentLoaded");
    if (!initialContentLoaded) {
      // This is the first user joining the realtime collaboration session.
      collaboration.doc
        .getMap("configuration")
        .set("initialContentLoaded", true);
      console.debug(
        "Setting initial content for realtime collaboration session.",
        content,
      );
      // Initialize the realtime collaboration session with the provided content.
      const fragment = collaboration.doc.getXmlFragment(name);
      blocksToYXmlFragment(editor, content, fragment);
    }
  }

  // Renders the editor instance using a React component.
  return (
    <BlockNoteView
      editor={editor}
      theme={theme}
      // Override some builtin components
      formattingToolbar={false}
      linkToolbar={false}
      filePanel={false}
      slashMenu={false}
      onChange={(editor) => onChange?.(editor)}
    >
      <SuggestionMenuController
        triggerCharacter={"/"}
        getItems={async (query) =>
          querySuggestionsMenuItems(editor, query, builtMacros)
        }
      />

      {/* TODO: suggestions menu for inline macros */}

      <FormattingToolbarController
        formattingToolbar={(props) => (
          <CustomFormattingToolbar
            formattingToolbarProps={props}
            linkEditionCtx={linkEditionCtx}
            imageEditionOverrideFn={overrides?.imageEdition}
          />
        )}
      />

      <LinkToolbarController
        linkToolbar={(props) => (
          <FormattingToolbar>
            <CustomLinkToolbar
              linkToolbarProps={props}
              linkEditionCtx={linkEditionCtx}
            />
          </FormattingToolbar>
        )}
      />

      <FilePanelController
        filePanel={({ blockId }) => (
          <FilePanel
            blockId={blockId}
            editor={editor}
            linkEditionCtx={linkEditionCtx}
          />
        )}
      />
    </BlockNoteView>
  );
};

export type {
  BlockNoteViewWrapperProps,
  DefaultBlockNoteEditorOptions,
  EditorSchema,
};

export { BlockNoteViewWrapper };
