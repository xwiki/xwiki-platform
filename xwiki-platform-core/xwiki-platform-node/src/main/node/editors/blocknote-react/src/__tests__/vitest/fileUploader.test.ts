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
  fileUploadListenerName,
  fileUploaderName,
  getFileUploader,
  uploadFile,
} from "../../files/fileUploader";
import {
  pickAndUploadFile,
  uploadedFileLink,
  uploadedImage,
} from "../../files/uploadedFile";
import {
  AttachmentReference,
  DocumentReference,
  SpaceReference,
} from "@xwiki/platform-model-api";
import { DefaultResourceReferenceParser } from "@xwiki/platform-rendering-api";
import { afterEach, describe, expect, test, vi } from "vitest";
import { mock } from "vitest-mock-extended";
import type { EditorInstance } from "../../editorInstance";
import type {
  FileUploadListener,
  FileUploader,
} from "../../files/fileUploader";
import type {
  Attachment,
  AttachmentsService,
} from "@xwiki/platform-attachments-api";
import type { DocumentService } from "@xwiki/platform-document-api";
import type { EntityReference } from "@xwiki/platform-model-api";
import type {
  ModelReferenceParserProvider,
  ModelReferenceSerializerProvider,
} from "@xwiki/platform-model-reference-api";
import type { Container } from "inversify";

const EDITED_PAGE = "Edited.Page";

const CURRENT_PAGE = "Current.Page";

const UPLOADED_URL = "https://example.org/attachments/uploaded.png";

/**
 * @returns an attachment reported by the attachments service, named differently than the uploaded file (e.g.
 *   because the server normalized its name)
 */
function buildUploadedAttachment(): Attachment {
  return {
    id: "uploaded",
    name: "uploaded.png",
    mimetype: "image/png",
    href: UPLOADED_URL,
    date: new Date(1000),
    size: 12,
    author: undefined,
  };
}

function buildFile(): File {
  return new File(["some content"], "picture.png", { type: "image/png" });
}

/**
 * Builds an editor instance whose service container provides what the default file uploader depends on.
 */
function buildInstance(
  options: {
    // Whether the edited content has a source document (the current page is used otherwise).
    sourceDocument?: boolean;
    // The upload option of the editor instance.
    upload?: boolean;
    // Whether the upload fails.
    failUpload?: boolean;
    // Whether the attachment references can be serialized (true by default).
    serializeReferences?: boolean;
    // The file uploader bound in the service container, if any.
    fileUploader?: FileUploader;
    // Whether the attachments service is bound (true by default).
    attachmentsService?: boolean;
    // The file upload listeners bound in the service container.
    listeners?: FileUploadListener[];
  } = {},
): { instance: EditorInstance; attachmentsService: AttachmentsService } {
  const container = mock<Container>();
  const attachmentsService = buildAttachmentsService(!!options.failUpload);
  mockBound(container, {
    AttachmentsService:
      options.attachmentsService === false ? undefined : attachmentsService,
    [fileUploaderName]: options.fileUploader,
    [fileUploadListenerName]: options.listeners,
  });
  container.getAll.mockImplementation((name) =>
    name === fileUploadListenerName ? (options.listeners ?? []) : [],
  );
  mockReferenceServices(container, options.serializeReferences !== false);

  const instance = {
    options: {
      upload: options.upload,
      sourceDocumentReference: options.sourceDocument
        ? new DocumentReference(EDITED_PAGE, new SpaceReference())
        : undefined,
    },
    serviceContainer: container,
  } as unknown as EditorInstance;
  return { instance, attachmentsService };
}

function buildAttachmentsService(failUpload: boolean): AttachmentsService {
  const attachmentsService = mock<AttachmentsService>();
  if (failUpload) {
    attachmentsService.upload.mockRejectedValue(new Error("Upload failed"));
  } else {
    attachmentsService.upload.mockResolvedValue([buildUploadedAttachment()]);
  }
  return attachmentsService;
}

/**
 * Mocks the services that are bound or not, depending on the test.
 */
