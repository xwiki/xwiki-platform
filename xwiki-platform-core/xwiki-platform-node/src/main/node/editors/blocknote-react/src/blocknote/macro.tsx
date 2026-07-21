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
import type { MacroCall } from "./utils";

/**
 * BlockNote block spec for an XWiki macro call (block macro).
 * `props.call` holds the macro call as a JSON string; `props.output` holds
 * the server-rendered output as a JSON string. Both are serialized/deserialized
 * by `XWikiBlockNoteProcessor` on load/save. Not exposed in the slash menu.
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
      output: { default: "[]" },
    },
    content: "none",
  },
  {
    render: ({ block, editor }) => {
      const call = JSON.parse(block.props.call) as MacroCall;
      const output = JSON.parse(block.props.output) as OutputNode[];
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
    render: ({ inlineContent, updateInlineContent }) => {
      const call = JSON.parse(inlineContent.props.call) as MacroCall;
      const output = JSON.parse(inlineContent.props.output) as OutputNode[];
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
