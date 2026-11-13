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
import XDialog from "../XDialog.vue";
import { flushPromises, mount } from "@vue/test-utils";
import { mountHelper, runTest } from "@xwiki/platform-test-accessibility";
import { mockRequireJS } from "@xwiki/platform-test-requirejs";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import type { DialogProps } from "@xwiki/platform-dsapi";

// Bootstrap is not loaded in the tests, so stub the jQuery plugin the component initializes on mount.
const modalStub = {
  modal: () => modalStub,
  on: () => modalStub,
};

function getAccessibilityMount() {
  mockRequireJS({ jquery: () => modalStub, bootstrap: {} });
  return mountHelper(XDialog);
}

const accessibilityMount = getAccessibilityMount();
describe("XDialog", () => {
  runTest(
    "render minimal props",
    accessibilityMount({
      props: {
        title: "Dialog Title",
      } satisfies DialogProps,
      slots: {
        default: "Some Text",
      },
    }),
    async () => {
      expect(document.body.innerHTML).toEqual(
        `<div data-v-app=""><span></span><!--teleport start--><!--teleport end--></div><div class="modal fade" tabindex="-1" role="dialog"><div class="modal-dialog" role="document"><div class="modal-content"><div class="modal-header"><button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">×</span></button><h4 class="modal-title">Dialog Title</h4></div><div class="modal-body">Some Text</div><!--v-if--></div></div></div>`,
      );
    },
  );
});

// A Bootstrap modal stub recording how the component drives the modal, and letting the tests trigger its events.
function createRecordingModalStub() {
  const stub = {
    calls: [] as unknown[],
    handlers: {} as Record<string, () => void>,
    modal(arg: unknown) {
      stub.calls.push(arg);
      return stub;
    },
    on(event: string, handler: () => void) {
      stub.handlers[event] = handler;
      return stub;
    },
  };
  return stub;
}

describe("XDialog behavior", () => {
  let modal: ReturnType<typeof createRecordingModalStub>;

  beforeEach(() => {
    modal = createRecordingModalStub();
    mockRequireJS({ jquery: () => modal, bootstrap: {} });
  });

  afterEach(() => {
    document.body.innerHTML = "";
  });

  async function mountDialog(props: DialogProps, content = "Some Text") {
    const wrapper = mount(XDialog, {
      props,
      slots: { default: content },
      attachTo: document.body,
    });
    // Wait for jQuery and Bootstrap to be loaded.
    await flushPromises();
    return wrapper;
  }

  it("closes when the backdrop is clicked by default", async () => {
    await mountDialog({ title: "Dialog Title", modelValue: true });

    expect(modal.calls[0]).toEqual({ show: true, backdrop: true });
  });

  it("stays open when the backdrop is clicked when persistent", async () => {
    await mountDialog({
      title: "Dialog Title",
      modelValue: true,
      persistent: true,
    });

    expect(modal.calls[0]).toEqual({ show: true, backdrop: "static" });
  });

  it("focuses the autofocus element of its content once shown", async () => {
    await mountDialog(
      { title: "Dialog Title", modelValue: true },
      `<input data-test="first" /><input data-test="second" autofocus />`,
    );

    modal.handlers["shown.bs.modal"]();

    expect(document.activeElement?.getAttribute("data-test")).toBe("second");
  });

  it("keeps the focus in the field focused before the modal is shown", async () => {
    await mountDialog(
      { title: "Dialog Title", modelValue: true },
      `<input data-test="first" /><input data-test="second" autofocus />`,
    );

    document.querySelector<HTMLElement>("[data-test='first']")!.focus();
    modal.handlers["shown.bs.modal"]();

    expect(document.activeElement?.getAttribute("data-test")).toBe("first");
  });

  it("hides the modal when unmounted", async () => {
    const wrapper = await mountDialog({
      title: "Dialog Title",
      modelValue: true,
    });

    wrapper.unmount();

    expect(modal.calls.at(-1)).toBe("hide");
  });

  it("updates its model when the modal is hidden", async () => {
    const wrapper = await mountDialog({
      title: "Dialog Title",
      modelValue: true,
    });

    modal.handlers["hidden.bs.modal"]();

    expect(wrapper.emitted("update:modelValue")).toEqual([[false]]);
  });
});
