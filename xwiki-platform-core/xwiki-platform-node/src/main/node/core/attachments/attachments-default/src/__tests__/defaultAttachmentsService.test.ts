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
import { DefaultAttachmentsService } from "../defaultAttachmentsService";
import { DocumentReference, SpaceReference } from "@xwiki/platform-model-api";
import { createPinia, setActivePinia } from "pinia";
import { beforeEach, describe, expect, it } from "vitest";
import { mock, mockReset } from "vitest-mock-extended";
import type { Storage } from "@xwiki/platform-api";
import type { StorageProvider } from "@xwiki/platform-backend-api";
import type { AttachmentReference } from "@xwiki/platform-model-api";
import type {
  ModelReferenceParserProvider,
  ModelReferenceSerializerProvider,
} from "@xwiki/platform-model-reference-api";
import type { RemoteURLSerializerProvider } from "@xwiki/platform-model-remote-url-api";

function initServiceWithMocks({ resolveURLs = true } = {}) {
  const storageProviderMock = mock<StorageProvider>();
  const storageMock = mock<Storage>();

  storageMock.saveAttachments.mockReturnValue(Promise.resolve(undefined));

  storageProviderMock.get.mockReturnValue(storageMock);
  const modelReferenceParserProvider = {
    get: () => ({
      parse: (page: string) =>
        new DocumentReference(page, new SpaceReference()),
    }),
  } as unknown as ModelReferenceParserProvider;
  const modelReferenceSerializerProvider = {
    get: () => ({
      serialize: (reference: AttachmentReference) =>
        `${reference.document.name}@${reference.name}`,
    }),
  } as unknown as ModelReferenceSerializerProvider;
  const remoteURLSerializerProvider = {
    get: () => ({
      serialize: (reference: AttachmentReference) =>
        resolveURLs
          ? `/download/${reference.document.name}/${reference.name}`
          : undefined,
    }),
  } as unknown as RemoteURLSerializerProvider;
  const service = new DefaultAttachmentsService(
    storageProviderMock,
    modelReferenceParserProvider,
    modelReferenceSerializerProvider,
    remoteURLSerializerProvider,
  );
  return { storageMock, service };
}

describe("defaultAttachmentsService", () => {
  beforeEach(() => {
    // creates a fresh pinia and makes it active
    // so it's automatically picked up by any useStore() call
    // without having to pass it to it: `useStore(pinia)`
    setActivePinia(createPinia());
  });

  const pageReference = "my.page";
  it("attachments are refresh before upload method returns", async () => {
    const { storageMock, service } = initServiceWithMocks();
    await service.refresh(pageReference);
    // Reset the mock to only spy the mocks during the upload call.
    mockReset(storageMock);
    await service.upload(pageReference, []);
    expect(service.isLoading().value).eq(false);

    // Check individually each method before checking if they are called in the
    // right order, to make it easier to debug in case of failure.
    expect(storageMock.saveAttachments).toHaveBeenCalled();
    expect(storageMock.getAttachments).toHaveBeenCalled();
    expect(storageMock.saveAttachments).toHaveBeenCalledBefore(
      storageMock.getAttachments,
    );
  });

  it("upload returns the uploaded attachments reported by the storage", async () => {
    const { storageMock, service } = initServiceWithMocks();
    const date = new Date(1000);
    storageMock.saveAttachments.mockResolvedValue([
      {
        id: "wiki:my.page@cat.png",
        reference: "cat.png",
        mimetype: "image/png",
        href: "/download/my/page/cat.png",
        size: 42,
        date,
        author: "XWiki.Alice",
      },
    ]);

    const uploadedAttachments = await service.upload(pageReference, [
      new File([""], "cat.png"),
    ]);

    expect(uploadedAttachments).toEqual([
      {
        id: "wiki:my.page@cat.png",
        name: "cat.png",
        mimetype: "image/png",
        href: "/download/my/page/cat.png",
        size: 42,
        date,
        author: { name: "XWiki.Alice" },
      },
    ]);
  });

  it("upload describes the uploaded files that the storage doesn't describe", async () => {
    const { storageMock, service } = initServiceWithMocks();
    storageMock.saveAttachments.mockResolvedValue([
      {
        id: "my.page@cat.png",
        reference: "cat.png",
        mimetype: "image/png",
        href: "/download/my.page/cat.png",
        size: 42,
        date: new Date(1000),
        author: undefined,
      },
      undefined,
    ]);

    const uploadedAttachments = await service.upload(pageReference, [
      new File(["cat"], "cat.png", { type: "image/png" }),
      new File(["dog!"], "dog.jpg", { type: "image/jpeg" }),
    ]);

    expect(uploadedAttachments[0].name).toBe("cat.png");
    // Assumed to be named after the uploaded file, attached to the page it was uploaded to.
    expect(uploadedAttachments[1]).toEqual({
      id: "my.page@dog.jpg",
      name: "dog.jpg",
      mimetype: "image/jpeg",
      href: "/download/my.page/dog.jpg",
      size: 4,
      date: expect.any(Date),
      author: undefined,
    });
  });

  it("upload describes all the uploaded files when the storage doesn't describe them", async () => {
    const { service } = initServiceWithMocks();

    const uploadedAttachments = await service.upload(pageReference, [
      new File(["cat"], "cat.png", { type: "image/png" }),
    ]);

    expect(uploadedAttachments.map(({ href }) => href)).toEqual([
      "/download/my.page/cat.png",
    ]);
  });

  it("upload fails when the URL of an uploaded file can't be resolved", async () => {
    const { service } = initServiceWithMocks({ resolveURLs: false });

    await expect(
      service.upload(pageReference, [new File(["cat"], "cat.png")]),
    ).rejects.toThrow(
      "Failed to compute the URL of the uploaded file [cat.png].",
    );
  });
});
