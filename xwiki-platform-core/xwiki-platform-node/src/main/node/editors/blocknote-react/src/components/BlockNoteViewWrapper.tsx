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
import { centerOnFirstLine } from "./SideMenu/centerOnFirstLine";
import {
  createBlockNoteSchema,
  createDictionary,
  querySuggestionsMenuItems,
} from "../blocknote";
import "@blocknote/core/fonts/inter.css";
import { EditorInstanceContext } from "../contexts";
import { contributeFeatures } from "../features/editorFeature";
import { createBlockMetadataStore } from "../misc/blockMetadata";
import { getSyntaxConfig } from "../misc/syntax";
import { blocksToYXmlFragment, withCollaboration } from "@blocknote/core/yjs";
import { BlockNoteView } from "@blocknote/mantine";
import "@blocknote/mantine/style.css";
import "./BlockNoteViewWrapper.css";
import {
  FormattingToolbarController,
  SideMenuController,
  SuggestionMenuController,
  useCreateBlockNote,
} from "@blocknote/react";
import { Container } from "inversify";
import { Fragment, useEffect, useMemo, useRef } from "react";
import { useTranslation } from "react-i18next";
import type { BlockType, EditorSchema, EditorType } from "../blocknote";
import type { EditorInstance } from "../editorInstance";
import type { EditorOptions } from "../editorOptions";
import type { SharedBlockMetadata } from "../misc/blockMetadata";
import type { CollaborationOptions } from "@blocknote/core/yjs";

/**
 * Properties for the BlockNote editor component.
 *
 * @since 0.16
 * @beta
 */
type BlockNoteViewWrapperProps = {
  /**
   * The configuration of the editor instance.
   *
   * @since 18.10.0RC1
   */
  options: EditorOptions;

  /**
   * The editor's initial content
   * If realtime is enabled, this content may be replaced by the other users' own editor content
   */
  content: BlockType[];

  /**
   * Run a function when the document's content change
   * WARN: this function may be fired at a rapid rate if the user types rapidly. Debouncing may be required on your end.
   */
  onChange?: (editor: EditorType) => void;

  /**
   * Make the wrapper forward some data through references
   */
  refs?: {
    /**
     * Called with the editor instance once it is created.
     *
     * @since 18.10.0RC1
     */
    setInstance?: (instance: EditorInstance) => void;
  };
};

/**
 * Replaces the middleware BlockNote uses to center the side menu vertically on the hovered block, which offsets the
 * menu by a fixed amount per block type that only matches BlockNote's own styling.
 */
const SIDE_MENU_FLOATING_OPTIONS = {
  useFloatingOptions: {
    middleware: [centerOnFirstLine()],
  },
};

/**
 * BlockNote editor wrapper
 */

