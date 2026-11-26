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
import { insertOrUpdateBlockForSlashMenu } from "@blocknote/core/extensions";
import type { InlineContentType } from "../blocknote";
import type {
  InlineMacroInvocation,
  MacroBlockInvocation,
  MacroCallParams,
} from "../services/macroEditor";
import type { BlockNoteEditor } from "@blocknote/core";

/**
 * A server-side macro call, as stored (JSON-serialized) in the `call` prop of the `xwikiMacroBlock` /
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

/**
 * Convert an editor macro invocation into the {@link MacroCall} stored in a macro block's `call` prop. The block or
 * inline distinction is carried by the content type rather than by the call, so the kind isn't needed here.
 */
function invocationToMacroCall(
  invocation:
    | Omit<MacroBlockInvocation, "kind">
    | Omit<InlineMacroInvocation, "kind">,
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
 * Build a server-rendered macro call as the `xwikiInlineMacro` inline content the macro feature adds to the schema,
 * without inserting it. The call is stored as the JSON `call` prop, and the macro has no output (it is displayed as a
 * placeholder) until it is rendered server-side.
 *
 * @param invocation - the macro call to build
 * @returns the inline content holding the given macro call
 *
 * @since 19.0.0RC1
 * @beta
 */
function createInlineMacro(
  invocation: Omit<InlineMacroInvocation, "kind">,
): InlineContentType {
  const props = { call: JSON.stringify(invocationToMacroCall(invocation)) };
  // NOTE: the schema is dynamically typed with macros, so the types are incorrect here.
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  return { type: "xwikiInlineMacro", props } as any;
}

/**
 * Insert (or replace the given selected block with) a server-rendered macro represented by the `xwikiMacroBlock` /
 * `xwikiInlineMacro` specs that the macro feature adds to the schema. The call (name, parameters and body) is stored
 * as the JSON `call` prop. The inserted macro has no output (it is displayed as a placeholder) until it is rendered
 * server-side.
 *
 * @since 19.0.0RC1
 * @beta
 */
function insertMacroInvocation(
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  editor: BlockNoteEditor<any, any, any>,
  invocation: MacroBlockInvocation | InlineMacroInvocation,
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  selectedBlock?: any,
): void {
  if (invocation.kind === "block") {
    const props = { call: JSON.stringify(invocationToMacroCall(invocation)) };
    // NOTE: the schema is dynamically typed with macros, so the types are incorrect here.
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const block = { type: "xwikiMacroBlock", props } as any;
    if (selectedBlock) {
      editor.replaceBlocks([selectedBlock], [block]);
    } else {
      insertOrUpdateBlockForSlashMenu(editor, block);
    }
  } else {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    editor.insertInlineContent([createInlineMacro(invocation) as any], {
      updateSelection: true,
    });
  }
}

export {
  createInlineMacro,
  insertMacroInvocation,
  invocationToMacroCall,
  macroCallToInvocation,
};

export type { MacroCall };
