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
import { XDialogStub, dsComponentStubs } from "./dsStubs";
import { linkTargetTypeFormRole } from "../../linkTargetTypeForm";
import LinkModal from "../LinkModal.vue";
import { enableAutoUnmount, flushPromises, mount } from "@vue/test-utils";
import {
  linkTargetTypeManagerRole,
  unsupportedLinkTargetType,
} from "@xwiki/platform-link-type-api";
import { afterEach, describe, expect, it } from "vitest";
import { defineComponent, reactive } from "vue";
import { createI18n } from "vue-i18n";
import type { LinkData, LinkTargetType } from "@xwiki/platform-link-type-api";
import type { Container } from "inversify";
import type { Component } from "vue";

// A fake link target type, along with the target field and option fields of its form, if any.
type FakeLinkTargetType = LinkTargetType & {
  fakeTargetField?: Component;
  fakeOptionFields?: Component;
};

// `LinkModal.vue` resolves the registered `LinkTargetType`s (through the link target type manager) and their forms out
// of the `depsContainer`, which it passes to the fields; a minimal fake satisfying these lookups is enough since the
// fake fields below don't resolve anything.
function fakeDepsContainer(targetTypes: FakeLinkTargetType[]): Container {
  const components: Record<string, unknown> = {
    [linkTargetTypeManagerRole]: { list: () => targetTypes },
  };
  const forms = targetTypes.flatMap((targetType) =>
    targetType.fakeTargetField
      ? [
          {
            type: targetType.type,
            targetField: () => targetType.fakeTargetField,
            optionFields: targetType.fakeOptionFields
              ? () => targetType.fakeOptionFields
              : undefined,
          },
        ]
      : [],
  );
  return {
    get: (token: string) => components[token],
    isBound: (token: string) =>
      token === linkTargetTypeFormRole ? forms.length > 0 : true,
    getAll: (token: string) => (token === linkTargetTypeFormRole ? forms : []),
  } as unknown as Container;
}

/**
 * @param targetField - the target field of the form of the link target type, or `undefined` to register it without a
 *   form
 * @param optionFields - the option fields of the form of the link target type, if any
 */
function fakeLinkTargetType(
  type: string,
  label: string,
  targetField?: Component,
  optionFields?: Component,
): FakeLinkTargetType {
  return {
    type,
    getLabel: async () => label,
    createDefaultConfig: () => ({}),
    fromURL: () => null,
    toURL: () => "",
    fakeTargetField: targetField,
    fakeOptionFields: optionFields,
  };
}

function mountLinkModal(
  current: LinkData,
  targetTypes: FakeLinkTargetType[] = [],
  excludedLinkTargetTypes?: string[],
  mode?: "createNew" | "editExisting",
) {
  const i18n = createI18n({ legacy: false, locale: "en", messages: {} });

  return mount(LinkModal, {
    props: {
      current,
      depsContainer: fakeDepsContainer(targetTypes),
      excludedLinkTargetTypes,
      mode,
    },
    global: { plugins: [i18n], components: dsComponentStubs },
    // The submit button designates its form by id, which requires them to be in the document.
    attachTo: document.body,
  });
}

enableAutoUnmount(afterEach);

/**
 * @returns a fake link target type with a form displaying its label
 */
function fakeLinkTargetTypeWithForm(
  type: string,
  label: string,
): FakeLinkTargetType {
  return fakeLinkTargetType(
    type,
    label,
    defineComponent({ template: `<div>form-${type}</div>` }),
  );
}

/**
 * @returns the data of a link to a resource that no link target type supports
 */
function unsupportedLinkData(): LinkData {
  return {
    displayText: "XWiki",
    target: {
      type: unsupportedLinkTargetType,
      config: {
        reference: {
          type: "interwiki",
          typed: true,
          reference: "wikipedia:XWiki",
          parameters: { anchor: "History" },
        },
      },
    },
  };
}

/**
 * @returns the value and the label of each option of the link type selector
 */
function linkTypeOptions(wrapper: ReturnType<typeof mountLinkModal>) {
  return wrapper
    .get("[data-test='linkTargetType']")
    .findAll("option")
    .map((option) => [option.attributes("value"), option.text()]);
}

