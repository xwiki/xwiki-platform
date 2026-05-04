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
// @ts-expect-error DisplayerToggle is not typed.
import DisplayerToggle from "../DisplayerToggle.vue";
import { flushPromises, mount } from "@vue/test-utils";
import * as liveDataUI from "@xwiki/platform-livedata-ui";
import { beforeEach, describe, expect, it, vi } from "vitest";

// --- Mocks ---
const bootstrapSwitchMock = vi.fn();
const jQueryMock = vi.fn(() => ({ bootstrapSwitch: bootstrapSwitchMock }));

// Mock the elements provided by @xwiki/platform-livedata-ui to provide simplified vue component instead of relying on
// shallow mount, making it easier to inspect their internal state in tests. require (from Requirejs) is mocked.
vi.mock("@xwiki/platform-livedata-ui", async (importOriginal) => {
  const actual: typeof liveDataUI = await importOriginal();
  return {
    ...actual,
    BaseDisplayer: {
      template: "<div><slot name='viewer'/><slot name='editor'/></div>",
      props: ["propertyId", "entry", "isEmpty", "interceptTouch", "viewOnly"],
    },
    XWikiIcon: {
      template: "<span ref='el'></span>",
      emits: ["ready"],
      props: ["iconDescriptor"],
    },
    loadById: vi.fn(),
  };
});

function initWrapper(props = {}) {
  return mount(DisplayerToggle, {
    props: {
      propertyId: "notify",
      entry: {
        notify_checked: true,
        notify_disabled: false,
        notify_data: { id: 42 },
      },
      iconName: "bell",
      ...props,
    },
    global: {
      provide: { jQuery: jQueryMock, logic: { triggerEvent: vi.fn() } },
    },
  });
}

// --- Tests ---
// eslint-disable-next-line max-statements
describe("DisplayerToggle", () => {
  beforeEach(() => vi.clearAllMocks());

  it("reads checked state from entry", () => {
    const wrapper = initWrapper();
    expect(wrapper.vm.innerChecked).toBe(true);
  });

  it("writes checked state back to entry", () => {
    const wrapper = initWrapper();
    wrapper.vm.innerChecked = false;
    expect(wrapper.props("entry").notify_checked).toBe(false);
  });

  it("reads disabled state from entry", () => {
    const wrapper = initWrapper();
    expect(wrapper.vm.innerDisabled).toBe(false);
  });

  it("reads data from entry", () => {
    const wrapper = initWrapper();
    expect(wrapper.vm.innerData).toEqual({ id: 42 });
  });

  describe("watcher: innerChecked", () => {
    it("calls bootstrapSwitch('state', ...) when innerChecked changes", async () => {
      const wrapper = initWrapper();
      wrapper.vm.innerChecked = false;
      await flushPromises();
      expect(bootstrapSwitchMock).toHaveBeenCalledWith("state", false, true);
    });
  });

  it("calls bootstrapSwitch('disabled', ...) when innerDisabled changes", async () => {
    const wrapper = initWrapper();
    wrapper.vm.innerDisabled = true;
    await flushPromises();
    expect(bootstrapSwitchMock).toHaveBeenCalledWith("disabled", true);
  });

  it("initializes bootstrapSwitch with correct options when iconReady becomes true", async () => {
    const wrapper = initWrapper();
    wrapper.vm.iconReady = true;
    await flushPromises();
    expect(bootstrapSwitchMock).toHaveBeenCalledWith(
      expect.objectContaining({
        size: "mini",
        state: true,
        disabled: false,
      }),
    );
  });

  it("does NOT initialize bootstrapSwitch when iconReady is false", async () => {
    initWrapper();
    // iconReady starts false, nothing should have been called yet
    expect(bootstrapSwitchMock).not.toHaveBeenCalled();
  });

  it("triggers 'xwiki:livedata:toggle' with correct payload", async () => {
    const triggerEvent = vi.fn();
    const wrapper = initWrapper();
    wrapper.vm.logic.triggerEvent = triggerEvent;
    wrapper.vm.iconReady = true;
    await flushPromises();

    // Extract and invoke the onSwitchChange callback
    const opts = bootstrapSwitchMock.mock.calls.find(
      (arg0) => typeof arg0 === "object",
    )![0];
    opts.onSwitchChange({}, true);

    expect(triggerEvent).toHaveBeenCalledWith(
      "toggle",
      expect.objectContaining({
        data: { id: 42 },
        checked: true,
        disabled: false,
        callback: expect.any(Function),
      }),
    );
  });

  it("callback updates entry state", async () => {
    const wrapper = initWrapper();
    wrapper.vm.iconReady = true;
    await flushPromises();

    const opts = bootstrapSwitchMock.mock.calls.find(
      (args) => typeof args[0] === "object",
    )![0];
    opts.onSwitchChange({}, false);

    const { callback } = wrapper.vm.logic.triggerEvent.mock.calls[0][1];
    callback({ checked: false, disabled: true, data: { id: 99 } });

    expect(wrapper.props("entry").notify_checked).toBe(false);
    expect(wrapper.props("entry").notify_disabled).toBe(true);
    expect(wrapper.props("entry").notify_data).toEqual({ id: 99 });
  });
});
