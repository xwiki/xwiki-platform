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
import { App } from "./App";
import { createRoot } from "react-dom/client";
import type { BlockNoteViewWrapperProps } from "./components/BlockNoteViewWrapper";

/**
 * Mount a BlockNote editor inside a DOM container
 *
 * @param containerEl - The container to put BlockNote in (must be empty and be a block type component, e.g. `<div>`)
 * @param props - Properties to setup the editor with
 *
 * @returns - An unmount function to properly dispose of the editor
 *
 * @since 18.0.0RC1
 * @beta
 */
function mountBlockNote(
  containerEl: HTMLElement,
  props: BlockNoteViewWrapperProps,
): { unmount: () => void } {
  const root = createRoot(containerEl);

  root.render(<App {...props} />);

  let unmounted = false;

  return {
    unmount() {
      if (unmounted) {
        throw new Error("BlockNote is already unmounted!");
      }

      unmounted = true;
      root.unmount();
    },
  };
}

export { mountBlockNote };
export type { BlockNoteViewWrapperProps };

export type {
  BlockOfType,
  BlockType,
  EditorBlockSchema,
  EditorInlineContentSchema,
  EditorLanguage,
  EditorLink,
  EditorPartialLink,
  EditorSchema,
  EditorStyleSchema,
  EditorStyledText,
  EditorType,
  InlineContentType,
} from "./blocknote";

export type { EditorInstance } from "./editorInstance";

export { createBlockMetadataStore } from "./misc/blockMetadata";

export {
  getMacroOutput,
  setMacroOutput,
} from "./features/macros/macroMetadata";

export type {
  BlockMetadata,
  BlockMetadataKey,
  BlockMetadataStore,
  SharedBlockMetadata,
} from "./misc/blockMetadata";

export type {
  CodeBlocksFeatureOptions,
  EditorAppearance,
  EditorFeatures,
  EditorOptions,
  MacroFeatureOptions,
} from "./editorOptions";

export {
  canUploadFiles,
  fileUploadListenerName,
  fileUploaderName,
  uploadFile,
} from "./files/fileUploader";

export type {
  FileUploadListener,
  FileUploader,
  UploadedFile,
} from "./files/fileUploader";

export { ComponentInit } from "./componentInit";

export { codeBlocksFeature } from "./features/codeBlocks/codeBlocks";

export { fileTransferFeature } from "./features/fileTransfer/fileTransfer";

export { imagesFeature } from "./features/images/images";

export { linksFeature } from "./features/links/links";

export { macrosFeature } from "./features/macros/macros";

export { mentionsFeature } from "./features/mentions/mentions";

export { textStylesFeature } from "./features/textStyles/textStyles";
export { xwikiRenderingFeature } from "./features/xwikiRendering/xwikiRendering";

export { editorFeatureName } from "./features/editorFeature";

export type {
  ContributedBlockNoteOptions,
  EditorFeature,
  FeatureContext,
  FeatureContribution,
  FormattingToolbarContribution,
  SchemaContribution,
  SlashMenuItem,
} from "./features/editorFeature";

export {
  imageEditorName,
  imageMetadataHandlerName,
} from "./services/imageEditor";

export type {
  ImageEditionData,
  ImageEditor,
  ImageMetadataHandler,
} from "./services/imageEditor";

export {
  MACRO_NAME_PREFIX,
  buildMacroRawContent,
  extractMacroRawContent,
} from "./features/macros/utils";

export { mentionFactoryName, userSuggesterName } from "./services/mentions";

export type {
  MentionFactory,
  MentionableUser,
  UserSuggester,
} from "./services/mentions";

export { macroEditorName } from "./services/macroEditor";

export type {
  InlineMacroInvocation,
  MacroBlockInvocation,
  MacroCallParams,
  MacroEditor,
  MacroInsertionEditorPrefillData,
} from "./services/macroEditor";

export {
  createInlineMacro,
  insertMacroInvocation,
} from "./misc/macroInvocation";

export type { MacroCall } from "./misc/macroInvocation";

export { linkEditorName, linkMetadataHandlerName } from "./services/linkEditor";

export type {
  LinkEditionData,
  LinkEditor,
  LinkMetadataHandler,
} from "./services/linkEditor";