describe("LinkModal", () => {
  it("renders the configuration component matching the current target's type, on the first render", () => {
    const targetTypes = [
      fakeLinkTargetType(
        "a",
        "A",
        defineComponent({ template: "<div>component-a</div>" }),
      ),
      fakeLinkTargetType(
        "b",
        "B",
        defineComponent({ template: "<div>component-b</div>" }),
      ),
    ];

    const wrapper = mountLinkModal(
      { displayText: "", target: { type: "b", config: {} } },
      targetTypes,
    );

    // No `flushPromises()`/`nextTick()` wait: everything resolves synchronously, so the correct component is
    // already rendered right after `mount()` returns.
    expect(wrapper.text()).toContain("component-b");
    expect(wrapper.text()).not.toContain("component-a");
  });

  it("swaps the rendered component when the target's type changes", async () => {
    const targetTypes = [
      fakeLinkTargetType(
        "a",
        "A",
        defineComponent({ template: "<div>component-a</div>" }),
      ),
      fakeLinkTargetType(
        "b",
        "B",
        defineComponent({ template: "<div>component-b</div>" }),
      ),
    ];

    // `reactive()` so that mutating `current.target` below goes through the same reactive proxy that
    // `LinkModal.vue` wraps its `current` prop into internally (`ref()` of a plain object creates a *new* proxy,
    // so mutating the original raw object afterwards would go unnoticed).
    const current = reactive<LinkData>({
      displayText: "",
      target: { type: "a", config: {} },
    });
    const wrapper = mountLinkModal(current, targetTypes);
    expect(wrapper.text()).toContain("component-a");

    current.target = { type: "b", config: {} };
    // Vue batches and flushes reactive DOM updates on the next tick, even when the underlying computation is
    // synchronous — this waits for that render, not for any link target type/component resolution.
    await flushPromises();

    expect(wrapper.text()).toContain("component-b");
    expect(wrapper.text()).not.toContain("component-a");
  });

  it("emits submit with the current link data when the submit button is clicked", async () => {
    const targetTypes = [
      fakeLinkTargetType(
        "a",
        "A",
        defineComponent({ template: "<div>component-a</div>" }),
      ),
    ];

    const current: LinkData = {
      displayText: "hello",
      target: { type: "a", config: {} },
    };
    const wrapper = mountLinkModal(current, targetTypes);

    await wrapper.find("[data-test='linkSubmit']").trigger("click");

    expect(wrapper.emitted("submit")).toEqual([[current]]);
  });

  it("edits the reference of a link to an unsupported resource as plain text, keeping its type", async () => {
    const wrapper = mountLinkModal(unsupportedLinkData(), [
      fakeLinkTargetType(
        "a",
        "A",
        defineComponent({ template: "<div>component-a</div>" }),
      ),
    ]);

    await wrapper.get("[data-test='linkReference']").setValue("wikipedia:Wiki");
    await wrapper.find("[data-test='linkSubmit']").trigger("click");

    expect(wrapper.text()).not.toContain("component-a");
    expect(wrapper.emitted("submit")?.[0]?.[0]).toEqual({
      displayText: "XWiki",
      target: {
        type: unsupportedLinkTargetType,
        config: {
          reference: {
            type: "interwiki",
            typed: true,
            reference: "wikipedia:Wiki",
            parameters: { anchor: "History" },
          },
        },
      },
    });
  });

  it("offers an unsupported resource type first, as is, and restores its reference when reselected", async () => {
    const wrapper = mountLinkModal(unsupportedLinkData(), [
      fakeLinkTargetTypeWithForm("a", "A"),
    ]);
    await flushPromises();
    expect(linkTypeOptions(wrapper)).toEqual([
      [unsupportedLinkTargetType, "interwiki"],
      ["a", "A"],
    ]);

    await wrapper.get("[data-test='linkReference']").setValue("wikipedia:Wiki");
    await wrapper.get("[data-test='linkTargetType']").setValue("a");
    await wrapper
      .get("[data-test='linkTargetType']")
      .setValue(unsupportedLinkTargetType);

    expect(
      (wrapper.get("[data-test='linkReference']").element as HTMLInputElement)
        .value,
    ).toBe("wikipedia:XWiki");
  });

  it("doesn't offer the excluded link target types, ignoring the unknown ones", async () => {
    const wrapper = mountLinkModal(
      { displayText: "", target: { type: "a", config: {} } },
      ["a", "b", "c"].map((type) =>
        fakeLinkTargetTypeWithForm(type, type.toUpperCase()),
      ),
      ["b", "unknown"],
    );
    await flushPromises();

    expect(linkTypeOptions(wrapper)).toEqual([
      ["a", "A"],
      ["c", "C"],
    ]);
  });

  it("offers the type of the edited link even when it's excluded, to edit the link with it", async () => {
    const wrapper = mountLinkModal(
      { displayText: "", target: { type: "b", config: {} } },
      ["a", "b"].map((type) =>
        fakeLinkTargetTypeWithForm(type, type.toUpperCase()),
      ),
      ["b"],
    );
    await flushPromises();
    expect(linkTypeOptions(wrapper)).toEqual([
      ["a", "A"],
      ["b", "B"],
    ]);

    // Still offered after switching to another link target type, so that the user can switch back.
    await wrapper.get("[data-test='linkTargetType']").setValue("a");
    await flushPromises();
    expect(linkTypeOptions(wrapper)).toEqual([
      ["a", "A"],
      ["b", "B"],
    ]);
  });

  it("edits a link target type registered without a form with a plain text field labelled with its label", async () => {
    const wrapper = mountLinkModal(
      {
        displayText: "Issue",
        target: { type: "jira", config: { location: "XWIKI-1" } },
      },
      [fakeLinkTargetType("jira", "Jira issue")],
    );
    await flushPromises();

    const field = wrapper.get("[data-test='linkLocation']");
    expect((field.element as HTMLInputElement).value).toBe("XWIKI-1");
    expect(
      field.element.parentElement?.querySelector("label")?.textContent,
    ).toBe("Jira issue");

    await field.setValue("XWIKI-2");
    await wrapper.find("[data-test='linkSubmit']").trigger("click");
    expect(wrapper.emitted("submit")?.[0]?.[0]).toMatchObject({
      target: { type: "jira", config: { location: "XWIKI-2" } },
    });
  });

  it("renders the shared fields once, and the fields and options of the selected link target type", async () => {
    const wrapper = mountLinkModal(
      { displayText: "", target: { type: "a", config: {} } },
      [
        fakeLinkTargetType(
          "a",
          "A",
          defineComponent({ template: "<div>fields-a</div>" }),
          defineComponent({ template: "<div>options-a</div>" }),
        ),
        fakeLinkTargetTypeWithForm("b", "B"),
      ],
    );
    await flushPromises();

    expect(wrapper.findAll("[data-test='linkTargetType']")).toHaveLength(1);
    expect(wrapper.text()).toContain("fields-a");
    expect(wrapper.get("details").text()).toContain("options-a");

    await wrapper.get("[data-test='linkTargetType']").setValue("b");
    expect(wrapper.text()).toContain("form-b");
    expect(wrapper.text()).not.toContain("options-a");
  });

  describe("fields of the selected link target type", () => {
    it("passes its container to the target and option fields, to resolve the services they need", async () => {
      const targetField = defineComponent({
        props: ["depsContainer"],
        template: "<div>fields-a</div>",
      });
      const optionFields = defineComponent({
        props: ["depsContainer"],
        template: "<div>options-a</div>",
      });
      const wrapper = mountLinkModal(
        { displayText: "", target: { type: "a", config: {} } },
        [fakeLinkTargetType("a", "A", targetField, optionFields)],
      );
      await flushPromises();

      const depsContainer = wrapper.props("depsContainer");
      expect(wrapper.getComponent(targetField).props("depsContainer")).toBe(
        depsContainer,
      );
      expect(wrapper.getComponent(optionFields).props("depsContainer")).toBe(
        depsContainer,
      );
    });

    it("asks the target field, not the option fields, to focus itself when the dialog opens", async () => {
      const targetField = defineComponent({
        props: { autofocus: Boolean },
        template: "<div>fields-a</div>",
      });
      const optionFields = defineComponent({
        props: { autofocus: Boolean },
        template: "<div>options-a</div>",
      });
      const wrapper = mountLinkModal(
        { displayText: "", target: { type: "a", config: {} } },
        [fakeLinkTargetType("a", "A", targetField, optionFields)],
      );
      await flushPromises();

      expect(wrapper.getComponent(targetField).props("autofocus")).toBe(true);
      expect(wrapper.getComponent(optionFields).props("autofocus")).toBe(false);
    });
  });
});

