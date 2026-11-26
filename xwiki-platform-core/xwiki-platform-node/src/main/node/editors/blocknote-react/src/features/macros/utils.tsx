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

import { useMacroEditing } from "./macroEditing";
import { BlockNoteEditor } from "@blocknote/core";
import { insertOrUpdateBlockForSlashMenu } from "@blocknote/core/extensions";
import {
  createReactBlockSpec,
  createReactInlineContentSpec,
} from "@blocknote/react";
import { assertUnreachable, objectEntries } from "@xwiki/platform-fn-utils";
import { uuidv4 } from "lib0/random";
import { RiFileList3Fill } from "react-icons/ri";
import type { InlineContentType } from "../../blocknote";
import type {
  InlineMacroInvocation,
  MacroBlockInvocation,
  MacroCallParams,
} from "../../services/macroEditor";
import type {
  BlockConfig,
  CustomInlineContentConfig,
  DefaultInlineContentSchema,
  DefaultStyleSchema,
  InlineContent,
  InlineContentSchema,
  PartialBlockFromConfig,
  PartialInlineContent,
  PropSchema,
  PropSpec,
  Props,
  StyleSchema,
} from "@blocknote/core";
import type {
  BlockTypeSelectItem,
  ReactCustomBlockImplementation,
  ReactInlineContentImplementation,
} from "@blocknote/react";
import type { MacroWithUnknownParamsType } from "@xwiki/platform-macros-api";
import type { UniAstToReactJsxConverter } from "@xwiki/platform-uniast-react-jsx";
import type { JSX, ReactNode } from "react";

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
function createCustomBlockSpec<
  const Name extends string,
  const Props extends PropSchema,
  const InlineType extends "inline" | "none",
>({
  config,
  implementation,
  slashMenu,
  customToolbar,
}: {
  config: BlockConfig<Name, Props, InlineType>;
  implementation: ReactCustomBlockImplementation<
    BlockConfig<Name, Props, InlineType>
  >;
  slashMenu:
    | false
    | {
        title: string;
        aliases?: string[];
        group: string;
        icon: ReactNode;
        default: () => PartialBlockFromConfig<
          BlockConfig<Name, Props, InlineType>,
          InlineContentSchema,
          StyleSchema
        >;
      };
  customToolbar: (() => ReactNode) | null;
}) {
  return {
    block: createReactBlockSpec(config, implementation),

    slashMenuEntry: !slashMenu
      ? (false as const)
      : // eslint-disable-next-line @typescript-eslint/no-explicit-any
        (editor: BlockNoteEditor<any, any, any>) => ({
          title: slashMenu.title,
          aliases: slashMenu.aliases,
          group: slashMenu.group,
          icon: slashMenu.icon,
          onItemClick: () => {
            insertOrUpdateBlockForSlashMenu(editor, slashMenu.default());
          },
        }),

    customToolbar,
  };
}

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
function createCustomInlineContentSpec<
  const I extends CustomInlineContentConfig,
  const S extends StyleSchema,
>({
  config,
  implementation,
  slashMenu,
  customToolbar,
}: {
  config: I;
  implementation: ReactInlineContentImplementation<I, S>;
  slashMenu:
    | false
    | {
        title: string;
        aliases?: string[];
        group: string;
        icon: ReactNode;
        default: () => PartialInlineContent<Record<I["type"], I>, S>;
      };
  customToolbar: (() => ReactNode) | null;
}) {
  return {
    inlineContent: createReactInlineContentSpec(config, implementation),

    slashMenuEntry: !slashMenu
      ? (false as const)
      : // eslint-disable-next-line @typescript-eslint/no-explicit-any
        (editor: BlockNoteEditor<any>) => ({
          title: slashMenu.title,
          aliases: slashMenu.aliases,
          group: slashMenu.group,
          icon: slashMenu.icon,
          onItemClick: () => {
            editor.insertInlineContent([
              // @ts-expect-error: the AST is dynamically-typed with macros, so the types are incorrect here
              slashMenu.default(),
            ]);
          },
        }),

    customToolbar,
  };
}

/**
 * Name prefix for macro blocks and inline contents in BlockNote
 *
 * @since 18.0.0RC1
 * @beta
 */
const MACRO_NAME_PREFIX = "Macro_";

/**
 * Description of a macro adapted by `adaptMacroForBlockNote`
 */
type BlockNoteConcreteMacro = {
  /** Type-erased macro */
  macro: MacroWithUnknownParamsType;

  /** Dropdown item for changing the selection to the current macro */
  dropdownTransformItem: BlockTypeSelectItem | null;

  /** Rendering part */
  bnRendering: // Block macro
  | {
        type: "block";
        block:
          | ReturnType<
              typeof createCustomBlockSpec<string, PropSchema, "inline">
            >
          | ReturnType<
              typeof createCustomBlockSpec<string, PropSchema, "none">
            >;
      }
    // Inline macro
    | {
        type: "inline";
        inlineContent: ReturnType<typeof createCustomInlineContentSpec>;
      };
};

