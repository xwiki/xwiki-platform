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
// @vitest-environment jsdom

import {
  triggerUserFileUpload,
  uploadSelectedFile,
} from "../../misc/fileUpload";
import {
  AttachmentReference,
  DocumentReference,
  SpaceReference,
} from "@xwiki/platform-model-api";
import { describe, expect, test } from "vitest";
import { mock } from "vitest-mock-extended";
import type { AttachmentsService } from "@xwiki/platform-attachments-api";
import type { DocumentService } from "@xwiki/platform-document-api";
import type { ModelReferenceParserProvider } from "@xwiki/platform-model-reference-api";
import type { RemoteURLSerializerProvider } from "@xwiki/platform-model-remote-url-api";
import type { Container } from "inversify";

const CURRENT_PAGE_REFERENCE_STRING = "Some.Page";
const CURRENT_PAGE_REFERENCE = new DocumentReference(
  "Page",
  new SpaceReference(),
);

function buildDocumentServiceMock(): DocumentService {
  const documentServiceMock = mock<DocumentService>();
  documentServiceMock.getCurrentDocumentReferenceString.mockReturnValue({
    value: CURRENT_PAGE_REFERENCE_STRING,
  } as ReturnType<DocumentService["getCurrentDocumentReferenceString"]>);
  return documentServiceMock;
}

function buildAttachmentsServiceMock(
  uploadedUrls: (string | undefined)[] | undefined,
): AttachmentsService {
  const attachmentsServiceMock = mock<AttachmentsService>();
  attachmentsServiceMock.upload.mockResolvedValue(uploadedUrls);
  return attachmentsServiceMock;
}

function buildModelReferenceParserProvider(): ModelReferenceParserProvider {
  return {
    get: () => ({
      parse: () => CURRENT_PAGE_REFERENCE,
      async parseAsync() {
        return CURRENT_PAGE_REFERENCE;
      },
    }),
  };
}

function buildRemoteURLSerializerProvider(
  serializedUrl: string | undefined,
): RemoteURLSerializerProvider {
  return {
    get: () => ({
      serialize: (reference) => {
        // Only the fallback path (no URL returned by the upload) should ever reach this,
        // and it must serialize an AttachmentReference built from the file name.
        expect(reference).toBeInstanceOf(AttachmentReference);
        expect((reference as AttachmentReference).document).toBe(
          CURRENT_PAGE_REFERENCE,
        );
        return serializedUrl;
      },
    }),
  };
}

/** Builds a minimal Container mock covering everything `uploadSelectedFile`/`triggerUserFileUpload` depend on. */
function buildContainer(options: {
  uploadedUrls?: (string | undefined)[];
  serializedUrl?: string;
}): Container {
  const container = mock<Container>();

  container.get
    .calledWith("DocumentService")
    .mockReturnValue(buildDocumentServiceMock());
  container.get
    .calledWith("AttachmentsService")
    .mockReturnValue(buildAttachmentsServiceMock(options.uploadedUrls));
  container.get
    .calledWith("ModelReferenceParserProvider")
    .mockReturnValue(buildModelReferenceParserProvider());
  container.get
    .calledWith("RemoteURLSerializerProvider")
    .mockReturnValue(buildRemoteURLSerializerProvider(options.serializedUrl));

  return container;
}

function buildFile(): File {
  return new File(["some content"], "picture.png", { type: "image/png" });
}

/** Resolves to whether `promise` had already settled once pending microtasks were flushed. */
async function isSettled(promise: Promise<unknown>): Promise<boolean> {
  let settled = false;
  void promise.finally(() => {
    settled = true;
  });

  // Let any pending microtasks from the (async) event handler under test flush.
  await Promise.resolve();
  await Promise.resolve();

  return settled;
}

describe("uploadSelectedFile", () => {
  test("returns the attachment service's resolved URL when it provides one", async () => {
    const container = buildContainer({
      uploadedUrls: ["https://example.org/attachments/picture.png"],
    });

    await expect(uploadSelectedFile(buildFile(), container)).resolves.toEqual({
      url: "https://example.org/attachments/picture.png",
    });
  });

  test("falls back to a serialized attachment reference when the upload doesn't resolve a URL", async () => {
    const container = buildContainer({
      uploadedUrls: undefined,
      serializedUrl: "https://example.org/fallback/picture.png",
    });

    await expect(uploadSelectedFile(buildFile(), container)).resolves.toEqual({
      url: "https://example.org/fallback/picture.png",
    });
  });

  test("also falls back when the upload resolves an array without a URL for the file", async () => {
    const container = buildContainer({
      uploadedUrls: [undefined],
      serializedUrl: "https://example.org/fallback/picture.png",
    });

    await expect(uploadSelectedFile(buildFile(), container)).resolves.toEqual({
      url: "https://example.org/fallback/picture.png",
    });
  });

  test("throws when no URL can be resolved at all", async () => {
    const container = buildContainer({
      uploadedUrls: undefined,
      serializedUrl: undefined,
    });

    await expect(uploadSelectedFile(buildFile(), container)).rejects.toThrow(
      "Internal error: could not get URL for uploaded file",
    );
  });
});

describe("triggerUserFileUpload", () => {
  function getHiddenFileInput(): HTMLInputElement {
    const input = document.body.querySelector('input[type="file"]');
    expect(input).not.toBeNull();
    return input as HTMLInputElement;
  }

  test("appends a hidden file input, resolves with the uploaded URL, and removes the input", async () => {
    const container = buildContainer({
      uploadedUrls: ["https://example.org/attachments/picture.png"],
    });

    const promise = triggerUserFileUpload(container);

    const input = getHiddenFileInput();
    expect(input.style.display).toBe("none");

    Object.defineProperty(input, "files", { value: [buildFile()] });
    input.dispatchEvent(new Event("change"));

    await expect(promise).resolves.toEqual({
      url: "https://example.org/attachments/picture.png",
    });
    expect(document.body.contains(input)).toBe(false);
  });

  test("removes the input without resolving when the change event carries no file", async () => {
    const container = buildContainer({});
    const promise = triggerUserFileUpload(container);

    const input = getHiddenFileInput();
    Object.defineProperty(input, "files", { value: [] });
    input.dispatchEvent(new Event("change"));

    expect(await isSettled(promise)).toBe(false);
    expect(document.body.contains(input)).toBe(false);
  });

  test("removes the input without resolving when the file picker is dismissed", async () => {
    const container = buildContainer({});
    const promise = triggerUserFileUpload(container);

    const input = getHiddenFileInput();
    input.dispatchEvent(new Event("cancel"));

    expect(await isSettled(promise)).toBe(false);
    expect(document.body.contains(input)).toBe(false);
  });
});
