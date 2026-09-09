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
import LivedataLayout from "./LivedataLayout.vue";
import { componentStore } from "@/components/store.js";
import flushPromises from "flush-promises";
import _ from "lodash";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("@/components/store.js", () => ({
  componentStore: { load: vi.fn() },
}));

const layoutComponent = { name: "LayoutTable", template: "<div></div>" };

/**
 * @param overrides the properties overriding the default mock logic
 * @returns a mock Live Data logic
 */
function mockLogic(overrides) {
  return _.merge({
    data: {
      id: "my-layout-id",
      meta: {},
    },
    triggerEvent: vi.fn(),
    changeLayout: vi.fn(),
    getLayoutDescriptor: vi.fn(),
  }, overrides);
}

/**
 * Initialize a shallow LivedataLayout component.
 *
 * @param logic the mock logic to provide to the component
 * @param props the props to pass to the component
 * @returns {Wrapper<Vue>} the initialized shallow wrapper
 */
function initWrapper(logic = mockLogic(), props = {}) {
  return shallowMount(LivedataLayout, {
    props,
    global: {
      provide: { logic },
    },
  });
}

describe("LivedataLayout.vue", () => {
  beforeEach(() => {
    // The component logs the layouts it fails to load, which we don't want in the test output.
    vi.spyOn(console, "error").mockImplementation(() => {});
    vi.spyOn(console, "warn").mockImplementation(() => {});
    // By default the requested layout is loaded successfully.
    componentStore.load.mockReset().mockResolvedValue(layoutComponent);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("Without a description", () => {
    const wrapper = initWrapper();
    expect(wrapper.find(".livedata-layout-description").exists()).toBe(false);
  });

  it("With a description", () => {
    const wrapper = initWrapper(mockLogic({ data: { meta: { description: "A description" } } }));
    expect(wrapper.find(".livedata-layout-description").text()).toBe("A description");
  });

  it("Triggers layoutLoaded with the loaded layout component", async () => {
    const logic = mockLogic();
    initWrapper(logic, { layoutId: "table" });
    await flushPromises();

    expect(componentStore.load).toHaveBeenCalledWith("layout", "table");
    expect(logic.triggerEvent).toHaveBeenCalledWith("layoutLoaded", {
      layoutId: "table",
      previousLayoutId: undefined,
      component: layoutComponent,
    });
    expect(logic.changeLayout).not.toHaveBeenCalled();
  });

  it("Falls back on the default layout when the requested one can't be loaded", async () => {
    componentStore.load.mockRejectedValue(new Error("Failed to load"));
    const logic = mockLogic({
      data: { meta: { defaultLayout: "table" } },
      getLayoutDescriptor: vi.fn().mockReturnValue({ id: "table" }),
    });
    initWrapper(logic, { layoutId: "cards" });
    await flushPromises();

    expect(logic.changeLayout).toHaveBeenCalledWith("table");
    // The default layout is going to be loaded, so we're not done yet.
    expect(logic.triggerEvent).not.toHaveBeenCalled();
  });

  it("Triggers layoutLoaded with the error when the default layout is the one that failed", async () => {
    const error = new Error("Failed to load");
    componentStore.load.mockRejectedValue(error);
    const logic = mockLogic({
      data: { meta: { defaultLayout: "table" } },
      getLayoutDescriptor: vi.fn().mockReturnValue({ id: "table" }),
    });
    initWrapper(logic, { layoutId: "table" });
    await flushPromises();

    expect(logic.changeLayout).not.toHaveBeenCalled();
    expect(logic.triggerEvent).toHaveBeenCalledWith("layoutLoaded", {
      layoutId: "table",
      previousLayoutId: undefined,
      error,
    });
  });

  it("Triggers layoutLoaded with the error when the default layout is not declared", async () => {
    const error = new Error("Failed to load");
    componentStore.load.mockRejectedValue(error);
    const logic = mockLogic({
      data: { meta: { defaultLayout: "table" } },
      getLayoutDescriptor: vi.fn().mockReturnValue(undefined),
    });
    initWrapper(logic, { layoutId: "cards" });
    await flushPromises();

    expect(logic.changeLayout).not.toHaveBeenCalled();
    expect(logic.triggerEvent).toHaveBeenCalledWith("layoutLoaded", {
      layoutId: "cards",
      previousLayoutId: undefined,
      error,
    });
  });
});
