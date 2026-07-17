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
import {
  insertMacroInvocation,
  invocationToMacroCall,
  macroCallToInvocation,
} from "../../blocknote/utils";
import { describe, expect, test, vi } from "vitest";
import type {
  InlineMacroInvocation,
  MacroBlockInvocation,
} from "../../blocknote/utils";

describe("invocationToMacroCall / macroCallToInvocation", () => {
  test("a block invocation with a raw body round-trips through a MacroCall", () => {
    const invocation: MacroBlockInvocation = {
      kind: "block",
      id: "code",
      params: { language: "java", start: 1 },
      body: { type: "raw", content: "System.out.println();" },
    };

    const call = invocationToMacroCall(invocation);
    expect(call).toEqual({
      name: "code",
      parameters: { language: "java", start: 1 },
      content: "System.out.println();",
    });

    // The block/inline distinction is carried by the target type, not the call, so it must be provided on the way back.
    expect(macroCallToInvocation(call, "block")).toEqual(invocation);
  });

  test("an invocation without a body maps to a MacroCall without content", () => {
    const invocation: InlineMacroInvocation = {
      kind: "inline",
      id: "info",
      params: {},
      body: { type: "none" },
    };

    const call = invocationToMacroCall(invocation);
    expect(call).toEqual({ name: "info", parameters: {} });
    expect(call.content).toBeUndefined();

    expect(macroCallToInvocation(call, "inline")).toEqual(invocation);
  });
});

describe("insertMacroInvocation", () => {
  test("a block invocation replaces the selected block with an xwikiMacroBlock carrying the call", () => {
    const editor = {
      replaceBlocks: vi.fn(),
      insertInlineContent: vi.fn(),
    };
    const invocation: MacroBlockInvocation = {
      kind: "block",
      id: "info",
      params: { cssClass: "warning" },
      body: { type: "none" },
    };
    const selectedBlock = { id: "block-1" };

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    insertMacroInvocation(editor as any, invocation, selectedBlock);

    expect(editor.insertInlineContent).not.toHaveBeenCalled();
    expect(editor.replaceBlocks).toHaveBeenCalledTimes(1);
    const [replaced, inserted] = editor.replaceBlocks.mock.calls[0];
    expect(replaced).toEqual([selectedBlock]);
    expect(inserted).toEqual([
      {
        type: "xwikiMacroBlock",
        props: {
          call: JSON.stringify(invocationToMacroCall(invocation)),
          output: "[]",
        },
      },
    ]);
  });

  test("an inline invocation inserts an xwikiInlineMacro carrying the call", () => {
    const editor = {
      replaceBlocks: vi.fn(),
      insertInlineContent: vi.fn(),
    };
    const invocation: InlineMacroInvocation = {
      kind: "inline",
      id: "mention",
      params: { reference: "XWiki.Admin" },
      body: { type: "none" },
    };

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    insertMacroInvocation(editor as any, invocation);

    expect(editor.replaceBlocks).not.toHaveBeenCalled();
    expect(editor.insertInlineContent).toHaveBeenCalledTimes(1);
    const [content, options] = editor.insertInlineContent.mock.calls[0];
    expect(content).toEqual([
      {
        type: "xwikiInlineMacro",
        props: {
          call: JSON.stringify(invocationToMacroCall(invocation)),
          output: "[]",
        },
      },
    ]);
    expect(options).toEqual({ updateSelection: true });
  });
});
