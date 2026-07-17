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
import "./macro.css";
import { applyXWikiParameters } from "./parameters";
import { invocationToMacroCall, macroCallToInvocation } from "./utils";
import { MacrosContext } from "../contexts";
import {
  createReactBlockSpec,
  createReactInlineContentSpec,
} from "@blocknote/react";
import { useContext } from "react";
import type { MacroCall } from "./utils";
import type { BlockNoteEditor, PartialBlock } from "@blocknote/core";

type RawBlock = {
  type: string;
  props?: Record<string, unknown>;
  children?: RawBlock[];
  content?: unknown;
  name?: string;
};

/** Wraps a call parameter/content value in a blocks array. */
function valueToBlocks(value: unknown): RawBlock[] {
  if (!value) {
    return [];
  } else if (typeof value === "string") {
    return [{ type: "paragraph", props: {}, content: value, children: [] }];
  } else if (Array.isArray(value)) {
    return value as RawBlock[];
  }
  return [value as RawBlock];
}

/** Wraps a call parameter/content value in an inline content array. */
function valueToInlineContent(value: unknown): RawBlock[] {
  if (!value) {
    return [];
  } else if (typeof value === "string") {
    return [{ type: "text", text: value, styles: {} } as unknown as RawBlock];
  } else if (Array.isArray(value)) {
    return value as RawBlock[];
  }
  return [value as RawBlock];
}

/**
 * Looks up a macro-call parameter value by name, case-insensitively. The `xwikiEditable` marker carries the macro
 * descriptor's (canonical) parameter name, whereas the macro call stores parameters under the case the user actually
 * wrote, so an exact-case lookup would miss (this mirrors the server's case-insensitive matching).
 */
function lookupParameter(
  parameters: MacroCall["parameters"],
  name: string,
): unknown {
  if (name in parameters) {
    return parameters[name];
  }
  const lowerName = name.toLowerCase();
  const key = Object.keys(parameters).find(
    (k) => k.toLowerCase() === lowerName,
  );
  return key === undefined ? undefined : parameters[key];
}

/**
 * Recursively replaces `xwikiEditable` markers in the macro output with the corresponding parameter/content value from
 * the macro call. The marker is replaced with the value converted through the provided `toValue` function (blocks for a
 * block macro output, inline content for an inline macro output).
 */
function substituteEditables(
  blocks: RawBlock[],
  call: MacroCall,
  toValue: (value: unknown) => RawBlock[],
): RawBlock[] {
  return blocks.flatMap((block) => {
    if (block.type === "xwikiEditable") {
      // Replace the editable marker with the corresponding parameter/content value.
      return toValue(
        block.name
          ? lookupParameter(call.parameters, block.name)
          : call.content,
      );
    } else if (Array.isArray(block.children) && block.children.length) {
      // Transform the children recursively.
      return [
        {
          ...block,
          children: substituteEditables(block.children, call, toValue),
        },
      ];
    }
    // Return the block as-is.
    return block;
  });
}

/** A node in a macro output tree: either a block or a bare inline string. */
type OutputNode = RawBlock | string;

/** Inline content node types (besides bare strings) that can't be handed to the block-level HTML export as blocks. */
const INLINE_TYPES = new Set(["text", "link", "xwikiInlineMacro"]);

/** @returns whether the node is inline content: a bare string or a node whose type is inline. */
function isInlineNode(node: OutputNode): boolean {
  return (
    typeof node === "string" || INLINE_TYPES.has((node as RawBlock).type ?? "")
  );
}

/**
 * Extracts the inline HTML out of the paragraph that wraps it (see {@link inlineNodesToHTML}). The inline content is
 * exported wrapped in a throwaway paragraph; this returns that paragraph's inner HTML, i.e. the inline HTML with no
 * wrapping element.
 */
function extractInlineHTML(blockHTML: string): string {
  const wrapper = document.createElement("div");
  wrapper.innerHTML = blockHTML;
  const paragraph = wrapper.querySelector("p");
  return paragraph ? paragraph.innerHTML : wrapper.innerHTML;
}

/**
 * Renders a run of inline content nodes to HTML with no wrapping element. Since the only HTML export API works on
 * blocks, the inline content is wrapped in a throwaway paragraph and the inner HTML of the produced element is
 * extracted so that it can be injected inline.
 */
