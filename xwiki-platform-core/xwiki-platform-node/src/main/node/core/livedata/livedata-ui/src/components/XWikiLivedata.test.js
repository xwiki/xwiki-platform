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
import XWikiLivedata from "./XWikiLivedata.vue";
import { LiveDataLogic } from "./services/LiveDataLogic";
import { shallowMount } from "@vue/test-utils";
import flushPromises from "flush-promises";
import jQuery from "jquery";
import _ from "lodash-es";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { nextTick, ref } from "vue";

vi.mock("./services/LiveDataLogic", () => ({
  LiveDataLogic: vi.fn(),
}));

const MAXIMIZED_CLASS = "livedata-maximized";

// The escape key is listened to on the document, because the focus can be on any element of the
// maximized Live Data. We count those listeners to check that they are only registered while
// maximized.
let keydownListeners;

/**
 * @returns the number of keydown listeners currently registered on the document
 */
function countKeydownListeners() {
  return keydownListeners;
}

beforeEach(() => {
  vi.clearAllMocks();

  keydownListeners = 0;
  const addEventListener = document.addEventListener.bind(document);
  const removeEventListener = document.removeEventListener.bind(document);
  vi.spyOn(document, "addEventListener").mockImplementation(
    (type, ...parameters) => {
      if (type === "keydown") {
        keydownListeners++;
      }
      addEventListener(type, ...parameters);
    },
  );
  vi.spyOn(document, "removeEventListener").mockImplementation(
    (type, ...parameters) => {
      if (type === "keydown") {
        keydownListeners--;
      }
      removeEventListener(type, ...parameters);
    },
  );
});

afterEach(() => {
  vi.restoreAllMocks();
  // The tests mounting in the document leave their elements behind otherwise.
  document.body.innerHTML = "";
});

/**
 * @param overrides - the properties overriding the default mock logic
 * @returns a mock Live Data logic, recording the registered event listeners so that the tests can fire the events
 */
function mockLogic(overrides) {
  const listeners = {};
  const maximized = ref(false);
  return _.merge(
    {
      listeners,
      setElement: vi.fn(),
      onEvent: vi.fn((event, callback) => {
        (listeners[event] ??= []).push(callback);
      }),
      triggerEvent: vi.fn(),
      translationsLoaded: vi.fn().mockResolvedValue(true),
      registerPanel: vi.fn(),
      updateEntries: vi.fn().mockResolvedValue(undefined),
      // Mirrors LiveDataLogic's own implementation, so that the maximized state actually toggles.
      isMaximized: vi.fn(() => maximized.value),
      toggleMaximized: vi.fn(() => {
        maximized.value = !maximized.value;
      }),
      t: (key) => key,
      firstEntriesLoading: ref(true),
      currentLayoutId: ref("table"),
      data: {
        id: "my-live-data-id",
        // The entries are fetched on mount when there are none.
        data: { entries: [] },
      },
    },
    overrides,
  );
}

/**
 * Mount a shallow XWikiLivedata component and let it initialize as far as it can.
 *
 * @param logic - the mock logic the component is expected to create
 * @param attachTo - an optional element to mount the component into, needed by the tests looking
 *   at the elements surrounding the Live Data
 * @returns the initialized shallow wrapper
 */
async function mount(logic, attachTo) {
  // The component creates its own logic, which we replace with the mock the test drives.
  LiveDataLogic.mockImplementation(
    class {
      constructor() {
        return logic;
      }
    },
  );
  const wrapper = shallowMount(XWikiLivedata, {
    attachTo,
    props: {
      liveDataSource: {},
      data: "{}",
      contentTrusted: false,
      locale: "en",
      i18n: {},
      resolveTranslations: vi.fn(),
    },
    global: {
      provide: { jQuery },
    },
  });
  await flushPromises();
  return wrapper;
}

/**
 * Fire the layoutLoaded event on the given mock logic, the way LivedataLayout does.
 *
 * @param logic - the mock logic to fire the event on
 * @param detail - the event data, holding the error when no layout could be loaded
 */
async function fireLayoutLoaded(logic, detail = {}) {
  const event = new CustomEvent("xwiki:livedata:layoutLoaded", { detail });
  logic.listeners.layoutLoaded.forEach((callback) => callback(event));
  await flushPromises();
}

/**
 * @param logic - the mock logic to check
 * @returns the instanceReady event data, or undefined if the event was not triggered yet
 */
function instanceReadyData(logic) {
  return logic.triggerEvent.mock.calls.find(
    ([event]) => event === "instanceReady",
  )?.[1];
}

/**
 * Presses a key on the document, where the maximized view listens for the escape key.
 *
 * @param key - the key to press
 */
async function pressKey(key) {
  document.dispatchEvent(new KeyboardEvent("keydown", { key }));
  await nextTick();
}

/**
 * Mounts a Live Data in the document, next to an element standing for the rest of the page.
 *
 * @param logic - the mock logic the component is expected to create
 * @returns the mounted wrapper, plus the element displayed next to the Live Data
 */
async function mountWithSibling(logic) {
  const sibling = document.createElement("div");
  document.body.appendChild(sibling);
  return { sibling, wrapper: await mount(logic, document.body) };
}

/**
 * @param overrides - the properties overriding the default mock logic
 * @returns the mock logic and the mounted wrapper for a new XWikiLivedata component
 */
async function mountWithLogic(overrides) {
  const logic = mockLogic(overrides);
  return { logic, wrapper: await mount(logic) };
}

