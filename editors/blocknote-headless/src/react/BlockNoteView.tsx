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
import { HocuspocusProvider } from "@hocuspocus/provider";
import { ReactivueChild } from "@xwiki/cristal-reactivue";
import React, { useState } from "react";
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
  blockNoteOptions?: Partial<Omit<DefaultEditorOptionsType, "schema">>;
  theme?: "light" | "dark";
  content: BlockType[];
  editorRef?: ShallowRef<EditorType | null>;

  /**
   * Prepend the default formatting toolbar for the provided block types
   * For all these blocks, the custom-provided `formattingToolbar` will be *appended* to the default toolbar instead of replacing it
   */
  prefixDefaultFormattingToolbarFor: Array<BlockType["type"]>;

  formattingToolbar: ReactivueChild<{
    editor: EditorType;
    currentBlock: BlockType;
  }>;

  linkToolbar: ReactivueChild<{
    editor: EditorType;
    linkToolbarProps: LinkToolbarProps;
  }>;

  filePanel: ReactivueChild<{
    editor: EditorType;
    filePanelProps: FilePanelProps<
      EditorInlineContentSchema,
      EditorStyleSchema
    >;
  }>;
};

/**
 * Load the provided content, parse it to Markdown and load it to the provided editor.
 * @param editor - the editor in which the parsed content will be loaded
 * @param blocks - the content to to load into the editor
 */
async function parseAndLoadContent(
  // TODO: MaybeUninit<EditorType>
  editor: EditorType,
  blocks: BlockType[],
) {
  editor.replaceBlocks(editor.document, blocks);
}

/**
 * BlockNote editor wrapper
 */
// eslint-disable-next-line max-statements
function BlockNoteViewWrapper({
  blockNoteOptions,
  theme,
  formattingToolbar: CustomFormattingToolbar,
  prefixDefaultFormattingToolbarFor,
  linkToolbar: CustomLinkToolbar,
  filePanel: CustomFilePanel,
  content,
  editorRef,
}: BlockNoteViewWrapperProps) {
  const schema = createBlockNoteSchema();

  const provider = blockNoteOptions?.collaboration?.provider as
    | HocuspocusProvider
    | undefined;

  // Prevent changes in the editor until the provider has synced with other clients
  // TODO: "ready" is not defined here (see below)
  const [, setReady] = useState(!provider);

  // Creates a new editor instance.
  const editor = useCreateBlockNote({
    ...blockNoteOptions,
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
  if (provider) {
    provider.on("synced", () => {
      console.debug("HocusPocus synced");

      if (
        !provider.document.getMap("configuration").get("initialContentLoaded")
      ) {
        provider.document
          .getMap("configuration")
          .set("initialContentLoaded", true);

        parseAndLoadContent(editor, content);
      }

      setReady(true);
    });

    provider.on("destroy", () => {
      provider.destroy();
    });
  } else {
    parseAndLoadContent(editor, content);
  }

  // TODO: this condition ensures the editor does not show until all changes have been synced with clients
  // Currently, there is a problem with realtime not syncing changes in a reliable fashion, so we disable it for now
  //
  // if (!ready) {
  //   return <h1>Syncing changes with other realtime users...</h1>;
  // }

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
