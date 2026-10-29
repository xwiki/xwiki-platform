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
  triggerUserImageUpload,
  uploadSelectedFile,
} from "../../misc/fileUpload";
import {
  AttachmentReference,
  DocumentReference,
  SpaceReference,
} from "@xwiki/platform-model-api";
import { DefaultResourceReferenceParser } from "@xwiki/platform-rendering-api";
import { afterEach, describe, expect, test, vi } from "vitest";
import { mock } from "vitest-mock-extended";
import type {
  Attachment,
  AttachmentsService,
} from "@xwiki/platform-attachments-api";
import type { DocumentService } from "@xwiki/platform-document-api";
import type {
  ModelReferenceParserProvider,
  ModelReferenceSerializerProvider,
} from "@xwiki/platform-model-reference-api";
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

/**
 * @returns an attachment reported by the attachments service, named differently than the uploaded file (e.g.
 *   because the server normalized its name)
 */
function buildUploadedAttachment(): Attachment {
  return {
    id: "uploaded",
    name: "uploaded.png",
    mimetype: "image/png",
    href: "https://example.org/attachments/uploaded.png",
    date: new Date(1000),
    size: 12,
    author: undefined,
  };
}

function buildAttachmentsServiceMock(
  uploadedAttachments: (Attachment | undefined)[] | undefined,
): AttachmentsService {
  const attachmentsServiceMock = mock<AttachmentsService>();
  attachmentsServiceMock.upload.mockResolvedValue(uploadedAttachments);
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
  serializedURL: string | undefined,
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
        return serializedURL;
      },
    }),
  };
}

function buildModelReferenceSerializerProvider(
  serializeReferences: boolean,
): ModelReferenceSerializerProvider {
  return {
    get: () => ({
      serialize: (reference) =>
        serializeReferences
          ? `attach:${CURRENT_PAGE_REFERENCE_STRING}@${reference!.name}`
          : undefined,
    }),
  };
}

/** Builds a minimal Container mock covering everything `uploadSelectedFile`/`triggerUserFileUpload` depend on. */
function buildContainer(options: {
  uploadedAttachments?: (Attachment | undefined)[];
  serializedURL?: string;
  // Whether the attachment references can be serialized (true by default).
  serializeReferences?: boolean;
}): Container {
  const container = mock<Container>();

  container.get
    .calledWith("DocumentService")
    .mockReturnValue(buildDocumentServiceMock());
  container.get
    .calledWith("AttachmentsService")
    .mockReturnValue(buildAttachmentsServiceMock(options.uploadedAttachments));
  container.get
    .calledWith("ModelReferenceParserProvider")
    .mockReturnValue(buildModelReferenceParserProvider());
  container.get
    .calledWith("RemoteURLSerializerProvider")
    .mockReturnValue(buildRemoteURLSerializerProvider(options.serializedURL));
  container.get
    .calledWith("ModelReferenceSerializerProvider")
    .mockReturnValue(
      buildModelReferenceSerializerProvider(
        options.serializeReferences ?? true,
      ),
    );
  container.get
    .calledWith("ResourceReferenceParser")
    .mockReturnValue(new DefaultResourceReferenceParser());

  return container;
}

function buildFile(): File {
  return new File(["some content"], "picture.png", { type: "image/png" });
}

function getHiddenFileInput(): HTMLInputElement {
  const input = document.body.querySelector('input[type="file"]');
  expect(input).not.toBeNull();
  return input as HTMLInputElement;
}

function selectFile(input: HTMLInputElement): void {
  Object.defineProperty(input, "files", { value: [buildFile()] });
  input.dispatchEvent(new Event("change"));
}

describe("uploadSelectedFile", () => {
  test("returns the URL and the reference of the attachment reported by the attachment service", async () => {
    const container = buildContainer({
      uploadedAttachments: [buildUploadedAttachment()],
    });

    await expect(uploadSelectedFile(buildFile(), container)).resolves.toEqual({
      url: "https://example.org/attachments/uploaded.png",
      reference: new AttachmentReference(
        "uploaded.png",
        CURRENT_PAGE_REFERENCE,
      ),
    });
  });

  test("falls back to a serialized attachment reference when the upload doesn't resolve a URL", async () => {
    const container = buildContainer({
      uploadedAttachments: undefined,
      serializedURL: "https://example.org/fallback/picture.png",
    });

    await expect(uploadSelectedFile(buildFile(), container)).resolves.toEqual({
      url: "https://example.org/fallback/picture.png",
      reference: new AttachmentReference("picture.png", CURRENT_PAGE_REFERENCE),
    });
  });

  test("also falls back when the upload doesn't report the uploaded file", async () => {
    const container = buildContainer({
      uploadedAttachments: [undefined],
      serializedURL: "https://example.org/fallback/picture.png",
    });

    await expect(uploadSelectedFile(buildFile(), container)).resolves.toEqual({
      url: "https://example.org/fallback/picture.png",
      reference: new AttachmentReference("picture.png", CURRENT_PAGE_REFERENCE),
    });
  });

  test("throws when no URL can be resolved at all", async () => {
    const container = buildContainer({
      uploadedAttachments: undefined,
      serializedURL: undefined,
    });

    await expect(uploadSelectedFile(buildFile(), container)).rejects.toThrow(
      "Internal error: could not get URL for uploaded file",
    );
  });
});

