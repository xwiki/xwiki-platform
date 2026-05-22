import { AttachmentsService } from '@xwiki/platform-attachments-api';
import { Block } from '@blocknote/core';
import { BlockConfig } from '@blocknote/core';
import { BlockNoteEditor } from '@blocknote/core';
import { BlockNoteEditorOptions } from '@blocknote/core';
import { BlockNoteSchema } from '@blocknote/core';
import { BlockSchemaFromSpecs } from '@blocknote/core';
import { BlockSpec } from '@blocknote/core';
import { Collaboration } from '@xwiki/platform-collaboration-api';
import { CustomInlineContentConfig } from '@blocknote/core';
import { DefaultInlineContentSchema } from '@blocknote/core';
import { DefaultReactSuggestionItem } from '@blocknote/react';
import { DefaultStyleSchema } from '@blocknote/core';
import { DocumentService } from '@xwiki/platform-document-api';
import { InlineContent } from '@blocknote/core';
import { InlineContentSchema } from '@blocknote/core';
import { InlineContentSchemaFromSpecs } from '@blocknote/core';
import { InlineContentSpec } from '@blocknote/core';
import { Link } from '@blocknote/core';
import { LinkSuggestService } from '@xwiki/platform-link-suggest-api';
import * as locales from '@blocknote/core/locales';
import { LooseBlockSpec } from '@blocknote/core';
import { MacroWithUnknownParamsType } from '@xwiki/platform-macros-api';
import { ModelReferenceHandler } from '@xwiki/platform-model-reference-api';
import { ModelReferenceParser } from '@xwiki/platform-model-reference-api';
import { ModelReferenceSerializer } from '@xwiki/platform-model-reference-api';
import { PartialBlockFromConfig } from '@blocknote/core';
import { PartialInlineContent } from '@blocknote/core';
import { PropSchema } from '@blocknote/core';
import { ReactCustomBlockImplementation } from '@blocknote/react';
import { ReactInlineContentImplementation } from '@blocknote/react';
import { ReactNode } from 'react';
import { RemoteURLParser } from '@xwiki/platform-model-remote-url-api';
import { RemoteURLSerializer } from '@xwiki/platform-model-remote-url-api';
import { StyledText } from '@blocknote/core';
import { StyleImplementation } from '@blocknote/core';
import { StyleSchema } from '@blocknote/core';
import { StyleSchemaFromSpecs } from '@blocknote/core';
import { StyleSpec } from '@blocknote/core';
import { UnknownMacroParamsType } from '@xwiki/platform-macros-api';

/**
 * Description of a macro adapted by `adaptMacroForBlockNote`
 *
 * @since 18.0.0RC1
 * @internal
 */
