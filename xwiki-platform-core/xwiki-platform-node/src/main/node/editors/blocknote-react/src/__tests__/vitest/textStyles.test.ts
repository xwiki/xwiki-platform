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
import { textStylesFeature } from "../../features/textStyles/textStyles";
import { FULL_SYNTAX } from "../playwright-ct/syntax.mock";
import { describe, expect, test } from "vitest";
import type { FeatureContext } from "../../features/editorFeature";

/**
 * @param subscript - whether the syntax of the edited content supports subscript
 * @param superscript - whether the syntax of the edited content supports superscript
 * @returns the context of an editor instance whose syntax supports the given text styles
 */
function context(subscript: boolean, superscript: boolean): FeatureContext {
  const syntax = structuredClone(FULL_SYNTAX);
  syntax.features.inlineContents.textStyles.subscript = subscript;
  syntax.features.inlineContents.textStyles.superscript = superscript;
  return { syntax, t: (key: string) => key } as FeatureContext;
}

describe("textStylesFeature", () => {
  test("adds the text styles that the syntax supports, with their toolbar buttons", () => {
    const contribution = textStylesFeature.contribute(context(true, false));

    expect(Object.keys(contribution?.schema?.styleSpecs ?? {})).toEqual([
      "subscript",
    ]);
    expect(
      contribution?.formattingToolbar?.items?.map((item) => item.key),
    ).toEqual(["subscriptStyleButton"]);
  });

  test("contributes nothing when the syntax supports neither subscript nor superscript", () => {
    expect(textStylesFeature.contribute(context(false, false))).toBeUndefined();
  });
});
