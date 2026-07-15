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

import { depsContainerMock } from "./depsContainer.mock";
import { BlockNoteToUniAstConverter } from "../uniast/bn-to-uniast";
import { UniAstToBlockNoteConverter } from "../uniast/uniast-to-bn";
import { describe, expect, test } from "vitest";
import type { BlockType } from "@xwiki/platform-editors-blocknote-react";
import type { UniAst } from "@xwiki/platform-uniast-api";

describe("Convert BlockNote AST to UniAST", () => {
  const depsContainer = depsContainerMock();

  const bnToUniAstConverter = new BlockNoteToUniAstConverter(depsContainer, []);
  const uniAstToBnConverter = new UniAstToBlockNoteConverter(depsContainer);

  function testTwoWayConversion(expected: {
    startingFrom: BlockType[];
    convertsBackTo: Omit<BlockType, "id">[];
    withUniAst: UniAst;
  }) {
    const uniAst = bnToUniAstConverter.blocksToUniAst(expected.startingFrom);

    expect(uniAst).toStrictEqual(expected.withUniAst);

    if (uniAst instanceof Error) {
      throw new Error("Unreachable");
    }

    expect(uniAstToBnConverter.uniAstToBlockNote(uniAst)).toMatchObject(
      expected.convertsBackTo,
    );
  }

  test("conversion", () => {
    testTwoWayConversion({
      startingFrom: [
        {
          type: "divider",
          id: "",
          children: [],
          content: undefined,
          props: {},
        },
      ],
      convertsBackTo: [
        {
          type: "divider",
          children: [],
          content: undefined,
          props: {},
        },
      ],
      withUniAst: {
        blocks: [
          {
            type: "break",
          },
        ],
      },
    });
  });
});
