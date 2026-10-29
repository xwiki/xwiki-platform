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
import type { Container } from "inversify";

const EDITED_PAGE = "Edited.Page";

const CURRENT_PAGE = "Current.Page";

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

/** Builds a minimal Container mock covering everything the upload functions depend on. */
function buildContainer(
  options: {
    // Whether the upload fails.
    failUpload?: boolean;
    // Whether the attachment references can be serialized (true by default).
    serializeReferences?: boolean;
  } = {},
): { container: Container; attachmentsService: AttachmentsService } {
  const container = mock<Container>();

  const attachmentsService = mock<AttachmentsService>();
  if (options.failUpload) {
    attachmentsService.upload.mockRejectedValue(new Error("Upload failed"));
  } else {
    attachmentsService.upload.mockResolvedValue([buildUploadedAttachment()]);
  }
  container.get
    .calledWith("AttachmentsService")
    .mockReturnValue(attachmentsService);
  mockReferenceServices(container, options.serializeReferences !== false);

  return { container, attachmentsService };
}

/**
 * Mocks the services used to resolve the page to upload to and to build the reference of the uploaded attachment.
 */
function mockReferenceServices(
  container: ReturnType<typeof mock<Container>>,
  serializeReferences: boolean,
) {
  container.get.calledWith("ModelReferenceParserProvider").mockReturnValue({
    get: () => ({
      parse: (page: string) =>
        new DocumentReference(page, new SpaceReference()),
    }),
  } as unknown as ModelReferenceParserProvider);
  container.get.calledWith("ModelReferenceSerializerProvider").mockReturnValue({
    get: () => ({
      serialize: (reference: AttachmentReference) =>
        serializeReferences
          ? `attach:${reference.document.name}@${reference.name}`
          : undefined,
    }),
  } as unknown as ModelReferenceSerializerProvider);
  container.get
    .calledWith("ResourceReferenceParser")
    .mockReturnValue(new DefaultResourceReferenceParser());
  container.get.calledWith("DocumentService").mockReturnValue({
    getCurrentDocumentReferenceString: () => ({ value: CURRENT_PAGE }),
  } as unknown as DocumentService);
}

function buildFile(): File {
  return new File(["some content"], "picture.png", { type: "image/png" });
}

function getHiddenFileInput(): HTMLInputElement {
  const input = document.body.querySelector('input[type="file"]');
  expect(input).not.toBeNull();
  return input as HTMLInputElement;
}

function selectFile(input: HTMLInputElement): File {
  const file = buildFile();
  Object.defineProperty(input, "files", { value: [file] });
  input.dispatchEvent(new Event("change"));
  return file;
}

describe("triggerUserFileUpload", () => {
  test("appends a hidden file input, uploads the selected file to the given page, and removes the input", async () => {
    const { container, attachmentsService } = buildContainer();

    const promise = triggerUserFileUpload(container, EDITED_PAGE);

    const input = getHiddenFileInput();
    expect(input.style.display).toBe("none");

    const file = selectFile(input);

    await expect(promise).resolves.toEqual(buildUploadedAttachment());
    expect(attachmentsService.upload).toHaveBeenCalledWith(EDITED_PAGE, [file]);
    expect(document.body.contains(input)).toBe(false);
  });

  test("offers all file types by default", () => {
    triggerUserFileUpload(buildContainer().container, EDITED_PAGE);

    const input = getHiddenFileInput();
    expect(input.hasAttribute("accept")).toBe(false);

    input.dispatchEvent(new Event("cancel"));
  });

  test("offers only the accepted file types when specified", () => {
    triggerUserFileUpload(buildContainer().container, EDITED_PAGE, "image/*");

    const input = getHiddenFileInput();
    expect(input.accept).toBe("image/*");

    input.dispatchEvent(new Event("cancel"));
  });

  test("removes the input and resolves with undefined when the change event carries no file", async () => {
    const promise = triggerUserFileUpload(
      buildContainer().container,
      EDITED_PAGE,
    );

    const input = getHiddenFileInput();
    Object.defineProperty(input, "files", { value: [] });
    input.dispatchEvent(new Event("change"));

    await expect(promise).resolves.toBeUndefined();
    expect(document.body.contains(input)).toBe(false);
  });

  test("removes the input and resolves with undefined when the file picker is dismissed", async () => {
    const promise = triggerUserFileUpload(
      buildContainer().container,
      EDITED_PAGE,
    );

    const input = getHiddenFileInput();
    input.dispatchEvent(new Event("cancel"));

    await expect(promise).resolves.toBeUndefined();
    expect(document.body.contains(input)).toBe(false);
  });

  test("rejects when the upload fails", async () => {
    const promise = triggerUserFileUpload(
      buildContainer({ failUpload: true }).container,
      EDITED_PAGE,
    );

    const input = getHiddenFileInput();
    selectFile(input);

    await expect(promise).rejects.toThrow("Upload failed");
    expect(document.body.contains(input)).toBe(false);
  });
});

describe("triggerUserImageUpload", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  test("offers only images and resolves with the uploaded image's URL and resource reference", async () => {
    const promise = triggerUserImageUpload(
      buildContainer().container,
      EDITED_PAGE,
    );

    const input = getHiddenFileInput();
    expect(input.accept).toBe("image/*");
    selectFile(input);

    // The reference of the uploaded image is built from the page it was uploaded to and the name of the uploaded
    // attachment.
    await expect(promise).resolves.toEqual({
      url: "https://example.org/attachments/uploaded.png",
      reference: {
        type: "attach",
        typed: true,
        reference: "Edited.Page@uploaded.png",
        parameters: {},
      },
    });
  });

  test("uploads the image to the current page when the page isn't specified", async () => {
    const { container, attachmentsService } = buildContainer();
    const promise = triggerUserImageUpload(container);

    const file = selectFile(getHiddenFileInput());

    await expect(promise).resolves.toMatchObject({
      reference: { reference: "Current.Page@uploaded.png" },
    });
    expect(attachmentsService.upload).toHaveBeenCalledWith(CURRENT_PAGE, [
      file,
    ]);
  });

  test("resolves with only the uploaded image's URL when its reference can't be serialized", async () => {
    const promise = triggerUserImageUpload(
      buildContainer({ serializeReferences: false }).container,
      EDITED_PAGE,
    );

    selectFile(getHiddenFileInput());

    await expect(promise).resolves.toEqual({
      url: "https://example.org/attachments/uploaded.png",
      reference: undefined,
    });
  });

  test("resolves with undefined when the file picker is dismissed", async () => {
    const promise = triggerUserImageUpload(
      buildContainer().container,
      EDITED_PAGE,
    );

    getHiddenFileInput().dispatchEvent(new Event("cancel"));

    await expect(promise).resolves.toBeUndefined();
  });

  test("logs the error and resolves with undefined when the upload fails", async () => {
    const consoleError = vi
      .spyOn(console, "error")
      .mockImplementation(() => {});
    const promise = triggerUserImageUpload(
      buildContainer({ failUpload: true }).container,
      EDITED_PAGE,
    );

    selectFile(getHiddenFileInput());

    await expect(promise).resolves.toBeUndefined();
    expect(consoleError).toHaveBeenCalledWith(
      "Failed to upload the selected image.",
      expect.objectContaining({ message: "Upload failed" }),
    );
  });
});
