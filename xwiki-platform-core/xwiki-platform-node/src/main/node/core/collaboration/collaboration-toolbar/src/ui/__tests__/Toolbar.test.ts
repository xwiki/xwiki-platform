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

import { Toolbar } from "../Toolbar";
import { flushPromises } from "@vue/test-utils";
import { mockRequireJS } from "@xwiki/platform-test-requirejs";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { nextTick } from "vue";
import { createI18n } from "vue-i18n";
import type { ToolbarEnvironment, ToolbarSession } from "../types";
import type { XWikiDocument } from "@xwiki/platform-document-xwiki";
import type { RequireJSMock } from "@xwiki/platform-test-requirejs";

const modalListeners = new Map<Element, Map<string, Set<() => void>>>();

function notify(modal: Element, eventName: string) {
  modalListeners
    .get(modal)
    ?.get(eventName)
    ?.forEach((listener) => listener());
}

function showModal(modal: Element) {
  notify(modal, "show.bs.modal");
  modal.classList.add("in");
  notify(modal, "shown.bs.modal");
}

function hideModal(modal: Element) {
  modal.classList.remove("in");
  notify(modal, "hidden.bs.modal");
}

/**
 * The actions performed on the Bootstrap popovers (which are a jQuery plugin), in order.
 */
const popoverActions: [Element, string][] = [];

/**
 * Simulates the Bootstrap modals (which are a jQuery plugin) by toggling the "in" CSS class and notifying listeners.
 */
function jQuery(element: Element) {
  return {
    popover(action = "init") {
      popoverActions.push([element, action]);
      if (action === "hide") {
        notify(element, "hide.bs.popover");
      }
    },
    one(eventName: string, listener: () => void) {
      const onceListener = () => {
        this.off(eventName, onceListener);
        listener();
      };
      this.on(eventName, onceListener);
    },
    modal(action: "show" | "hide") {
      if (action === "show") {
        showModal(element);
      } else {
        hideModal(element);
      }
    },
    on(eventName: string, listener: () => void) {
      const listeners = modalListeners.get(element) ?? new Map();
      modalListeners.set(element, listeners);
      const eventListeners = listeners.get(eventName) ?? new Set();
      listeners.set(eventName, eventListeners);
      eventListeners.add(listener);
    },
    off(eventName: string, listener: () => void) {
      modalListeners.get(element)?.get(eventName)?.delete(listener);
    },
  };
}

function createEnvironment(
  overrides: Partial<ToolbarEnvironment> = {},
): ToolbarEnvironment {
  return {
    getSaveOptions: () => ({
      editComment: { enabled: true, mandatory: false },
      minorEdit: true,
    }),
    defaultAvatarURL: "noavatar.png",
    icons: {
      check: '<span class="icon-check"></span>',
      clock: "",
      warning: '<span class="icon-warning"></span>',
    },
    formatDate: (date) => `date-${date}`,
    getUserURL: (reference) => `/profile/${reference}`,
    getVersionURL: (xwikiDocument, version) =>
      `/view/${xwikiDocument.version}?rev=${version}`,
    getVersionContentURL: (xwikiDocument, version) =>
      `/content/${xwikiDocument.version}?rev=${version}`,
    fetchChanges: vi.fn(async () => '<div class="diff-group">diff</div>'),
    setChangeSummary: vi.fn(),
    ...overrides,
  };
}

/**
 * @returns the environment overrides that configure the version summary (edit comment)
 */
function editComment(
  enabled: boolean,
  mandatory: boolean,
): Partial<ToolbarEnvironment> {
  return {
    getSaveOptions: () => ({
      editComment: { enabled, mandatory },
      minorEdit: true,
    }),
  };
}

/**
 * @returns the edited document, with only the fields used by the toolbar
 */
function createDocument(isNew = false): XWikiDocument {
  return { version: "1.1", isNew } as XWikiDocument;
}

/**
 * @returns the translations of the toolbar; the missing translations are displayed as translation keys
 */
function createTranslations() {
  return createI18n({
    legacy: false,
    locale: "en",
    messages: {
      en: {
        "core.document.modificationWithVersion": "Version {0} by {1} on {2}",
      },
    },
    missingWarn: false,
    fallbackWarn: false,
  });
}