describe("XWikiLivedata.vue", () => {
  describe("instance readiness", () => {
    it("Triggers instanceReady once the layout is displayed", async () => {
      const logic = mockLogic();
      const wrapper = await mount(logic);

      // The live data is not displayed until its layout is loaded.
      expect(instanceReadyData(logic)).toBeUndefined();
      expect(wrapper.find(".loading").exists()).toBe(true);

      await fireLayoutLoaded(logic);

      expect(instanceReadyData(logic)).toStrictEqual({ error: undefined });
      expect(wrapper.find(".loading").exists()).toBe(false);
    });

    it("Triggers instanceReady with the error when no layout can be displayed", async () => {
      const logic = mockLogic();
      const wrapper = await mount(logic);
      const error = new Error("Unknown layout [table]");

      await fireLayoutLoaded(logic, { error });

      expect(instanceReadyData(logic)).toStrictEqual({ error });
      // There's nothing to display in place of the loader, so it keeps running.
      expect(wrapper.find(".loading").exists()).toBe(true);
    });

    it("Waits for the entries to be fetched before triggering instanceReady", async () => {
      let fetchEntries;
      const logic = mockLogic({
        updateEntries: vi.fn(
          () => new Promise((resolve) => (fetchEntries = resolve)),
        ),
      });
      await mount(logic);
      await fireLayoutLoaded(logic);

      // The layout is displayed but the entries are still being fetched.
      expect(logic.updateEntries).toHaveBeenCalled();
      expect(instanceReadyData(logic)).toBeUndefined();

      fetchEntries();
      await flushPromises();

      expect(instanceReadyData(logic)).toStrictEqual({ error: undefined });
    });
  });

  describe("maximized view", () => {
    it("Is not maximized by default", async () => {
      const wrapper = await mount(mockLogic());

      expect(wrapper.classes()).not.toContain(MAXIMIZED_CLASS);
    });

    it("Covers the page once maximized", async () => {
      const logic = mockLogic();
      const wrapper = await mount(logic);

      logic.toggleMaximized();
      await nextTick();

      expect(wrapper.classes()).toContain(MAXIMIZED_CLASS);
    });

    it("Escape exits the maximized view", async () => {
      const logic = mockLogic();
      const wrapper = await mount(logic);

      logic.toggleMaximized();
      await nextTick();

      await pressKey("Escape");

      expect(logic.isMaximized()).toBe(false);
      expect(wrapper.classes()).not.toContain(MAXIMIZED_CLASS);
    });

    it("Keys other than escape don't exit the maximized view", async () => {
      const logic = mockLogic();
      const wrapper = await mount(logic);

      logic.toggleMaximized();
      await nextTick();

      await pressKey("Enter");

      expect(wrapper.classes()).toContain(MAXIMIZED_CLASS);
    });

    it("Only listens to the document while maximized", async () => {
      const logic = mockLogic();
      await mount(logic);

      expect(countKeydownListeners()).toBe(0);

      logic.toggleMaximized();
      await nextTick();
      expect(countKeydownListeners()).toBe(1);

      logic.toggleMaximized();
      await nextTick();
      expect(countKeydownListeners()).toBe(0);
    });

    it("Stops listening to escape once unmounted", async () => {
      const logic = mockLogic();
      const wrapper = await mount(logic);

      logic.toggleMaximized();
      await nextTick();

      wrapper.unmount();
      expect(countKeydownListeners()).toBe(0);

      await pressKey("Escape");

      expect(logic.isMaximized()).toBe(true);
    });

    it("Makes the rest of the page inert while maximized", async () => {
      const logic = mockLogic();
      const { sibling } = await mountWithSibling(logic);

      expect(sibling.hasAttribute("inert")).toBe(false);

      logic.toggleMaximized();
      await nextTick();
      expect(sibling.hasAttribute("inert")).toBe(true);

      logic.toggleMaximized();
      await nextTick();
      expect(sibling.hasAttribute("inert")).toBe(false);
    });

    it("Leaves the rest of the page inert as it found it", async () => {
      const logic = mockLogic();
      const sibling = document.createElement("div");
      sibling.setAttribute("inert", "");
      document.body.appendChild(sibling);
      await mount(logic, document.body);

      logic.toggleMaximized();
      await nextTick();
      logic.toggleMaximized();
      await nextTick();

      // The element was inert before the Live Data was maximized, so it stays inert.
      expect(sibling.hasAttribute("inert")).toBe(true);
    });

    it("Restores the rest of the page when unmounted while maximized", async () => {
      const logic = mockLogic();
      const { wrapper, sibling } = await mountWithSibling(logic);

      logic.toggleMaximized();
      await nextTick();
      expect(sibling.hasAttribute("inert")).toBe(true);

      wrapper.unmount();

      expect(sibling.hasAttribute("inert")).toBe(false);
    });

    it("Escape only exits the maximized Live Data when several are displayed", async () => {
      const first = await mountWithLogic();
      const second = await mountWithLogic();

      second.logic.toggleMaximized();
      await nextTick();
      // Only the maximized Live Data listens to the document.
      expect(countKeydownListeners()).toBe(1);

      await pressKey("Escape");

      expect(second.logic.isMaximized()).toBe(false);
      expect(first.logic.isMaximized()).toBe(false);
      expect(first.wrapper.classes()).not.toContain(MAXIMIZED_CLASS);
    });
  });
});
