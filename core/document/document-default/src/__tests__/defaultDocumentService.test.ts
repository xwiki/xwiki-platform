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
import { DefaultDocumentService } from "../defaultDocumentService";
import { flushPromises } from "@vue/test-utils";
import {
  ModelReferenceParser,
  ModelReferenceParserProvider,
  ModelReferenceSerializer,
  ModelReferenceSerializerProvider,
} from "@xwiki/cristal-model-reference-api";
import { Container } from "inversify";
import { createPinia, setActivePinia } from "pinia";
import { beforeEach, describe, expect, it } from "vitest";
import { MockProxy, any, mock } from "vitest-mock-extended";
import type { CristalApp, PageData } from "@xwiki/cristal-api";

function initMocks(): MockProxy<CristalApp> & CristalApp {
  const cristalMock = mock<CristalApp>();
  const containerMock = mock<Container>();
  const modelReferenceParserProviderMock = mock<ModelReferenceParserProvider>();
  modelReferenceParserProviderMock.get.mockReturnValue(
    mock<ModelReferenceParser>(),
  );
  const modelReferenceSerializerProviderMock =
    mock<ModelReferenceSerializerProvider>();
  modelReferenceSerializerProviderMock.get.mockReturnValue(
    mock<ModelReferenceSerializer>(),
  );

  containerMock.get
    .calledWith("ModelReferenceParserProvider")
    .mockReturnValue(modelReferenceParserProviderMock);
  containerMock.get
    .calledWith("ModelReferenceSerializerProvider")
    .mockReturnValue(modelReferenceSerializerProviderMock);
  cristalMock.getContainer.mockReturnValue(containerMock);

  return cristalMock;
}

describe("defaultDocumentService", () => {
  beforeEach(() => {
    // creates a fresh pinia and makes it active so it's automatically picked up by any useStore() call without having
    // to pass it to it: `useStore(pinia)`
    setActivePinia(createPinia());
  });
  it("setCurrentDocument", async () => {
    const mockPageData = mock<PageData>();
    const cristalMock = initMocks();
    cristalMock.getPage
      .calledWith("A.B.C", any())
      .mockReturnValue(Promise.resolve(mockPageData));
    const defaultDocumentService = new DefaultDocumentService(cristalMock);

    const currentDocument = defaultDocumentService.getCurrentDocument();

    expect(currentDocument.value).toBeUndefined();
    defaultDocumentService.setCurrentDocument("A.B.C");
    // Wait for the asynchronous store operations
    await flushPromises();
    expect(currentDocument.value).toBe(mockPageData);
  });
});