function mockBound(
  container: ReturnType<typeof mock<Container>>,
  bound: Record<string, unknown>,
) {
  container.isBound.mockImplementation(
    (name) => bound[name as string] !== undefined,
  );
  for (const [name, service] of Object.entries(bound)) {
    container.get.calledWith(name).mockReturnValue(service);
  }
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
      serialize: (reference: EntityReference) => {
        if (reference instanceof AttachmentReference) {
          return serializeReferences
            ? `${reference.document.name}@${reference.name}`
            : undefined;
        }
        return (reference as DocumentReference).name;
      },
    }),
  } as unknown as ModelReferenceSerializerProvider);
  container.get
    .calledWith("ResourceReferenceParser")
    .mockReturnValue(new DefaultResourceReferenceParser());
  container.get.calledWith("DocumentService").mockReturnValue({
    getCurrentDocumentReferenceString: () => ({ value: CURRENT_PAGE }),
  } as unknown as DocumentService);
}

function selectFile(): File {
  const input = document.body.querySelector(
    'input[type="file"]',
  ) as HTMLInputElement;
  const file = buildFile();
  Object.defineProperty(input, "files", { value: [file] });
  input.dispatchEvent(new Event("change"));
  return file;
}

describe("getFileUploader", () => {
  test("returns the bound file uploader", () => {
    const fileUploader = mock<FileUploader>();
    const { instance } = buildInstance({ fileUploader });

    expect(getFileUploader(instance)).toBe(fileUploader);
  });

  test("returns nothing when uploading is disabled, even when a file uploader is bound", () => {
    const { instance } = buildInstance({
      upload: false,
      fileUploader: mock<FileUploader>(),
    });

    expect(getFileUploader(instance)).toBeUndefined();
  });

  test("returns nothing when there's nothing to store the files with", () => {
    const { instance } = buildInstance({ attachmentsService: false });

    expect(getFileUploader(instance)).toBeUndefined();
  });
});

describe("the default file uploader", () => {
  test("attaches the file to the source document and returns the stored file", async () => {
    const { instance, attachmentsService } = buildInstance({
      sourceDocument: true,
    });
    const file = buildFile();

    // The name is the one of the attachment, rather than the one of the uploaded file.
    await expect(
      getFileUploader(instance)!.upload(instance, file),
    ).resolves.toEqual({
      url: UPLOADED_URL,
      name: "uploaded.png",
      reference: "Edited.Page@uploaded.png",
    });
    expect(attachmentsService.upload).toHaveBeenCalledWith(EDITED_PAGE, [file]);
  });

  test("attaches the file to the current page when there's no source document", async () => {
    const { instance, attachmentsService } = buildInstance();
    const file = buildFile();

    await expect(
      getFileUploader(instance)!.upload(instance, file),
    ).resolves.toMatchObject({ reference: "Current.Page@uploaded.png" });
    expect(attachmentsService.upload).toHaveBeenCalledWith(CURRENT_PAGE, [
      file,
    ]);
  });

  test("returns no reference when the attachment reference can't be serialized", async () => {
    const { instance } = buildInstance({ serializeReferences: false });

    await expect(
      getFileUploader(instance)!.upload(instance, buildFile()),
    ).resolves.toEqual({
      url: UPLOADED_URL,
      name: "uploaded.png",
      reference: undefined,
    });
  });

  test("rejects when the upload fails", async () => {
    const { instance } = buildInstance({ failUpload: true });

    await expect(
      getFileUploader(instance)!.upload(instance, buildFile()),
    ).rejects.toThrow("Upload failed");
  });
});