function inlineNodesToHTML(
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  editor: BlockNoteEditor<any, any, any>,
  nodes: readonly OutputNode[],
): string {
  if (!nodes.length) {
    return "";
  }
  const paragraph = {
    type: "paragraph",
    props: {},
    content: [...nodes],
    children: [],
  };
  try {
    return extractInlineHTML(
      editor.blocksToHTMLLossy([
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        paragraph as PartialBlock<typeof editor.schema.blockSchema, any, any>,
      ]),
    );
  } catch (error) {
    console.error(
      "Failed to render the macro output inline content to HTML: ",
      error,
    );
    return "";
  }
}

/** Parses an HTML string (as produced by blocksToHTMLLossy) into nodes and appends them to the given parent. */
function appendHTML(parent: HTMLElement, html: string): void {
  const holder = document.createElement("div");
  holder.innerHTML = html;
  parent.append(...Array.from(holder.childNodes));
}

/**
 * Appends an xwikiRaw node to the parent with no container element. HTML syntaxes are injected verbatim (unlike the
 * xwikiRaw block spec, which must wrap them in a &lt;div class="xwiki-raw"&gt; because a BlockNote block always
 * serializes to a single root element); other syntaxes are appended as a &lt;pre&gt; whose text is set (and thus
 * escaped) through the DOM.
 */
function renderRaw(parent: HTMLElement, block: RawBlock): void {
  const { syntax, text } = (block.props ?? {}) as {
    syntax?: string;
    text?: string;
  };
  if ((syntax ?? "").startsWith("html/")) {
    appendHTML(parent, text ?? "");
  } else {
    const pre = document.createElement("pre");
    pre.className = "xwiki-raw";
    pre.textContent = text ?? "";
    parent.append(pre);
  }
}

/**
 * Builds a group (DIV) element with no extra wrapping element: its xwikiParameters become the DIV attributes and its
 * children are rendered recursively, so inline runs and nested groups keep the structure the server produced.
 */
function renderGroup(
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  editor: BlockNoteEditor<any, any, any>,
  block: RawBlock,
): HTMLElement {
  const div = document.createElement("div");
  applyXWikiParameters(div, block.props?.xwikiParameters);
  renderInto(editor, div, (block.children ?? []) as OutputNode[]);
  return div;
}

/**
 * Replaces every xwikiGroup nested inside the given plain blocks with a self-contained xwikiRaw html carrier holding
 * the group's already-rendered (zero-wrapper) HTML. This is the fallback for a group trapped inside a block that must
 * be exported wholesale by blocksToHTMLLossy (e.g. a group inside a list item): the group can't be rendered inline
 * there, so it is pre-rendered here. blocksToHTMLLossy then wraps the carrier in a &lt;div class="xwiki-raw"&gt; which
 * {@link unwrapRawDivs} strips afterwards, keeping the output zero-wrapper. Groups reached directly by renderInto are
 * rendered by {@link renderGroup} and never go through this path.
 */
function sanitizeGroups(
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  editor: BlockNoteEditor<any, any, any>,
  node: OutputNode,
): OutputNode {
  if (typeof node !== "object" || !node) {
    return node;
  }
  if (node.type === "xwikiGroup") {
    return {
      type: "xwikiRaw",
      props: { syntax: "html/5.0", text: renderGroup(editor, node).outerHTML },
    };
  }
  if (Array.isArray(node.children)) {
    return {
      ...node,
      children: node.children.map((child) =>
        sanitizeGroups(editor, child),
      ) as RawBlock[],
    };
  }
  return node;
}

/**
 * Renders a run of plain blocks (no group/raw/inline) through the block-level HTML export, batched so that e.g.
 * consecutive list items still collapse into a single list.
 */
function plainBlocksToHTML(
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  editor: BlockNoteEditor<any, any, any>,
  blocks: readonly OutputNode[],
): string {
  if (!blocks.length) {
    return "";
  }
  const sanitized = blocks.map((block) => sanitizeGroups(editor, block));
  try {
    return editor.blocksToHTMLLossy(
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      sanitized as PartialBlock<typeof editor.schema.blockSchema, any, any>[],
    );
  } catch (error) {
    console.error("Failed to render the macro output to HTML: ", error);
    return "";
  }
}

/** The kind of a node for segmentation: consecutive `inline`/`blocks` nodes are batched; `group`/`raw` stand alone. */
type SegmentKind = "inline" | "blocks" | "group" | "raw";

