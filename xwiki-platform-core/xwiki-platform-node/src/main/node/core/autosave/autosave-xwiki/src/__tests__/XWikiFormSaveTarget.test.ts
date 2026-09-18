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

import { XWikiFormSaveTarget } from "../XWikiFormSaveTarget";
import { mockRequireJS } from "@xwiki/platform-test-requirejs";
import jquery from "jquery";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { DocumentVersion, XWikiFormSaveContext } from "../types";
import type { Saver } from "@xwiki/platform-autosave-api";
import type {
  XWikiDocument,
  XWikiDocumentRevision,
} from "@xwiki/platform-document-xwiki";
import type { RequireJSMock } from "@xwiki/platform-test-requirejs";

const FORM_HTML = `
  <form id="edit">
    <input name="comment" value="user summary" />
    <input type="submit" name="action_preview" value="Preview" />
    <input type="submit" name="action_saveandcontinue" value="Save" />
    <input type="submit" name="action_save" value="Save & View" />
  </form>
`;

let formIsValid: boolean;
let requirejs: RequireJSMock;
let restoreGlobals: () => void;
let versions: DocumentVersion[];
let document_: XWikiDocument;
let saver: Saver<XWikiFormSaveContext>;
let target: XWikiFormSaveTarget;

function $(selector: string): JQuery {
  return jquery(selector);
}

function getButton(name: string): HTMLInputElement {
  return document.querySelector<HTMLInputElement>(`input[name="${name}"]`)!;
}

/**
 * Stand in for actionButtons.js, which fires "xwiki:actions:save" on the button that was clicked, but only once the
 * form validates. When it does not, no such event is fired at all, which is how the saver detects that the save was
 * prevented.
 */
function fakeActionButtons(): void {
  for (const name of ["action_save", "action_saveandcontinue"]) {
    getButton(name).addEventListener("click", () => {
      if (formIsValid) {
        jquery(getButton(name)).trigger("xwiki:actions:save");
      }
    });
  }
}

/**
 * Make an element pass jQuery's ":visible" test. jsdom performs no layout, so every element reports a zero size
 * and would otherwise be considered hidden.
 */
function makeVisible(element: Element): void {
  Object.defineProperty(element, "offsetWidth", {
    value: 10,
    configurable: true,
  });
}

/**
 * Create the target, with the document, the saver and the environment it expects.
 */
async function createTarget(
  overrides: Partial<XWikiDocument> = {},
): Promise<XWikiFormSaveTarget> {
  document.body.innerHTML = FORM_HTML;
  makeVisible(getButton("action_preview"));
  fakeActionButtons();
  document_ = {
    version: "1.1",
    modified: 1000,
    isNew: false,
    documentReference: undefined,
    update: vi.fn(function (this: unknown, data: Record<string, unknown>) {
      Object.assign(document_, data);
      return document_;
    }),
    reload: vi.fn(async () => document_),
    getRevision: vi.fn(
      async (): Promise<XWikiDocumentRevision> => ({
        version: "1.1",
        modified: 500,
        author: "Admin",
        authorName: "Administrator",
      }),
    ),
    ...overrides,
  } as unknown as XWikiDocument;

  saver = {
    isSaving: vi.fn(() => false),
    save: vi.fn(async () => {}),
    getClientId: vi.fn(() => "alice"),
    whenSettled: vi.fn(async () => {}),
  } as unknown as Saver<XWikiFormSaveContext>;

  const created = new XWikiFormSaveTarget(saver, {
    document: document_,
    autoSaveVersionSummary: "Auto-saved",
    onCreateVersion: (version) => versions.push(version),
  });
  await created.initialize();
  return created;
}

beforeEach(() => {
  versions = [];
  formIsValid = true;
  requirejs = mockRequireJS();
  requirejs.set("jquery", jquery);
  restoreGlobals = installXWikiGlobal();
});

afterEach(() => {
  target?.dispose();
  requirejs.restore();
  restoreGlobals();
});

function installXWikiGlobal(): () => void {
  const globals = globalThis as Record<string, unknown>;
  const saved = globals.XWiki;
  globals.XWiki = {
    actionButtons: {
      AjaxSaveAndContinue: {
        prototype: {
          reloadEditor: (): void => {},
          maybeRedirect: (): boolean => false,
        },
      },
    },
  };
  return () => {
    globals.XWiki = saved;
  };
}

