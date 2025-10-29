/**
 * See the LICENSE file distributed with this work for additional
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
import "reflect-metadata";
import CSidebar from "../c-sidebar.vue";
import { config, flushPromises, mount } from "@vue/test-utils";
import { DefaultPageData } from "@xwiki/cristal-api";
import { mockI18n, wrapInSuspense } from "@xwiki/cristal-dev-test-utils";
import { DocumentReference, SpaceReference } from "@xwiki/cristal-model-api";
import { beforeAll, describe, expect, it, vi } from "vitest";
import { mock } from "vitest-mock-extended";
import { ref } from "vue";
import type { WikiConfig } from "@xwiki/cristal-api";
import type { StorageProvider } from "@xwiki/cristal-backend-api";
import type { DocumentService } from "@xwiki/cristal-document-api";
import type {
  ModelReferenceHandler,
  ModelReferenceHandlerProvider,
  ModelReferenceSerializer,
  ModelReferenceSerializerProvider,
} from "@xwiki/cristal-model-reference-api";
import type { Container } from "inversify";

// eslint-disable-next-line max-statements
function mountCComponent() {
  const container = mock<Container>();
  const wikiConfig = mock<WikiConfig>({
    getNewPageDefaultName() {
      return "NewPage";
    },
  });

  const mockDocumentService = mock<DocumentService>({
    getCurrentDocumentReference() {
      return ref(
        new DocumentReference(
          "Reference",
          new SpaceReference(undefined, "The", "Page"),
        ),
      );
    },
  });
  const mockModelReferenceHandlerProvider =
    mock<ModelReferenceHandlerProvider>();
  const mockModelReferenceHandler = mock<ModelReferenceHandler>();
  mockModelReferenceHandlerProvider.get.mockReturnValue(
    mockModelReferenceHandler,
  );

  const mockModelReferenceSerializerProvider =
    mock<ModelReferenceSerializerProvider>();
  const mockModelReferenceSerializer = mock<ModelReferenceSerializer>();
  mockModelReferenceSerializerProvider.get.mockReturnValue(
    mockModelReferenceSerializer,
  );

  const mockStorageProvider = mock<StorageProvider>();

  container.get
    .calledWith("DocumentService")
    .mockReturnValue(mockDocumentService);
  container.get
    .calledWith("ModelReferenceHandlerProvider")
    .mockReturnValue(mockModelReferenceHandlerProvider);
  container.get
    .calledWith("ModelReferenceSerializerProvider")
    .mockReturnValue(mockModelReferenceSerializerProvider);
  container.get
    .calledWith("StorageProvider")
    .mockReturnValue(mockStorageProvider);

  return mount(wrapInSuspense(CSidebar, {}), {
    provide: {
      cristal: {
        getContainer() {
          return container;
        },
        getWikiConfig() {
          return wikiConfig;
        },
        getPage() {
          return new Promise((resolve) => {
            resolve(new DefaultPageData());
          });
        },
      },
    },
    shallow: true,
    global: {
      stubs: {
        CSidebar: false,
        Suspense: false,
        CPageCreationMenu: false,
        XAlert: {
          template: "<div class='alert'><slot></slot></div>",
        },
        XBtn: true,
        XDialog: {
          data() {
            return {
              opened: ref(false),
            };
          },
          template: `<div class="dialog">
            <div @click="opened = true"><slot name="activator"></slot></div>
            <div v-if="opened"><slot></slot></div>
          </div>`,
        },
        XImg: true,
        XForm: {
          emits: ["formSubmit"],
          template: `<form @submit.prevent="$emit('formSubmit')"><slot></slot></form>`,
        },
        XNavigationTree: true,
        XNavigationTreeSelect: true,
        XTextField: true,
        "i18n-t": true,
      },
    },
  });
}

describe("c-sidebar", () => {
  beforeAll(() => {
    vi.mock("vue-i18n");
    mockI18n();
    config.global.renderStubDefaultSlot = true;
  });

  // eslint-disable-next-line max-statements
  it("check new page errors are cleaned on reopen", async () => {
    const component = mountCComponent();

    // Wait for all the asynchronous operations to be terminated before starting
    // to assert the rendered content.
    await flushPromises();

    // We open the new page dialog.
    component.find("#new-page-button").trigger("click");
    await flushPromises();
    // We ensure it has no error displayed.
    expect(
      component.find("#new-page-content .alerts div[class=alert]").exists(),
    ).toBeFalsy();

    // We submit the form and check for an error.
    component.find("#page-creation-form").trigger("submit");
    await flushPromises();
    expect(
      component.find("#new-page-content .alerts div[class=alert]").exists(),
    ).toBeTruthy();

    // We re-open the new page dialog and check the error disappeared.
    component.find("#new-page-button").trigger("click");
    await flushPromises();
    expect(
      component.find("#new-page-content .alerts div[class=alert]").exists(),
    ).toBeFalsy();
  });
});
