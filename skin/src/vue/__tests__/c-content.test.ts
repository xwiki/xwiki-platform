/*
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
import CContent from "../c-content.vue";
import { config, mount } from "@vue/test-utils";
import { PageData } from "@xwiki/cristal-api";
import {
  makeInjectable,
  mockI18n,
  wrapInSuspense,
} from "@xwiki/cristal-dev-test-utils";
import { DocumentService } from "@xwiki/cristal-document-api";
import {
  PageHierarchyResolver,
  PageHierarchyResolverProvider,
} from "@xwiki/cristal-hierarchy-api";
import { MarkdownRenderer } from "@xwiki/cristal-markdown-api";
import { ClickListener } from "@xwiki/cristal-model-click-listener";
import { Container } from "inversify";
import { DeepPartial } from "ts-essentials";
import { beforeAll, describe, expect, it, vi } from "vitest";
import { ref } from "vue";
import { useRoute } from "vue-router";

// TODO: reduce the number of statements in the following method and reactivate the disabled eslint rule.
// eslint-disable-next-line max-statements
function mountCComponent(params: {
  isLoading?: boolean;
  error?: string;
  currentDocument?: PageData;
}) {
  const isLoading = params.isLoading || false;
  const error = params.error || undefined;
  const currentDocument = params.currentDocument || undefined;

  const container = new Container();

  const mockDocumentService = makeInjectable(
    class MockDocumentService implements DeepPartial<DocumentService> {
      isLoading() {
        return ref(isLoading);
      }

      getError() {
        if (error) {
          return ref<Error>(new Error(error));
        } else {
          return ref<undefined>();
        }
      }

      getCurrentDocument() {
        return ref(currentDocument);
      }

      getCurrentDocumentRevision() {
        return ref(undefined);
      }

      getCurrentDocumentReferenceString() {
        return ref("The.Page.Reference");
      }
    },
  );

  const pageHierarchyResolver = vi.fn().mockImplementation(() => {
    const newVar: DeepPartial<PageHierarchyResolver> = {
      async getPageHierarchy() {
        return [];
      },
    };
    return newVar;
  });

  const pageHierarchyResolverProvider = vi.fn().mockImplementation(() => {
    return {
      get() {
        return new pageHierarchyResolver();
      },
    };
  });

  container.bind<DocumentService>("DocumentService").to(mockDocumentService);
  container
    .bind<PageHierarchyResolverProvider>("PageHierarchyResolverProvider")
    .to(makeInjectable(pageHierarchyResolverProvider));
  container.bind<ClickListener>("ClickListener").to(
    makeInjectable(
      vi.fn().mockImplementation(() => {
        return {
          handleHTMLElement() {},
        };
      }),
    ),
  );
  class MockMarkdownRenderer implements DeepPartial<MarkdownRenderer> {}
  container
    .bind<MarkdownRenderer>("MarkdownRenderer")
    .to(makeInjectable(vi.fn().mockImplementation(() => MockMarkdownRenderer)));

  return mount(wrapInSuspense(CContent, {}), {
    provide: {
      cristal: {
        getContainer() {
          return container;
        },
      },
    },
    shallow: true,
    global: {
      stubs: {
        CContent: false,
        Suspense: false,
        CArticle: false,
        UIX: true,
        XBtn: true,
        XAvatar: true,
        RouterLink: true,
        XAlert: true,
        XBreadcrumb: true,
        "i18n-t": true,
      },
    },
  });
}

describe("c-context", () => {
  beforeAll(() => {
    vi.mock("vue-i18n");
    mockI18n();
    vi.mock("vue-router");
    useRoute.mockReturnValue({
      params: {
        page: "testPage",
      },
    });
    config.global.renderStubDefaultSlot = true;
  });

  it("display a message on missing page", () => {
    const component = mountCComponent({});

    expect(component.find(".doc-content.unknown-page").text()).eq(
      "The requested page could not be found. You can edit the page to create it.",
    );

    expect(
      component.find("u-i-extensions-stub[uixname='content.after']").exists(),
    ).eq(true);
  });

  it("display a loading message", () => {
    const component = mountCComponent({ isLoading: true });
    expect(component.find("h3").text()).eq("article.loading");
  });

  it("display an error message", () => {
    const errorMessage = "ErrorMessage";
    const component = mountCComponent({ error: errorMessage });
    expect(component.find(".content-error").text()).eq(
      `Error: ${errorMessage}`,
    );
  });

  it("display page with empty content", () => {
    const component = mountCComponent({
      currentDocument: new (vi.fn().mockImplementation(() => {
        return {};
      }))(),
    });
    expect(component.find("#xwikicontent").text()).eq("");
    expect(component.find(".doc-info-extra").exists()).eq(true);
  });

  it("display page with html content", () => {
    const component = mountCComponent({
      currentDocument: new (vi.fn().mockImplementation(() => {
        return {
          html: "<strong>content</strong>",
        };
      }))(),
    });

    // Both assert that the content is
    expect(component.find("#xwikicontent").text()).eq("content");
    expect(component.find(".doc-info-extra").exists()).eq(true);
  });
});
