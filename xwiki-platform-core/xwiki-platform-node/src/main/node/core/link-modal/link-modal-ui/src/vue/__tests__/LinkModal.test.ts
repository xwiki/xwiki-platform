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
import LinkModal from "../LinkModal.vue";
import { flushPromises, mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";
import { defineComponent, reactive } from "vue";
import { createI18n } from "vue-i18n";
import type {
  LinkData,
  LinkTargetTypeExtension,
} from "@xwiki/platform-link-modal-api";
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
    getLabel: () => label,
    createDefaultConfig: () => ({}),
    component: () => component,
    tryParseUrl: () => null,
    serializeUrl: () => "",
  };
}

function mountLinkModal(
  current: LinkData,
  extensions: LinkTargetTypeExtension[] = [],
) {
  const i18n = createI18n({ legacy: false, locale: "en", messages: {} });

  return mount(LinkModal, {
    props: { current, depsContainer: fakeDepsContainer(extensions) },
    global: { plugins: [i18n] },
  });
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
});