/** @returns the segmentation kind of the given node. */
function segmentKind(node: OutputNode): SegmentKind {
  if (isInlineNode(node)) {
    return "inline";
  }
  const type = (node as RawBlock).type;
  if (type === "xwikiGroup") {
    return "group";
  }
  return type === "xwikiRaw" ? "raw" : "blocks";
}

/** A contiguous run of same-kind nodes. `inline`/`blocks` runs may hold several nodes; `group`/`raw` runs hold one. */
type Segment = { kind: SegmentKind; nodes: OutputNode[] };

/** Splits a sibling array into segments, merging consecutive `inline` (resp. `blocks`) nodes into a single segment. */
function segmentNodes(nodes: readonly OutputNode[]): Segment[] {
  const segments: Segment[] = [];
  for (const node of nodes) {
    const kind = segmentKind(node);
    const last = segments.at(-1);
    if (last?.kind === kind && (kind === "inline" || kind === "blocks")) {
      last.nodes.push(node);
    } else {
      segments.push({ kind, nodes: [node] });
    }
  }
  return segments;
}

/** Appends a single segment's rendered content to the given parent element. */
function appendSegment(
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  editor: BlockNoteEditor<any, any, any>,
  parent: HTMLElement,
  segment: Segment,
): void {
  switch (segment.kind) {
    case "inline":
      appendHTML(parent, inlineNodesToHTML(editor, segment.nodes));
      break;
    case "blocks":
      appendHTML(parent, plainBlocksToHTML(editor, segment.nodes));
      break;
    case "group":
      parent.append(renderGroup(editor, segment.nodes[0] as RawBlock));
      break;
    default:
      renderRaw(parent, segment.nodes[0] as RawBlock);
  }
}

/**
 * Renders a sibling array that may freely mix inline content and block-level nodes (which BlockNote's schema forbids
 * inside a single parent, but XWiki groups/DIVs allow) into the given parent element. Inline runs are appended with no
 * wrapping element, groups and raw content are appended directly, and everything else is batched through the
 * block-level export.
 */
function renderInto(
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  editor: BlockNoteEditor<any, any, any>,
  parent: HTMLElement,
  nodes: readonly OutputNode[],
): void {
  for (const segment of segmentNodes(nodes)) {
    appendSegment(editor, parent, segment);
  }
}

/**
 * Strips the &lt;div class="xwiki-raw"&gt; container that blocksToHTMLLossy is forced to emit around raw HTML (a
 * BlockNote block always serializes to a single root element). Only the trapped-group carriers produced by
 * {@link sanitizeGroups} go through that path; the common paths already append raw HTML directly.
 * &lt;pre class="xwiki-raw"&gt; (non-HTML verbatim) is intentionally left untouched.
 */
function unwrapRawDivs(root: HTMLElement): void {
  root.querySelectorAll("div.xwiki-raw").forEach((element) => {
    element.replaceWith(...Array.from(element.childNodes));
  });
}

/**
 * Renders a macro output to HTML. The output may freely mix inline content, blocks, groups and raw content (which
 * BlockNote's schema forbids under a single parent, but macro output produced by the server does not respect); it is
 * rendered through {@link renderInto} so each kind is emitted correctly and with no extra wrapper element. `toValue`
 * decides how editable markers are substituted: as blocks for a block macro, as inline content for an inline macro.
 */
function outputToHTML(
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  editor: BlockNoteEditor<any, any, any>,
  output: RawBlock[],
  call: MacroCall,
  toValue: (value: unknown) => RawBlock[],
): string {
  if (!output.length) {
    return "";
  }
  const processed = substituteEditables(output, call, toValue);
  const container = document.createElement("div");
  renderInto(editor, container, processed as OutputNode[]);
  unwrapRawDivs(container);
  return container.innerHTML;
}

/** Renders a block macro output (an array of blocks, possibly with groups mixing inline and block content) to HTML. */
function blockOutputToHTML(
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  editor: BlockNoteEditor<any, any, any>,
  output: RawBlock[],
  call: MacroCall,
): string {
  return outputToHTML(editor, output, call, valueToBlocks);
}

/** Renders an inline macro output (inline content, possibly interleaved with raw content) to HTML. */
function inlineOutputToHTML(
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  editor: BlockNoteEditor<any, any, any>,
  output: RawBlock[],
  call: MacroCall,
): string {
  return outputToHTML(editor, output, call, valueToInlineContent);
}