function createSession(): ToolbarSession & {
  save: ReturnType<typeof vi.fn>;
  leave: ReturnType<typeof vi.fn>;
  selectUser: ReturnType<typeof vi.fn>;
} {
  return {
    save: vi.fn(async () => {}),
    leave: vi.fn(),
    selectUser: vi.fn(),
  };
}

function query<T extends Element = HTMLElement>(selector: string): T {
  const element = document.querySelector<T>(selector);
  expect(element, selector).not.toBeNull();
  return element!;
}

function queryAll(selector: string): HTMLElement[] {
  return [...document.querySelectorAll<HTMLElement>(selector)];
}

/**
 * Clicks the given element and prevents the default behavior (e.g. following a link) once the click has been handled
 * by the element listeners.
 *
 * @returns whether the click listeners of the element have prevented the default behavior
 */
function clickAndCheckPrevented(element: HTMLElement): boolean {
  let prevented = false;
  const listener = (event: Event) => {
    prevented = event.defaultPrevented;
    event.preventDefault();
  };
  document.addEventListener("click", listener);
  element.click();
  document.removeEventListener("click", listener);
  return prevented;
}

function users(count: number) {
  return Array.from({ length: count }, (_, index) => ({
    id: `id${index}`,
    reference: `XWiki.User${index}`,
    name: `First${index} Last${index}`,
    avatar: index % 2 ? `avatar${index}.png` : undefined,
  }));
}

