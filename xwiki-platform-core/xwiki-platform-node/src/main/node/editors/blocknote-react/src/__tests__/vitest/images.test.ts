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
import { imagesFeature } from "../../features/images/images";
import { FULL_SYNTAX } from "../playwright-ct/syntax.mock";
import { describe, expect, test, vi } from "vitest";
import type { FeatureContext } from "../../features/editorFeature";

/**
 * @param basicImages - whether the syntax of the edited content supports images
 * @returns the context of an editor instance whose syntax supports images or not
 */
function context(basicImages: boolean): FeatureContext {
  const syntax = structuredClone(FULL_SYNTAX);
  syntax.features.blocks.images.basicImages = basicImages;
  return { syntax, getInstance: vi.fn() } as unknown as FeatureContext;
}

describe("imagesFeature", () => {
  test("adds the image slash menu item, toolbar and menus when the syntax supports images", () => {
    const contribution = imagesFeature.contribute(context(true));

    expect(contribution?.slashMenuItems).toBeTypeOf("function");
    expect(
      Object.keys(contribution?.formattingToolbar?.blockToolbars ?? {}),
    ).toEqual(["image"]);
    expect(contribution?.components).toBeDefined();
  });

  test("contributes nothing when the syntax doesn't support images", () => {
    expect(imagesFeature.contribute(context(false))).toBeUndefined();
  });
});
