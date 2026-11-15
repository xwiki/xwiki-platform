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
import { DefaultImageWizard } from "./DefaultImageWizard";
import { EntityType, Model } from "@xwiki/platform-xwiki-model-api";
import { beforeEach, describe, expect, it, vi } from "vitest";
// Import for the side effect of registering the global define / requirejs used to load the legacy image wizard.
import "../../testUtils";

// eslint-disable-next-line max-statements
describe("DefaultImageWizard", () => {
  let capturedOptions;
  let resolvedImageData;
  let uploadedAttachments;
  let uploadTarget;
  let wizard;

  beforeEach(() => {
    capturedOptions = undefined;
    resolvedImageData = undefined;
    uploadedAttachments = [];
    uploadTarget = undefined;

    // Register a fake legacy image wizard that captures its options and resolves with a chosen image data.
    global.define("xwiki-wysiwyg-image-wizard", () => (options) => {
      capturedOptions = options;
      return Promise.resolve(resolvedImageData);
    });

    global.XWiki = {
      // A legacy document whose URLs point to the given document reference.
      Document: class {
        constructor(reference) {
          this.reference = reference;
        }
        getURL(action, parameters) {
          const path = [
            ...this.reference
              .getReversedReferenceChain()
              .filter((entity) => entity.type === EntityType.SPACE)
              .map((space) => space.name),
            this.reference.name,
          ].join("/");
          return `/xwiki/bin/${action}/${path}?${parameters.toString()}`;
        }
      },
      currentDocument: {
        documentReference: Model.resolve(
          "wiki:Space.Page",
          EntityType.DOCUMENT,
        ),
        getURL: (action, parameters) =>
          `/xwiki/bin/${action}/Space/Page?${parameters.toString()}`,
      },
    };

    const resourceReferenceParser = {
      parse: () => ({ type: "attach", typed: true, reference: "" }),
    };
    const storageProvider = {
      get: () => ({
        saveAttachments: async (page) => {
          uploadTarget = page;
          return uploadedAttachments;
        },
      }),
    };
    wizard = new DefaultImageWizard(resourceReferenceParser, storageProvider);
  });

  function edit(image, context) {
    return new Promise((resolve) => {
      wizard.edit(
        image,
        { submit: resolve, cancel: () => resolve(undefined) },
        context,
      );
    });
  }

  describe("insert", () => {
    it("reports the inserted image", async () => {
      const reference = { type: "attach", typed: false, reference: "cat.png" };
      // No width nor alignment set for the new image.
      resolvedImageData = {
        width: "",
        src: "catURL",
        resourceReference: reference,
      };

      const result = await new Promise((resolve) => {
        wizard.insert({ submit: resolve, cancel: () => resolve(undefined) });
      });

      expect(capturedOptions.isInsert).toBe(true);
      expect(result.url).toBe("catURL");
      expect(result.reference).toBe(reference);
      // Without width, the image is displayed with its natural width.
      expect(result.previewWidth).toBeUndefined();
    });

    it("notifies when the image insertion is cancelled", async () => {
      // The legacy wizard rejects when the user closes it.
      global.define("xwiki-wysiwyg-image-wizard", () => (options) => {
        capturedOptions = options;
        return Promise.reject(new Error("cancelled"));
      });

      const cancelled = await new Promise((resolve) => {
        wizard.insert({
          submit: () => resolve(false),
          cancel: () => resolve(true),
        });
      });

      expect(cancelled).toBe(true);
    });
  });

  it("passes the stored resource reference to the image wizard", async () => {
    const reference = { type: "attach", typed: true, reference: "photo.png" };

    await edit({
      url: "imageURL",
      textAlignment: "left",
      previewWidth: 200,
      reference,
    });

    expect(capturedOptions.isInsert).toBe(false);
    expect(capturedOptions.imageData.resourceReference).toBe(reference);
    expect(capturedOptions.imageData.src).toBe("imageURL");
    expect(capturedOptions.imageData.width).toBe("200px");
    expect(capturedOptions.imageData.alignment).toBe("start");
  });

  it("preserves the original reference when the image is unchanged", async () => {
    const reference = {
      type: "attach",
      typed: true,
      reference: "photo.png",
      parameters: { queryString: "width=50" },
    };
    // The wizard echoes an equivalent reference (a different object with the same type and reference).
    resolvedImageData = {
      width: "150px",
      alignment: "end",
      src: "updatedImageURL",
      resourceReference: {
        type: "attach",
        reference: "photo.png",
      },
    };

    const result = await edit({
      reference,
    });

    expect(result.reference).toBe(reference);
    expect(result.reference.parameters).toEqual({ queryString: "width=50" });
    expect(result.reference.typed).toBe(true);
    expect(result.url).toBe("updatedImageURL");
    expect(result.previewWidth).toBe(150);
    expect(result.textAlignment).toBe("right");
  });

  it("removes the width of the image when the user removes it", async () => {
    resolvedImageData = { width: "", src: "imageURL" };

    const result = await edit({ url: "imageURL", previewWidth: 200 });

    expect(result.previewWidth).toBeUndefined();
  });

  it("reports the newly selected reference when the user picks a different image", async () => {
    const reference = { type: "attach", typed: true, reference: "old.png" };
    const newReference = { type: "attach", typed: true, reference: "new.png" };
    resolvedImageData = {
      resourceReference: newReference,
    };

    const result = await edit({
      reference,
    });

    expect(result.reference).toBe(newReference);
  });

  describe("edited document", () => {
    const attachReference = {
      type: "attach",
      typed: false,
      reference: "cat.png",
    };

    it("uses the current document by default", async () => {
      await edit({});

      expect(Model.serialize(capturedOptions.currentDocument)).toBe(
        "wiki:Space.Page",
      );
      expect(capturedOptions.getImageResourceURL(attachReference, {})).toMatch(
        /^\/xwiki\/bin\/get\/Space\/Page\?.*reference=cat.png/,
      );
    });

    it("resolves the image references relative to the specified edited document", async () => {
      await edit(
        {},
        {
          documentReference: Model.resolve(
            "wiki:Other.Page",
            EntityType.DOCUMENT,
          ),
        },
      );

      expect(Model.serialize(capturedOptions.currentDocument)).toBe(
        "wiki:Other.Page",
      );
      // The image preview is computed for the edited document, which the image reference is relative to.
      expect(capturedOptions.getImageResourceURL(attachReference, {})).toMatch(
        /^\/xwiki\/bin\/get\/Other\/Page\?.*reference=cat.png/,
      );
    });

    it("passes the edited document when inserting an image", async () => {
      resolvedImageData = { src: "catURL", resourceReference: attachReference };

      await new Promise((resolve) => {
        wizard.insert(
          { submit: resolve, cancel: () => resolve(undefined) },
          {
            documentReference: Model.resolve(
              "wiki:Other.Page",
              EntityType.DOCUMENT,
            ),
          },
        );
      });

      expect(Model.serialize(capturedOptions.currentDocument)).toBe(
        "wiki:Other.Page",
      );
    });
  });

  describe("upload", () => {
    async function upload(context) {
      await edit({}, context);
      const callback = {
        onSuccess: vi.fn(),
        onError: vi.fn(),
        onAbort: vi.fn(),
      };
      await capturedOptions.upload(new File([""], "cat.png"), callback);
      return callback;
    }

    it("reports the reference of the uploaded attachment, attached to the edited document", async () => {
      // The storage identifies the uploaded attachment by its name, which can contain reference separators.
      uploadedAttachments = [
        {
          reference: "cat@home.png",
          href: "/xwiki/bin/download/Space/Page/cat%40home.png",
        },
      ];

      const callback = await upload();

      expect(callback.onSuccess).toHaveBeenCalledOnce();
      expect(Model.serialize(callback.onSuccess.mock.calls[0][0])).toBe(
        "wiki:Space.Page@cat\\@home.png",
      );
    });

    it("uploads to the specified edited document", async () => {
      uploadedAttachments = [{ reference: "cat.png" }];

      const callback = await upload({
        documentReference: Model.resolve(
          "wiki:Other.Page",
          EntityType.DOCUMENT,
        ),
      });

      expect(uploadTarget).toBe("wiki:Other.Page");
      expect(Model.serialize(callback.onSuccess.mock.calls[0][0])).toBe(
        "wiki:Other.Page@cat.png",
      );
    });

    it("aborts when the storage doesn't report the uploaded attachment", async () => {
      uploadedAttachments = [undefined];

      const callback = await upload();

      expect(callback.onSuccess).not.toHaveBeenCalled();
      expect(callback.onAbort).toHaveBeenCalledOnce();
    });
  });
});
