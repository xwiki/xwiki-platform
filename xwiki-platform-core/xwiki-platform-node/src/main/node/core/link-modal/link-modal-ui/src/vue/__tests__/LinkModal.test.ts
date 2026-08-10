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
import { beforeEach, describe, expect, it, vi } from "vitest";
import { defineComponent, reactive } from "vue";
import { createI18n } from "vue-i18n";
import type {
  LinkData,
  LinkTargetTypeExtension,
} from "@xwiki/platform-link-modal-api";
import type { Container } from "inversify";

const { getAllAsyncMock } = vi.hoisted(() => ({ getAllAsyncMock: vi.fn() }));

vi.mock("@xwiki/platform-component-manager-default", () => ({
  resolverPromise: Promise.resolve({ getAllAsync: getAllAsyncMock }),
}));

// `LinkModal.vue` resolves domain services out of `depsContainer` via `createLinkEditionContext`; a minimal fake
// satisfying every lookup it performs is enough since none of these services are exercised in this test (the fake
// configuration components below don't use them).
function fakeDepsContainer(): Container {
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
  return { get: (token: string) => providers[token] } as unknown as Container;
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
    component: async () => component,
    tryParseUrl: () => null,
    serializeUrl: () => "",
  };
}

function mountLinkModal(current: LinkData) {
  const i18n = createI18n({ legacy: false, locale: "en", messages: {} });

  return mount(LinkModal, {
    props: { current, depsContainer: fakeDepsContainer() },
    global: { plugins: [i18n] },
  });
}

describe("LinkModal", () => {
  beforeEach(() => {
    getAllAsyncMock.mockReset();
  });

  it("shows a loading state until the registered extensions have resolved", async () => {
    let resolveExtensions!: (value: LinkTargetTypeExtension[]) => void;
    getAllAsyncMock.mockReturnValue(
      new Promise((resolve) => {
        resolveExtensions = resolve;
      }),
    );

    const wrapper = mountLinkModal({
      displayText: "",
      target: { type: "fake", config: {} },
    });

    await flushPromises();
    expect(wrapper.text()).toContain("Loading");

    resolveExtensions([
      fakeExtension(
        "fake",
        "Fake",
        defineComponent({ template: "<div>fake config</div>" }),
      ),
    ]);
    await flushPromises();

    expect(wrapper.text()).not.toContain("Loading");
    expect(wrapper.text()).toContain("fake config");
  });

  it("renders the configuration component matching the current target's type", async () => {
    getAllAsyncMock.mockResolvedValue([
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
    ]);

    const wrapper = mountLinkModal({
      displayText: "",
      target: { type: "b", config: {} },
    });

    await flushPromises();

    expect(wrapper.text()).toContain("component-b");
    expect(wrapper.text()).not.toContain("component-a");
  });

  it("swaps the rendered component when the target's type changes", async () => {
    getAllAsyncMock.mockResolvedValue([
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
    ]);

    // `reactive()` so that mutating `current.target` below goes through the same reactive proxy that
    // `LinkModal.vue` wraps its `current` prop into internally (`ref()` of a plain object creates a *new* proxy,
    // so mutating the original raw object afterwards would go unnoticed).
    const current = reactive<LinkData>({
      displayText: "",
      target: { type: "a", config: {} },
    });
    const wrapper = mountLinkModal(current);
    await flushPromises();
    expect(wrapper.text()).toContain("component-a");

    current.target = { type: "b", config: {} };
    await flushPromises();

    expect(wrapper.text()).toContain("component-b");
    expect(wrapper.text()).not.toContain("component-a");
  });

  it("emits submit with the current link data when the submit button is clicked", async () => {
    getAllAsyncMock.mockResolvedValue([
      fakeExtension(
        "a",
        "A",
        defineComponent({ template: "<div>component-a</div>" }),
      ),
    ]);

    const current: LinkData = {
      displayText: "hello",
      target: { type: "a", config: {} },
    };
    const wrapper = mountLinkModal(current);
    await flushPromises();

    await wrapper.find("[data-test='linkSubmit']").trigger("click");

    expect(wrapper.emitted("submit")).toEqual([[current]]);
  });
});
