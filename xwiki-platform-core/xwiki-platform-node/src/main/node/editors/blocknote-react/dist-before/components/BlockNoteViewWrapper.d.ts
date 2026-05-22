import { BlockType, EditorBlockSchema, EditorInlineContentSchema, EditorLanguage, EditorSchema, EditorStyleSchema, EditorType } from '../blocknote';
import { ContextForMacros } from '../blocknote/utils';
import { LinkEditionContext } from '../misc/linkEditionCtx';
import { ImageEditionOverrideFn } from './images/CustomImageToolbar';
import { LinkEditionHandler } from './links/linkEdition';
import { BlockNoteEditorOptions } from '@blocknote/core';
import { Collaboration } from '@xwiki/platform-collaboration-api';
import { MacroWithUnknownParamsType } from '@xwiki/platform-macros-api';
/**
 * Default options for the BlockNote editor
 *
 * @since 0.16
 * @beta
 */
type DefaultBlockNoteEditorOptions = BlockNoteEditorOptions<EditorBlockSchema, EditorInlineContentSchema, EditorStyleSchema>;
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
    blockNoteOptions?: Partial<Omit<DefaultBlockNoteEditorOptions, "schema" | "collaboration">>;
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
    macros: {
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
    } | false;
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
     * Intercept link edition mechanism (i.e. inserting or editing a link)
     *
     * @since 18.4.0RC1
     * @beta
     */
    linkEditionHandler: LinkEditionHandler;
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
declare const BlockNoteViewWrapper: React.FC<BlockNoteViewWrapperProps>;
export type { BlockNoteViewWrapperProps, DefaultBlockNoteEditorOptions, EditorSchema, };
export { BlockNoteViewWrapper };
