/*
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
import { shallowMount } from "@vue/test-utils";
import XWikiLivedata from "./XWikiLivedata.vue";
import flushPromises from "flush-promises";
import _ from "lodash";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ref } from "vue";

/**
 * @param overrides the properties overriding the default mock logic
 * @returns a mock Live Data logic, recording the registered event listeners so that the tests can fire the events
 */
function mockLogic(overrides) {
  const listeners = {};
  return _.merge({
    listeners,
    onEvent: vi.fn((event, callback) => {
      (listeners[event] ??= []).push(callback);
    }),
    triggerEvent: vi.fn(),
    translationsLoaded: vi.fn().mockResolvedValue(true),
    // The entries are fetched by the logic, right after the component is mounted.
    firstEntriesLoaded: Promise.resolve(),
    currentLayoutId: ref("table"),
    data: {
      id: "my-live-data-id",
    },
  }, overrides);
}

/**
 * Mount a shallow XWikiLivedata component and let it initialize as far as it can.
 *
 * @param logic the mock logic to provide to the component
 * @returns {Wrapper<Vue>} the initialized shallow wrapper
 */
async function initWrapper(logic) {
  const wrapper = shallowMount(XWikiLivedata, {
    global: {
      provide: { logic },
    },
  });
  await flushPromises();
  return wrapper;
}

/**
 * Fire the layoutLoaded event on the given mock logic, the way LivedataLayout does.
 *
 * @param logic the mock logic to fire the event on
 * @param detail the event data, holding the error when no layout could be loaded
 */
async function fireLayoutLoaded(logic, detail = {}) {
  const event = new CustomEvent("xwiki:livedata:layoutLoaded", { detail });
  logic.listeners.layoutLoaded.forEach(callback => callback(event));
  await flushPromises();
}

/**
 * @param logic the mock logic to check
 * @returns the instanceReady event data, or undefined if the event was not triggered yet
 */
function instanceReadyData(logic) {
  return logic.triggerEvent.mock.calls.find(([event]) => event === "instanceReady")?.[1];
}

describe("XWikiLivedata.vue", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("Triggers instanceReady once the layout is displayed", async () => {
    const logic = mockLogic();
    const wrapper = await initWrapper(logic);

    // The live data is not displayed until its layout is loaded.
    expect(instanceReadyData(logic)).toBeUndefined();
    expect(wrapper.find(".loading").exists()).toBe(true);

    await fireLayoutLoaded(logic);

    expect(instanceReadyData(logic)).toStrictEqual({ error: undefined });
    expect(wrapper.find(".loading").exists()).toBe(false);
  });

  it("Triggers instanceReady with the error when no layout can be displayed", async () => {
    const logic = mockLogic();
    const wrapper = await initWrapper(logic);
    const error = new Error("Unknown layout [table]");

    await fireLayoutLoaded(logic, { error });

    expect(instanceReadyData(logic)).toStrictEqual({ error });
    // There's nothing to display in place of the loader, so it keeps running.
    expect(wrapper.find(".loading").exists()).toBe(true);
  });

  it("Waits for the entries to be fetched before triggering instanceReady", async () => {
    let fetchEntries;
    const logic = mockLogic({
      firstEntriesLoaded: new Promise(resolve => (fetchEntries = resolve)),
    });
    await initWrapper(logic);
    await fireLayoutLoaded(logic);

    // The layout is displayed but the entries are still being fetched.
    expect(instanceReadyData(logic)).toBeUndefined();

    fetchEntries();
    await flushPromises();

    expect(instanceReadyData(logic)).toStrictEqual({ error: undefined });
  });
});
