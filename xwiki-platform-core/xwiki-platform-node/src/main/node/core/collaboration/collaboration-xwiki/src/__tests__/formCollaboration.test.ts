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

// Test suites and test scenarios are naturally made of many statements.
/* eslint-disable max-statements */

import { joinFormCollaboration } from "../formCollaboration";
import { Saver } from "@xwiki/platform-autosave-api";
import { XWikiFormSaveTarget } from "@xwiki/platform-autosave-xwiki";
import { createYjsToolbar } from "@xwiki/platform-collaboration-toolbar";
import { EntityReference, EntityType } from "@xwiki/platform-xwiki-model-api";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type {
  DocumentVersion,
  XWikiFormSaveTargetConfig,
} from "@xwiki/platform-autosave-xwiki";
import type {
  Collaboration,
  CollaborationManager,
} from "@xwiki/platform-collaboration-api";
import type {
  YjsToolbar,
  YjsToolbarOptions,
} from "@xwiki/platform-collaboration-toolbar";
import type { XWikiDocument } from "@xwiki/platform-document-xwiki";
import type { DocumentReference } from "@xwiki/platform-model-api";

vi.mock("@xwiki/platform-autosave-api", () => ({
  Saver: vi.fn(
    class {
      transport: unknown;
      target: unknown;
      toBeReady = vi.fn(async () => {});
      constructor(
        _config: unknown,
        createTransport: (saver: unknown) => unknown,
        createTarget: (saver: unknown) => unknown,
      ) {
        this.transport = createTransport(this);
        this.target = createTarget(this);
      }
    },
  ),
}));

vi.mock("@xwiki/platform-autosave-xwiki", () => ({
  XWikiFormSaveTarget: vi.fn(
    class {
      constructor(
        _saver: unknown,
        public readonly config: XWikiFormSaveTargetConfig,
      ) {}
    },
  ),
}));

vi.mock("@xwiki/platform-collaboration-api", () => ({
  YjsAwarenessSaveTransport: vi.fn(class {}),
}));

vi.mock("@xwiki/platform-collaboration-toolbar", () => ({
  createYjsToolbar: vi.fn(),
}));

type TargetMock = {
  config: XWikiFormSaveTargetConfig;
};

function createDocument(): XWikiDocument {
  const wiki = new EntityReference("xwiki", EntityType.WIKI);
  const space = new EntityReference("Space", EntityType.SPACE, wiki);
  return {
    documentReference: new EntityReference("Page", EntityType.DOCUMENT, space),
    language: "fr",
  } as XWikiDocument;
}

function createToolbar(): YjsToolbar & {
  onCreateVersion: ReturnType<typeof vi.fn>;
  destroy: ReturnType<typeof vi.fn>;
} {
  return { onCreateVersion: vi.fn(), destroy: vi.fn() };
}

function createVersion(number: number): DocumentVersion {
  return { number: `${number}.1`, date: number, author: "1" };
}

/**
 * Lets the toolbar module load and the toolbar be created.
 */
async function flushPromises(): Promise<void> {
  for (let i = 0; i < 5; i++) {
    await new Promise((resolve) => setTimeout(resolve, 0));
  }
}

