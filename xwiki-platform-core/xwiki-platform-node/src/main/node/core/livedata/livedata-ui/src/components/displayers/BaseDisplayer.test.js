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
import BaseDisplayer from "./BaseDisplayer.vue";
import { initWrapper } from "./displayerTestsHelper";
import flushPromises from "flush-promises";
import { fake, restore, stub } from "sinon";
import { afterEach, describe, expect, it } from "vitest";
import { nextTick, ref } from "vue";

/**
 * Waits for the next animation frame, so that the focus settling logic of
 * `onDisplayerBlur` gets a chance to run.
 */
async function flushAnimationFrame() {
  await new Promise((resolve) => requestAnimationFrame(resolve));
  await flushPromises();
}

/**
 * Returns the root element of the displayer. The component root is a fragment (its template starts
 * with comments), so it cannot be reached through the wrapper itself.
 *
 * @param wrapper - an initialized BaseDisplayer
 * @returns the wrapper of the displayer cell element
 */
function cell(wrapper) {
  return wrapper.find(".displayer-actions-popover");
}

/**
 * Initializes a displayer in a Live Data that is in edit mode (i.e., where every editable cell
 * becomes editable as soon as it gets the focus).
 *
 * @param props - the props to pass to the displayer (merged with the default ones)
 * @param logic - a mock of `Logic.js` (merged with the edit mode defaults)
 * @param editBus - a mock of the edit bus
 * @returns an initialized BaseDisplayer
 */
function initEditModeWrapper({ props, logic, editBus } = {}) {
  return initWrapper(BaseDisplayer, {
    props,
    logic: {
      isEditMode: () => true,
      getEntryId: () => "entry-1",
      ...logic,
    },
    editBus,
  });
}

