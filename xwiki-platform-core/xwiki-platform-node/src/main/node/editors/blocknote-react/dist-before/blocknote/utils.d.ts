import { BlockNoteEditor, BlockConfig, CustomInlineContentConfig, DefaultInlineContentSchema, DefaultStyleSchema, InlineContent, InlineContentSchema, PartialBlockFromConfig, PartialInlineContent, PropSchema, StyleSchema } from '@blocknote/core';
import { ReactCustomBlockImplementation, ReactInlineContentImplementation } from '@blocknote/react';
import { MacroWithUnknownParamsType, UnknownMacroParamsType } from '@xwiki/platform-macros-api';
import { MacrosAstToReactJsxConverter } from '@xwiki/platform-macros-ast-react-jsx';
import { ReactNode } from 'react';
/**
 * Create a custom block to use in the BlockNote editor
 *
 * @param block - The block specification
 *
 * @returns A block definition
 *
 * @since 18.0.0RC1
 * @internal
 */
declare function createCustomBlockSpec<const Name extends string, const Props extends PropSchema, const InlineType extends "inline" | "none">({ config, implementation, slashMenu, customToolbar, }: {
    config: BlockConfig<Name, Props, InlineType>;
    implementation: ReactCustomBlockImplementation<BlockConfig<Name, Props, InlineType>>;
    slashMenu: false | {
        title: string;
        aliases?: string[];
        group: string;
        icon: ReactNode;
        default: () => PartialBlockFromConfig<BlockConfig<Name, Props, InlineType>, InlineContentSchema, StyleSchema>;
    };
    customToolbar: (() => ReactNode) | null;
}): {
    block: (options?: undefined) => import('@blocknote/core').BlockSpec<Name, Props, InlineType>;
    slashMenuEntry: false | ((editor: BlockNoteEditor<any, any, any>) => {
        title: string;
        aliases: string[] | undefined;
        group: string;
        icon: ReactNode;
        onItemClick: () => void;
    });
    customToolbar: (() => ReactNode) | null;
};
/**
 * Create a custom inilne content to use in the BlockNote editor
 *
 * @param inlineContent - The inline content specification
 *
 * @returns An inline content definition
 *
 * @since 18.0.0RC1
 * @beta
 */
/**
 * @since 18.0.0RC1
 * @internal
 */
declare function createCustomInlineContentSpec<const I extends CustomInlineContentConfig, const S extends StyleSchema>({ config, implementation, slashMenu, customToolbar, }: {
    config: I;
    implementation: ReactInlineContentImplementation<I, S>;
    slashMenu: false | {
        title: string;
        aliases?: string[];
        group: string;
        icon: ReactNode;
        default: () => PartialInlineContent<Record<I["type"], I>, S>;
    };
    customToolbar: (() => ReactNode) | null;
}): {
    inlineContent: import('@blocknote/core').InlineContentSpec<I>;
    slashMenuEntry: false | ((editor: BlockNoteEditor<any>) => {
        title: string;
        aliases: string[] | undefined;
        group: string;
        icon: ReactNode;
        onItemClick: () => void;
    });
    customToolbar: (() => ReactNode) | null;
};
/**
 * Name prefix for macro blocks and inline contents in BlockNote
 *
 * @since 18.0.0RC1
 * @beta
 */
declare const MACRO_NAME_PREFIX = "Macro_";
/**
 * Description of a macro adapted by `adaptMacroForBlockNote`
 *
 * @since 18.0.0RC1
 * @internal
 */
type BlockNoteConcreteMacro = {
    /** Type-erased macro */
    macro: MacroWithUnknownParamsType;
    /** Rendering part */
    bnRendering: {
        type: "block";
        block: ReturnType<typeof createCustomBlockSpec<string, PropSchema, "inline">> | ReturnType<typeof createCustomBlockSpec<string, PropSchema, "none">>;
    } | {
        type: "inline";
        inlineContent: ReturnType<typeof createCustomInlineContentSpec>;
    };
};
/**
 * Internal context required for macros execution
 *
 * @since 18.0.0RC1
 * @beta
 */
type ContextForMacros = {
    /**
     * Request the opening of an UI to edit the macro's parameters (e.g. a modal)
     *
     * @param macro - Description of the macro being edited
     * @param params - Current parameters of the macro
     * @param update - Calling this function will replace the existing macro's parameters with the provided ones
     */
    openParamsEditor(macro: MacroWithUnknownParamsType, params: UnknownMacroParamsType, update: (newProps: UnknownMacroParamsType) => void): void;
};
/**
 * Adapt a macro to be used inside BlockNote.
 *
 * @param macro - The macro to adapt
 * @param ctx - The context used to handle the macro inside BlockNote
 * @param jsxConverter - A converter that transforms macro ASTs to React JSX
 *
 * @returns - The BlockNote-compatible macro
 *
 * @since 18.0.0RC1
 * @beta
 */
declare function adaptMacroForBlockNote(macro: MacroWithUnknownParamsType, ctx: ContextForMacros, jsxConverter: MacrosAstToReactJsxConverter): BlockNoteConcreteMacro;
/**
 * Extract a macro's raw content from its BlockNote AST sub-tree
 *
 * Identity function with `buildMacroRawContent`
 *
 * @param content - The BlockNote AST to extract the macro's raw content from
 *
 * @returns The extracted raw content
 *
 * @since 18.0.0RC1
 * @beta
 */
declare function extractMacroRawContent(content: InlineContent<DefaultInlineContentSchema, DefaultStyleSchema>[]): string;
/**
 * Wrap a macro's raw content inside a BlockNote AST
 *
 * Identify function with `extractMacroRawContent`
 *
 * @param content - The raw content to wrap the macro's raw content in
 *
 * @returns A BlockNote wrapper AST
 *
 * @since 18.0.0RC1
 * @beta
 */
declare function buildMacroRawContent(content: string): InlineContent<DefaultInlineContentSchema, DefaultStyleSchema>;
export { MACRO_NAME_PREFIX, adaptMacroForBlockNote, buildMacroRawContent, createCustomBlockSpec, createCustomInlineContentSpec, extractMacroRawContent, };
export type { BlockNoteConcreteMacro, ContextForMacros };