/**
 * Adapt a macro to be used inside BlockNote.
 *
 * @param macro - The macro to adapt
 * @param ctx - The context used to handle the macro inside BlockNote
 * @param jsxConverter - A converter that transforms a macro's rendered UniAst content to React JSX
 *
 * @returns - The BlockNote-compatible macro
 *
 * @since 18.0.0RC1
 * @beta
 */
// eslint-disable-next-line max-statements
function adaptMacroForBlockNote(
  macro: MacroWithUnknownParamsType,
  jsxConverter: UniAstToReactJsxConverter,
): BlockNoteConcreteMacro {
  const { id, name, params, defaultParameters } = macro.infos;

  // Compute the macro name
  const blockNoteName = `${MACRO_NAME_PREFIX}${id}`;

  const propSchema: Record<
    string,
    PropSpec<boolean | number | string> & { optional?: true }
  > = {};

  for (const [name, { type, optional }] of objectEntries(params)) {
    propSchema[name] = {
      type:
        type === "string"
          ? "string"
          : type === "float"
            ? "number"
            : type === "boolean"
              ? "boolean"
              : assertUnreachable(type),
      optional,
      default: undefined,
    };
  }

  const getSlashMenu = <T,>(opts: (defaultValue: () => unknown) => T) =>
    defaultParameters
      ? {
          title: name,
          group: "Macros",
          icon: "M",
          aliases: [],
          ...opts(() => ({
            // TODO: statically type parameters so that the `type` name cannot be used,
            //       as it would be shadowed here otherwise
            type: `${MACRO_NAME_PREFIX}${id}`,
            props: defaultParameters,
          })),
        }
      : false;

  /**
   * The rendering function
   *
   * @param contentRef - React reference to the WYSIWYG-editable content zone
   * @param props - The macro's properties
   * @param content - The macro's content
   * @param update - An update function for the macro
   *
   * @returns The rendered macro, as a JSX element
   */
  function renderMacro(
    contentRef: ((node: HTMLElement | null) => void) | null,
    props: Props<PropSchema>,
    content: InlineContent<DefaultInlineContentSchema, DefaultStyleSchema>[],
    // TODO: should also allow to replace the macro's body
    // Tracking issue: https://jira.xwiki.org/browse/CRISTAL-742
    update: (newParams: Props<PropSchema>) => void,
  ): JSX.Element {
    // When no macro editor is available, no double-click handler is attached. The client-rendered macro is exposed to
    // the macro editor as a macro invocation, and the updated invocation's parameters are written back as block props.
    // Called unconditionally, first thing in the render function of the macro spec, so it can use a React hook.
    const macroEditing = useMacroEditing();
    const openParamsEditor = macroEditing
      ? () =>
          macroEditing.edit(
            macroToInvocation(macro, props, content),
            (updated) => update(updated.params as Props<PropSchema>),
          )
      : undefined;

    /** The macro's raw body */
    const rawBody =
      macro.infos.bodyType === "raw" ? extractMacroRawContent(content) : null;

    const renderedJsx =
      macro.renderAs === "block"
        ? jsxConverter.blocksToReactJSX(macro.render(props, rawBody), {
            type: "block",
            ref: macro.infos.bodyType === "wysiwyg" ? contentRef : null,
          })
        : jsxConverter.inlineContentsToReactJSX(macro.render(props, rawBody), {
            type: "inline",
            ref: macro.infos.bodyType === "wysiwyg" ? contentRef : null,
          });

    if (renderedJsx instanceof Error) {
      // TODO: how to display properly an error?
      return (
        <strong onDoubleClick={openParamsEditor}>
          Failed to render macro: {renderedJsx.message}
        </strong>
      );
    }

    return macro.renderAs === "block" ? (
      <div onDoubleClick={openParamsEditor}>{renderedJsx}</div>
    ) : (
      <span onDoubleClick={openParamsEditor}>{renderedJsx}</span>
    );
  }

  // Block and inline macros are defined pretty differently, so a bit of logic was computed ahead of time
  // to share it between the two definitions here.
  const slashMenu = getSlashMenu((getDefaultValue) => ({
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    default: () => getDefaultValue() as any,
  }));

  const bnRendering: BlockNoteConcreteMacro["bnRendering"] =
    macro.renderAs === "block"
      ? {
          type: "block",
          block:
            macro.infos.bodyType === "wysiwyg"
              ? createCustomBlockSpec({
                  config: {
                    type: blockNoteName,
                    // NOTE: Nesting is not supported
                    // Tracking issue: https://github.com/TypeCellOS/BlockNote/issues/1540
                    content: "inline" as const,
                    propSchema,
                  },
                  implementation: {
                    render: ({ contentRef, block, editor }) =>
                      renderMacro(
                        contentRef,
                        block.props,
                        // eslint-disable-next-line @typescript-eslint/no-explicit-any
                        block.content as any,
                        (newProps) => {
                          editor.updateBlock(block.id, { props: newProps });
                        },
                      ),
                  },
                  slashMenu,
                  // TODO: allow macros to define their own toolbar, using a set of provided UI components (buttons, ...)
                  // Tracking issue: https://jira.xwiki.org/browse/CRISTAL-708
                  customToolbar: null,
                })
              : createCustomBlockSpec({
                  config: {
                    type: blockNoteName,
                    content: "none" as const,
                    propSchema,
                  },
                  implementation: {
                    render: ({ block, editor }) =>
                      renderMacro(
                        null,
                        block.props,
                        // eslint-disable-next-line @typescript-eslint/no-explicit-any
                        block.content as any,
                        (newProps) => {
                          editor.updateBlock(block.id, { props: newProps });
                        },
                      ),
                  },
                  slashMenu,
                  customToolbar: null,
                }),
        }
      : {
          type: "inline",
          inlineContent: createCustomInlineContentSpec({
            config: {
              type: blockNoteName,
              // NOTE: Nesting is not supported
              // Tracking issue: https://github.com/TypeCellOS/BlockNote/issues/1540
              content: macro.infos.bodyType === "wysiwyg" ? "styled" : "none",
              propSchema,
            },
            implementation: {
              render: ({ contentRef, inlineContent, updateInlineContent }) =>
                renderMacro(
                  contentRef,
                  inlineContent.props,
                  inlineContent.content,
                  (newProps) => {
                    updateInlineContent({
                      type: inlineContent.type,
                      props: newProps,
                      content: inlineContent.content,
                    });
                  },
                ),
            },
            slashMenu: getSlashMenu((getDefaultValue) => ({
              default: () => [
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                getDefaultValue() as any,
              ],
            })),
            // TODO: allow macros to define their own toolbar, using a set of provided UI components (buttons, ...)
            // Tracking issue: https://jira.xwiki.org/browse/CRISTAL-708
            customToolbar: null,
          }),
        };

  const dropdownTransformItem =
    macro.renderAs === "block"
      ? ({
          type: blockNoteName,
          icon: (props) => <RiFileList3Fill {...props} />,
          name: macro.infos.name,
          props:
            macro.infos.defaultParameters !== false
              ? macro.infos.defaultParameters
              : {},
        } satisfies BlockTypeSelectItem)
      : null;

  return { macro, bnRendering, dropdownTransformItem };
}

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
function extractMacroRawContent(
  content: InlineContent<DefaultInlineContentSchema, DefaultStyleSchema>[],
): string {
  if (content.length !== 1) {
    throw new Error(
      "Internal error: raw-body macro does not have precisely 1 inline content",
    );
  }

  if (content[0].type !== "text") {
    throw new Error(
      "Internal error: raw-body macro's inline content is not a text",
    );
  }

  return content[0].text;
}

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
function buildMacroRawContent(
  content: string,
): InlineContent<DefaultInlineContentSchema, DefaultStyleSchema> {
  return { type: "text", text: content, styles: {} };
}

