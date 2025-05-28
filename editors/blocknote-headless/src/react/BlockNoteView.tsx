/*
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

import { DefaultFormattingToolbar } from "./DefaultFormattingToolbar";
import {
  BlockType,
  EditorBlockSchema,
  EditorInlineContentSchema,
  EditorSchema,
  EditorStyleSchema,
  EditorType,
  createBlockNoteSchema,
  createDictionary,
  querySuggestionsMenuItems,
} from "../blocknote";
import { BlockNoteEditorOptions } from "@blocknote/core";
import "@blocknote/core/fonts/inter.css";
import { BlockNoteView } from "@blocknote/mantine";
import "@blocknote/mantine/style.css";
import {
  FilePanelController,
  FilePanelProps,
  FormattingToolbar,
  FormattingToolbarController,
  LinkToolbarController,
  LinkToolbarProps,
  SuggestionMenuController,
  useCreateBlockNote,
} from "@blocknote/react";
import { multiColumnDropCursor } from "@blocknote/xl-multi-column";
import {
  HocuspocusProvider,
  // BUG: ESLint incorrectly reports this as an error even though it's not (TODO: investigate)
  // eslint-disable-next-line import/named
  HocuspocusProviderConfiguration,
} from "@hocuspocus/provider";
import { ReactivueChild } from "@xwiki/cristal-reactivue";
import { useEffect, useState } from "react";
import { ShallowRef } from "vue";

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
   * The editor's initial content
   * If realtime is enabled, this content may be replaced by the other users' own editor content
   */
  content: BlockType[];

  /**
   * Realtime options
   */
  realtime?: {
    hocusPocus: HocuspocusProviderConfiguration;
    user: { name: string; color: string };
  };

  /**
   * Run a function when the document's content change
   * WARN: this function may be fired at a rapid rate if the user types rapidly. Debouncing may be required on your end.
   */
  onChange?: (editor: EditorType) => void;

  /**
   * Message to display while syncing changes with other users
   */
  pendingSyncMessage: string;

  /**
   * Prepend the default formatting toolbar for the provided block types
   * For all these blocks, the custom-provided `formattingToolbar` will be *appended* to the default toolbar instead of replacing it
   */
  prefixDefaultFormattingToolbarFor: Array<BlockType["type"]>;

  /**
   * Replace BlockNote's default formatting toolbar with a custom one
   * Can be used together with `prefixDefaultFormattingToolbarFor` to make this one be _appended_ to the default one
   */
  formattingToolbar: ReactivueChild<{
    editor: EditorType;
    currentBlock: BlockType;
  }>;

  /**
   * Replace BlockNote's link toolbar with a custom one
   */
  linkToolbar: ReactivueChild<{
    editor: EditorType;
    linkToolbarProps: LinkToolbarProps;
  }>;

  /**
   * Replace BlockNote's file/image panel with a custom one
   */
  filePanel: ReactivueChild<{
    editor: EditorType;
    filePanelProps: FilePanelProps<
      EditorInlineContentSchema,
      EditorStyleSchema
    >;
  }>;

  /**
   * Make the wrapper forward some data through references
   */
  refs?: {
    editorRef?: ShallowRef<EditorType | null>;
    providerRef?: ShallowRef<HocuspocusProvider | null>;
  };
};

/**
 * BlockNote editor wrapper
 */
// eslint-disable-next-line max-statements
function BlockNoteViewWrapper({
  blockNoteOptions,
  theme,
  content,
  realtime,
  onChange,
  pendingSyncMessage,
  formattingToolbar: CustomFormattingToolbar,
  prefixDefaultFormattingToolbarFor,
  linkToolbar: CustomLinkToolbar,
  filePanel: CustomFilePanel,
  refs: { editorRef, providerRef } = {},
}: BlockNoteViewWrapperProps) {
  const schema = createBlockNoteSchema();

  const provider = realtime?.hocusPocus
    ? new HocuspocusProvider(realtime.hocusPocus)
    : undefined;

  if (providerRef && provider) {
    providerRef.value = provider;
  }

  // Prevent changes in the editor until the provider has synced with other clients
  const [ready, setReady] = useState(!provider);

  // Creates a new editor instance.
  const editor = useCreateBlockNote({
    ...blockNoteOptions,
    collaboration: provider
      ? {
          provider,
          fragment: provider.document.getXmlFragment("document-store"),
          user: realtime!.user,
        }
      : undefined,
    // Editor's schema, with custom blocks definition
    schema,
    dropCursor: multiColumnDropCursor,
    // Merges the default dictionary with the multi-column dictionary.
    dictionary: createDictionary(),
    // The default drop cursor only shows up above and below blocks - we replace
    // it with the multi-column one that also shows up on the sides of blocks.
    tables: {
      headers: true,
    },
  });

  if (editorRef) {
    editorRef.value = editor;
  }

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

    if (provider) {
      console.debug("Trying to connect to realtime server...");

      provider.on("synced", () => {
        console.debug("Connected to realtime server and synced!");

        const initialContentLoaded = provider.document
          .getMap("configuration")
          .get("initialContentLoaded");

        if (!initialContentLoaded) {
          provider.document
            .getMap("configuration")
            .set("initialContentLoaded", true);

          console.debug("Setting initial content for realtime first player", {
            content,
          });

          replaceContent(content);
        }

        setReady(true);
      });

      provider.on("destroy", () => {
        provider.destroy();
      });
    } else {
      // If we don't have a provider, we can simply load the content directly
      console.debug(
        "No realtime provider, setting document's content directly",
      );

      replaceContent(content);
    }
  }, [provider]);

  // Disconnect from the realtime provider when the component is unmounted
  // Otherwise, our user profile may be left over and still be displayed to other users
  useEffect(() => {
    return () => {
      console.debug(
        "BlockNoteView is being unmounted, disconnecting from the realtime provider...",
      );

      provider?.disconnect();
    };
  }, []);

  if (!ready) {
    return (
      <h3>
        <em>{pendingSyncMessage}</em>
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
        getItems={async (query) => querySuggestionsMenuItems(editor, query)}
      />

      <FormattingToolbarController
        formattingToolbar={() => {
          const currentBlock = editor.getTextCursorPosition().block;

          return (
            <FormattingToolbar>
              {
                // Prepend the default formatting toolbar for blocks that require it
                prefixDefaultFormattingToolbarFor.includes(
                  currentBlock.type,
                ) && (
                  <DefaultFormattingToolbar
                    disableButtons={{ createLink: true }}
                  />
                )
              }

              <CustomFormattingToolbar
                editor={editor}
                currentBlock={currentBlock}
              />
            </FormattingToolbar>
          );
        }}
      />

      <LinkToolbarController
        linkToolbar={(props) => (
          <CustomLinkToolbar editor={editor} linkToolbarProps={props} />
        )}
      />

      <FilePanelController
        filePanel={(props) => (
          <CustomFilePanel editor={editor} filePanelProps={props} />
        )}
      />
    </BlockNoteView>
  );
}

export type { BlockNoteViewWrapperProps, EditorSchema };
export { BlockNoteViewWrapper };
