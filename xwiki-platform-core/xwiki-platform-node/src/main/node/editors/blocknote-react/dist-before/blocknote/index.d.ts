import { default as translations } from '../translations';
import { BlockNoteEditor, BlockNoteSchema, Block, InlineContent, Link, StyledText } from '@blocknote/core';
import { BlockNoteConcreteMacro } from './utils';
import { DefaultReactSuggestionItem } from '@blocknote/react';
import * as locales from "@blocknote/core/locales";
/**
 * Create the BlockNote editor's schema
 *
 * Contains all the blocks usable inside the editor
 *
 * @returns The created schema
 * @since 18.0.0RC1
 * @beta
 */
declare function createBlockNoteSchema(macros: BlockNoteConcreteMacro[]): BlockNoteSchema<import('@blocknote/core').BlockSchemaFromSpecs<{
    bulletListItem: import('@blocknote/core').BlockSpec<"bulletListItem", {
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
    checkListItem: import('@blocknote/core').BlockSpec<"checkListItem", {
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
    codeBlock: import('@blocknote/core').BlockSpec<"codeBlock", {
        readonly language: {
            readonly default: string;
        };
    }, "inline">;
    divider: import('@blocknote/core').BlockSpec<"divider", {}, "none">;
    heading: import('@blocknote/core').BlockSpec<"heading", {
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
    image: import('@blocknote/core').BlockSpec<"image", {
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
    numberedListItem: import('@blocknote/core').BlockSpec<"numberedListItem", {
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
    paragraph: import('@blocknote/core').BlockSpec<"paragraph", {
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
    quote: import('@blocknote/core').BlockSpec<"quote", {
        readonly backgroundColor: {
            default: "default";
        };
        readonly textColor: {
            default: "default";
        };
    }, "inline">;
    table: import('@blocknote/core').LooseBlockSpec<"table", {
        textColor: {
            default: "default";
        };
    }, "table">;
}>, import('@blocknote/core').InlineContentSchemaFromSpecs<{
    text: {
        config: "text";
        implementation: any;
    };
    link: {
        config: "link";
        implementation: any;
    };
}>, import('@blocknote/core').StyleSchemaFromSpecs<{
    bold: {
        config: {
            type: string;
            propSchema: "boolean";
        };
        implementation: import('@blocknote/core').StyleImplementation<{
            type: string;
            propSchema: "boolean";
        }>;
    };
    italic: {
        config: {
            type: string;
            propSchema: "boolean";
        };
        implementation: import('@blocknote/core').StyleImplementation<{
            type: string;
            propSchema: "boolean";
        }>;
    };
    underline: {
        config: {
            type: string;
            propSchema: "boolean";
        };
        implementation: import('@blocknote/core').StyleImplementation<{
            type: string;
            propSchema: "boolean";
        }>;
    };
    strike: {
        config: {
            type: string;
            propSchema: "boolean";
        };
        implementation: import('@blocknote/core').StyleImplementation<{
            type: string;
            propSchema: "boolean";
        }>;
    };
    code: {
        config: {
            type: string;
            propSchema: "boolean";
        };
        implementation: import('@blocknote/core').StyleImplementation<{
            type: string;
            propSchema: "boolean";
        }>;
    };
    textColor: import('@blocknote/core').StyleSpec<{
        readonly type: "textColor";
        readonly propSchema: "string";
    }>;
    backgroundColor: import('@blocknote/core').StyleSpec<{
        readonly type: "backgroundColor";
        readonly propSchema: "string";
    }>;
}>>;
/**
 * Create a translated dictionary for the BlockNote editor
 *
 * @param lang - The dictionary's language
 *
 * @returns The dictionary in the requested language
 * @since 18.0.0RC1
 * @beta
 */
declare function createDictionary(lang: EditorLanguage): {
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
 * @since 18.0.0RC1
 * @beta
 */
type EditorLanguage = keyof typeof locales & keyof typeof translations;
/**
 * Suggests a set of suggestion from the menu items.
 *
 * @param editor - the editor type
 * @param query - the query to filter the suggestions by
 * @param macros - the available macros
 * @since 18.0.0RC1
 * @beta
 */
declare function querySuggestionsMenuItems(editor: EditorType, query: string, macros: BlockNoteConcreteMacro[]): DefaultReactSuggestionItem[];
/**
 * Schema of the BlockNote editor
 *
 * @since 18.0.0RC1
 * @beta
 */
type EditorSchema = ReturnType<typeof createBlockNoteSchema>;
/**
 * Block schema for BlockNote
 *
 * @since 18.0.0RC1
 * @beta
 */
type EditorBlockSchema = EditorSchema extends BlockNoteSchema<infer BlockSchema, infer _, infer __> ? BlockSchema : never;
/**
 * Inline content schema for BlockNote
 *
 * @since 18.0.0RC1
 * @beta
 */
type EditorInlineContentSchema = EditorSchema extends BlockNoteSchema<infer _, infer InlineContentSchema, infer __> ? InlineContentSchema : never;
/**
 * Style schema for BlockNote
 *
 * @since 18.0.0RC1
 * @beta
 */
type EditorStyleSchema = EditorSchema extends BlockNoteSchema<infer _, infer __, infer StyleSchema> ? StyleSchema : never;
/**
 * Type of a BlockNote editor instance
 *
 * @since 18.0.0RC1
 * @beta
 */
type EditorType = BlockNoteEditor<EditorBlockSchema, EditorInlineContentSchema, EditorStyleSchema>;
/**
 * Typesafe BlockNote type
 *
 * @since 18.0.0RC1
 * @beta
 */
type BlockType = Block<EditorBlockSchema, EditorInlineContentSchema, EditorStyleSchema>;
/**
 * Typesafe BlockNote type of the a given kind
 *
 * @since 18.0.0RC1
 * @beta
 */
type BlockOfType<B extends BlockType["type"]> = Extract<BlockType, {
    type: B;
}>;
/**
 * Typesafe BlockNote inline content
 *
 * @since 18.0.0RC1
 * @beta
 */
type InlineContentType = InlineContent<EditorInlineContentSchema, EditorStyleSchema>;
/**
 * Typesafe BlockNote styled text
 *
 * @since 18.0.0RC1
 * @beta
 */
type EditorStyledText = StyledText<EditorStyleSchema>;
/**
 * Typesafe BlockNote link
 *
 * @since 18.0.0RC1
 * @beta
 */
type EditorLink = Link<EditorStyleSchema>;
export type { BlockNoteConcreteMacro, BlockOfType, BlockType, EditorBlockSchema, EditorInlineContentSchema, EditorLanguage, EditorLink, EditorSchema, EditorStyleSchema, EditorStyledText, EditorType, InlineContentType, };
export { createBlockNoteSchema, createDictionary, querySuggestionsMenuItems };
