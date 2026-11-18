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
import { macrosFeature } from "../../features/macros/macros";
import { FULL_SYNTAX } from "../playwright-ct/syntax.mock";
import { BlockNoteEditor } from "@blocknote/core";
import { describe, expect, test } from "vitest";
import { mock } from "vitest-mock-extended";
import type { EditorInstance } from "../../editorInstance";
import type { FeatureContext } from "../../features/editorFeature";
import type { MacroWithUnknownParamsType } from "@xwiki/platform-macros-api";
import type { SyntaxConfig } from "@xwiki/platform-syntaxes-config";
import type { Container } from "inversify";

const sampleMacro: MacroWithUnknownParamsType = {
  renderAs: "block",
  infos: {
    id: "sample",
    name: "Sample Macro",
    description: "A sample macro",
    params: {},
    bodyType: "none",
    defaultParameters: {},
    paramsDescription: {},
  },
  render: () => [],
};

/**
 * @param syntax - the configuration of the syntax of the edited content
 * @param macroEditorBound - whether a macro editor is bound in the service container
 * @returns the context of an editor instance with the sample client-rendered macro
 */
function context(
  syntax: SyntaxConfig = FULL_SYNTAX,
  macroEditorBound = false,
): FeatureContext {
  const serviceContainer = mock<Container>();
  serviceContainer.get.mockReturnValue({ get: () => ({}) });
  serviceContainer.isBound.mockReturnValue(macroEditorBound);
  const options = {
    syntax: syntax.id,
    features: { macros: { list: [sampleMacro] } },
  };
  const editor = BlockNoteEditor.create({ schema: createBlockNoteSchema() });
  return {
    options,
    serviceContainer,
    syntax,
    t: (key: string) => key,
    getInstance: () =>
      ({ editor, options, serviceContainer }) as unknown as EditorInstance,
  } as unknown as FeatureContext;
}

describe("macrosFeature", () => {
  test("adds the macro calls to the schema, and the client-rendered macros to the slash menu", () => {
    const contribution = macrosFeature.contribute(context());

    expect(Object.keys(contribution?.schema?.blockSpecs ?? {})).toEqual([
      "xwikiMacroBlock",
      "Macro_sample",
    ]);
    expect(Object.keys(contribution?.schema?.inlineContentSpecs ?? {})).toEqual(
      ["xwikiInlineMacro"],
    );
    expect(contribution?.slashMenuItems?.().map((item) => item.title)).toEqual([
      "Sample Macro",
    ]);
  });

  test("adds the item opening the macro editor to the slash menu when one is bound", () => {
    const contribution = macrosFeature.contribute(context(FULL_SYNTAX, true));

    expect(contribution?.slashMenuItems?.().map((item) => item.title)).toEqual([
      "Sample Macro",
      "blocknote.slashMenu.macro.title",
    ]);
  });

  test("contributes nothing when the syntax doesn't support macros", () => {
    const syntax = structuredClone(FULL_SYNTAX);
    syntax.features.blocks.macros = false;
    syntax.features.inlineContents.macros = false;

    expect(macrosFeature.contribute(context(syntax))).toBeUndefined();
  });
});
