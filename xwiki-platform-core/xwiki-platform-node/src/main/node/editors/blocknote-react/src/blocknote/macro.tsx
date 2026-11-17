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
import { MacroOutput } from "./macroOutput";
import { invocationToMacroCall, macroCallToInvocation } from "./utils";
import { MacrosContext } from "../contexts";
import {
  createReactBlockSpec,
  createReactInlineContentSpec,
} from "@blocknote/react";
import { useContext } from "react";
import type { OutputNode } from "./macroOutput";
import type { ContextForMacros, MacroCall } from "./utils";

/**
 * @param macros - the macro context, used to retrieve the macro output
 * @param id - identifies the macro call (the block id or the `id` property)
 * @param call - the current macro call (JSON)
 * @returns the server-rendered output of the specified macro call, or an empty array if not available
 */
function getMacroOutput(
  macros: ContextForMacros | null,
  id: string,
  call: string,
): OutputNode[] {
  return ((id && macros?.getMacroOutput?.(id, call)) || []) as OutputNode[];
}

/**
 * BlockNote block spec for an XWiki macro call (block macro).
 * `props.call` holds the macro call as a JSON string, serialized/deserialized
 * by `XWikiBlockNoteProcessor` on load/save. Not exposed in the slash menu.
 *
 * The server-rendered macro output is not stored in the block: it is retrieved by block id through the macro context
 * (see {@link ContextForMacros.getMacroOutput}). The output is rendered for the current user (it can contain information
 * that only the current user is allowed to view) so it must not be part of the edited content, which is shared with the
 * other users when editing in realtime.
 *
 * The output is rendered as React components by {@link MacroOutput}, so a macro whose parameter/content is used
 * verbatim (an `xwikiEditable` marker) and contains nested macro calls resolves through plain React composition, to
 * any depth. The double-click-to-edit handler lives only on this outermost wrapper; a double-click inside a nested
 * macro bubbles up to it, so the whole macro island is edited as one call.
 *
 * @since 18.6.0RC1
 * @beta
 */
const XWikiMacroBlock = createReactBlockSpec(
  {
    type: "xwikiMacroBlock",
    propSchema: {
      call: { default: "{}" },
    },
    content: "none",
  },
  {
    render: ({ block, editor }) => {
      const call = JSON.parse(block.props.call) as MacroCall;
      const macros = useContext(MacrosContext);
      const output = getMacroOutput(macros, block.id, block.props.call);
      // Double-click opens the macro params editor (when a macro context is available), editing the call in place.
      const openParamsEditor = macros?.openParamsEditor;
      const onDoubleClick = openParamsEditor
        ? () =>
            openParamsEditor(macroCallToInvocation(call, "block"), (updated) =>
              editor.updateBlock(block.id, {
                props: {
                  call: JSON.stringify(invocationToMacroCall(updated)),
                },
              }),
            )
        : undefined;
      if (output.length) {
        return (
          <div
            className="xwiki-macro-block"
            data-macro-name={call.name}
            title={"macro:" + call.name}
            onDoubleClick={onDoubleClick}
          >
            <MacroOutput nodes={output} call={call} inline={false} />
          </div>
        );
      }
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
    },
  },
);

/**
 * BlockNote inline content spec for an XWiki macro call (inline macro).
 * Mirrors {@link XWikiMacroBlock} but for inline content: `props.call` holds the
 * macro call as a JSON string, serialized/deserialized by `XWikiBlockNoteProcessor`
 * on load/save. Not exposed in the slash menu. Inline content has no id that BlockNote
 * preserves so `props.id` is used to retrieve the server-rendered macro output.
 *
 * @since 18.6.0RC1
 * @beta
 */
const XWikiInlineMacro = createReactInlineContentSpec(
  {
    type: "xwikiInlineMacro",
    propSchema: {
      call: { default: "{}" },
      id: { default: "" },
    },
    content: "none",
  },
  {
    render: ({ inlineContent, updateInlineContent }) => {
      const call = JSON.parse(inlineContent.props.call) as MacroCall;
      const macros = useContext(MacrosContext);
      const output = getMacroOutput(
        macros,
        inlineContent.props.id,
        inlineContent.props.call,
      );
      // Double-click opens the macro params editor (when a macro context is available), editing the call in place.
      const openParamsEditor = macros?.openParamsEditor;
      const onDoubleClick = openParamsEditor
        ? () =>
            openParamsEditor(macroCallToInvocation(call, "inline"), (updated) =>
              updateInlineContent({
                type: "xwikiInlineMacro",
                props: {
                  call: JSON.stringify(invocationToMacroCall(updated)),
                  id: inlineContent.props.id,
                },
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
              } as any),
            )
        : undefined;
      if (output.length) {
        return (
          <span
            className="xwiki-macro-inline"
            data-macro-name={call.name}
            title={"macro:" + call.name}
            onDoubleClick={onDoubleClick}
          >
            <MacroOutput nodes={output} call={call} inline={true} />
          </span>
        );
      }
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
    },
  },
);

export { XWikiInlineMacro, XWikiMacroBlock };
