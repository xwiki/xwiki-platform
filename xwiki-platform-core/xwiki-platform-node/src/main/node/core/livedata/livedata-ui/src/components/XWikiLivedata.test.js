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
import { mount } from "@vue/test-utils";
import flushPromises from "flush-promises";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { nextTick } from "vue";

// The logic resolves its translations through vue-i18n, which requires an active Vue application.
// The maximized view does not depend on the translations, so a pass-through is enough.
vi.mock("vue-i18n", () => ({
  useI18n: () => ({ t: (key) => key }),
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
 * Vue component initializer for `XWikiLivedata` components. Calls `mount()` with preconfigured
 * values.
 *
 * @param attachTo - an optional element to mount the component into, needed by the tests
 *   looking at the elements surrounding the Live Data
 * @returns a map containing a wrapper for `XWikiLivedata` components and the logic instance it
 *   created
 */
function initWrapper(attachTo) {
  global.XWiki = {
    contextPath: "",
  };

  const data = {
    id: 0,
    query: {
      source: { id: "liveTable" },
      properties: [],
    },
    meta: {
      defaultLayout: "table",
      layouts: [{ id: "table" }],
      propertyDescriptors: [],
      propertyTypes: [],
      selection: { enabled: false },
    },
    // A non-empty list of entries prevents the component from fetching them on mount.
    data: { count: 1, entries: [{}] },
  };

  let logic;
  // The component registers the logic on the parent of its root element through jQuery. We reuse
  // that registration to access the logic instance, the same way the public API does.
  const jQuery = () => ({
    parent: () => ({
      data: (key, value) => {
        logic = value;
      },
    }),
  });

  const wrapper = mount(XWikiLivedata, {
    attachTo,
    props: {
      liveDataSource: {},
      data: JSON.stringify(data),
      contentTrusted: true,
      locale: "en",
      i18n: {},
      resolveTranslations: async () => ({}),
    },
    global: {
      provide: {
        jQuery,
      },
      stubs: {
        LivedataAdvancedPanels: true,
        LivedataFootnotes: true,
        LivedataLayout: true,
        LivedataPersistentConfiguration: true,
      },
    },
  });

  return { wrapper, getLogic: () => logic };
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
 * @returns the result of `initWrapper()`, plus the element displayed next to the Live Data
 */
function initWrapperWithSibling() {
  const sibling = document.createElement("div");
  document.body.appendChild(sibling);
  return { sibling, ...initWrapper(document.body) };
}

describe("XWikiLivedata.vue", () => {
  it("Is not maximized by default", async () => {
    const { wrapper } = initWrapper();
    await flushPromises();

    expect(wrapper.classes()).not.toContain(MAXIMIZED_CLASS);
  });

  it("Covers the page once maximized", async () => {
    const { wrapper, getLogic } = initWrapper();
    await flushPromises();

    getLogic().toggleMaximized();
    await nextTick();

    expect(wrapper.classes()).toContain(MAXIMIZED_CLASS);
  });

  it("Escape exits the maximized view", async () => {
    const { wrapper, getLogic } = initWrapper();
    await flushPromises();

    getLogic().toggleMaximized();
    await nextTick();

    await pressKey("Escape");

    expect(getLogic().isMaximized()).toBe(false);
    expect(wrapper.classes()).not.toContain(MAXIMIZED_CLASS);
  });

  it("Keys other than escape don't exit the maximized view", async () => {
    const { wrapper, getLogic } = initWrapper();
    await flushPromises();

    getLogic().toggleMaximized();
    await nextTick();

    await pressKey("Enter");

    expect(wrapper.classes()).toContain(MAXIMIZED_CLASS);
  });

  it("Only listens to the document while maximized", async () => {
    const { getLogic } = initWrapper();
    await flushPromises();

    expect(countKeydownListeners()).toBe(0);

    getLogic().toggleMaximized();
    await nextTick();
    expect(countKeydownListeners()).toBe(1);

    getLogic().toggleMaximized();
    await nextTick();
    expect(countKeydownListeners()).toBe(0);
  });

  it("Stops listening to escape once unmounted", async () => {
    const { wrapper, getLogic } = initWrapper();
    await flushPromises();

    const logic = getLogic();
    logic.toggleMaximized();
    await nextTick();

    wrapper.unmount();
    expect(countKeydownListeners()).toBe(0);

    await pressKey("Escape");

    expect(logic.isMaximized()).toBe(true);
  });

  it("Makes the rest of the page inert while maximized", async () => {
    const { sibling, getLogic } = initWrapperWithSibling();
    await flushPromises();

    expect(sibling.hasAttribute("inert")).toBe(false);

    getLogic().toggleMaximized();
    await nextTick();
    expect(sibling.hasAttribute("inert")).toBe(true);

    getLogic().toggleMaximized();
    await nextTick();
    expect(sibling.hasAttribute("inert")).toBe(false);
  });

  it("Leaves the rest of the page inert as it found it", async () => {
    const { sibling, getLogic } = initWrapperWithSibling();
    sibling.setAttribute("inert", "");
    await flushPromises();

    getLogic().toggleMaximized();
    await nextTick();
    getLogic().toggleMaximized();
    await nextTick();

    // The element was inert before the Live Data was maximized, so it stays inert.
    expect(sibling.hasAttribute("inert")).toBe(true);
  });

  it("Restores the rest of the page when unmounted while maximized", async () => {
    const { wrapper, sibling, getLogic } = initWrapperWithSibling();
    await flushPromises();

    getLogic().toggleMaximized();
    await nextTick();
    expect(sibling.hasAttribute("inert")).toBe(true);

    wrapper.unmount();

    expect(sibling.hasAttribute("inert")).toBe(false);
  });

  it("Escape only exits the maximized Live Data when several are displayed", async () => {
    const first = initWrapper();
    const second = initWrapper();
    await flushPromises();

    second.getLogic().toggleMaximized();
    await nextTick();
    // Only the maximized Live Data listens to the document.
    expect(countKeydownListeners()).toBe(1);

    await pressKey("Escape");

    expect(second.getLogic().isMaximized()).toBe(false);
    expect(first.getLogic().isMaximized()).toBe(false);
    expect(first.wrapper.classes()).not.toContain(MAXIMIZED_CLASS);
  });
});