/**
 * Build a macro invocation from a client-rendered macro (System B) and its current block props / content, so that the
 * generic (invocation-based) params editor can edit it. The parameters are carried over, and the body is mapped
 * according to the macro's body type: a raw body to `raw`, a WYSIWYG body to `inlineContents` (block) / `inlineContent`
 * (inline) so the editable content is preserved, and no body to `none`.
 *
 * @since 18.6.0RC1
 * @beta
 */
function macroToInvocation(
  macro: MacroWithUnknownParamsType,
  params: Props<PropSchema>,
  content: InlineContent<DefaultInlineContentSchema, DefaultStyleSchema>[],
): MacroBlockInvocation | InlineMacroInvocation {
  const macroParams = params as MacroCallParams;
  // The default inline content schema is a subset of the editor's, so widening the content is safe here.
  const inlineContent = content as unknown as InlineContentType[];

  let body: MacroBlockInvocation["body"] | InlineMacroInvocation["body"] = {
    type: "none",
  };
  if (macro.infos.bodyType === "raw") {
    body = { type: "raw", content: extractMacroRawContent(content) };
  } else if (macro.infos.bodyType === "wysiwyg") {
    body =
      macro.renderAs === "block"
        ? { type: "inlineContents", content: inlineContent }
        : // NOTE: an inline macro invocation body currently holds a single inline content (see InlineMacroInvocation),
          // so only the first inline content is carried over. This should carry the whole run once the invocation model
          // supports it (same tracking issue as InlineMacroInvocation.body).
          { type: "inlineContent", content: inlineContent[0] };
  }

  return macro.renderAs === "block"
    ? {
        kind: "block",
        id: macro.infos.id,
        params: macroParams,
        body: body as MacroBlockInvocation["body"],
      }
    : {
        kind: "inline",
        id: macro.infos.id,
        params: macroParams,
        body: body as InlineMacroInvocation["body"],
      };
}

