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
import { DefaultMacroEditor } from "./DefaultMacroEditor";
import { toCristalEntityReference } from "@xwiki/platform-model-xwiki";
import { EntityType, Model } from "@xwiki/platform-xwiki-model-api";
import { beforeEach, describe, expect, it } from "vitest";

describe("DefaultMacroEditor", () => {
  let macroWizardCalls;
  let macroWizardResult;
  let macroEditor;

  const instance = {
    options: {
      syntax: "xwiki/2.1",
      sourceDocumentReference: toCristalEntityReference(
        Model.resolve("wiki:Space.Source", EntityType.DOCUMENT),
      ),
    },
  };

  beforeEach(() => {
    macroWizardCalls = [];
    macroWizardResult = () =>
      Promise.resolve({
        name: "info",
        parameters: { title: { name: "title", value: "Note" } },
        content: "Some content",
        inline: false,
      });
    macroEditor = new DefaultMacroEditor({
      insertOrUpdate(macroCall, options) {
        macroWizardCalls.push({ macroCall, options });
        return macroWizardResult();
      },
    });

    global.XWiki = {
      currentDocument: {
        documentReference: Model.resolve(
          "wiki:Space.Current",
          EntityType.DOCUMENT,
        ),
      },
    };
  });

  describe("insert", () => {
    it("renders the macro in the syntax of the edited content, for its source document", async () => {
      const inserted = await macroEditor.insert(instance, {
        kind: "inline",
        id: null,
        params: { title: 1 },
        body: { type: "raw", content: "Prefilled" },
      });

      expect(macroWizardCalls).toHaveLength(1);
      const { macroCall, options } = macroWizardCalls[0];
      expect(macroCall).toEqual({
        inline: true,
        parameters: { title: "1" },
        content: "Prefilled",
      });
      expect(options.syntax).toBe("xwiki/2.1");
      expect(Model.serialize(options.sourceDocumentReference)).toBe(
        "wiki:Space.Source",
      );
      expect(options.inlineParametersSyntax).toBe("blocknote/1.0");
      expect(inserted).toEqual({
        kind: "block",
        id: "info",
        params: { title: "Note" },
        body: { type: "raw", content: "Some content" },
      });
    });

    it("uses the current document when the edited content has no source document", async () => {
      await macroEditor.insert(
        { options: { syntax: "xwiki/2.1" } },
        { kind: "block", id: null, params: null, body: { type: "none" } },
      );

      expect(
        Model.serialize(macroWizardCalls[0].options.sourceDocumentReference),
      ).toBe("wiki:Space.Current");
    });
  });

  describe("edit", () => {
    it("edits the macro call, with its in-place editable parameters and content", async () => {
      const content = [{ type: "text", text: "Editable", styles: {} }];
      const edited = await macroEditor.edit(instance, {
        kind: "block",
        id: "box",
        params: { Title: "Old", count: 2 },
        body: { type: "inlineContents", content },
      });

      const { macroCall, options } = macroWizardCalls[0];
      expect(macroCall).toEqual({
        name: "box",
        // The parameters that aren't plain text are edited in-place.
        parameters: { title: { name: "Title", value: "Old" } },
        inline: false,
      });
      expect(options.inlineParameters).toEqual({
        count: JSON.stringify([2]),
        $content: JSON.stringify(content),
      });
      expect(options.syntax).toBe("xwiki/2.1");
      expect(Model.serialize(options.sourceDocumentReference)).toBe(
        "wiki:Space.Source",
      );
      expect(edited.id).toBe("info");
    });

    it("returns nothing when the user cancels", async () => {
      // The Macro Wizard rejects without a reason when the user cancels.
      macroWizardResult = () => Promise.reject();

      await expect(
        macroEditor.edit(instance, {
          kind: "block",
          id: "box",
          params: {},
          body: { type: "none" },
        }),
      ).resolves.toBeUndefined();
    });

    it("fails when the Macro Wizard fails", async () => {
      macroWizardResult = () => Promise.reject(new Error("Failed to load"));

      await expect(
        macroEditor.edit(instance, {
          kind: "block",
          id: "box",
          params: {},
          body: { type: "none" },
        }),
      ).rejects.toThrow("Failed to load");
    });
  });
});