describe("XWikiFormSaveTarget", () => {
  describe("save priority", () => {
    it("ranks Save & View above Save & Continue, and both above auto-save", async () => {
      target = await createTarget();

      expect(target.getSavePriority({ button: getButton("action_save") })).toBe(
        3,
      );
      expect(
        target.getSavePriority({ button: getButton("action_saveandcontinue") }),
      ).toBe(2);
      expect(target.getSavePriority({})).toBe(1);
    });

    it("resolves the button performing a given save", async () => {
      target = await createTarget();

      expect(target.getSaveButton(false)).toBe(getButton("action_save"));
      expect(target.getSaveButton(true)).toBe(
        getButton("action_saveandcontinue"),
      );
    });
  });

  describe("save interval", () => {
    it("falls back on the default when the form doesn't specify one", async () => {
      target = await createTarget();

      expect(target.getSaveInterval()).toBeUndefined();
    });

    it("reads the number of seconds from the form, in milliseconds", async () => {
      target = await createTarget();
      $("#edit").attr("data-auto-save-interval", "5");

      expect(target.getSaveInterval()).toBe(5000);
    });

    it("reads the value again on each call, so that it can change during the editing session", async () => {
      target = await createTarget();
      $("#edit").attr("data-auto-save-interval", "5");
      expect(target.getSaveInterval()).toBe(5000);

      $("#edit").attr("data-auto-save-interval", "10");
      expect(target.getSaveInterval()).toBe(10000);
    });

    it.each(["", "   ", "soon", "0", "-5"])(
      "falls back on the default when the value is %j",
      async (value) => {
        target = await createTarget();
        $("#edit").attr("data-auto-save-interval", value);

        expect(target.getSaveInterval()).toBeUndefined();
      },
    );
  });

  describe("initialize", () => {
    it("hides the preview button and restores it when disposed", async () => {
      target = await createTarget();
      expect(getButton("action_preview").style.display).toBe("none");

      target.dispose();

      expect(getButton("action_preview").style.display).not.toBe("none");
    });

    it("reports the version the editing session starts from", async () => {
      target = await createTarget();
      await vi.waitFor(() => expect(versions).toHaveLength(1));

      expect(versions[0]).toMatchObject({
        number: "1.1",
        date: 500,
        author: { name: "Administrator" },
      });
    });

    it("reports no initial version for a new document", async () => {
      target = await createTarget({ isNew: true });

      expect(document_.getRevision).not.toHaveBeenCalled();
      expect(versions).toHaveLength(0);
    });

    it("routes the save requests of the user through the saver", async () => {
      target = await createTarget();

      $("#edit").trigger("xwiki:actions:beforeSave");

      expect(saver.save).toHaveBeenCalledOnce();
    });
  });
  describe("submit", () => {
    it("records the auto-save version summary and restores the one the user typed", async () => {
      target = await createTarget();
      const comment = document.querySelector<HTMLInputElement>(
        'input[name="comment"]',
      )!;
      let summaryWhileSaving: string | undefined;
      $("#edit").on("xwiki:actions:save", () => {
        summaryWhileSaving = comment.value;
      });

      const submitting = target.submit({});
      $("#edit").trigger("xwiki:document:saved", [{ newVersion: "1.2" }]);
      await submitting;

      expect(summaryWhileSaving).toBe("Auto-saved");
      expect(comment.value).toBe("user summary");
    });

    it("keeps the version summary of a manual save", async () => {
      target = await createTarget();
      const comment = document.querySelector<HTMLInputElement>(
        'input[name="comment"]',
      )!;
      let summaryWhileSaving: string | undefined;
      $("#edit").on("xwiki:actions:save", () => {
        summaryWhileSaving = comment.value;
      });

      const submitting = target.submit({ button: getButton("action_save") });
      $("#edit").trigger("xwiki:document:saved", [{ newVersion: "1.2" }]);
      await submitting;

      expect(summaryWhileSaving).toBe("user summary");
    });

    it("reports the created version", async () => {
      target = await createTarget();

      const submitting = target.submit({});
      $("#edit").trigger("xwiki:document:saved", [{ newVersion: "1.2" }]);

      await expect(submitting).resolves.toEqual({ version: "1.2" });
      expect(versions.at(-1)).toMatchObject({ number: "1.2", author: "alice" });
    });

    it("reports no version when the document was not modified", async () => {
      target = await createTarget();

      const submitting = target.submit({});
      $("#edit").trigger("xwiki:document:saved", [{ newVersion: "1.1" }]);

      await expect(submitting).resolves.toEqual({});
    });

    it("rejects when the form has invalid data", async () => {
      target = await createTarget();
      // This is how the form validation refuses a save: actionButtons.js never fires the event.
      formIsValid = false;

      await expect(target.submit({})).rejects.toThrow("Save prevented");
    });

    it("rejects when the save button is missing", async () => {
      target = await createTarget();
      getButton("action_saveandcontinue").remove();

      await expect(target.submit({})).rejects.toThrow(
        "The save button is disabled or missing",
      );
    });

    it("rejects when the save fails", async () => {
      target = await createTarget();

      const submitting = target.submit({});
      $("#edit").trigger("xwiki:document:saveFailed", [
        { response: { status: 500 } },
      ]);

      await expect(submitting).rejects.toThrow("Failed to save.");
    });

    it("gives up when no save result comes back", async () => {
      vi.useFakeTimers();
      try {
        target = await createTarget();

        // Assert on the rejection before advancing the timers, so that the handler is attached before it fires.
        const submitting = expect(target.submit({})).rejects.toThrow(
          "Timeout while waiting for the save result.",
        );
        await vi.advanceTimersByTimeAsync(120000);
        await submitting;
      } finally {
        vi.useRealTimers();
      }
    });
  });

  describe("merge conflict", () => {
    /**
     * Start a save and answer it with the conflict the server reports. The pending save is returned wrapped,
     * because awaiting a promise that resolves to another promise would wait for that inner one too.
     */
    async function submitIntoConflict(): Promise<{
      submitting: Promise<unknown>;
    }> {
      target = await createTarget();
      const submitting = target.submit({});
      // Attach a handler right away, so that a rejection raised while this test drives the DOM synchronously is
      // never seen as unhandled. The assertions attach their own handler afterwards.
      submitting.catch(() => {});
      $("#edit").trigger("xwiki:document:saveFailed", [
        { response: { status: 409 } },
      ]);
      return { submitting };
    }

    it("keeps waiting, and resolves once the conflict is resolved and the save goes through", async () => {
      const { submitting } = await submitIntoConflict();

      $("#edit").trigger("xwiki:document:saved", [{ newVersion: "1.3" }]);

      await expect(submitting).resolves.toEqual({ version: "1.3" });
    });

    it("rejects when the user discards the local changes", async () => {
      const { submitting } = await submitIntoConflict();

      $("#edit").trigger("xwiki:actions:reload");

      await expect(submitting).rejects.toThrow("Discarding local changes");
    });

    it("rejects when the user cancels the conflict modal", async () => {
      const { submitting } = await submitIntoConflict();
      const modal = globalThis.document.createElement("div");
      modal.id = "previewDiffModal";
      globalThis.document.body.append(modal);
      $(modal).data("action", "cancel");

      $(modal).trigger("hide.bs.modal");

      await expect(submitting).rejects.toThrow("Save canceled.");
    });

    it("refuses a new save while the conflict modal is open", async () => {
      target = await createTarget();
      const modal = globalThis.document.createElement("div");
      modal.id = "previewDiffModal";
      globalThis.document.body.append(modal);
      makeVisible(modal);

      await expect(target.submit({})).rejects.toThrow(
        "Merge conflict prevents save.",
      );
    });
  });

  describe("versions created by the other clients", () => {
    it("takes the highest remote version into account", async () => {
      target = await createTarget();

      target.onStatesChanged(
        { bob: { version: "1.3" }, carol: { version: "1.2" } },
        "alice",
      );

      expect(document_.version).toBe("1.3");
      expect(document_.isNew).toBe(false);
      expect(versions.at(-1)).toMatchObject({ number: "1.3", author: "bob" });
    });

    it("ignores a remote version that is not newer than the one we have", async () => {
      target = await createTarget();
      const count = versions.length;

      target.onStatesChanged({ bob: { version: "1.1" } }, "alice");

      expect(document_.version).toBe("1.1");
      expect(versions).toHaveLength(count);
    });

    it("does not report the version this client created itself", async () => {
      target = await createTarget();
      const count = versions.length;

      target.onStatesChanged({ alice: { version: "1.4" } }, "alice");

      expect(document_.version).toBe("1.4");
      expect(versions).toHaveLength(count);
    });
  });
});
