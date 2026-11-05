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
import { dsComponentStubs } from "./dsStubs";
import LinkForm from "../LinkForm.vue";
import LinkModal from "../LinkModal.vue";
import { flushPromises, mount } from "@vue/test-utils";
import { unsupportedLinkTargetType } from "@xwiki/platform-link-type-api";
import { describe, expect, it } from "vitest";
import { defineComponent, reactive } from "vue";
import { createI18n } from "vue-i18n";
import type {
  LinkData,
  LinkTargetTypeExtension,
} from "@xwiki/platform-link-type-api";
import type { Container } from "inversify";

// `LinkModal.vue` resolves domain services, as well as the registered `LinkTargetTypeExtension`s, out of the
// same `depsContainer`; a minimal fake satisfying every lookup it performs is enough since none of the domain
// services are exercised in this test (the fake configuration components below don't use them).
function fakeDepsContainer(extensions: LinkTargetTypeExtension[]): Container {
  const noopProvider = { get: () => null };
  const providers: Record<string, unknown> = {
    LinkSuggestServiceProvider: noopProvider,
    ModelReferenceParserProvider: noopProvider,
    ModelReferenceSerializerProvider: noopProvider,
    ModelReferenceHandlerProvider: noopProvider,
    RemoteURLParserProvider: noopProvider,
    RemoteURLSerializerProvider: noopProvider,
    AttachmentsService: {},
    DocumentService: {},
  };
  return {
    get: (token: string) => providers[token],
    getAll: () => extensions,
  } as unknown as Container;
}

function fakeExtension(
  type: string,
  label: string,
  component: ReturnType<typeof defineComponent>,
): LinkTargetTypeExtension {
  return {
    type,
    getLabel: async () => label,
    createDefaultConfig: () => ({}),
    component: () => component,
    tryParseURL: () => null,
    serializeURL: () => "",
  };
}

function mountLinkModal(
  current: LinkData,
  extensions: LinkTargetTypeExtension[] = [],
  excludedLinkTargetTypes?: string[],
) {
  const i18n = createI18n({ legacy: false, locale: "en", messages: {} });

  return mount(LinkModal, {
    props: {
      current,
      depsContainer: fakeDepsContainer(extensions),
      excludedLinkTargetTypes,
    },
    global: { plugins: [i18n], components: dsComponentStubs },
  });
}

/**
 * @returns a fake link target type whose form renders the link type selector (see LinkForm)
 */
function fakeExtensionWithLinkForm(
  type: string,
  label: string,
): LinkTargetTypeExtension {
  return fakeExtension(
    type,
    label,
    defineComponent({
      components: { LinkForm },
      props: ["linkData"],
      template: '<LinkForm :link-data="linkData" />',
    }),
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
    const extensions = [
      fakeExtension(
        "a",
        "A",
        defineComponent({ template: "<div>component-a</div>" }),
      ),
      fakeExtension(
        "b",
        "B",
        defineComponent({ template: "<div>component-b</div>" }),
      ),
    ];

    const wrapper = mountLinkModal(
      { displayText: "", target: { type: "b", config: {} } },
      extensions,
    );

    // No `flushPromises()`/`nextTick()` wait: everything resolves synchronously, so the correct component is
    // already rendered right after `mount()` returns.
    expect(wrapper.text()).toContain("component-b");
    expect(wrapper.text()).not.toContain("component-a");
  });

  it("swaps the rendered component when the target's type changes", async () => {
    const extensions = [
      fakeExtension(
        "a",
        "A",
        defineComponent({ template: "<div>component-a</div>" }),
      ),
      fakeExtension(
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
    const wrapper = mountLinkModal(current, extensions);
    expect(wrapper.text()).toContain("component-a");

    current.target = { type: "b", config: {} };
    // Vue batches and flushes reactive DOM updates on the next tick, even when the underlying computation is
    // synchronous — this waits for that render, not for any extension/component resolution.
    await flushPromises();

    expect(wrapper.text()).toContain("component-b");
    expect(wrapper.text()).not.toContain("component-a");
  });

  it("emits submit with the current link data when the submit button is clicked", async () => {
    const extensions = [
      fakeExtension(
        "a",
        "A",
        defineComponent({ template: "<div>component-a</div>" }),
      ),
    ];

    const current: LinkData = {
      displayText: "hello",
      target: { type: "a", config: {} },
    };
    const wrapper = mountLinkModal(current, extensions);

    await wrapper.find("[data-test='linkSubmit']").trigger("click");

    expect(wrapper.emitted("submit")).toEqual([[current]]);
  });

  it("edits the reference of a link to an unsupported resource as plain text, keeping its type", async () => {
    const wrapper = mountLinkModal(unsupportedLinkData(), [
      fakeExtension(
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
    // A link target type whose form renders the link type selector, to switch back to the unsupported type.
    const wrapper = mountLinkModal(unsupportedLinkData(), [
      fakeExtensionWithLinkForm("a", "A"),
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
        fakeExtensionWithLinkForm(type, type.toUpperCase()),
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
        fakeExtensionWithLinkForm(type, type.toUpperCase()),
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
});