describe("BaseDisplayer.vue", () => {
  afterEach(function () {
    // completely restore all fakes created through the sandbox
    restore();
  });

  it("Renders an entry in view mode", () => {
    const wrapper = initWrapper(BaseDisplayer, {
      logic: {
        isEditMode: () => false,
      },
    });

    expect(wrapper.text()).toMatch("red");
  });

  it("Switch to edit mode", () => {
    const wrapper = initWrapper(BaseDisplayer, {
      logic: {
        isEditMode: () => false,
      },
    });

    // Manually triggers setEdit until we find a way to simulate the hovering of the displayer and
    // get access the popover content.
    wrapper.vm.setEdit();

    expect(wrapper.emitted()).toEqual({ "update:isView": [[false]] });
  });

  it("Renders an entry in edit mode", () => {
    const wrapper = initWrapper(BaseDisplayer, {
      props: {
        isView: false,
      },
      logic: {
        isEditMode: () => false,
      },
    });

    const inputElement = wrapper.find("input").element;
    expect(inputElement.value).toMatch("red");
    expect(inputElement).toBe(document.activeElement);
  });

  it("Send an event on save", async () => {
    const wrapper = initWrapper(BaseDisplayer, {
      props: {
        isView: false,
      },
      logic: {
        isEditMode: () => false,
      },
    });

    wrapper.find("input").setValue("test-value");

    await wrapper.find(".edit > div").trigger("keydown.enter");

    let events = wrapper.emitted();
    // Checks that the value is sent on the save event.
    // Then checks that we switch back to the view mode.
    expect(events.saveEdit[0]).toEqual(["test-value"]);
    expect(events["update:isView"][0]).toEqual([true]);
  });

  it("Renders an non viewable entry with an empty content", () => {
    const wrapper = initWrapper(BaseDisplayer, {
      props: {
        entry: {
          color: undefined,
        },
      },
      logic: {
        isActionAllowed() {
          return false;
        },
        isEditMode: () => false,
      },
    });

    expect(wrapper.find("div.view > div").text()).toBe(
      "livedata.displayer.emptyValue*",
    );
  });

  it("Renders a viewable entry with an empty content", () => {
    const wrapper = initWrapper(BaseDisplayer, {
      props: {
        entry: {
          color: undefined,
        },
      },
      logic: {
        isEditMode: () => false,
      },
    });

    expect(wrapper.find("div .view > div").text()).toBe("");
  });

  it("Renders an entry when isEmpty is set to false", () => {
    const wrapper = initWrapper(BaseDisplayer, {
      props: {
        entry: {
          color: undefined,
        },
        isEmpty: false,
      },
      logic: {
        isEditMode: () => false,
      },
    });

    // Even when the action is not allowed and the property value is undefined, 'N/A' is not
    // displayed if the props isEmpty is set to false. This is useful when a displayed has his own
    // way to present empty values, such as the link displayer.
    expect(wrapper.find("div.view > div").text()).toBe("");
  });

  // The tests below cover the behavior of a cell when the whole Live Data is in edit mode, where
  // every editable cell becomes editable as soon as it gets the focus.
  describe("Starting an edit", () => {
    it("Is reachable with the keyboard in view mode", () => {
      const wrapper = initWrapper(BaseDisplayer, {
        logic: { isEditMode: () => false },
      });

      expect(cell(wrapper).attributes("tabindex")).toBe("0");
    });

    it("Is not reachable with the keyboard while its editor is open", () => {
      const wrapper = initWrapper(BaseDisplayer, {
        props: { isView: false },
        logic: { isEditMode: () => false },
      });

      // The cell itself must not be a tab stop while its editor is open, otherwise tabbing out of
      // the editor would give the focus back to the cell.
      expect(cell(wrapper).attributes("tabindex")).toBe("-1");
    });

    it("Stops triggering the actions popover in edit mode", async () => {
      const isEditMode = ref(false);
      const wrapper = initEditModeWrapper({
        logic: { isEditMode: () => isEditMode.value },
      });

      expect(wrapper.vm.$refs.tippy.$props.trigger).toBe(
        "mouseenter focus manual",
      );

      isEditMode.value = true;
      await nextTick();

      expect(wrapper.vm.$refs.tippy.$props.trigger).toBe("manual");
    });

    it("Disables the actions popover in edit mode", async () => {
      const isEditMode = ref(false);
      const wrapper = initEditModeWrapper({
        logic: { isEditMode: () => isEditMode.value },
      });
      const disable = stub(wrapper.vm.$refs.tippy.tippy, "disable");
      const enable = stub(wrapper.vm.$refs.tippy.tippy, "enable");

      isEditMode.value = true;
      await nextTick();
      expect(disable.calledOnce).toBe(true);

      isEditMode.value = false;
      await nextTick();
      expect(enable.calledOnce).toBe(true);
    });

    it("Switches to edit when the cell is focused in edit mode", async () => {
      const start = fake();
      const wrapper = initEditModeWrapper({ editBus: { start } });

      await cell(wrapper).trigger("focusin");

      expect(wrapper.emitted()["update:isView"]).toEqual([[false]]);
      expect(start.calledOnce).toBe(true);
    });

    it("Does not switch to edit when the cell is focused outside of edit mode", async () => {
      const wrapper = initWrapper(BaseDisplayer, {
        logic: { isEditMode: () => false },
      });

      await cell(wrapper).trigger("focusin");

      expect(wrapper.emitted()["update:isView"]).toBeUndefined();
    });

    it("Requests an edit instead of editing when a save is pending", async () => {
      const requestEdit = fake();
      const start = fake();
      const wrapper = initEditModeWrapper({
        editBus: { hasPendingSave: () => true, requestEdit, start },
      });

      await cell(wrapper).trigger("focusin");

      // The pending save is going to refresh the table and re-render this cell, so the edit is
      // only requested and must be resumed by the re-rendered cell.
      expect(requestEdit.calledOnceWith("entry-1", "color")).toBe(true);
      expect(start.called).toBe(false);
      expect(wrapper.emitted()["update:isView"]).toBeUndefined();
    });

    it("Resumes a requested edit when the cell is re-created", () => {
      const enablePendingEdit = fake.returns(true);
      const wrapper = initEditModeWrapper({ editBus: { enablePendingEdit } });

      expect(enablePendingEdit.calledOnceWith("entry-1", "color")).toBe(true);
      expect(wrapper.emitted()["update:isView"]).toEqual([[false]]);
    });

    it("Resumes a requested edit when the entry is refreshed", async () => {
      let pendingEdit = false;
      const wrapper = initEditModeWrapper({
        editBus: { enablePendingEdit: () => pendingEdit },
      });

      expect(wrapper.emitted()["update:isView"]).toBeUndefined();

      // The refresh following a save re-uses the cell as-is and only updates its entry.
      pendingEdit = true;
      await wrapper.setProps({ entry: { color: "blue", age: "13" } });

      expect(wrapper.emitted()["update:isView"]).toEqual([[false]]);
    });

    it("Does not resume an edit outside of edit mode", () => {
      const enablePendingEdit = fake.returns(true);
      initWrapper(BaseDisplayer, {
        logic: { isEditMode: () => false },
        editBus: { enablePendingEdit },
      });

      expect(enablePendingEdit.called).toBe(false);
    });
  });

  describe("Ending an edit", () => {
    it("Saves a new entry on enter", async () => {
      const saveNewEntry = fake.resolves(undefined);
      const addEntry = fake();
      const wrapper = initEditModeWrapper({
        props: { isView: false, entry: { color: "red", _new: true } },
        logic: { saveNewEntry, addEntry },
      });

      await wrapper.find("input").setValue("test-value");
      await wrapper.find(".edit > div").trigger("keydown.enter");
      await flushPromises();

      expect(wrapper.emitted().saveEdit[0]).toEqual(["test-value"]);
      expect(wrapper.emitted()["update:isView"][0]).toEqual([true]);
      expect(saveNewEntry.calledOnce).toBe(true);
      // A plain enter only saves the new entry, it does not start another one.
      expect(addEntry.called).toBe(false);
    });

    it("Saves a new entry and starts another one on ctrl+enter", async () => {
      const saveNewEntry = fake.resolves(undefined);
      const addEntry = fake();
      const wrapper = initEditModeWrapper({
        props: { isView: false, entry: { color: "red", _new: true } },
        logic: { saveNewEntry, addEntry },
      });

      await wrapper
        .find(".edit > div")
        .trigger("keydown.enter", { ctrlKey: true });
      await flushPromises();

      expect(saveNewEntry.calledOnce).toBe(true);
      expect(addEntry.calledOnce).toBe(true);
    });

    it("Does not save the entry on enter when it is not new", async () => {
      const saveNewEntry = fake.resolves(undefined);
      const wrapper = initEditModeWrapper({
        props: { isView: false },
        logic: { saveNewEntry },
      });

      await wrapper.find("input").setValue("test-value");
      await wrapper.find(".edit > div").trigger("keydown.enter");
      await flushPromises();

      // Existing entries are only saved once the whole entry is done editing.
      expect(wrapper.emitted().saveEdit[0]).toEqual(["test-value"]);
      expect(saveNewEntry.called).toBe(false);
    });

    it("Keeps editing when the focus moves inside the cell", async () => {
      const wrapper = initEditModeWrapper({ props: { isView: false } });

      await wrapper.find(".edit > div").trigger("focusout", {
        relatedTarget: wrapper.find("input").element,
      });
      await flushAnimationFrame();

      expect(wrapper.emitted().saveEdit).toBeUndefined();
      expect(wrapper.emitted()["update:isView"]).toBeUndefined();
    });

    it("Applies the edit when the focus moves outside of the cell", async () => {
      const wrapper = initEditModeWrapper({ props: { isView: false } });
      const outside = document.createElement("input");
      document.body.appendChild(outside);

      await wrapper.find("input").setValue("test-value");
      await wrapper
        .find(".edit > div")
        .trigger("focusout", { relatedTarget: outside });
      await flushAnimationFrame();

      expect(wrapper.emitted().saveEdit[0]).toEqual(["test-value"]);
      expect(wrapper.emitted()["update:isView"][0]).toEqual([true]);
    });

    it("Keeps editing when the focus bounces back inside the cell", async () => {
      const wrapper = initEditModeWrapper({ props: { isView: false } });

      // A focusout without a related target while the focus lands back inside the cell is a focus
      // bounce and must not end the edition.
      await wrapper.find(".edit > div").trigger("focusout");
      await flushAnimationFrame();

      expect(wrapper.emitted().saveEdit).toBeUndefined();
    });

    it("Applies the edit when the focus is lost without a related target", async () => {
      const wrapper = initEditModeWrapper({ props: { isView: false } });

      await wrapper.find("input").setValue("test-value");
      wrapper.find("input").element.blur();
      await flushAnimationFrame();

      expect(wrapper.emitted().saveEdit[0]).toEqual(["test-value"]);
      expect(wrapper.emitted()["update:isView"][0]).toEqual([true]);
    });

    it("Keeps editing when the editor has not been focused yet", async () => {
      const wrapper = initEditModeWrapper({ props: { isView: false } });
      // Simulates the actions popover closing right after it opened the editor, i.e. before the
      // editor got the focus.
      wrapper.vm.editorFocused = false;

      await wrapper.find(".edit > div").trigger("focusout");
      await flushAnimationFrame();

      expect(wrapper.emitted().saveEdit).toBeUndefined();
    });
  });
});