describe("LinkModal display text", () => {
  /**
   * @returns a link target type whose links are labelled with their configured name, or that doesn't compute labels
   *   when the name is `undefined`
   */
  function labellingLinkTargetType(
    getDefaultLabel?: (config: { name?: string }) => string,
  ): FakeLinkTargetType {
    return {
      ...fakeLinkTargetTypeWithForm("a", "A"),
      toURL: (config) =>
        `https://example.org/${(config as { name: string }).name}`,
      getDefaultLabel: getDefaultLabel as LinkTargetType["getDefaultLabel"],
    };
  }

  async function submit(wrapper: ReturnType<typeof mountLinkModal>) {
    await wrapper.find("[data-test='linkSubmit']").trigger("click");
    return wrapper.emitted("submit")?.[0]?.[0];
  }

  it("is submitted as typed", async () => {
    const wrapper = mountLinkModal(
      {
        displayText: "Some text",
        target: { type: "a", config: { name: "Page" } },
      },
      [labellingLinkTargetType(({ name }) => name ?? "")],
    );

    expect(await submit(wrapper)).toMatchObject({ displayText: "Some text" });
  });

  it("is replaced, when left empty or blank, by the default label of the link target, also used as placeholder", async () => {
    const wrapper = mountLinkModal(
      {
        displayText: "Some text",
        target: { type: "a", config: { name: "Page" } },
      },
      [labellingLinkTargetType(({ name }) => `Label of ${name}`)],
    );

    await wrapper.get("[data-test='linkDisplayText']").setValue("  ");

    expect(
      wrapper.get("[data-test='linkDisplayText']").attributes("placeholder"),
    ).toBe("Label of Page");
    expect(await submit(wrapper)).toEqual({
      displayText: "Label of Page",
      target: { type: "a", config: { name: "Page" } },
    });
  });

  it("is replaced, when left empty, by the URL of the link for the link target types that don't compute labels", async () => {
    const wrapper = mountLinkModal(
      { displayText: "", target: { type: "a", config: { name: "Page" } } },
      [labellingLinkTargetType()],
    );

    expect(await submit(wrapper)).toMatchObject({
      displayText: "https://example.org/Page",
    });
  });
});

