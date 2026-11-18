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
import { XWikiFileUploader } from "./XWikiFileUploader";
import { temporaryUploadsName } from "./temporaryUploads";
import { ConnectionStatus } from "@xwiki/platform-collaboration-api";
import { toCristalEntityReference } from "@xwiki/platform-model-xwiki";
import { EntityType, Model } from "@xwiki/platform-xwiki-model-api";
import { beforeEach, describe, expect, it, vi } from "vitest";

function file(name) {
  return new File(["content"], name, { type: "image/png" });
}

// What the storage returns for an uploaded file: the attachment, described as the REST API describes attachments.
function uploadedAttachment(name) {
  return { reference: name, href: `/xwiki/bin/download/Space/Page/${name}` };
}

describe("XWikiFileUploader", () => {
  let saveAttachments, temporaryUploads, fileUploader;

  /**
   * @param options - the options of the editor instance
   * @param withTemporaryUploads - whether the editor instance records its temporary uploads
   * @returns an editor instance editing the content of the source document, when specified
   */
  function editorInstance(
    { sourceDocumentReference, ...options } = {},
    withTemporaryUploads = true,
  ) {
    return {
      serviceContainer: {
        isBound: (name) =>
          withTemporaryUploads && name === temporaryUploadsName,
        get: () => temporaryUploads,
      },
      options: {
        sourceDocumentReference:
          sourceDocumentReference &&
          toCristalEntityReference(
            Model.resolve(sourceDocumentReference, EntityType.DOCUMENT),
          ),
        ...options,
      },
    };
  }

  // Connected to a realtime session.
  function collaboration(connectionStatus) {
    return { connectionStatus: { value: connectionStatus } };
  }

  beforeEach(() => {
    saveAttachments = vi.fn(async (_page, [{ name }]) => [
      uploadedAttachment(name),
    ]);
    temporaryUploads = { add: vi.fn() };
    global.XWiki = {
      currentDocument: {
        documentReference: Model.resolve(
          "wiki:Space.Page",
          EntityType.DOCUMENT,
        ),
      },
    };
    fileUploader = new XWikiFileUploader({ get: () => ({ saveAttachments }) });
  });

  it("attaches the file to the current document and returns the stored file", async () => {
    const uploadedFile = await fileUploader.upload(
      editorInstance(),
      file("cat.png"),
    );

    expect(saveAttachments.mock.calls[0][0]).toBe("wiki:Space.Page");
    expect(uploadedFile).toEqual({
      url: "/xwiki/bin/download/Space/Page/cat.png",
      name: "cat.png",
      reference: "wiki:Space.Page@cat.png",
    });
  });

  it("attaches the file to the source document, when specified", async () => {
    const uploadedFile = await fileUploader.upload(
      editorInstance({ sourceDocumentReference: "wiki:Other.Page" }),
      file("cat.png"),
    );

    expect(saveAttachments.mock.calls[0][0]).toBe("wiki:Other.Page");
    expect(uploadedFile.reference).toBe("wiki:Other.Page@cat.png");
  });

  it("escapes a file name the reference syntax would otherwise split", async () => {
    const uploadedFile = await fileUploader.upload(
      editorInstance(),
      file("logo@2x.png"),
    );

    expect(uploadedFile.reference).toBe("wiki:Space.Page@logo\\@2x.png");
  });

  it("returns the name the storage gave the file", async () => {
    saveAttachments.mockResolvedValue([uploadedAttachment("cat-1.png")]);

    const uploadedFile = await fileUploader.upload(
      editorInstance(),
      file("cat.png"),
    );

    expect(uploadedFile.name).toBe("cat-1.png");
  });

  describe("temporary uploads", () => {
    it("attaches the file right away when the editor instance has no name", async () => {
      await fileUploader.upload(editorInstance(), file("cat.png"));

      expect(saveAttachments.mock.calls[0][2]).toEqual({ temporary: false });
      expect(temporaryUploads.add).not.toHaveBeenCalled();
    });

    it("attaches the file right away when editing in realtime", async () => {
      await fileUploader.upload(
        editorInstance({
          name: "content",
          collaboration: collaboration(ConnectionStatus.Connected),
        }),
        file("cat.png"),
      );

      expect(saveAttachments.mock.calls[0][2]).toEqual({ temporary: false });
    });

    it("uploads to the temporary store and records the name to commit, the realtime session being disconnected", async () => {
      saveAttachments.mockResolvedValue([uploadedAttachment("cat-1.png")]);

      await fileUploader.upload(
        editorInstance({
          name: "content",
          collaboration: collaboration(ConnectionStatus.Disconnected),
        }),
        file("cat.png"),
      );

      expect(saveAttachments.mock.calls[0][2]).toEqual({ temporary: true });
      expect(temporaryUploads.add).toHaveBeenCalledWith("cat-1.png");
    });

    it("uploads to the temporary store even when nothing records the temporary uploads", async () => {
      await expect(
        fileUploader.upload(
          editorInstance({ name: "content" }, false),
          file("cat.png"),
        ),
      ).resolves.toMatchObject({ name: "cat.png" });
    });
  });

  describe("failures", () => {
    const failures = {
      "the storage fails": () => saveAttachments.mockRejectedValue(new Error()),
      "the storage reports no attachment": () =>
        saveAttachments.mockResolvedValue([]),
    };

    for (const [label, fail] of Object.entries(failures)) {
      describe(`when ${label}`, () => {
        it("rejects without recording a temporary upload", async () => {
          fail();

          await expect(
            fileUploader.upload(
              editorInstance({ name: "content" }),
              file("cat.png"),
            ),
          ).rejects.toThrow();
          expect(temporaryUploads.add).not.toHaveBeenCalled();
        });
      });
    }
  });
});