/**
 * A server-side macro call, as stored (JSON-serialized) in the `call` prop of the {@link macro.tsx} `xwikiMacroBlock` /
 * `xwikiInlineMacro` specs and produced/consumed by the server-side BlockNote syntax: the macro name, its parameters
 * and an optional body. Whether the macro is inline or block-level is carried by the block/inline-content type, not by
 * this object. A parameter value and the body can each be either a plain string or a BlockNote fragment (an array of
 * blocks or inline content) when the macro output uses it verbatim (so it can be edited in-place).
 *
 * @since 18.6.0RC1
 * @beta
 */
type MacroCall = {
  name: string;
  parameters: Record<string, unknown>;
  content?: unknown;
};

/** Convert an editor macro invocation into the {@link MacroCall} stored in a macro block's `call` prop. */
function invocationToMacroCall(
  invocation: MacroBlockInvocation | InlineMacroInvocation,
): MacroCall {
  const call: MacroCall = {
    name: invocation.id,
    parameters: invocation.params,
  };
  const body = invocation.body;
  // A raw body is a string; an inline(Contents) body is a BlockNote fragment. Both are carried verbatim into the call
  // content; a "none" body leaves the content unset.
  if (
    body.type === "raw" ||
    body.type === "inlineContents" ||
    body.type === "inlineContent"
  ) {
    call.content = body.content;
  }
  return call;
}

/** Convert a {@link MacroCall} (read from a macro block's `call` prop) into an editor macro invocation. */
function macroCallToInvocation(
  call: MacroCall,
  kind: "block" | "inline",
): MacroBlockInvocation | InlineMacroInvocation {
  const params = (call.parameters ?? {}) as MacroCallParams;
  const { content } = call;
  const hasContent =
    content !== undefined && content !== null && content !== "";

  if (kind === "block") {
    const body: MacroBlockInvocation["body"] = !hasContent
      ? { type: "none" }
      : typeof content === "string"
        ? { type: "raw", content }
        : // A fragment content is carried verbatim (it may be a block or inline-content array).
          { type: "inlineContents", content: content as InlineContentType[] };
    return { kind: "block", id: call.name, params, body };
  }

  const body: InlineMacroInvocation["body"] = !hasContent
    ? { type: "none" }
    : typeof content === "string"
      ? { type: "raw", content }
      : // A fragment content is carried verbatim. NOTE: the inline body currently holds a single inline content (see
        // InlineMacroInvocation), but the server fragment can be an array; it is preserved as-is and round-trips.
        { type: "inlineContent", content: content as InlineContentType };
  return { kind: "inline", id: call.name, params, body };
}

/**
 * Insert (or replace the given selected block with) a server-rendered macro represented by the {@link macro.tsx}
 * `xwikiMacroBlock` / `xwikiInlineMacro` specs. The call (name, parameters and body) is stored as the JSON `call` prop.
 * The inserted macro has no output (it is displayed as a placeholder) until it is rendered server-side.
 *
 * @since 18.6.0RC1
 * @beta
 */
function insertMacroInvocation(
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  editor: BlockNoteEditor<any, any, any>,
  invocation: MacroBlockInvocation | InlineMacroInvocation,
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  selectedBlock?: any,
): void {
  const props = {
    call: JSON.stringify(invocationToMacroCall(invocation)),
  };
  if (invocation.kind === "block") {
    // NOTE: the schema is dynamically typed with macros, so the types are incorrect here.
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const block = { type: "xwikiMacroBlock", props } as any;
    if (selectedBlock) {
      editor.replaceBlocks([selectedBlock], [block]);
    } else {
      insertOrUpdateBlockForSlashMenu(editor, block);
    }
  } else {
    // Inline content has no id that BlockNote preserves, so the id used to keep the macro output is a property.
    const inlineMacro = {
      type: "xwikiInlineMacro",
      props: { ...props, id: uuidv4() },
    };
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    editor.insertInlineContent([inlineMacro as any], {
      updateSelection: true,
    });
  }
}

export {
  MACRO_NAME_PREFIX,
  adaptMacroForBlockNote,
  buildMacroRawContent,
  createCustomBlockSpec,
  createCustomInlineContentSpec,
  extractMacroRawContent,
  insertMacroInvocation,
  invocationToMacroCall,
  macroCallToInvocation,
  macroToInvocation,
};

export type { BlockNoteConcreteMacro, MacroCall };
