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
import { FilePanel } from "./files/FilePanel";
import { ImageSuggestionController } from "./images/ImageSuggestionController";
import { CustomLinkToolbar } from "./links/CustomLinkToolbar";
import { LinkSuggestionController } from "./links/LinkSuggestionController";
import {
  createBlockNoteSchema,
  createDictionary,
  querySuggestionsMenuItems,
} from "../blocknote";
import "@blocknote/core/fonts/inter.css";
import { adaptMacroForBlockNote } from "../blocknote/utils";
import {
  EditorInstanceContext,
  MacrosContext,
  ServiceContainerContext,
  SourceDocumentReferenceContext,
} from "../contexts";
import { createBlockMetadataStore } from "../misc/blockMetadata";
import { getSyntaxConfig } from "../misc/syntax";
import { syntaxHighlighter } from "@blocknote/code-block";
import { blocksToYXmlFragment, withCollaboration } from "@blocknote/core/yjs";
import { BlockNoteView } from "@blocknote/mantine";
import "@blocknote/mantine/style.css";
import "./BlockNoteViewWrapper.css";
import {
  FilePanelController,
  FormattingToolbar,
  FormattingToolbarController,
  LinkToolbarController,
  SideMenuController,
  SuggestionMenuController,
  useCreateBlockNote,
} from "@blocknote/react";
import { filterMap } from "@xwiki/platform-fn-utils";
import { UniAstToReactJsxConverter } from "@xwiki/platform-uniast-react-jsx";
import { Container } from "inversify";
import { useEffect, useMemo } from "react";
import { useTranslation } from "react-i18next";
import type { BlockType, EditorSchema, EditorType } from "../blocknote";
import type { BlockNoteConcreteMacro } from "../blocknote/utils";
import type { EditorInstance } from "../editorInstance";
import type { EditorOptions } from "../editorOptions";
import type { LinkEditionHandler } from "./links/linkEdition";
import type { CollaborationOptions } from "@blocknote/core/yjs";
import type { ModelReferenceSerializerProvider } from "@xwiki/platform-model-reference-api";
import type { RemoteURLSerializerProvider } from "@xwiki/platform-model-remote-url-api";

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
   * Intercept link edition mechanism (i.e. inserting or editing a link)
   *
   * @since 18.4.0RC1
   * @beta
   */
  linkEditionHandler: LinkEditionHandler;

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
  linkEditionHandler,
  onChange,
  refs: { setInstance } = {},
  // eslint-disable-next-line max-statements
}: BlockNoteViewWrapperProps) => {
  const {
    serviceContainer: parentServiceContainer,
    name,
    sourceDocumentReference,
    syntax: syntaxId,
    lang,
    label,
    autofocus,
    appearance: { theme, contentStyles, codeBlockClassName } = {},
    collaboration: requestedCollaboration,
    upload,
    features: { links, images, macros } = {},
  } = options;
  const { t } = useTranslation();

  // The editor instance gets its own service container, where the integration can bind what is specific to it, the
  // shared services being provided by the parent container.
  const serviceContainer = useMemo(
    () => new Container({ parent: parentServiceContainer }),
    [parentServiceContainer],
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

  // The components below take the source document reference serialized.
  const serializedSourceDocumentReference = useMemo(
    () =>
      sourceDocumentReference &&
      serviceContainer
        .get<ModelReferenceSerializerProvider>(
          "ModelReferenceSerializerProvider",
        )
        .get()
        ?.serialize(sourceDocumentReference),
    [serviceContainer, sourceDocumentReference],
  );

  // The editor features that are available depend on the syntax of the edited content.
  const syntax = useMemo(
    () => getSyntaxConfig(serviceContainer, syntaxId),
    [serviceContainer, syntaxId],
  );

  const builtMacros: BlockNoteConcreteMacro[] = [];

  if (macros) {
    const uniAstToReactJsxConverter = new UniAstToReactJsxConverter(
      serviceContainer
        .get<RemoteURLSerializerProvider>("RemoteURLSerializerProvider")
        .get()!,
    );

    for (const macro of macros.list ?? []) {
      builtMacros.push(
        adaptMacroForBlockNote(macro, macros.ctx, uniAstToReactJsxConverter),
      );
    }
  }

  const schema = createBlockNoteSchema(builtMacros, { codeBlockClassName });

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
    initialContent,
    // Editor's schema, with custom blocks definition
    schema,
    // Highlight the code blocks based on their language.
    extensions: [syntaxHighlighter],
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
    uploadFile: upload?.uploadFile,
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

  // Allow the parent component to access the editor instance.
  useEffect(() => {
    setInstance?.(instance);
  }, [setInstance, instance]);

  // When realtime collaboration is enabled, the first user joining the session must set the initial content. The rest
  // of the participants will retrieve the editor content from the realtime server. We know who is the first user
  // joining the session by checking whether the name of the editor instance is in the map of the loaded contents
  // (shared across all session participants), because each editor instance taking part in the session has its own
  // initial content.
  if (collaboration) {
    const initialContentLoaded = collaboration.doc.getMap<boolean>(
      "initialContentLoaded",
    );
    if (!initialContentLoaded.get(name!)) {
      // This is the first user joining the realtime collaboration session with this editor instance.
      initialContentLoaded.set(name!, true);
      console.debug(
        "Setting initial content for realtime collaboration session.",
        content,
      );
      // Initialize the realtime collaboration session with the provided content.
      const fragment = collaboration.doc.getXmlFragment(name!);
      blocksToYXmlFragment(editor, content, fragment);
    }
  }

  // Renders the editor instance using a React component.
  return (
    <ServiceContainerContext.Provider value={serviceContainer}>
      <EditorInstanceContext.Provider value={instance}>
        <SourceDocumentReferenceContext.Provider
          value={serializedSourceDocumentReference}
        >
          <MacrosContext.Provider value={macros ? macros.ctx : null}>
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
              <SideMenuController
                floatingUIOptions={SIDE_MENU_FLOATING_OPTIONS}
              />

              <SuggestionMenuController
                triggerCharacter={"/"}
                getItems={async (query) =>
                  querySuggestionsMenuItems({
                    editor,
                    query,
                    macros: builtMacros,
                    syntax,
                    lang,
                    t,
                    openMacroInsertionEditor: macros
                      ? macros.ctx.openInsertionEditor
                      : undefined,
                  })
                }
              />

              {syntax.features.blocks.images.basicImages && (
                <ImageSuggestionController
                  maxSuggestions={5}
                  imageEditionHooks={images?.hooks}
                  imageInsertionOverrideFn={images?.insertion}
                />
              )}

              {syntax.features.inlineContents.links.basicLinks && (
                <LinkSuggestionController
                  linkEditionHandler={linkEditionHandler}
                  linkEditionHooks={links?.hooks}
                  maxSuggestions={8}
                  pageSuggestionsShare={5}
                />
              )}

              {/* TODO: suggestions menu for inline macros */}

              <FormattingToolbarController
                formattingToolbar={(props) => (
                  <CustomFormattingToolbar
                    formattingToolbarProps={props}
                    imageEditionOverrideFn={images?.edition}
                    imageEditionHooks={images?.hooks}
                    linkEditionHandler={linkEditionHandler}
                    linkEditionHooks={links?.hooks}
                    additionalBlockTypes={filterMap(
                      builtMacros,
                      (built) => built.dropdownTransformItem,
                    )}
                    macros={macros ?? false}
                  />
                )}
              />

              <LinkToolbarController
                linkToolbar={(props) => (
                  <FormattingToolbar>
                    <CustomLinkToolbar
                      linkToolbarProps={props}
                      linkEditionHandler={linkEditionHandler}
                      linkEditionHooks={links?.hooks}
                    />
                  </FormattingToolbar>
                )}
              />

              <FilePanelController
                filePanel={({ blockId }) => (
                  <FilePanel blockId={blockId} editor={editor} />
                )}
              />
            </BlockNoteView>
          </MacrosContext.Provider>
        </SourceDocumentReferenceContext.Provider>
      </EditorInstanceContext.Provider>
    </ServiceContainerContext.Provider>
  );
};

export type { BlockNoteViewWrapperProps, EditorSchema };

export { BlockNoteViewWrapper };