describe("uploadFile", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  function listener(): FileUploadListener {
    return { onUpload: vi.fn(), onUploadFailed: vi.fn() };
  }

  test("tells the listeners about the stored file", async () => {
    const listeners = [listener(), listener()];
    const { instance } = buildInstance({ listeners });

    const uploadedFile = await uploadFile(instance, buildFile());

    expect(uploadedFile).toMatchObject({ url: UPLOADED_URL });
    for (const { onUpload, onUploadFailed } of listeners) {
      expect(onUpload).toHaveBeenCalledWith(instance, uploadedFile);
      expect(onUploadFailed).not.toHaveBeenCalled();
    }
  });

  test("tells the listeners about the failure, and rejects", async () => {
    const listeners = [listener(), listener()];
    const { instance } = buildInstance({ listeners, failUpload: true });
    const file = buildFile();

    await expect(uploadFile(instance, file)).rejects.toThrow("Upload failed");
    for (const { onUpload, onUploadFailed } of listeners) {
      expect(onUploadFailed).toHaveBeenCalledWith(
        instance,
        file,
        expect.objectContaining({ message: "Upload failed" }),
      );
      expect(onUpload).not.toHaveBeenCalled();
    }
  });

  test("tells the other listeners when one of them fails, and listeners can ignore an event", async () => {
    const consoleError = vi
      .spyOn(console, "error")
      .mockImplementation(() => {});
    const failing: FileUploadListener = {
      onUpload: () => {
        throw new Error("Listener failed");
      },
    };
    const other = listener();
    const { instance } = buildInstance({
      listeners: [failing, {}, other],
    });

    await uploadFile(instance, buildFile());

    expect(other.onUpload).toHaveBeenCalledOnce();
    expect(consoleError).toHaveBeenCalledWith(
      "A file upload listener failed.",
      expect.objectContaining({ message: "Listener failed" }),
    );
  });

  test("rejects when the user can't upload files", async () => {
    const { instance } = buildInstance({ upload: false });

    await expect(uploadFile(instance, buildFile())).rejects.toThrow(
      "Uploading files is disabled",
    );
  });
});

describe("pickAndUploadFile", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  test("uploads the selected file", async () => {
    const { instance, attachmentsService } = buildInstance({
      sourceDocument: true,
    });

    const promise = pickAndUploadFile(instance, "image/*", "Failed.");
    const file = selectFile();

    await expect(promise).resolves.toMatchObject({ url: UPLOADED_URL });
    expect(attachmentsService.upload).toHaveBeenCalledWith(EDITED_PAGE, [file]);
  });

  test("logs the error and resolves with undefined when the upload fails", async () => {
    const consoleError = vi
      .spyOn(console, "error")
      .mockImplementation(() => {});
    const { instance } = buildInstance({ failUpload: true });

    const promise = pickAndUploadFile(instance, undefined, "Failed.");
    selectFile();

    await expect(promise).resolves.toBeUndefined();
    expect(consoleError).toHaveBeenCalledWith(
      "Failed.",
      expect.objectContaining({ message: "Upload failed" }),
    );
  });

  test("doesn't open the file picker when the user can't upload files", async () => {
    const { instance } = buildInstance({ upload: false });

    await expect(
      pickAndUploadFile(instance, undefined, "Failed."),
    ).resolves.toBeUndefined();
    expect(document.body.querySelector('input[type="file"]')).toBeNull();
  });
});

describe("the uploaded files", () => {
  const uploadedFile = {
    url: UPLOADED_URL,
    name: "uploaded.png",
    reference: "Edited.Page@uploaded.png",
  };

  test("are inserted as images with an untyped attachment reference", () => {
    const { instance } = buildInstance({ sourceDocument: true });

    // Untyped, like the references of the image suggestions, since an image designates an attachment by default.
    expect(uploadedImage(instance, uploadedFile)).toEqual({
      url: UPLOADED_URL,
      reference: {
        type: "attach",
        typed: false,
        reference: "Edited.Page@uploaded.png",
        parameters: {},
      },
    });
  });

  test("are linked with a typed attachment reference and a label generated from their name", () => {
    const { instance } = buildInstance({ sourceDocument: true });

    // Typed, since a link designates a page by default.
    expect(uploadedFileLink(instance, uploadedFile)).toMatchObject({
      title: "uploaded.png",
      generatedTitle: true,
      url: UPLOADED_URL,
      reference: { type: "attach", typed: true },
    });
  });

  test("without a reference are inserted and linked by their URL", () => {
    const { instance } = buildInstance();
    const { url, name } = uploadedFile;

    expect(uploadedImage(instance, { url, name }).reference).toBeUndefined();
    expect(uploadedFileLink(instance, { url, name }).reference).toBeUndefined();
  });
});
