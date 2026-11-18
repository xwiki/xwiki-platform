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
// @vitest-environment jsdom

import { createBlockNoteSchema } from "../../blocknote";
import { xwikiRenderingFeature } from "../../features/xwikiRendering/xwikiRendering";
import { BlockNoteEditor } from "@blocknote/core";
import { beforeAll, describe, expect, test } from "vitest";
import type { FeatureContext } from "../../features/editorFeature";

describe("xwikiRenderingFeature", () => {
  test("adds the block types and text styles produced by the XWiki rendering", () => {
    const schema = xwikiRenderingFeature.contribute(
      {} as FeatureContext,
    )?.schema;

    expect(Object.keys(schema?.blockSpecs ?? {})).toEqual([
      "xwikiDefinitionListItem",
      "xwikiGroup",
      "xwikiRaw",
    ]);
    expect(Object.keys(schema?.styleSpecs ?? {})).toEqual([
      "xwikiVerbatim",
      "xwikiParameters",
    ]);
  });

  test("leaves them out of the editor schema when not contributed", () => {
    const schema = createBlockNoteSchema();

    expect(schema.blockSchema).not.toHaveProperty("xwikiGroup");
    expect(schema.styleSchema).not.toHaveProperty("xwikiParameters");
  });
});

describe("XWikiParametersStyle", () => {
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  let editor: BlockNoteEditor<any, any, any>;

  beforeAll(() => {
    editor = BlockNoteEditor.create({
      schema: createBlockNoteSchema(
        xwikiRenderingFeature.contribute({} as FeatureContext)?.schema,
      ),
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
    }) as BlockNoteEditor<any, any, any>;
  });

  test("applies xwikiParameters (JSON string form) as span attributes on export", () => {
    const html = editor.blocksToHTMLLossy([
      {
        type: "paragraph",
        content: [
          {
            type: "text",
            text: "x",
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            styles: { xwikiParameters: '{"class":"foo"}' } as any,
          },
        ],
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
      } as any,
    ]);

    const container = document.createElement("div");
    container.innerHTML = html;
    const span = container.querySelector<HTMLElement>("span.foo");
    expect(span).not.toBeNull();
    expect(span!.textContent).toBe("x");
  });
});
