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
import { beforeEach, describe, expect, it } from "vitest";
// Import for the side effect of registering the global define / requirejs used to load the legacy image wizard.
import "../../testUtils";

describe("DefaultImageWizard", () => {
  let capturedOptions;
  let resolvedImageData;
  let wizard;

  beforeEach(() => {
    capturedOptions = undefined;
    resolvedImageData = undefined;

    // Register a fake legacy image wizard that captures its options and resolves with a chosen image data.
    global.define("xwiki-wysiwyg-image-wizard", () => (options) => {
      capturedOptions = options;
      return Promise.resolve(resolvedImageData);
    });

    global.XWiki = {
      currentDocument: {
        documentReference: { type: "DOCUMENT" },
        getURL: (action, parameters) =>
          `/xwiki/bin/${action}/Space/Page?${parameters.toString()}`,
      },
    };

    const resourceReferenceParser = {
      parse: () => ({ type: "attach", typed: true, reference: "" }),
    };
    const storageProvider = {
      get: () => ({ saveAttachments: async () => [] }),
    };
    wizard = new DefaultImageWizard(resourceReferenceParser, storageProvider);
  });

  function edit(image) {
    return new Promise((resolve) => {
      wizard.edit(image, { submit: resolve, cancel: () => resolve(undefined) });
    });
  }

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
});
