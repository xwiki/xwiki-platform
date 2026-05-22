import { MACRO_NAME_PREFIX, ContextForMacros } from './blocknote/utils';
import { BlockNoteViewWrapperProps } from './components/BlockNoteViewWrapper';
import { LinkEditionContext } from './misc/linkEditionCtx';
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
declare function mountBlockNote(containerEl: HTMLElement, props: BlockNoteViewWrapperProps): {
    unmount: () => void;
};
export { MACRO_NAME_PREFIX, mountBlockNote };
export type { BlockNoteViewWrapperProps, ContextForMacros, LinkEditionContext };
export type { BlockNoteConcreteMacro, BlockOfType, BlockType, EditorBlockSchema, EditorInlineContentSchema, EditorLanguage, EditorLink, EditorSchema, EditorStyleSchema, EditorStyledText, EditorType, InlineContentType, } from './blocknote';
export { createBlockNoteSchema, createDictionary, querySuggestionsMenuItems, } from './blocknote';
export type { createCustomBlockSpec, createCustomInlineContentSpec, } from './blocknote/utils';
export { buildMacroRawContent, extractMacroRawContent, } from './blocknote/utils';
export type { DefaultBlockNoteEditorOptions } from './components/BlockNoteViewWrapper';
export type { ImageEditionOverrideFn, ImageUpdateResult, } from './components/images/CustomImageToolbar';
export type { LinkEditionHandler, LinkEditionHandlerProps, } from './components/links/linkEdition';