describe("triggerUserFileUpload", () => {
  test("appends a hidden file input, resolves with the uploaded file, and removes the input", async () => {
    const container = buildContainer({
      uploadedAttachments: [buildUploadedAttachment()],
    });

    const promise = triggerUserFileUpload(container);

    const input = getHiddenFileInput();
    expect(input.style.display).toBe("none");

    selectFile(input);

    await expect(promise).resolves.toEqual({
      url: "https://example.org/attachments/uploaded.png",
      reference: new AttachmentReference(
        "uploaded.png",
        CURRENT_PAGE_REFERENCE,
      ),
    });
    expect(document.body.contains(input)).toBe(false);
  });

  test("offers all file types by default", () => {
    triggerUserFileUpload(buildContainer({}));

    const input = getHiddenFileInput();
    expect(input.hasAttribute("accept")).toBe(false);

    input.dispatchEvent(new Event("cancel"));
  });

  test("offers only the accepted file types when specified", () => {
    triggerUserFileUpload(buildContainer({}), "image/*");

    const input = getHiddenFileInput();
    expect(input.accept).toBe("image/*");

    input.dispatchEvent(new Event("cancel"));
  });

  test("removes the input and resolves with undefined when the change event carries no file", async () => {
    const promise = triggerUserFileUpload(buildContainer({}));

    const input = getHiddenFileInput();
    Object.defineProperty(input, "files", { value: [] });
    input.dispatchEvent(new Event("change"));

    await expect(promise).resolves.toBeUndefined();
    expect(document.body.contains(input)).toBe(false);
  });

  test("removes the input and resolves with undefined when the file picker is dismissed", async () => {
    const promise = triggerUserFileUpload(buildContainer({}));

    const input = getHiddenFileInput();
    input.dispatchEvent(new Event("cancel"));

    await expect(promise).resolves.toBeUndefined();
    expect(document.body.contains(input)).toBe(false);
  });

  test("rejects when the upload fails", async () => {
    const promise = triggerUserFileUpload(buildContainer({}));

    const input = getHiddenFileInput();
    selectFile(input);

    await expect(promise).rejects.toThrow(
      "Internal error: could not get URL for uploaded file",
    );
    expect(document.body.contains(input)).toBe(false);
  });
});

describe("triggerUserImageUpload", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  test("offers only images and resolves with the uploaded image's URL and resource reference", async () => {
    const promise = triggerUserImageUpload(
      buildContainer({ uploadedAttachments: [buildUploadedAttachment()] }),
    );

    const input = getHiddenFileInput();
    expect(input.accept).toBe("image/*");
    selectFile(input);

    await expect(promise).resolves.toEqual({
      url: "https://example.org/attachments/uploaded.png",
      reference: {
        type: "attach",
        typed: true,
        reference: "Some.Page@uploaded.png",
        parameters: {},
      },
    });
  });

  test("resolves with only the uploaded image's URL when its reference can't be serialized", async () => {
    const promise = triggerUserImageUpload(
      buildContainer({
        uploadedAttachments: [buildUploadedAttachment()],
        serializeReferences: false,
      }),
    );

    selectFile(getHiddenFileInput());

    await expect(promise).resolves.toEqual({
      url: "https://example.org/attachments/uploaded.png",
      reference: undefined,
    });
  });

  test("resolves with undefined when the file picker is dismissed", async () => {
    const promise = triggerUserImageUpload(buildContainer({}));

    getHiddenFileInput().dispatchEvent(new Event("cancel"));

    await expect(promise).resolves.toBeUndefined();
  });

  test("logs the error and resolves with undefined when the upload fails", async () => {
    const consoleError = vi
      .spyOn(console, "error")
      .mockImplementation(() => {});
    const promise = triggerUserImageUpload(buildContainer({}));

    selectFile(getHiddenFileInput());

    await expect(promise).resolves.toBeUndefined();
    expect(consoleError).toHaveBeenCalledWith(
      "Failed to upload the selected image.",
      expect.objectContaining({
        message: "Internal error: could not get URL for uploaded file",
      }),
    );
  });
});
