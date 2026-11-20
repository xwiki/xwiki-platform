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
import { setMacroOutput } from "../../features/macros/macroMetadata";
import {
  MacroOutputUpdater,
  getMacroCalls,
} from "../../features/macros/macroOutputUpdate";
import { createBlockMetadataStore } from "../../misc/blockMetadata";
import { afterEach, beforeEach, describe, expect, test, vi } from "vitest";
import type { EditorInstance } from "../../editorInstance";
import type { BlockMetadataStore } from "../../misc/blockMetadata";
import type { MacroRenderer } from "../../services/macroRenderer";

const infoCall = { name: "info", parameters: {}, content: "Information" };
const errorCall = { name: "error", parameters: {}, content: "Error" };

/**
 * @param id - the block id
 * @param call - the macro call
 * @returns a block macro, as found in the editor document (the call is a JSON string)
 */
function macroBlock(id: string, call: object): object {
  return {
    id,
    type: "xwikiMacroBlock",
    props: { call: JSON.stringify(call) },
    children: [],
  };
}

describe("getMacroCalls", () => {
  test("collects the server-rendered macro calls of the content, in document order", () => {
    const nestedCall = {
      name: "box",
      parameters: {},
      content: [
        { type: "xwikiMacroBlock", props: { call: errorCall, id: "n1" } },
      ],
    };
    const content = [
      macroBlock("b1", nestedCall),
      {
        id: "p1",
        type: "paragraph",
        props: {},
        content: [
          { type: "text", text: "before ", styles: {} },
          {
            type: "xwikiInlineMacro",
            props: { call: JSON.stringify(infoCall), id: "i1" },
          },
          // Without an id, the output of the macro call can't be kept.
          {
            type: "xwikiInlineMacro",
            props: { call: JSON.stringify(infoCall), id: "" },
          },
        ],
        children: [macroBlock("b2", infoCall)],
      },
      {
        id: "t1",
        type: "table",
        props: {},
        content: {
          type: "tableContent",
          rows: [
            {
              cells: [
                {
                  type: "tableCell",
                  content: [
                    {
                      type: "xwikiInlineMacro",
                      props: { call: JSON.stringify(errorCall), id: "i2" },
                    },
                  ],
                },
              ],
            },
          ],
        },
        children: [],
      },
    ];

    expect(getMacroCalls(content)).toEqual([
      { id: "b1", call: JSON.stringify(nestedCall) },
      // The call of a nested macro is an object, part of the call of its parent.
      { id: "n1", call: JSON.stringify(errorCall) },
      { id: "i1", call: JSON.stringify(infoCall) },
      { id: "b2", call: JSON.stringify(infoCall) },
      { id: "i2", call: JSON.stringify(errorCall) },
    ]);
  });

  test("skips the macro calls that can't be parsed", () => {
    const content = [
      {
        id: "b1",
        type: "xwikiMacroBlock",
        props: { call: "{not JSON" },
        children: [],
      },
    ];

    expect(getMacroCalls(content)).toEqual([]);
  });
});

/**
 * @param content - the edited content
 * @param metadata - the metadata of the editor instance
 * @returns an editor instance editing the given content, as far as the updater is concerned
 */
function createInstance(
  content: object[],
  metadata: BlockMetadataStore = createBlockMetadataStore(),
): EditorInstance {
  return { editor: { document: content }, metadata } as never;
}

/**
 * @returns a renderer whose renderings complete only when asked to, and the function completing the next one
 */
function createRenderer(): {
  renderer: MacroRenderer;
  complete: () => Promise<void>;
} {
  const pending: (() => void)[] = [];
  const renderer = {
    render: vi.fn(() => new Promise<void>((resolve) => pending.push(resolve))),
  };
  return {
    renderer,
    complete: async () => {
      pending.shift()?.();
      // Let the updater handle the end of the rendering.
      await new Promise((resolve) => setTimeout(resolve));
    },
  };
}

describe("MacroOutputUpdater", () => {
  test("renders the content when the output of a macro call is missing", () => {
    const instance = createInstance([macroBlock("b1", infoCall)]);
    const { renderer } = createRenderer();

    new MacroOutputUpdater(instance, renderer).update();

    expect(renderer.render).toHaveBeenCalledWith(instance);
  });

  test("doesn't render the content when the output of the macro calls is up to date, even if empty", () => {
    const metadata = createBlockMetadataStore();
    setMacroOutput(metadata, "b1", JSON.stringify(infoCall), ["output"]);
    setMacroOutput(metadata, "b2", JSON.stringify(errorCall), []);
    const instance = createInstance(
      [macroBlock("b1", infoCall), macroBlock("b2", errorCall)],
      metadata,
    );
    const { renderer } = createRenderer();

    new MacroOutputUpdater(instance, renderer).update();

    expect(renderer.render).not.toHaveBeenCalled();
  });

  test("renders the content when a macro call is modified after its output was rendered", () => {
    const metadata = createBlockMetadataStore();
    setMacroOutput(metadata, "b1", JSON.stringify(infoCall), ["output"]);
    const instance = createInstance(
      [macroBlock("b1", { ...infoCall, content: "Modified" })],
      metadata,
    );
    const { renderer } = createRenderer();

    new MacroOutputUpdater(instance, renderer).update();

    expect(renderer.render).toHaveBeenCalledTimes(1);
  });
});

describe("MacroOutputUpdater (while rendering)", () => {
  test("doesn't render again a macro call that was rendered without output, until it's modified", async () => {
    const content = [macroBlock("b1", infoCall)];
    const { renderer, complete } = createRenderer();
    const updater = new MacroOutputUpdater(createInstance(content), renderer);

    updater.update();
    // The rendering fails to produce the output of the macro call.
    await complete();
    updater.update();
    expect(renderer.render).toHaveBeenCalledTimes(1);

    content[0] = macroBlock("b1", { ...infoCall, content: "Modified" });
    updater.update();
    expect(renderer.render).toHaveBeenCalledTimes(2);
  });

  test("renders the content again when a macro call is modified while the content is being rendered", async () => {
    const content = [macroBlock("b1", infoCall)];
    const instance = createInstance(content);
    const { renderer, complete } = createRenderer();
    const updater = new MacroOutputUpdater(instance, renderer);

    updater.update();
    // Modified (e.g. by another user editing in realtime) while being rendered.
    content.push(macroBlock("b2", errorCall));
    updater.update();
    expect(renderer.render).toHaveBeenCalledTimes(1);

    await complete();
    expect(renderer.render).toHaveBeenCalledTimes(2);
  });
});

describe("MacroOutputUpdater (scheduled)", () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  test("renders the content once the edited content stops changing", () => {
    const { renderer } = createRenderer();
    const updater = new MacroOutputUpdater(
      createInstance([macroBlock("b1", infoCall)]),
      renderer,
      100,
    );

    updater.scheduleUpdate();
    vi.advanceTimersByTime(50);
    updater.scheduleUpdate();
    vi.advanceTimersByTime(50);
    expect(renderer.render).not.toHaveBeenCalled();

    vi.advanceTimersByTime(50);
    expect(renderer.render).toHaveBeenCalledTimes(1);
  });

  test("doesn't render the content once destroyed", () => {
    const { renderer } = createRenderer();
    const updater = new MacroOutputUpdater(
      createInstance([macroBlock("b1", infoCall)]),
      renderer,
    );

    updater.destroy();
    updater.update();

    expect(renderer.render).not.toHaveBeenCalled();
  });
});