describe("joinFormCollaboration", () => {
  let collaboration: Collaboration;
  let collaborationManager: CollaborationManager & {
    join: ReturnType<typeof vi.fn>;
    leave: ReturnType<typeof vi.fn>;
  };
  let toolbar: ReturnType<typeof createToolbar>;

  beforeEach(() => {
    vi.clearAllMocks();
    collaboration = {} as Collaboration;
    collaborationManager = {
      join: vi.fn(async () => collaboration),
      leave: vi.fn(),
    };
    toolbar = createToolbar();
    vi.mocked(createYjsToolbar).mockImplementation(async () => toolbar);
    document.body.innerHTML = `<form id="edit"></form><form id="custom"></form>`;
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  function getTarget(): TargetMock {
    return vi.mocked(XWikiFormSaveTarget).mock
      .instances[0] as unknown as TargetMock;
  }

  function getToolbarOptions(): YjsToolbarOptions {
    return vi.mocked(createYjsToolbar).mock.calls[0][0];
  }

  it("joins the collaboration session of the edited document translation", async () => {
    const formCollaboration = await joinFormCollaboration({
      collaborationManager,
      document: createDocument(),
    });

    expect(formCollaboration.collaboration).toBe(collaboration);
    const reference: DocumentReference =
      collaborationManager.join.mock.calls[0][0];
    expect(reference.name).toBe("Page");
    expect(reference.space?.names).toEqual(["Space"]);
    expect(reference.space?.wiki?.name).toBe("xwiki");
    expect(reference.locale).toBe("fr");

    formCollaboration.leave();
    expect(collaborationManager.leave).toHaveBeenCalledWith(reference);
    // Leaving again has no effect.
    formCollaboration.leave();
    expect(collaborationManager.leave).toHaveBeenCalledTimes(1);
    // The auto-save was never started.
    expect(Saver).not.toHaveBeenCalled();
  });

  it("starts the auto-save and shows the toolbar once per session", async () => {
    const xwikiDocument = createDocument();
    const first = await joinFormCollaboration({
      collaborationManager,
      document: xwikiDocument,
      formId: "custom",
      autoSaveVersionSummary: "Auto-saved",
    });
    const second = await joinFormCollaboration({
      collaborationManager,
      document: createDocument(),
    });

    first.startAutoSave();
    second.startAutoSave();
    first.startAutoSave();
    expect(Saver).toHaveBeenCalledTimes(1);
    const saver = collaboration.saver as unknown as {
      toBeReady: ReturnType<typeof vi.fn>;
    };
    expect(saver).toBe(vi.mocked(Saver).mock.instances[0]);
    expect(saver.toBeReady).toHaveBeenCalled();
    expect(getTarget().config).toMatchObject({
      document: xwikiDocument,
      formId: "custom",
      autoSaveVersionSummary: "Auto-saved",
    });

    // A version is reported (e.g. the initial version) before the toolbar is created.
    getTarget().config.onCreateVersion!(createVersion(1));
    await flushPromises();

    expect(createYjsToolbar).toHaveBeenCalledTimes(1);
    const toolbarOptions = getToolbarOptions();
    expect(toolbarOptions.form).toBe(document.getElementById("custom"));
    expect(toolbarOptions.document).toBe(xwikiDocument);
    expect(toolbarOptions.collaboration).toBe(collaboration);
    expect(toolbarOptions.saver).toBe(saver);
    expect(toolbarOptions.versions).toEqual([createVersion(1)]);

    // The versions reported after the toolbar is created are forwarded.
    getTarget().config.onCreateVersion!(createVersion(2));
    expect(toolbar.onCreateVersion).toHaveBeenCalledWith(createVersion(2));

    // The toolbar is removed when the last editor leaves.
    first.leave();
    expect(toolbar.destroy).not.toHaveBeenCalled();
    second.leave();
    expect(toolbar.destroy).toHaveBeenCalledTimes(1);
    expect(collaborationManager.leave).toHaveBeenCalledTimes(2);
  });

  it("keeps only the most recent versions for the toolbar", async () => {
    const formCollaboration = await joinFormCollaboration({
      collaborationManager,
      document: createDocument(),
    });
    // Delay the toolbar creation.
    let resolveToolbar: (toolbar: YjsToolbar) => void = () => {};
    vi.mocked(createYjsToolbar).mockImplementation(
      () => new Promise((resolve) => (resolveToolbar = resolve)),
    );
    formCollaboration.startAutoSave();
    for (let i = 1; i <= 7; i++) {
      getTarget().config.onCreateVersion!(createVersion(i));
    }
    await flushPromises();

    // Uses the edit form of the standalone edit mode by default.
    expect(getToolbarOptions().form).toBe(document.getElementById("edit"));
    expect(getToolbarOptions().versions).toEqual(
      [3, 4, 5, 6, 7].map(createVersion),
    );
    resolveToolbar(toolbar);
  });

  it("removes the toolbar created after everyone left", async () => {
    const formCollaboration = await joinFormCollaboration({
      collaborationManager,
      document: createDocument(),
    });
    formCollaboration.startAutoSave();
    formCollaboration.leave();
    await flushPromises();

    expect(createYjsToolbar).toHaveBeenCalled();
    expect(toolbar.destroy).toHaveBeenCalledTimes(1);
  });

  it("doesn't start the auto-save after leaving", async () => {
    const formCollaboration = await joinFormCollaboration({
      collaborationManager,
      document: createDocument(),
    });
    formCollaboration.leave();
    formCollaboration.startAutoSave();

    expect(Saver).not.toHaveBeenCalled();
  });

  it("reports the failure to create the toolbar", async () => {
    const error = vi.spyOn(console, "error").mockImplementation(() => {});
    document.body.innerHTML = "";
    const formCollaboration = await joinFormCollaboration({
      collaborationManager,
      document: createDocument(),
    });
    formCollaboration.startAutoSave();
    await flushPromises();

    expect(createYjsToolbar).not.toHaveBeenCalled();
    expect(error).toHaveBeenCalledWith(
      "Failed to create the realtime toolbar.",
      new Error("Failed to find the edit form."),
    );
    // The auto-save is started anyway.
    expect(collaboration.saver).toBeDefined();
  });
});
