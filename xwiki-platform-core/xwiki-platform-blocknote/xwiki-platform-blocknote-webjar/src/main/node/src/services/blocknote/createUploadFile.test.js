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
import { BlockNoteDocument } from "./BlockNoteProcessor";
import { DefaultBlockNoteIterator } from "./DefaultBlockNoteIterator";
import { XWikiBlockNoteProcessor } from "./XWikiBlockNoteProcessor";
import { createUploadFile } from "./createUploadFile";
import { extractLinkId, stripLinkId } from "./linkId";
import { beforeEach, describe, expect, it, vi } from "vitest";

function file(name, type) {
  return new File(["content"], name, { type });
}

describe("createUploadFile", () => {
  let blockNoteDocument, saveAttachments, notifyUploadFailed, paste, uploadFile;

  function dependencies() {
    return {
      storageProvider: { get: () => ({ saveAttachments }) },
      modelReferenceParser: {
        parse: (reference) => ({ type: "ATTACHMENT", reference }),
      },
      remoteURLSerializer: {
        serialize: (entityReference) =>
          `/xwiki/bin/download/Space/Page/${entityReference.reference}`,
      },
      getDocumentReference: () => "Space.Page",
      getBlockNoteDocument: () => blockNoteDocument,
      isPaste: () => paste,
      notifyUploadFailed,
    };
  }

  beforeEach(() => {
    blockNoteDocument = new BlockNoteDocument([]);
    paste = false;
    notifyUploadFailed = vi.fn();
    // The storage returns the reference of the created attachment, relative to the edited document.
    saveAttachments = vi.fn(async (page, files) => [files[0].name]);
    uploadFile = createUploadFile(dependencies());
  });

  describe("image files", () => {
    const blockId = "block-id";

    it("points the image block at the uploaded attachment", async () => {
      const result = await uploadFile(file("photo.png", "image/png"), blockId);

      expect(result).toEqual({
        props: { url: "/xwiki/bin/download/Space/Page/photo.png", name: "" },
      });
    });

    it("stores an untyped attachment reference as block metadata", async () => {
      await uploadFile(file("photo.png", "image/png"), blockId);

      expect(blockNoteDocument.getMetadata(blockId)).toEqual({
        // Untyped, so that the image is saved as image:photo.png, like the CKEditor integration does.
        xwikiReference: {
          type: "attach",
          typed: false,
          reference: "photo.png",
          parameters: {},
        },
      });
    });

    it("attaches the file to the edited document", async () => {
      await uploadFile(file("photo.png", "image/png"), blockId);

      expect(saveAttachments).toHaveBeenCalledTimes(1);
      expect(saveAttachments.mock.calls[0][0]).toBe("Space.Page");
    });
  });

  describe("other files", () => {
    const blockId = "block-id";

    it("replaces the file block with a free-standing attachment link", async () => {
      const result = await uploadFile(
        file("report.pdf", "application/pdf"),
        blockId,
      );

      expect(result.type).toBe("paragraph");
      expect(result.content).toHaveLength(1);

      const [link] = result.content;
      expect(link.type).toBe("link");
      expect(link.content).toBe("report.pdf");
      expect(stripLinkId(link.href)).toBe(
        "/xwiki/bin/download/Space/Page/report.pdf",
      );
    });

    it("stores a typed attachment reference as link metadata", async () => {
      const result = await uploadFile(
        file("report.pdf", "application/pdf"),
        blockId,
      );

      const linkId = extractLinkId(result.content[0].href);
      expect(blockNoteDocument.getMetadata(linkId)).toEqual({
        // Typed, otherwise the link would be saved as [[report.pdf]], which is a document link.
        xwikiReference: {
          type: "attach",
          typed: true,
          reference: "report.pdf",
          parameters: {},
        },
        xwikiGeneratedLabel: "report.pdf",
      });
    });

    it("doesn't use the block metadata of the replaced block", async () => {
      await uploadFile(file("report.pdf", "application/pdf"), blockId);

      expect(blockNoteDocument.getMetadata(blockId)).toBeUndefined();
    });
  });

  describe("file names", () => {
    it("renames a pasted file, which the browser names after its media type", async () => {
      paste = true;

      const result = await uploadFile(file("image.png", "image/png"), "id");

      const uploaded = saveAttachments.mock.calls[0][1][0].name;
      expect(uploaded).not.toBe("image.png");
      expect(uploaded).toMatch(/^\d+-[1-9]\d\d\.png$/);
      expect(result.props.url).toBe(
        `/xwiki/bin/download/Space/Page/${uploaded}`,
      );
    });

    it("keeps the name of a dropped file", async () => {
      const result = await uploadFile(file("photo.png", "image/png"), "id");

      expect(saveAttachments.mock.calls[0][1][0].name).toBe("photo.png");
      expect(result.props.url).toBe("/xwiki/bin/download/Space/Page/photo.png");
    });

    it("generates a name for a dropped file that has none", async () => {
      const result = await uploadFile(file("", "image/png"), "id");

      const uploaded = saveAttachments.mock.calls[0][1][0].name;
      expect(uploaded).toMatch(/^\d+-[1-9]\d\d$/);
      expect(result.props.url).toBe(
        `/xwiki/bin/download/Space/Page/${uploaded}`,
      );
    });

    it("labels the link of a nameless file with the generated name", async () => {
      const result = await uploadFile(file("", "application/pdf"), "id");

      const uploaded = saveAttachments.mock.calls[0][1][0].name;
      expect(result.content[0].content).toBe(uploaded);
    });

    it("labels the link with the name the attachment was created with", async () => {
      paste = true;

      const result = await uploadFile(
        file("image.pdf", "application/pdf"),
        "id",
      );

      const uploaded = saveAttachments.mock.calls[0][1][0].name;
      expect(result.content[0].content).toBe(uploaded);
    });
  });

  // What the server receives, which is where the resource reference stored as metadata actually matters.
  describe("saved content", () => {
    let processor;

    beforeEach(() => {
      processor = new XWikiBlockNoteProcessor(new DefaultBlockNoteIterator());
      blockNoteDocument = processor.load("[]");
    });

    // Applies to the document what BlockNote does with the result of the upload, then saves it.
    function save(blockId, uploadResult) {
      blockNoteDocument.content = [
        {
          id: blockId,
          type: "image",
          props: {},
          content: undefined,
          children: [],
          ...uploadResult,
        },
      ];
      return JSON.parse(processor.save(blockNoteDocument));
    }

    it("saves an image with its attachment reference", async () => {
      const result = await uploadFile(file("photo.png", "image/png"), "i1");

      const [image] = save("i1", result);
      expect(image.type).toBe("image");
      expect(image.props.xwikiReference).toEqual({
        type: "attach",
        typed: false,
        reference: "photo.png",
        parameters: {},
      });
      expect(image.props.url).toBe("/xwiki/bin/download/Space/Page/photo.png");
    });

    it("saves another file as a free-standing attachment link without a label", async () => {
      const result = await uploadFile(
        file("report.pdf", "application/pdf"),
        "f1",
      );

      const [paragraph] = save("f1", result);
      expect(paragraph.type).toBe("paragraph");

      const [link] = paragraph.content;
      // The synthetic id is stripped from the href once the metadata has been restored as link props.
      expect(link.href).toBe("/xwiki/bin/download/Space/Page/report.pdf");
      expect(link.props).toEqual({
        xwikiReference: {
          type: "attach",
          typed: true,
          reference: "report.pdf",
          parameters: {},
        },
        // Kept, so that the link is saved as [[attach:report.pdf]] rather than [[report.pdf>>attach:report.pdf]].
        xwikiGeneratedLabel: "report.pdf",
      });
    });
  });

  describe("failures", () => {
    const failures = {
      "the storage throws": () => {
        saveAttachments.mockRejectedValue(new Error("Upload failed"));
      },
      "the storage returns no reference": () => {
        saveAttachments.mockResolvedValue([undefined]);
      },
    };

    Object.entries(failures).forEach(([label, fail]) => {
      describe(`when ${label}`, () => {
        beforeEach(fail);

        it("collapses the inserted block into an empty paragraph", async () => {
          const result = await uploadFile(file("photo.png", "image/png"), "id");

          // BlockNote doesn't catch our errors and updates the block it inserted with whatever we return.
          expect(result).toEqual({ type: "paragraph", content: [] });
        });

        it("notifies the user", async () => {
          await uploadFile(file("photo.png", "image/png"), "id");

          expect(notifyUploadFailed).toHaveBeenCalledTimes(1);
          expect(notifyUploadFailed.mock.calls[0][0]).toBe("photo.png");
        });

        it("leaves no metadata behind", async () => {
          await uploadFile(file("photo.png", "image/png"), "id");

          expect(blockNoteDocument.getMetadata("id")).toBeUndefined();
        });
      });
    });
  });
});
