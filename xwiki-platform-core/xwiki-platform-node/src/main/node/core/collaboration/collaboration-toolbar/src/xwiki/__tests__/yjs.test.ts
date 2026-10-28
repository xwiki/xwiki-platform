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

import { Toolbar } from "../../ui/Toolbar";
import { createXWikiToolbarEnvironment } from "../environment";
import { createYjsToolbar } from "../yjs";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { nextTick, ref } from "vue";
import type { ToolbarOptions } from "../../ui/types";
import type { YjsToolbarOptions } from "../yjs";
import type { Saver } from "@xwiki/platform-autosave-api";
import type { XWikiFormSaveContext } from "@xwiki/platform-autosave-xwiki";
import type {
  Collaboration,
  Collaborator,
} from "@xwiki/platform-collaboration-api";
import type { XWikiDocument } from "@xwiki/platform-document-xwiki";

vi.mock("../../ui/Toolbar", () => ({
  Toolbar: vi.fn(
    class {
      onConnectionStatusChange = vi.fn();
      onUserListChange = vi.fn();
      onSaveStatusChange = vi.fn();
      onCreateVersion = vi.fn();
      addWarning = vi.fn();
      removeWarning = vi.fn();
      destroy = vi.fn();
    },
  ),
}));

vi.mock("../environment", () => ({
  createXWikiToolbarEnvironment: vi.fn(async () => ({ name: "environment" })),
}));

vi.mock("../translations", () => ({
  loadXWikiToolbarTranslations: vi.fn(async () => ({
    name: "i18n",
    global: { t: (key: string) => `translated(${key})` },
  })),
}));

type ToolbarMock = {
  onConnectionStatusChange: ReturnType<typeof vi.fn>;
  onUserListChange: ReturnType<typeof vi.fn>;
  onSaveStatusChange: ReturnType<typeof vi.fn>;
  onCreateVersion: ReturnType<typeof vi.fn>;
  addWarning: ReturnType<typeof vi.fn>;
  removeWarning: ReturnType<typeof vi.fn>;
  destroy: ReturnType<typeof vi.fn>;
};

const alice: Collaborator = {
  id: "11",
  user: {
    name: "xwiki:XWiki.Alice",
    username: "Alice",
    avatar: "alice.png",
  },
  color: "red",
};

const bob: Collaborator = {
  id: "22",
  user: { name: "xwiki:XWiki.Bob" },
  color: "blue",
};

function createSaver() {
  const statusListeners = new Set<(status: number) => void>();
  return {
    status: undefined as number | undefined,
    statusListeners,
    save: vi.fn(async () => {}),
    getStatus() {
      return this.status;
    },
    addStatusListener(listener: (status: number) => void) {
      statusListeners.add(listener);
      return () => statusListeners.delete(listener);
    },
  };
}