describe("LinkModal dialog", () => {
  it("is displayed in a dialog that stays open when clicking outside of it", () => {
    const wrapper = mountLinkModal({
      displayText: "hello",
      target: { type: "a", config: {} },
    });

    const dialog = wrapper.findComponent(XDialogStub);
    expect(dialog.props("modelValue")).toBe(true);
    expect(dialog.props("persistent")).toBe(true);
  });

  it("emits cancel when the dialog is closed otherwise than with its buttons", async () => {
    const wrapper = mountLinkModal({
      displayText: "hello",
      target: { type: "a", config: {} },
    });

    // E.g. with the Escape key or the close button of the dialog.
    wrapper.findComponent(XDialogStub).vm.$emit("update:modelValue", false);
    await flushPromises();

    expect(wrapper.emitted("cancel")).toEqual([[]]);
    expect(wrapper.emitted("submit")).toBeUndefined();
  });

  it("titles itself and labels its submit button after the creation or the edition of a link", () => {
    const current: LinkData = {
      displayText: "hello",
      target: { type: "a", config: {} },
    };

    const insertModal = mountLinkModal(current);
    expect(insertModal.getComponent(XDialogStub).props("title")).toBe(
      "Insert link",
    );
    expect(insertModal.get("[data-test='linkSubmit']").text()).toBe("Insert");

    const editModal = mountLinkModal(current, [], undefined, "editExisting");
    expect(editModal.getComponent(XDialogStub).props("title")).toBe(
      "Edit link",
    );
    expect(editModal.get("[data-test='linkSubmit']").text()).toBe("Update");
  });

  it("stays open, without emitting submit, while a required field is empty", async () => {
    const wrapper = mountLinkModal(
      { displayText: "hello", target: { type: "a", config: {} } },
      [
        fakeLinkTargetType(
          "a",
          "A",
          defineComponent({
            template: `<input data-test="requiredTarget" required />`,
          }),
        ),
      ],
    );

    await wrapper.find("[data-test='linkSubmit']").trigger("click");
    expect(wrapper.emitted("submit")).toBeUndefined();

    await wrapper.get("[data-test='requiredTarget']").setValue("target");
    await wrapper.find("[data-test='linkSubmit']").trigger("click");
    expect(wrapper.emitted("submit")?.[0]?.[0]).toMatchObject({
      displayText: "hello",
    });
  });

  it("emits cancel when the cancel button is clicked", async () => {
    const wrapper = mountLinkModal({
      displayText: "hello",
      target: { type: "a", config: {} },
    });

    await wrapper.find("[data-test='linkCancel']").trigger("click");

    expect(wrapper.emitted("cancel")).toEqual([[]]);
  });
});
