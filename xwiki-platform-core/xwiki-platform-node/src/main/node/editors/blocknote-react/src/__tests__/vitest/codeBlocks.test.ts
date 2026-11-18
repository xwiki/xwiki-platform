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
import { codeBlocksFeature } from "../../features/codeBlocks/codeBlocks";
import { FULL_SYNTAX } from "../playwright-ct/syntax.mock";
import { syntaxHighlighter } from "@blocknote/code-block";
import { describe, expect, test } from "vitest";
import type { FeatureContext } from "../../features/editorFeature";

/**
 * @param basicCodeBlocks - whether the syntax of the edited content supports code blocks
 * @returns the context of an editor instance whose syntax supports code blocks or not
 */
function context(basicCodeBlocks: boolean): FeatureContext {
  const syntax = structuredClone(FULL_SYNTAX);
  syntax.features.blocks.code.basicCodeBlocks = basicCodeBlocks;
  return { syntax } as FeatureContext;
}

describe("codeBlocksFeature", () => {
  test("highlights the code blocks when the syntax supports code blocks", () => {
    expect(codeBlocksFeature.contribute(context(true))).toEqual({
      blockNoteExtensions: [syntaxHighlighter],
    });
  });

  test("contributes nothing when the syntax doesn't support code blocks", () => {
    expect(codeBlocksFeature.contribute(context(false))).toBeUndefined();
  });
});