const BlockNoteViewWrapper: React.FC<BlockNoteViewWrapperProps> = ({
  options,
  content,
  onChange,
  refs: { setInstance } = {},
  // eslint-disable-next-line max-statements
}: BlockNoteViewWrapperProps) => {
  const {
    name,
    syntax: syntaxId,
    lang,
    label,
    autofocus,
    appearance: { theme, contentStyles } = {},
    collaboration: requestedCollaboration,
  } = options;
  const { t } = useTranslation();

  // The editor instance gets its own service container, where the integration can bind what is specific to it, the
  // shared services being provided by the parent container.
  const serviceContainer = useMemo(
    () => new Container({ parent: options.serviceContainer }),
    [options.serviceContainer],
  );

  // The data about the edited content that isn't part of the content is kept by the integration, when it needs it.
  const metadata = useMemo(
    () => options.metadata ?? createBlockMetadataStore(),
    [options.metadata],
  );

  // The collaboration session holds the content of all the editor instances taking part in it, each instance editing
  // the content stored under its name, so an instance without a name can't take part.
  const collaboration = name ? requestedCollaboration : undefined;
  useEffect(() => {
    if (requestedCollaboration && !name) {
      console.warn(
        "The editor instance has no name: editing without realtime collaboration.",
      );
    }
  }, [requestedCollaboration, name]);

  // The editor features that are available depend on the syntax of the edited content.
  const syntax = useMemo(
    () => getSyntaxConfig(serviceContainer, syntaxId),
    [serviceContainer, syntaxId],
  );

  // The editing features contribute to the editor before it is created, and get the editor instance once it is (from
  // the event handlers they contribute).
  const instanceRef = useRef<EditorInstance>(undefined);
  const contributions = contributeFeatures({
    options,
    serviceContainer,
    syntax,
    t,
    getInstance: () => instanceRef.current!,
  });

  const schema = createBlockNoteSchema(contributions.schema);

  // When realtime collaboration is enabled, the initial content is set through the shared document. Moreover, BlockNote
  // doesn't support empty content, as in an empty array of blocks, so instead of passing an empty array we don't pass
  // the initial content at all and let it initialize the content with whatever makes sense (e.g. an empty paragraph).
  const initialContent = collaboration || !content.length ? undefined : content;

  const getCollaborationOptions = (): CollaborationOptions | undefined => {
    if (!collaboration) {
      return undefined;
    }

    const { collaborator } = collaboration;

    return {
      provider: collaboration.provider,
      fragment: collaboration.doc.getXmlFragment(name!),
      user: {
        name: collaborator.user.username!,
        color: collaborator.color,
      },
    };
  };

  const editorOptions = {
    ...contributions.blockNoteOptions,
    initialContent,
    // Editor's schema, with custom blocks definition
    schema,
    extensions: contributions.blockNoteExtensions,
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
    // BlockNote's default styles are applied to the edited content unless the hosting page styles it.
    defaultStyles: contentStyles !== "host",
    // Put the caret at the start of the content.
    autofocus: autofocus ? ("start" as const) : false,
  };

  const collaborationOptions = getCollaborationOptions();

  // Create the BlockNote editor instance. Yjs based collaboration is opt-in: wrapping the options registers the Yjs
  // extensions the editor needs, so it is only applied when a collaboration session is available.
  const editor = useCreateBlockNote(
    collaborationOptions
      ? withCollaboration({
          ...editorOptions,
          collaboration: collaborationOptions,
        })
      : editorOptions,
  );

  // The editor instance, passed to the services acting on it. The options resolved for the editor instance (the
  // service container and the metadata store) are exposed only by the editor instance itself.
  const instance: EditorInstance = useMemo(() => {
    const {
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      serviceContainer: _serviceContainer,
      // eslint-disable-next-line @typescript-eslint/no-unused-vars
      metadata: _metadata,
      ...instanceOptions
    } = options;
    return {
      editor,
      options: instanceOptions,
      metadata,
      serviceContainer,
    };
  }, [editor, options, metadata, serviceContainer]);

  // Allow the editing features and the parent component to access the editor instance.
  useEffect(() => {
    instanceRef.current = instance;
    setInstance?.(instance);
  }, [setInstance, instance]);

  // The shared metadata entries of the edited content are kept in the shared document, next to the content, so that
  // all the session participants see the same metadata for the same ids (the private entries stay with each user).
  const sharedMetadata = collaboration?.doc.getMap(`${name}:metadata`) as
    | SharedBlockMetadata
    | undefined;

  // When realtime collaboration is enabled, the first user joining the session must set the initial content. The rest
  // of the participants will retrieve the editor content from the realtime server. We know who is the first user
  // joining the session by checking whether the name of the editor instance is in the map of the loaded contents
  // (shared across all session participants), because each editor instance taking part in the session has its own
  // initial content.
  if (collaboration && sharedMetadata) {
    const initialContentLoaded = collaboration.doc.getMap<boolean>(
      "initialContentLoaded",
    );
    if (!initialContentLoaded.get(name!)) {
      // This is the first user joining the realtime collaboration session with this editor instance.
      console.debug(
        "Setting initial content for realtime collaboration session.",
        content,
      );
      // Initialize the realtime collaboration session with the provided content and its shared metadata, at once, so
      // that the other participants never get the content without its metadata.
      collaboration.doc.transact(() => {
        initialContentLoaded.set(name!, true);
        const fragment = collaboration.doc.getXmlFragment(name!);
        blocksToYXmlFragment(editor, content, fragment);
        metadata.connect(sharedMetadata, { initialize: true });
      });
    }
  }

  // The participants that join a session already initialized use the shared metadata of the session, because they
  // edit the content of the session (with its ids), not the content they loaded. Connecting is done only once per
  // shared metadata, so this doesn't affect the participant that initialized the session.
  useEffect(
    () =>
      sharedMetadata && metadata.connect(sharedMetadata, { initialize: false }),
    [metadata, sharedMetadata],
  );

  // Renders the editor instance using a React component.
  return (
    <EditorInstanceContext.Provider value={instance}>
      <BlockNoteView
        editor={editor}
        theme={theme}
        // Override some builtin components
        formattingToolbar={false}
        linkToolbar={false}
        filePanel={false}
        slashMenu={false}
        sideMenu={false}
        onChange={(editor) => onChange?.(editor)}
      >
        {/* BlockNote's default side menu, only centered differently (the default one is disabled above). */}
        <SideMenuController floatingUIOptions={SIDE_MENU_FLOATING_OPTIONS} />

        <SuggestionMenuController
          triggerCharacter={"/"}
          getItems={async (query) =>
            querySuggestionsMenuItems({
              editor,
              query,
              syntax,
              featureItems: contributions.slashMenuItems(),
            })
          }
        />

        {/* TODO: suggestions menu for inline macros */}

        <FormattingToolbarController
          formattingToolbar={(props) => (
            <CustomFormattingToolbar
              formattingToolbarProps={props}
              additionalBlockTypes={
                contributions.formattingToolbar.blockTypeSelectItems
              }
              featureItems={contributions.formattingToolbar.items}
              blockToolbars={contributions.formattingToolbar.blockToolbars}
            />
          )}
        />

        {/* What the editing features render with the editor (e.g. their menus). */}
        {contributions.components.map((components, index) => (
          <Fragment key={index}>{components}</Fragment>
        ))}
      </BlockNoteView>
    </EditorInstanceContext.Provider>
  );
};

export type { BlockNoteViewWrapperProps, EditorSchema };

export { BlockNoteViewWrapper };