export declare type BlockNoteConcreteMacro = {
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
 * Properties for the BlockNote editor component.
 *
 * @since 0.16
 * @beta
 */
export declare type BlockNoteViewWrapperProps = {
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
 * Typesafe BlockNote type of the a given kind
 *
 * @since 18.0.0RC1
 * @beta
 */
export declare type BlockOfType<B extends BlockType["type"]> = Extract<BlockType, {
    type: B;
}>;

/**
 * Typesafe BlockNote type
 *
 * @since 18.0.0RC1
 * @beta
 */
export declare type BlockType = Block<EditorBlockSchema, EditorInlineContentSchema, EditorStyleSchema>;

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
export declare function buildMacroRawContent(content: string): InlineContent<DefaultInlineContentSchema, DefaultStyleSchema>;

/**
 * Internal context required for macros execution
 *
 * @since 18.0.0RC1
 * @beta
 */
export declare type ContextForMacros = {
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
 * Create the BlockNote editor's schema
 *
 * Contains all the blocks usable inside the editor
 *
 * @returns The created schema
 * @since 18.0.0RC1
 * @beta
 */
export declare function createBlockNoteSchema(macros: BlockNoteConcreteMacro[]): BlockNoteSchema<BlockSchemaFromSpecs<    {
bulletListItem: BlockSpec<"bulletListItem", {
readonly backgroundColor: {
default: "default";
};
readonly textColor: {
default: "default";
};
readonly textAlignment: {
default: "left";
values: readonly ["left", "center", "right", "justify"];
};
}, "inline">;
checkListItem: BlockSpec<"checkListItem", {
readonly checked: {
readonly default: false;
readonly type: "boolean";
};
readonly backgroundColor: {
default: "default";
};
readonly textColor: {
default: "default";
};
readonly textAlignment: {
default: "left";
values: readonly ["left", "center", "right", "justify"];
};
}, "inline">;
codeBlock: BlockSpec<"codeBlock", {
readonly language: {
readonly default: string;
};
}, "inline">;
divider: BlockSpec<"divider", {}, "none">;
heading: BlockSpec<"heading", {
readonly isToggleable?: {
readonly default: false;
readonly optional: true;
} | undefined;
readonly level: {
readonly default: 1 | 4 | 2 | 3 | 5 | 6;
readonly values: readonly number[];
};
readonly backgroundColor: {
default: "default";
};
readonly textColor: {
default: "default";
};
readonly textAlignment: {
default: "left";
values: readonly ["left", "center", "right", "justify"];
};
}, "inline">;
image: BlockSpec<"image", {
readonly textAlignment: {
default: "left";
values: readonly ["left", "center", "right", "justify"];
};
readonly backgroundColor: {
default: "default";
};
readonly name: {
readonly default: "";
};
readonly url: {
readonly default: "";
};
readonly caption: {
readonly default: "";
};
readonly showPreview: {
readonly default: true;
};
readonly previewWidth: {
readonly default: undefined;
readonly type: "number";
};
}, "none">;
numberedListItem: BlockSpec<"numberedListItem", {
readonly start: {
readonly default: undefined;
readonly type: "number";
};
readonly backgroundColor: {
default: "default";
};
readonly textColor: {
default: "default";
};
readonly textAlignment: {
default: "left";
values: readonly ["left", "center", "right", "justify"];
};
}, "inline">;
paragraph: BlockSpec<"paragraph", {
backgroundColor: {
default: "default";
};
textColor: {
default: "default";
};
textAlignment: {
default: "left";
values: readonly ["left", "center", "right", "justify"];
};
}, "inline">;
quote: BlockSpec<"quote", {
readonly backgroundColor: {
default: "default";
};
readonly textColor: {
default: "default";
};
}, "inline">;
table: LooseBlockSpec<"table", {
textColor: {
default: "default";
};
}, "table">;
}>, InlineContentSchemaFromSpecs<    {
text: {
config: "text";
implementation: any;
};
link: {
config: "link";
implementation: any;
};
}>, StyleSchemaFromSpecs<    {
bold: {
config: {
type: string;
propSchema: "boolean";
};
implementation: StyleImplementation<    {
type: string;
propSchema: "boolean";
}>;
};
italic: {
config: {
type: string;
propSchema: "boolean";
};
implementation: StyleImplementation<    {
type: string;
propSchema: "boolean";
}>;
};
underline: {
config: {
type: string;
propSchema: "boolean";
};
implementation: StyleImplementation<    {
type: string;
propSchema: "boolean";
}>;
};
strike: {
config: {
type: string;
propSchema: "boolean";
};
implementation: StyleImplementation<    {
type: string;
propSchema: "boolean";
}>;
};
code: {
config: {
type: string;
propSchema: "boolean";
};
implementation: StyleImplementation<    {
type: string;
propSchema: "boolean";
}>;
};
textColor: StyleSpec<    {
readonly type: "textColor";
readonly propSchema: "string";
}>;
backgroundColor: StyleSpec<    {
readonly type: "backgroundColor";
readonly propSchema: "string";
}>;
}>>;

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
export declare function createCustomBlockSpec<const Name extends string, const Props extends PropSchema, const InlineType extends "inline" | "none">({ config, implementation, slashMenu, customToolbar, }: {
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
    block: (options?: undefined) => BlockSpec<Name, Props, InlineType>;
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
export declare function createCustomInlineContentSpec<const I extends CustomInlineContentConfig, const S extends StyleSchema>({ config, implementation, slashMenu, customToolbar, }: {
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
    inlineContent: InlineContentSpec<I>;
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
 * Create a translated dictionary for the BlockNote editor
 *
 * @param lang - The dictionary's language
 *
 * @returns The dictionary in the requested language
 * @since 18.0.0RC1
 * @beta
 */
export declare function createDictionary(lang: EditorLanguage): {
    slash_menu: {
        heading: {
            title: string;
            subtext: string;
            aliases: string[];
            group: string;
        };
        heading_2: {
            title: string;
            subtext: string;
            aliases: string[];
            group: string;
        };
        heading_3: {
            title: string;
            subtext: string;
            aliases: string[];
            group: string;
        };
        heading_4: {
            title: string;
            subtext: string;
            aliases: string[];
            group: string;
        };
        heading_5: {
            title: string;
            subtext: string;
            aliases: string[];
            group: string;
        };
        heading_6: {
            title: string;
            subtext: string;
            aliases: string[];
            group: string;
        };
        toggle_heading: {
            title: string;
            subtext: string;
            aliases: string[];
            group: string;
        };
        toggle_heading_2: {
            title: string;
            subtext: string;
            aliases: string[];
            group: string;
        };
        toggle_heading_3: {
            title: string;
            subtext: string;
            aliases: string[];
            group: string;
        };
        quote: {
            title: string;
            subtext: string;
            aliases: string[];
            group: string;
        };
        toggle_list: {
            title: string;
            subtext: string;
            aliases: string[];
            group: string;
        };
        numbered_list: {
            title: string;
            subtext: string;
            aliases: string[];
            group: string;
        };
        bullet_list: {
            title: string;
            subtext: string;
            aliases: string[];
            group: string;
        };
        check_list: {
            title: string;
            subtext: string;
            aliases: string[];
            group: string;
        };
        paragraph: {
            title: string;
            subtext: string;
            aliases: string[];
            group: string;
        };
        code_block: {
            title: string;
            subtext: string;
            aliases: string[];
            group: string;
        };
        page_break: {
            title: string;
            subtext: string;
            aliases: string[];
            group: string;
        };
        table: {
            title: string;
            subtext: string;
            aliases: string[];
            group: string;
        };
        image: {
            title: string;
            subtext: string;
            aliases: string[];
            group: string;
        };
        video: {
            title: string;
            subtext: string;
            aliases: string[];
            group: string;
        };
        audio: {
            title: string;
            subtext: string;
            aliases: string[];
            group: string;
        };
        file: {
            title: string;
            subtext: string;
            aliases: string[];
            group: string;
        };
        emoji: {
            title: string;
            subtext: string;
            aliases: string[];
            group: string;
        };
        divider: {
            title: string;
            subtext: string;
            aliases: string[];
            group: string;
        };
    };
    placeholders: Record<string | "default" | "emptyDocument", string | undefined>;
    file_blocks: {
        add_button_text: Record<string, string>;
    };
    toggle_blocks: {
        add_block_button: string;
    };
    side_menu: {
        add_block_label: string;
        drag_handle_label: string;
    };
    drag_handle: {
        delete_menuitem: string;
        colors_menuitem: string;
        header_row_menuitem: string;
        header_column_menuitem: string;
    };
    table_handle: {
        delete_column_menuitem: string;
        delete_row_menuitem: string;
        add_left_menuitem: string;
        add_right_menuitem: string;
        add_above_menuitem: string;
        add_below_menuitem: string;
        split_cell_menuitem: string;
        merge_cells_menuitem: string;
        background_color_menuitem: string;
    };
    suggestion_menu: {
        no_items_title: string;
    };
    color_picker: {
        text_title: string;
        background_title: string;
        colors: {
            default: string;
            gray: string;
            brown: string;
            red: string;
            orange: string;
            yellow: string;
            green: string;
            blue: string;
            purple: string;
            pink: string;
        };
    };
    formatting_toolbar: {
        bold: {
            tooltip: string;
            secondary_tooltip: string;
        };
        italic: {
            tooltip: string;
            secondary_tooltip: string;
        };
        underline: {
            tooltip: string;
            secondary_tooltip: string;
        };
        strike: {
            tooltip: string;
            secondary_tooltip: string;
        };
        code: {
            tooltip: string;
            secondary_tooltip: string;
        };
        colors: {
            tooltip: string;
        };
        link: {
            tooltip: string;
            secondary_tooltip: string;
        };
        file_caption: {
            tooltip: string;
            input_placeholder: string;
        };
        file_replace: {
            tooltip: Record<string, string>;
        };
        file_rename: {
            tooltip: Record<string, string>;
            input_placeholder: Record<string, string>;
        };
        file_download: {
            tooltip: Record<string, string>;
        };
        file_delete: {
            tooltip: Record<string, string>;
        };
        file_preview_toggle: {
            tooltip: string;
        };
        nest: {
            tooltip: string;
            secondary_tooltip: string;
        };
        unnest: {
            tooltip: string;
            secondary_tooltip: string;
        };
        align_left: {
            tooltip: string;
        };
        align_center: {
            tooltip: string;
        };
        align_right: {
            tooltip: string;
        };
        align_justify: {
            tooltip: string;
        };
        table_cell_merge: {
            tooltip: string;
        };
        comment: {
            tooltip: string;
        };
    };
    file_panel: {
        upload: {
            title: string;
            file_placeholder: Record<string, string>;
            upload_error: string;
        };
        embed: {
            title: string;
            embed_button: Record<string, string>;
            url_placeholder: string;
        };
    };
    link_toolbar: {
        delete: {
            tooltip: string;
        };
        edit: {
            text: string;
            tooltip: string;
        };
        open: {
            tooltip: string;
        };
        form: {
            title_placeholder: string;
            url_placeholder: string;
        };
    };
    comments: {
        edited: string;
        save_button_text: string;
        cancel_button_text: string;
        deleted_reference_text: string;
        actions: {
            add_reaction: string;
            resolve: string;
            reopen: string;
            edit_comment: string;
            delete_comment: string;
            more_actions: string;
        };
        reactions: {
            reacted_by: string;
        };
        sidebar: {
            marked_as_resolved: string;
            more_replies: (count: number) => string;
        };
    };
    generic: {
        ctrl_shortcut: string;
    };
};

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
declare const _default: {
    en: {
        "blocknote.imageToolbar.buttons.edit": string;
        "blocknote.imageToolbar.buttons.open": string;
        "blocknote.linkToolbar.buttons.edit": string;
        "blocknote.linkToolbar.buttons.open": string;
        "blocknote.linkToolbar.buttons.delete": string;
        "blocknote.toolbar.unknownBlockType": string;
        "blocknote.imageSelector.uploadButton": string;
        "blocknote.imageSelector.placeholder": string;
        "blocknote.linkEditor.submit": string;
        "blocknote.linkEditor.placeholder": string;
        "blocknote.combobox.backendSearchUnsupported": string;
        "blocknote.combobox.noResultFound": string;
        "blocknote.combobox.loadingSuggestions": string;
    };
};

/**
 * Default options for the BlockNote editor
 *
 * @since 0.16
 * @beta
 */
export declare type DefaultBlockNoteEditorOptions = BlockNoteEditorOptions<EditorBlockSchema, EditorInlineContentSchema, EditorStyleSchema>;

/**
 * Block schema for BlockNote
 *
 * @since 18.0.0RC1
 * @beta
 */
export declare type EditorBlockSchema = EditorSchema extends BlockNoteSchema<infer BlockSchema, infer _, infer __> ? BlockSchema : never;

/**
 * Inline content schema for BlockNote
 *
 * @since 18.0.0RC1
 * @beta
 */
export declare type EditorInlineContentSchema = EditorSchema extends BlockNoteSchema<infer _, infer InlineContentSchema, infer __> ? InlineContentSchema : never;

/**
 * @since 18.0.0RC1
 * @beta
 */
export declare type EditorLanguage = keyof typeof locales & keyof typeof _default;

/**
 * Typesafe BlockNote link
 *
 * @since 18.0.0RC1
 * @beta
 */
export declare type EditorLink = Link<EditorStyleSchema>;

/**
 * Schema of the BlockNote editor
 *
 * @since 18.0.0RC1
 * @beta
 */
export declare type EditorSchema = ReturnType<typeof createBlockNoteSchema>;

/**
 * Typesafe BlockNote styled text
 *
 * @since 18.0.0RC1
 * @beta
 */
export declare type EditorStyledText = StyledText<EditorStyleSchema>;

/**
 * Style schema for BlockNote
 *
 * @since 18.0.0RC1
 * @beta
 */
export declare type EditorStyleSchema = EditorSchema extends BlockNoteSchema<infer _, infer __, infer StyleSchema> ? StyleSchema : never;

/**
 * Type of a BlockNote editor instance
 *
 * @since 18.0.0RC1
 * @beta
 */
export declare type EditorType = BlockNoteEditor<EditorBlockSchema, EditorInlineContentSchema, EditorStyleSchema>;

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
export declare function extractMacroRawContent(content: InlineContent<DefaultInlineContentSchema, DefaultStyleSchema>[]): string;

/**
 * Interceptor for the image edition mechanism
 *
 * @since 0.26
 * @beta
 */
export declare type ImageEditionOverrideFn = (image: BlockOfType<"image">["props"], update: (updateResult: ImageUpdateResult) => void) => void;

/**
 * Result of an image update process, from `ImageEditionOverrideFn`
 *
 * @since 18.0.0RC1
 * @beta
 */
export declare type ImageUpdateResult = {
    type: "update";
    updatedProps: Partial<BlockOfType<"image">["props"]>;
} | {
    type: "aborted";
};

/**
 * Typesafe BlockNote inline content
 *
 * @since 18.0.0RC1
 * @beta
 */
export declare type InlineContentType = InlineContent<EditorInlineContentSchema, EditorStyleSchema>;

/**
 * @since 18.0.0RC1
 * @beta
 */
export declare type LinkEditionContext = {
    linkSuggestService: LinkSuggestService | null;
    modelReferenceParser: ModelReferenceParser;
    modelReferenceSerializer: ModelReferenceSerializer;
    modelReferenceHandler: ModelReferenceHandler;
    remoteURLParser: RemoteURLParser;
    remoteURLSerializer: RemoteURLSerializer;
    attachmentsService: AttachmentsService;
    documentService: DocumentService;
};

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
/**
 * Function called to open the link editor
 *
 * @since 18.4.0RC-1
 * @beta
 */
export declare type LinkEditionHandler = (props: LinkEditionHandlerProps) => void;

/**
 * Properties for the link editor handler
 *
 * @since 18.4.0RC-1
 * @beta
 */
export declare type LinkEditionHandlerProps = {
    current: {
        title: string;
        url: string;
    };
    onSubmit: (link: {
        title: string;
        url: string;
    }) => void;
    mode: "createNew" | "editExisting";
};

/**
 * Name prefix for macro blocks and inline contents in BlockNote
 *
 * @since 18.0.0RC1
 * @beta
 */
export declare const MACRO_NAME_PREFIX = "Macro_";

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
export declare function mountBlockNote(containerEl: HTMLElement, props: BlockNoteViewWrapperProps): {
    unmount: () => void;
};

/**
 * Suggests a set of suggestion from the menu items.
 *
 * @param editor - the editor type
 * @param query - the query to filter the suggestions by
 * @param macros - the available macros
 * @since 18.0.0RC1
 * @beta
 */
export declare function querySuggestionsMenuItems(editor: EditorType, query: string, macros: BlockNoteConcreteMacro[]): DefaultReactSuggestionItem[];

export { }