describe("createYjsToolbar", () => {
  let form: HTMLFormElement;
  let collaboration: Collaboration;
  let saver: ReturnType<typeof createSaver>;
  let options: YjsToolbarOptions;

  beforeEach(() => {
    vi.mocked(Toolbar).mockClear();
    form = document.createElement("form");
    collaboration = {
      connectionStatus: ref(1),
      collaborators: ref([alice]),
      collaborator: alice,
    } as unknown as Collaboration;
    saver = createSaver();
    options = {
      form,
      document: { version: "1.1" } as XWikiDocument,
      collaboration,
      saver: saver as unknown as Saver<XWikiFormSaveContext>,
    };
  });

  function getToolbarMock(index = 0): ToolbarMock {
    return vi.mocked(Toolbar).mock.instances[index] as unknown as ToolbarMock;
  }

  function getToolbarOptions(index = 0): ToolbarOptions {
    return vi.mocked(Toolbar).mock.calls[index][0];
  }

  it("creates the toolbar for the edit form", async () => {
    await createYjsToolbar(options);

    const toolbarOptions = getToolbarOptions();
    expect(toolbarOptions.form).toBe(form);
    expect(toolbarOptions.document).toBe(options.document);
    expect(toolbarOptions.environment).toEqual({ name: "environment" });
    expect(toolbarOptions.i18n).toMatchObject({ name: "i18n" });
    // Leaving the collaboration session and selecting a user are not supported.
    expect(toolbarOptions.session.leave).toBeUndefined();
    expect(toolbarOptions.session.selectUser).toBeUndefined();
    // The document is saved independently of the collaboration server.
    expect(toolbarOptions.session.canSaveWhileDisconnected).toBe(true);

    // Saving tells the save target whether to leave the edit mode, so that it clicks the right save button.
    await toolbarOptions.session.save(true);
    expect(saver.save).toHaveBeenCalledWith({ action: "saveAndContinue" });
    await toolbarOptions.session.save();
    expect(saver.save).toHaveBeenLastCalledWith({ action: "saveAndView" });
  });

  it("follows the connection status and the collaborators", async () => {
    await createYjsToolbar(options);
    const toolbar = getToolbarMock();

    expect(toolbar.onConnectionStatusChange).toHaveBeenCalledWith(1, "11");
    expect(toolbar.onUserListChange).toHaveBeenCalledWith([
      {
        id: "11",
        reference: "xwiki:XWiki.Alice",
        name: "Alice",
        avatar: "alice.png",
      },
    ]);

    collaboration.connectionStatus.value = 2;
    collaboration.collaborators.value = [alice, bob];
    await nextTick();
    expect(toolbar.onConnectionStatusChange).toHaveBeenLastCalledWith(2, "11");
    // The user name falls back on the user reference.
    expect(toolbar.onUserListChange).toHaveBeenLastCalledWith([
      expect.objectContaining({ id: "11" }),
      {
        id: "22",
        reference: "xwiki:XWiki.Bob",
        name: "xwiki:XWiki.Bob",
        avatar: undefined,
      },
    ]);
  });

  it("follows the warnings", async () => {
    collaboration.warnings = ref([]);
    await createYjsToolbar(options);
    const toolbar = getToolbarMock();
    expect(toolbar.addWarning).not.toHaveBeenCalled();

    collaboration.warnings.value = ["sessionReplaced"];
    await nextTick();
    expect(toolbar.addWarning).toHaveBeenCalledExactlyOnceWith({
      id: "sessionReplaced",
      title: "translated(realtime.sessionReplacedWarning)",
      message: "translated(realtime.sessionReplacedWarning.message)",
    });

    collaboration.warnings.value = [];
    await nextTick();
    expect(toolbar.removeWarning).toHaveBeenCalledExactlyOnceWith(
      "sessionReplaced",
    );
  });

  it("follows the save status", async () => {
    saver.status = 2;
    await createYjsToolbar(options);
    const toolbar = getToolbarMock();
    expect(toolbar.onSaveStatusChange).toHaveBeenCalledWith(2);

    saver.statusListeners.forEach((listener) => listener(0));
    expect(toolbar.onSaveStatusChange).toHaveBeenLastCalledWith(0);
  });

  it("doesn't report an unknown save status", async () => {
    await createYjsToolbar(options);
    expect(getToolbarMock().onSaveStatusChange).not.toHaveBeenCalled();
  });

  it("shows the versions, resolving their authors", async () => {
    options.versions = [
      {
        number: "1.1",
        date: 1,
        author: { reference: "xwiki:XWiki.Carol", name: "Carol" },
      },
      { number: "2.1", date: 2, author: "11" },
    ];
    const yjsToolbar = await createYjsToolbar(options);
    const toolbar = getToolbarMock();

    expect(toolbar.onCreateVersion.mock.calls).toEqual([
      [
        {
          number: "1.1",
          date: 1,
          author: { reference: "xwiki:XWiki.Carol", name: "Carol" },
        },
      ],
      [
        {
          number: "2.1",
          date: 2,
          author: {
            id: "11",
            reference: "xwiki:XWiki.Alice",
            name: "Alice",
            avatar: "alice.png",
          },
        },
      ],
    ]);

    // The author left the collaboration session.
    yjsToolbar.onCreateVersion({ number: "3.1", date: 3, author: "33" });
    expect(toolbar.onCreateVersion).toHaveBeenLastCalledWith({
      number: "3.1",
      date: 3,
      author: "33",
    });

    // The author is unknown.
    yjsToolbar.onCreateVersion({ number: "4.1", date: 4 });
    expect(toolbar.onCreateVersion).toHaveBeenLastCalledWith({
      number: "4.1",
      date: 4,
      author: "",
    });
  });

  it("stops following the collaboration session when destroyed", async () => {
    const yjsToolbar = await createYjsToolbar(options);
    const toolbar = getToolbarMock();
    toolbar.onConnectionStatusChange.mockClear();
    toolbar.onUserListChange.mockClear();

    yjsToolbar.destroy();
    expect(toolbar.destroy).toHaveBeenCalled();
    expect(saver.statusListeners.size).toBe(0);

    collaboration.connectionStatus.value = 0;
    collaboration.collaborators.value = [];
    await nextTick();
    expect(toolbar.onConnectionStatusChange).not.toHaveBeenCalled();
    expect(toolbar.onUserListChange).not.toHaveBeenCalled();

    // A new toolbar can be created for the same edit form.
    await createYjsToolbar(options);
    expect(Toolbar).toHaveBeenCalledTimes(2);
  });

  it("creates at most one toolbar per edit form", async () => {
    await createYjsToolbar(options);
    await expect(createYjsToolbar(options)).rejects.toThrow(
      "The edit form already has a realtime toolbar.",
    );
    expect(Toolbar).toHaveBeenCalledTimes(1);
  });

  it("allows retrying after failing to create the toolbar", async () => {
    vi.mocked(createXWikiToolbarEnvironment).mockRejectedValueOnce(
      new Error("Failed to load the environment."),
    );
    await expect(createYjsToolbar(options)).rejects.toThrow(
      "Failed to load the environment.",
    );

    await createYjsToolbar(options);
    expect(Toolbar).toHaveBeenCalledTimes(1);
  });
});