describe("Toolbar", () => {
  let oldToolbar: HTMLElement;
  let toolbar: Toolbar | undefined;
  let requireJS: RequireJSMock;

  beforeEach(() => {
    requireJS = mockRequireJS({ jquery: jQuery });
    document.body.innerHTML = `
      <form id="edit">
        <div class="bottombuttons sticky-buttons">
          <div class="buttons">
            <input name="comment" />
            <input type="checkbox" name="minorEdit" />
          </div>
        </div>
      </form>`;
    oldToolbar = query(".buttons");
  });

  afterEach(() => {
    toolbar?.destroy();
    toolbar = undefined;
    modalListeners.clear();
    popoverActions.length = 0;
    requireJS.restore();
  });

  async function createToolbar(
    session: ToolbarSession = createSession(),
    environment: ToolbarEnvironment = createEnvironment(),
    xwikiDocument: XWikiDocument = createDocument(),
  ) {
    toolbar = new Toolbar({
      form: query("form#edit"),
      document: xwikiDocument,
      session,
      environment,
      i18n: createTranslations(),
    });
    // Wait for the modal event listeners to be registered (jQuery is loaded asynchronously).
    await flushPromises();
    return toolbar;
  }

  it("replaces the standard edit toolbar", async () => {
    const realtimeToolbar = await createToolbar();

    expect(oldToolbar.hidden).toBe(true);
    const toolbarElement = query(".realtime-edit-toolbar");
    expect(toolbarElement.nextElementSibling).toBe(oldToolbar);
    expect(toolbarElement.classList).toContain("buttons");
    expect(toolbarElement.dataset.userId).toBe("");
    expect(realtimeToolbar.getForm()).toBe(query("form#edit"));
    expect(
      query<HTMLInputElement>(
        ".realtime-edit-toolbar input[name=preventEmptyRevision]",
      ).value,
    ).toBe("true");

    // Connecting.
    const connectionStatus = query(".realtime-connection-status");
    expect(connectionStatus.dataset.status).toBe("cleaning");
    expect(connectionStatus.textContent!.trim()).toBe("realtime.connecting");
    expect(connectionStatus.querySelector(".realtime-spinner")).not.toBeNull();
    expect(query<HTMLButtonElement>(".realtime-action-done").disabled).toBe(
      true,
    );
    // The save status is unknown.
    expect(query(".realtime-save-status").dataset.status).toBeUndefined();
  });

  it("updates the connection status", async () => {
    const realtimeToolbar = await createToolbar();
    realtimeToolbar.onUserListChange(users(2));

    realtimeToolbar.onConnectionStatusChange(2, "myId");
    await flushPromises();
    expect(query(".realtime-edit-toolbar").dataset.userId).toBe("myId");
    expect(query(".realtime-connection-status").dataset.status).toBe("clean");
    expect(query<HTMLButtonElement>(".realtime-action-done").disabled).toBe(
      false,
    );
    expect(queryAll(".realtime-users .realtime-user")).toHaveLength(2);

    realtimeToolbar.onConnectionStatusChange(0);
    await flushPromises();
    expect(query(".realtime-edit-toolbar").dataset.userId).toBe("myId");
    expect(query(".realtime-connection-status").textContent!.trim()).toBe(
      "realtime.disconnected",
    );
    expect(query<HTMLButtonElement>(".realtime-action-done").disabled).toBe(
      true,
    );
    // The user list is cleared when disconnected.
    expect(queryAll(".realtime-users .realtime-user")).toHaveLength(0);
  });

  it("allows saving while disconnected when the session can save without the connection", async () => {
    const realtimeToolbar = await createToolbar({
      ...createSession(),
      canSaveWhileDisconnected: true,
    });

    realtimeToolbar.onConnectionStatusChange(0);
    await flushPromises();
    expect(query(".realtime-connection-status").textContent!.trim()).toBe(
      "realtime.disconnected",
    );
    expect(query<HTMLButtonElement>(".realtime-action-done").disabled).toBe(
      false,
    );

    // Saving is still not possible while saving.
    realtimeToolbar.onSaveStatusChange(1);
    await flushPromises();
    expect(query<HTMLButtonElement>(".realtime-action-done").disabled).toBe(
      true,
    );
  });

  it("shows the warnings", async () => {
    vi.useFakeTimers();
    try {
      const realtimeToolbar = await createToolbar();
      expect(queryAll(".realtime-warning")).toHaveLength(0);

      realtimeToolbar.addWarning({ id: "first", title: "One", message: "1" });
      realtimeToolbar.addWarning({ id: "second", title: "Two", message: "2" });
      await flushPromises();
      const [first, second] = queryAll(".realtime-warning");
      expect(first.dataset.warning).toBe("first");
      expect(first.title).toBe("One");
      expect(first.dataset.content).toBe("1");
      expect(first.querySelector(".icon-warning")).not.toBeNull();
      expect(second.dataset.warning).toBe("second");
      expect(popoverActions).toEqual([
        [first, "init"],
        [second, "init"],
      ]);

      // The warning messages are shown once, a bit later, and then hidden automatically after a while, unless the user
      // hides them before.
      vi.advanceTimersByTime(1000);
      expect(popoverActions.slice(2)).toEqual([
        [first, "show"],
        [second, "show"],
      ]);
      jQuery(first).popover("hide");
      vi.advanceTimersByTime(9000);
      expect(popoverActions.slice(4)).toEqual([
        [first, "hide"],
        [second, "hide"],
      ]);

      // Replace the first warning.
      realtimeToolbar.addWarning({ id: "first", title: "Uno", message: "1" });
      await flushPromises();
      expect(
        queryAll(".realtime-warning").map(({ dataset }) => dataset.warning),
      ).toEqual(["second", "first"]);
      expect(query(".realtime-warning[data-warning=first]").title).toBe("Uno");
      expect(popoverActions).toContainEqual([first, "destroy"]);

      realtimeToolbar.removeWarning("second");
      await flushPromises();
      expect(
        queryAll(".realtime-warning").map(({ dataset }) => dataset.warning),
      ).toEqual(["first"]);
      expect(popoverActions).toContainEqual([second, "destroy"]);
    } finally {
      vi.useRealTimers();
    }
  });

  it("updates the save status", async () => {
    const realtimeToolbar = await createToolbar();
    realtimeToolbar.onConnectionStatusChange(2);

    realtimeToolbar.onSaveStatusChange(1);
    await flushPromises();
    const saveStatus = query(".realtime-save-status");
    expect(saveStatus.dataset.status).toBe("cleaning");
    expect(saveStatus.textContent!.trim()).toBe("realtime.saveStatus.saving");
    expect(query<HTMLButtonElement>(".realtime-action-done").disabled).toBe(
      true,
    );

    realtimeToolbar.onSaveStatusChange(2);
    await flushPromises();
    expect(saveStatus.dataset.status).toBe("clean");
    expect(saveStatus.textContent!.trim()).toBe("realtime.saveStatus.saved");
    expect(query<HTMLButtonElement>(".realtime-action-done").disabled).toBe(
      false,
    );

    realtimeToolbar.onSaveStatusChange(0);
    await flushPromises();
    expect(saveStatus.textContent!.trim()).toBe("realtime.saveStatus.unsaved");
  });

  it("lists the users", async () => {
    const session = createSession();
    const realtimeToolbar = await createToolbar(session);
    realtimeToolbar.onConnectionStatusChange(2);

    realtimeToolbar.onUserListChange(users(3));
    await flushPromises();
    const visibleUsers = queryAll(".realtime-users .realtime-user");
    expect(visibleUsers).toHaveLength(3);
    expect(query(".realtime-users-dropdown").hidden).toBe(true);

    const [first, second] = visibleUsers;
    expect(first.classList).toContain("realtime-user-compact");
    expect(first.dataset.id).toBe("id0");
    expect(first.dataset.reference).toBe("XWiki.User0");
    expect(first.getAttribute("href")).toBe("/profile/XWiki.User0");
    expect(first.querySelector(".realtime-user-name")!.textContent).toBe(
      "First0 Last0",
    );
    const firstAvatar = first.querySelector("img")!;
    expect(firstAvatar.getAttribute("src")).toBe("noavatar.png");
    expect(firstAvatar.title).toBe("First0 Last0");
    expect(
      first.querySelector<HTMLElement>(".realtime-user-avatar-wrapper")!.dataset
        .abbr,
    ).toBe("FL");
    expect(second.querySelector("img")!.getAttribute("src")).toBe(
      "avatar1.png",
    );

    expect(clickAndCheckPrevented(first)).toBe(true);
    expect(session.selectUser).toHaveBeenCalledWith("id0");

    // More users than the limit.
    realtimeToolbar.onUserListChange(users(6));
    await flushPromises();
    const updatedVisibleUsers = queryAll(".realtime-users .realtime-user");
    expect(updatedVisibleUsers).toHaveLength(4);
    // The elements of the users already listed are reused.
    expect(updatedVisibleUsers[0]).toBe(first);
    expect(updatedVisibleUsers[1]).toBe(second);
    const usersDropdown = query(".realtime-users-dropdown");
    expect(usersDropdown.hidden).toBe(false);
    expect(query(".realtime-action-users").dataset.more).toBe("2");
    const dropdownUsers = queryAll(".realtime-users-dropdown .realtime-user");
    expect(dropdownUsers).toHaveLength(6);
    expect(dropdownUsers[0].classList).not.toContain("realtime-user-compact");
  });

  it("links to the user profile when the session doesn't handle the user selection", async () => {
    const realtimeToolbar = await createToolbar({ save: vi.fn() });
    realtimeToolbar.onConnectionStatusChange(2);
    realtimeToolbar.onUserListChange(users(1));
    await flushPromises();

    const user = query(".realtime-users .realtime-user");
    expect(user.getAttribute("href")).toBe("/profile/XWiki.User0");
    expect(clickAndCheckPrevented(user)).toBe(false);
  });

  it("abbreviates single user names", async () => {
    const realtimeToolbar = await createToolbar();
    realtimeToolbar.onConnectionStatusChange(2);
    realtimeToolbar.onUserListChange([
      { id: "1", reference: "XWiki.John", name: "john" },
    ]);
    await flushPromises();
    expect(query(".realtime-user-avatar-wrapper").dataset.abbr).toBe("Jo");
  });

  it("lists the created versions", async () => {
    const realtimeToolbar = await createToolbar();
    for (let i = 1; i <= 6; i++) {
      realtimeToolbar.onCreateVersion({
        number: `1.${i}`,
        date: i,
        author: { reference: "XWiki.Alice", name: "Alice" },
      });
    }
    // The oldest version is removed when the limit is reached. A version number can be reused (e.g. after a version is
    // deleted) in which case the old entry is replaced.
    realtimeToolbar.onCreateVersion({
      number: "1.4",
      date: 7,
      author: "sessionId",
    });
    await flushPromises();

    const versions = queryAll(".realtime-versions .realtime-version");
    expect(
      versions.map(
        (version) =>
          version.querySelector(".realtime-version-number")!.textContent,
      ),
    ).toEqual(["1.2", "1.3", "1.5", "1.6", "1.4"]);

    const [first, , , , last] = versions;
    expect(first.getAttribute("href")).toBe("/view/1.1?rev=1.2");
    expect(JSON.parse(first.dataset.version!).number).toBe("1.2");
    expect(first.querySelector(".realtime-version-date")!.textContent).toBe(
      "date-2",
    );
    const author = first.querySelector<HTMLElement>(".realtime-user")!;
    expect(author.tagName).toBe("SPAN");
    expect(author.dataset.reference).toBe("XWiki.Alice");
    // Unknown author.
    expect(last.querySelector(".realtime-user")).toBeNull();
    expect(last.querySelector(".realtime-version-icon")).not.toBeNull();
  });

  it("limits the number of listed versions", async () => {
    const realtimeToolbar = await createToolbar();
    for (let i = 1; i <= 7; i++) {
      realtimeToolbar.onCreateVersion({
        number: `1.${i}`,
        date: i,
        author: "x",
      });
    }
    await flushPromises();
    expect(
      queryAll(".realtime-versions .realtime-version-number").map(
        (version) => version.textContent,
      ),
    ).toEqual(["1.3", "1.4", "1.5", "1.6", "1.7"]);
  });

  it("saves when clicking Done", async () => {
    const session = createSession();
    const realtimeToolbar = await createToolbar(session);
    realtimeToolbar.onConnectionStatusChange(2);
    await flushPromises();

    const done = query<HTMLButtonElement>(".realtime-action-done");
    expect(done.title).toBe("realtime.done.hint");
    expect(done.dataset.toggle).toBeUndefined();
    expect(done.innerHTML).toContain("icon-check");
    done.click();
    expect(session.save).toHaveBeenCalledWith(undefined);
  });

  it("swallows save failures", async () => {
    const session = createSession();
    session.save.mockRejectedValue(new Error("failed"));
    const realtimeToolbar = await createToolbar(session);
    realtimeToolbar.onConnectionStatusChange(2);
    await flushPromises();

    const debug = vi.spyOn(console, "debug").mockImplementation(() => {});
    query(".realtime-action-done").click();
    await flushPromises();
    expect(debug).toHaveBeenCalledWith("Failed to save.", expect.any(Error));
    debug.mockRestore();
  });

  it("doesn't offer to summarize when edit comments are disabled", async () => {
    await createToolbar(
      createSession(),
      createEnvironment(editComment(false, false)),
    );
    expect(queryAll(".realtime-action-summarize")).toHaveLength(0);
    expect(document.querySelector("#realtime-changeSummaryModal")).toBeNull();
    // The divider is the last item of the history dropdown (hidden by CSS).
    expect(query(".realtime-versions").lastElementChild!.classList).toContain(
      "divider",
    );
  });

  it("opens the change summary modal from Done when the summary is mandatory", async () => {
    const session = createSession();
    const realtimeToolbar = await createToolbar(
      session,
      createEnvironment(editComment(true, true)),
    );
    realtimeToolbar.onConnectionStatusChange(1);
    await flushPromises();

    const done = query<HTMLButtonElement>(".realtime-action-done");
    expect(done.title).toBe("realtime.summarizeAndDone.hint");
    expect(done.dataset.toggle).toBe("modal");
    expect(done.dataset.target).toBe("#realtime-changeSummaryModal");
    // The modal can be opened even while connecting.
    expect(done.disabled).toBe(false);
    // The Summarize & Done menu entry is not needed.
    expect(
      queryAll(".realtime-edit-toolbar-left .realtime-action-summarize"),
    ).toHaveLength(0);

    done.click();
    expect(session.save).not.toHaveBeenCalled();
    expect(query("#realtime-changeSummaryModal").dataset.continue).toBe(
      "false",
    );
    expect(
      query<HTMLTextAreaElement>("#realtime-changeSummaryModal-summary")
        .required,
    ).toBe(true);
  });

  describe("change summary modal", () => {
    async function openModal(
      continueEditing: boolean,
      session = createSession(),
      environment = createEnvironment(),
      xwikiDocument = createDocument(),
    ) {
      const realtimeToolbar = await createToolbar(
        session,
        environment,
        xwikiDocument,
      );
      realtimeToolbar.onConnectionStatusChange(2);
      realtimeToolbar.onCreateVersion({ number: "2.1", date: 1, author: "x" });
      await flushPromises();
      query(
        continueEditing
          ? ".realtime-versions .realtime-action-summarize"
          : ".realtime-edit-toolbar-left .realtime-action-summarize",
      ).click();
      const modal = query("#realtime-changeSummaryModal");
      showModal(modal);
      await flushPromises();
      return { realtimeToolbar, modal, session, environment };
    }

    it("summarizes and saves", async () => {
      const { modal, session, environment } = await openModal(true);
      expect(modal.parentElement).toBe(document.body);
      expect(modal.dataset.continue).toBe("true");
      const textArea = query<HTMLTextAreaElement>(
        "#realtime-changeSummaryModal-summary",
      );
      expect(document.activeElement).toBe(textArea);
      expect(textArea.required).toBe(false);
      const submit = query<HTMLButtonElement>(
        "#realtime-changeSummaryModal .modal-footer .btn-primary",
      );
      expect(submit.textContent!.trim()).toBe("save");

      // The summary can be set without firing input events (e.g. by the functional tests).
      textArea.value = "My summary";
      query<HTMLInputElement>("input[name=minorChange]").click();
      submit.click();
      await flushPromises();

      expect(environment.setChangeSummary).toHaveBeenCalledWith(
        query("form#edit"),
        "My summary",
        true,
      );
      expect(modal.classList).not.toContain("in");
      expect(session.save).toHaveBeenCalledWith(true);
    });

    it("summarizes and leaves the edit mode", async () => {
      const { modal, session, environment } = await openModal(false);
      expect(modal.dataset.continue).toBe("false");
      query<HTMLButtonElement>(
        "#realtime-changeSummaryModal .modal-footer .btn-primary",
      ).click();
      await flushPromises();
      expect(environment.setChangeSummary).toHaveBeenCalledWith(
        query("form#edit"),
        "",
        false,
      );
      expect(session.save).toHaveBeenCalledWith(false);
    });

    it("doesn't set the minor edit flag for new documents", async () => {
      const { environment } = await openModal(
        false,
        createSession(),
        createEnvironment(),
        createDocument(true),
      );
      query<HTMLButtonElement>(
        "#realtime-changeSummaryModal .modal-footer .btn-primary",
      ).click();
      await flushPromises();
      expect(environment.setChangeSummary).toHaveBeenCalledWith(
        query("form#edit"),
        "",
        undefined,
      );
    });

    it("requires a summary when mandatory", async () => {
      const session = createSession();
      const environment = createEnvironment(editComment(true, true));
      const realtimeToolbar = await createToolbar(session, environment);
      realtimeToolbar.onConnectionStatusChange(2);
      await flushPromises();
      query(".realtime-action-done").click();
      // Select the changes tab to check that the summary tab is selected back on submit.
      query(
        'a[aria-controls="realtime-changeSummaryModal-changesTab"]',
      ).click();
      query<HTMLButtonElement>(
        "#realtime-changeSummaryModal .modal-footer .btn-primary",
      ).click();
      await flushPromises();

      expect(session.save).not.toHaveBeenCalled();
      expect(
        query("#realtime-changeSummaryModal-summaryTab").classList,
      ).toContain("active");
    });

    it("is disabled while saving", async () => {
      const { realtimeToolbar } = await openModal(true);
      realtimeToolbar.onSaveStatusChange(1);
      await flushPromises();
      expect(
        query<HTMLButtonElement>(
          "#realtime-changeSummaryModal .modal-footer .btn-primary",
        ).disabled,
      ).toBe(true);
    });

    it("shows the changes", async () => {
      const { environment } = await openModal(true);
      const changesTab = query("#realtime-changeSummaryModal-changesTab");
      expect(changesTab.classList).not.toContain("active");

      query(
        'a[aria-controls="realtime-changeSummaryModal-changesTab"]',
      ).click();
      await nextTick();
      expect(changesTab.classList).toContain("active");
      expect(changesTab.dataset.state).toBe("loading");
      await flushPromises();
      expect(changesTab.dataset.state).toBe("loaded");
      expect(changesTab.querySelector(".diff-group")!.innerHTML).toBe("diff");
      expect(environment.fetchChanges).toHaveBeenCalledWith(
        query("form#edit"),
        expect.objectContaining({ version: "1.1" }),
        "2.1",
        query(".realtime-edit-toolbar"),
      );

      // The changes are not reloaded while the modal is open.
      query(
        'a[aria-controls="realtime-changeSummaryModal-summaryTab"]',
      ).click();
      query(
        'a[aria-controls="realtime-changeSummaryModal-changesTab"]',
      ).click();
      expect(environment.fetchChanges).toHaveBeenCalledTimes(1);
    });

    it("compares with the version created after the last summary", async () => {
      const { realtimeToolbar, environment } = await openModal(true);
      query<HTMLButtonElement>(
        "#realtime-changeSummaryModal .modal-footer .btn-primary",
      ).click();
      await flushPromises();
      realtimeToolbar.onCreateVersion({ number: "2.2", date: 2, author: "x" });
      realtimeToolbar.onCreateVersion({ number: "2.3", date: 3, author: "x" });
      await flushPromises();

      query(".realtime-versions .realtime-action-summarize").click();
      query(
        'a[aria-controls="realtime-changeSummaryModal-changesTab"]',
      ).click();
      await flushPromises();
      expect(environment.fetchChanges).toHaveBeenLastCalledWith(
        expect.any(HTMLElement),
        expect.anything(),
        "2.2",
        expect.any(HTMLElement),
      );
    });

    it("reports when there are no changes", async () => {
      await openModal(
        true,
        createSession(),
        createEnvironment({ fetchChanges: async () => "<p>No changes</p>" }),
      );
      query(
        'a[aria-controls="realtime-changeSummaryModal-changesTab"]',
      ).click();
      await flushPromises();
      expect(
        query("#realtime-changeSummaryModal-changesTab").dataset.state,
      ).toBe("empty");
    });

    it("reports new documents", async () => {
      await openModal(
        true,
        createSession(),
        createEnvironment(),
        createDocument(true),
      );
      query(
        'a[aria-controls="realtime-changeSummaryModal-changesTab"]',
      ).click();
      await flushPromises();
      expect(
        query("#realtime-changeSummaryModal-changesTab").dataset.state,
      ).toBe("isNew");
    });

    it("reports failures to load the changes", async () => {
      const error = vi.spyOn(console, "error").mockImplementation(() => {});
      await openModal(
        true,
        createSession(),
        createEnvironment({
          fetchChanges: async () => {
            throw new Error("failed");
          },
        }),
      );
      query(
        'a[aria-controls="realtime-changeSummaryModal-changesTab"]',
      ).click();
      await flushPromises();
      expect(
        query("#realtime-changeSummaryModal-changesTab").dataset.state,
      ).toBe("error");
      error.mockRestore();
    });
  });

  describe("version modal", () => {
    beforeEach(() => {
      vi.stubGlobal(
        "fetch",
        vi.fn(async (url: string) => ({
          ok: !url.endsWith("1.2"),
          statusText: "Not Found",
          text: async () => `<p>Content of ${url}</p>`,
        })),
      );
    });

    afterEach(() => {
      vi.unstubAllGlobals();
    });

    it("shows the selected version", async () => {
      const realtimeToolbar = await createToolbar();
      realtimeToolbar.onCreateVersion({
        number: "1.1",
        date: 1,
        author: { reference: "XWiki.Alice", name: "Alice" },
      });
      await flushPromises();
      query(".realtime-version").click();
      await flushPromises();

      const modal = query("#realtime-version-modal");
      expect(
        modal.querySelector(".modal-title")!.textContent!.replace(/\s+/g, " "),
      ).toBe("Version 1.1 by Alice on date-1");
      expect(modal.querySelector(".realtime-version-number")!.textContent).toBe(
        "1.1",
      );
      expect(modal.querySelector(".realtime-version-date")!.textContent).toBe(
        "date-1",
      );
      const author = modal.querySelector<HTMLAnchorElement>(
        ".realtime-version-author .realtime-user",
      )!;
      expect(author.getAttribute("href")).toBe("/profile/XWiki.Alice");
      // The version author is not selectable.
      expect(clickAndCheckPrevented(author)).toBe(false);
      const content = modal.querySelector(".realtime-version-content")!;
      expect(content.shadowRoot!.innerHTML).toBe(
        "<p>Content of /content/1.1?rev=1.1</p>",
      );
      expect(modal.querySelector(".realtime-spinner")).toBeNull();
    });

    it("handles unknown authors and loading failures", async () => {
      const error = vi.spyOn(console, "error").mockImplementation(() => {});
      const realtimeToolbar = await createToolbar();
      realtimeToolbar.onCreateVersion({
        number: "1.2",
        date: 2,
        author: "sessionId",
      });
      await flushPromises();
      query(".realtime-version").click();
      await flushPromises();

      const modal = query("#realtime-version-modal");
      expect(modal.querySelector(".realtime-version-author")!.textContent).toBe(
        "sessionId",
      );
      expect(modal.querySelector(".modal-body")!.textContent).toContain(
        "realtime.failedToLoadVersion",
      );
      error.mockRestore();
    });
  });

  describe("leave", () => {
    it("is not available when the session can't be left", async () => {
      await createToolbar({ save: vi.fn() });
      expect(queryAll(".realtime-action-leave")).toHaveLength(0);
      expect(document.querySelector("#realtime-leave-modal")).toBeNull();
    });

    it("leaves the session after confirmation", async () => {
      const session = createSession();
      const environment = createEnvironment();
      const realtimeToolbar = await createToolbar(session, environment);
      session.leave.mockImplementation(() => realtimeToolbar.destroy());
      toolbar = undefined;

      const leaveAction = query(".realtime-action-leave");
      expect(leaveAction.dataset.target).toBe("#realtime-leave-modal");
      const modal = query("#realtime-leave-modal");
      showModal(modal);
      const leaveButton = query<HTMLButtonElement>(
        "#realtime-leave-modal .modal-footer .btn-primary",
      );
      expect(document.activeElement).toBe(leaveButton);

      leaveButton.click();
      expect(session.leave).toHaveBeenCalled();
      expect(oldToolbar.hidden).toBe(false);
      expect(document.querySelector(".realtime-edit-toolbar")).toBeNull();
      // The modal is removed only after it is closed.
      expect(modal.isConnected).toBe(true);
      hideModal(modal);
      await flushPromises();
      expect(modal.isConnected).toBe(false);
      expect(document.querySelector("#realtime-changeSummaryModal")).toBeNull();
    });
  });

  it("restores the standard edit toolbar when destroyed", async () => {
    const realtimeToolbar = await createToolbar();
    toolbar = undefined;
    realtimeToolbar.destroy();
    expect(oldToolbar.hidden).toBe(false);
    expect(queryAll(".realtime-edit-toolbar")).toHaveLength(0);
    expect(queryAll(".modal")).toHaveLength(0);
  });

  it("replaces only the edit toolbar of the given form", async () => {
    document.body.insertAdjacentHTML(
      "afterbegin",
      `<form id="other">
        <div class="bottombuttons sticky-buttons">
          <div class="buttons"></div>
        </div>
      </form>`,
    );
    const realtimeToolbar = await createToolbar();
    expect(realtimeToolbar.getForm()).toBe(query("form#edit"));
    expect(query("form#other .buttons").hidden).toBe(false);
    expect(query("form#edit .realtime-edit-toolbar").nextElementSibling).toBe(
      oldToolbar,
    );
  });

  it("fails when there's no edit toolbar to replace", () => {
    expect(
      () =>
        new Toolbar({
          form: document.createElement("form"),
          document: createDocument(),
          session: createSession(),
          environment: createEnvironment(),
          i18n: createTranslations(),
        }),
    ).toThrow("Failed to find the edit toolbar to replace.");
  });
});
