/**
 * See the LICENSE file distributed with this work for additional
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
import { ImageFilePanel } from "./images/ImageFilePanel";
import { CustomLinkToolbar } from "./links/CustomLinkToolbar";
import {
  BlockType,
  EditorBlockSchema,
  EditorInlineContentSchema,
  EditorLanguage,
  EditorSchema,
  EditorStyleSchema,
  EditorType,
  createBlockNoteSchema,
  createDictionary,
  querySuggestionsMenuItems,
} from "../blocknote";
import { BuildableMacro, ContextForMacros } from "../blocknote/utils";
import { LinkEditionContext } from "../misc/linkSuggest";
import { BlockNoteEditorOptions } from "@blocknote/core";
import "@blocknote/core/fonts/inter.css";
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
import { useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import type { CollaborationInitializer } from "@xwiki/cristal-collaboration-api";

type DefaultEditorOptionsType = BlockNoteEditorOptions<
  EditorBlockSchema,
  EditorInlineContentSchema,
  EditorStyleSchema
>;

/**
 * Properties for the {@link BlockNoteEditor} component
 */
type BlockNoteViewWrapperProps = {
  /**
   * Options to forward to the BlockNote editor
   */
  blockNoteOptions?: Partial<
    Omit<DefaultEditorOptionsType, "schema" | "collaboration">
  >;

  /**
   * The display theme to use
   */
  theme?: "light" | "dark";

  /**
   * The editor's language
   */
  lang: EditorLanguage;

  /**
   * The editor's initial content
   * If realtime is enabled, this content may be replaced by the other users' own editor content
   */
  content: BlockType[];

  /**
   * Macros to show in the editor
   *
   * @since 0.21
   */
  macros: null | {
    /**
     * List of buildable macros
     */
    buildable: BuildableMacro[];

    /**
     * Open the macros parameters editor
     */
    openMacroParamsEditor: ContextForMacros["openParamsEditor"];
  };

  /**
   * Realtime options
   */
  realtime?: {
    collaborationProvider: () => CollaborationInitializer;
    user: { name: string; color: string };
  };

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
  theme,
  content,
  macros,
  realtime,
  onChange,
  lang,
  linkEditionCtx,
  refs: { setEditor } = {},
}: BlockNoteViewWrapperProps) => {
  const { t } = useTranslation();
  const collaborationProvider = realtime?.collaborationProvider;

  const builtMacros = macros
    ? macros.buildable.map((builder) =>
        builder({ openParamsEditor: macros.openMacroParamsEditor }),
      )
    : [];

  const schema = createBlockNoteSchema(builtMacros);

  const initializer: CollaborationInitializer | undefined =
    collaborationProvider ? collaborationProvider() : undefined;

  // Prevent changes in the editor until the provider has synced with other clients
  const [ready, setReady] = useState(!initializer);

  // Creates a new editor instance.
  const editor = useCreateBlockNote({
    ...blockNoteOptions,
    collaboration: initializer?.provider
      ? {
          provider: initializer.provider,
          fragment: initializer.doc.getXmlFragment("document-store"),
          user: realtime!.user,
        }
      : undefined,
    // Editor's schema, with custom blocks definition
    schema,
    // Use the provided language for the dictionary
    dictionary: createDictionary(lang),
    // The default drop cursor only shows up above and below blocks - we replace
    // it with the multi-column one that also shows up on the sides of blocks.
    tables: {
      headers: true,
    },
  });

  useEffect(() => {
    setEditor?.(editor);
  }, [setEditor, editor]);

  // When realtime is activated, the first user to join the session sets the content for everybody.
  // The rest of the participants will just retrieve the editor content from the realtime server.
  // We know who is the first user joining the session by checking for the absence of an initialContentLoaded key in the
  // document's configuration map (shared across all session participants).
  useEffect(() => {
    // Replace the editor's current content with the provided blocks
    const replaceContent = (blocks: BlockType[]) => {
      // NOTE: replacing the content immediately seems to lead to a data race with the provider and/or BlockNote,
      //       and we end up with an empty editor
      //       So, to avoid this, we run the content replacement function separately with the smallest possible non-zero delay
      // This is something we'll need to investigate, but this does the trick for now.
      setTimeout(() => {
        // TODO: with time, see if this fix actually works fine
        //
        // Some BlockNote crash seems to happen when replacing the whole document, but only sometimes.
        // So here we ensure the document is never empty by introducing some empty inline content, which will be put
        // inside a paragraph.
        if (editor.document.length === 0) {
          editor.insertInlineContent("");
        }

        editor.replaceBlocks(editor.document, blocks);
      }, 1);
    };

    if (initializer?.provider) {
      console.debug("Trying to connect to realtime server...");

      // eslint-disable-next-line promise/catch-or-return
      initializer.initialized.then(() => {
        console.debug("Connected to realtime server and synced!");

        const initialContentLoaded = initializer.doc
          .getMap("configuration")
          .get("initialContentLoaded");

        // eslint-disable-next-line promise/always-return
        if (!initialContentLoaded) {
          initializer.doc
            .getMap("configuration")
            .set("initialContentLoaded", true);

          console.debug("Setting initial content for realtime first player", {
            content,
          });

          replaceContent(content);
        }

        setReady(true);
      });

      initializer.provider.on("destroy", () => {
        initializer.provider.destroy();
      });
    } else {
      // If we don't have a provider, we can simply load the content directly
      console.debug(
        "No realtime provider, setting document's content directly",
      );

      replaceContent(content);
    }
  }, [initializer]);

  // Disconnect from the realtime provider when the component is unmounted
  // Otherwise, our user profile may be left over and still be displayed to other users
  useEffect(() => {
    return () => {
      console.debug(
        "BlockNoteView is being unmounted, disconnecting from the realtime provider...",
      );

      initializer?.provider?.disconnect();
    };
  }, []);

  if (!ready) {
    return (
      <h3>
        <em>{t("blocknote.realtime.pendingSync")}</em>
      </h3>
    );
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
        filePanel={(props) => {
          const block = props.block as unknown as BlockType;

          return block.type === "image" ? (
            <ImageFilePanel
              linkEditionCtx={linkEditionCtx}
              currentBlock={block}
            />
          ) : (
            <>Unknown block type: {block.type}</>
          );
        }}
      />
    </BlockNoteView>
  );
};

export type { BlockNoteViewWrapperProps, EditorSchema };
export { BlockNoteViewWrapper };