/**
 * BlockNote block spec for an XWiki macro call (block macro).
 * `props.call` holds the macro call as a JSON string; `props.output` holds
 * the server-rendered output as a JSON string. Both are serialized/deserialized
 * by `XWikiBlockNoteProcessor` on load/save. Not exposed in the slash menu.
 *
 * @since 18.6.0RC1
 * @beta
 */
const XWikiMacroBlock = createReactBlockSpec(
  {
    type: "xwikiMacroBlock",
    propSchema: {
      call: { default: "{}" },
      output: { default: "[]" },
    },
    content: "none",
  },
  {
    render: ({ block, editor }) => {
      const call = JSON.parse(block.props.call) as MacroCall;
      const output = JSON.parse(block.props.output) as RawBlock[];
      const html = blockOutputToHTML(editor, output, call);
      // Double-click opens the macro params editor (when a macro context is available), editing the call in place.
      const openParamsEditor = useContext(MacrosContext)?.openParamsEditor;
      const onDoubleClick = openParamsEditor
        ? () =>
            openParamsEditor(macroCallToInvocation(call, "block"), (updated) =>
              editor.updateBlock(block.id, {
                props: {
                  call: JSON.stringify(invocationToMacroCall(updated)),
                  output: "[]",
                },
              }),
            )
        : undefined;
      if (html) {
        return (
          <div
            className="xwiki-macro-block"
            data-macro-name={call.name}
            title={"macro:" + call.name}
            onDoubleClick={onDoubleClick}
            dangerouslySetInnerHTML={{ __html: html }}
          />
        );
      } else {
        return (
          <div
            className="xwiki-macro-block xwiki-macro-placeholder"
            data-macro-name={call.name}
            title={"macro:" + call.name}
            onDoubleClick={onDoubleClick}
          >
            macro:{call.name}
          </div>
        );
      }
    },
  },
);

/**
 * BlockNote inline content spec for an XWiki macro call (inline macro).
 * Mirrors {@link XWikiMacroBlock} but for inline content: `props.call` holds the
 * macro call as a JSON string and `props.output` holds the server-rendered inline
 * output as a JSON string. Both are serialized/deserialized by `XWikiBlockNoteProcessor`
 * on load/save. Not exposed in the slash menu.
 *
 * @since 18.6.0RC1
 * @beta
 */
const XWikiInlineMacro = createReactInlineContentSpec(
  {
    type: "xwikiInlineMacro",
    propSchema: {
      call: { default: "{}" },
      output: { default: "[]" },
    },
    content: "none",
  },
  {
    render: ({ inlineContent, editor, updateInlineContent }) => {
      const call = JSON.parse(inlineContent.props.call) as MacroCall;
      const output = JSON.parse(inlineContent.props.output) as RawBlock[];
      const html = inlineOutputToHTML(editor, output, call);
      // Double-click opens the macro params editor (when a macro context is available), editing the call in place.
      const openParamsEditor = useContext(MacrosContext)?.openParamsEditor;
      const onDoubleClick = openParamsEditor
        ? () =>
            openParamsEditor(macroCallToInvocation(call, "inline"), (updated) =>
              updateInlineContent({
                type: "xwikiInlineMacro",
                props: {
                  call: JSON.stringify(invocationToMacroCall(updated)),
                  output: "[]",
                },
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
              } as any),
            )
        : undefined;
      if (html) {
        return (
          <span
            className="xwiki-macro-inline"
            data-macro-name={call.name}
            title={"macro:" + call.name}
            onDoubleClick={onDoubleClick}
            dangerouslySetInnerHTML={{ __html: html }}
          />
        );
      } else {
        return (
          <span
            className="xwiki-macro-inline xwiki-macro-placeholder"
            data-macro-name={call.name}
            title={"macro:" + call.name}
            onDoubleClick={onDoubleClick}
          >
            macro:{call.name}
          </span>
        );
      }
    },
  },
);

// blockOutputToHTML / inlineOutputToHTML are exported for unit testing of the macro-output rendering (mixed
// inline/block group content, zero-wrapper HTML).
export {
  XWikiInlineMacro,
  XWikiMacroBlock,
  blockOutputToHTML,
  inlineOutputToHTML,
};
