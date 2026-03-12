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

import { BlockNoteToUniAstConverter } from "../uniast/bn-to-uniast";
import { UniAstToBlockNoteConverter } from "../uniast/uniast-to-bn";
import { BlockType } from "@xwiki/platform-editors-blocknote-react";
import { ModelReferenceSerializer } from "@xwiki/platform-model-reference-api";
import {
  RemoteURLParser,
  RemoteURLSerializer,
} from "@xwiki/platform-model-remote-url-api";
import { describe, expect, test } from "vitest";
import { mock } from "vitest-mock-extended";
import type { UniAst } from "@xwiki/platform-uniast-api";

function init() {
  const remoteURLParser = mock<RemoteURLParser>();
  const modelReferenceSerializer = mock<ModelReferenceSerializer>();

  const bnToUniAstConverter = new BlockNoteToUniAstConverter(
    remoteURLParser,
    modelReferenceSerializer,
    [],
  );

  const remoteURLSerializer = mock<RemoteURLSerializer>();

  const uniAstToBnConverter = new UniAstToBlockNoteConverter(
    remoteURLSerializer,
  );

  return { bnToUniAstConverter, uniAstToBnConverter };
}

describe("Convert BlockNote AST to UniAST", () => {
  const { bnToUniAstConverter, uniAstToBnConverter } = init();

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
