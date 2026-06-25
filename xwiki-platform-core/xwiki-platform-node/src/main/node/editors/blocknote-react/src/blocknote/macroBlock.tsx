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
import { createReactBlockSpec } from "@blocknote/react";
import type { BlockNoteEditor, PartialBlock } from "@blocknote/core";

type MacroCall = {
  name: string;
  parameters: Record<string, unknown>;
  content?: unknown;
};

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

/**
 * Recursively replaces `xwikiEditable` markers in the macro output with the corresponding parameter/content value from
 * the macro call.
 */
function substituteEditables(blocks: RawBlock[], call: MacroCall): RawBlock[] {
  const result: RawBlock[] = [];
  for (const block of blocks) {
    if (typeof block !== "object" || !block) {
      continue;
    } else if (block.type === "xwikiEditable") {
      const value = block.name ? call.parameters[block.name] : call.content;
      result.push(...valueToBlocks(value));
    } else {
      result.push({
        ...block,
        children: block.children
          ? substituteEditables(block.children, call)
          : undefined,
      });
    }
  }
  return result;
}

function toHTML(
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  editor: BlockNoteEditor<any, any, any>,
  output: RawBlock[],
  call: MacroCall,
): string {
  if (!output.length) {
    return "";
  }

  const processed = substituteEditables(output, call);
  try {
    return editor.blocksToHTMLLossy(
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      processed as PartialBlock<typeof editor.schema.blockSchema, any, any>[],
    );
  } catch (error) {
    console.error("Failed to render the macro output to HTML: ", error);
    return "";
  }
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
      const html = toHTML(editor, output, call);
      if (html) {
        return (
          <div
            className="xwiki-macro-block"
            data-macro-name={call.name}
            dangerouslySetInnerHTML={{ __html: html }}
          />
        );
      } else {
        return (
          <div
            className="xwiki-macro-block xwiki-macro-placeholder"
            data-macro-name={call.name}
          >
            macro:{call.name}
          </div>
        );
      }
    },
  },
);

export { XWikiMacroBlock };
