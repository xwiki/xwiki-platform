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

import { mockRequireJS } from "@xwiki/platform-test-requirejs";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import type { RequireJSMock } from "@xwiki/platform-test-requirejs";

const ICONS_SOURCE = readFileSync(
  resolve(
    import.meta.dirname,
    "../../../../../../main/webapp/resources/uicomponents/attachments/icons.js",
  ),
  "utf-8",
);

type Icon = Record<string, unknown>;

type Attachment = {
  label: string;
  icon: Icon;
  data: { name: string; mimeType: string; xwikiRelativeUrl: string };
};

type AttachmentsIcon = {
  getIcon: (attachment: Attachment["data"]) => Icon;
  loadIcon: (attachment: Attachment) => Promise<Attachment>;
};

type Require = (
  ids: string[],
  onLoad: (module: AttachmentsIcon) => void,
) => void;

type Globals = Record<string, unknown>;

const globals = globalThis as unknown as Globals;

const IMAGE_ICON = { iconSetType: "FONT", cssClass: "fa fa-file-image-o" };
const PDF_ICON = { iconSetType: "FONT", cssClass: "fa fa-file-pdf-o" };
const ATTACHMENT_ICON = { iconSetType: "FONT", cssClass: "fa fa-paperclip" };

/**
 * The globals that the Velocity code of icons.js would have filled: each map associates a media type or a file name
 * extension with the icon metadata and the translated file type.
 */
const pageGlobals: Globals = {
  $jsontool: { serialize: (input: unknown) => input },
  $mimetypeMap: {
    "image/": [IMAGE_ICON, "Image"],
    "application/pdf": [PDF_ICON, "PDF"],
    attachment: [ATTACHMENT_ICON, "Attachment"],
  },
  $extensionMap: {},
};

/**
 * The URLs of the images that the browser fails to load or decode.
 */
const brokenImageURLs = new Set<string>();

/**
 * Simulate the browser loading an image: jsdom doesn't load images, so it never calls the load or error handlers.
 */
class FakeImage {
  onload: (() => void) | null = null;
  onerror: (() => void) | null = null;

  set src(url: string) {
    setTimeout(() => {
      if (brokenImageURLs.has(url)) {
        this.onerror?.();
      } else {
        this.onload?.();
      }
    });
  }
}

let requirejs: RequireJSMock;
let attachmentsIcon: AttachmentsIcon;

function loadAttachmentsIcon(): AttachmentsIcon {
  Object.assign(globals, pageGlobals);
  requirejs = mockRequireJS();
  // Evaluate the script in the global scope, where the RequireJS mock is installed.
  (0, eval)(ICONS_SOURCE);
  const require = globals.require as Require;
  let module: AttachmentsIcon | undefined;
  require(["xwiki-attachments-icon"], (loadedModule) => {
    module = loadedModule;
  });
  return module!;
}

function unloadAttachmentsIcon(): void {
  requirejs.restore();
  for (const name of Object.keys(pageGlobals)) {
    delete globals[name];
  }
}

function createAttachment(name: string, mimeType: string): Attachment {
  const data = {
    name,
    mimeType,
    xwikiRelativeUrl: `/xwiki/bin/download/Space/Page/${name}`,
  };
  // The attachment pickers compute the icon when they adapt the attachment metadata returned by the REST API.
  return { label: name, icon: attachmentsIcon.getIcon(data), data };
}

describe("xwiki-attachments-icon", () => {
  beforeEach(() => {
    vi.stubGlobal("Image", FakeImage);
    attachmentsIcon = loadAttachmentsIcon();
  });

  afterEach(() => {
    unloadAttachmentsIcon();
    brokenImageURLs.clear();
    vi.unstubAllGlobals();
  });

  describe("loadIcon", () => {
    it("keeps the image thumbnail when the browser loads it", async () => {
      const attachment = createAttachment("photo.png", "image/png");

      const loadedAttachment = await attachmentsIcon.loadIcon(attachment);

      expect(loadedAttachment).toBe(attachment);
      expect(loadedAttachment.icon).toEqual({
        iconSetType: "IMAGE",
        url: "/xwiki/bin/download/Space/Page/photo.png?width=48",
      });
    });

    it("falls back on the media type icon when the browser cannot display the image", async () => {
      const attachment = createAttachment("scan.tif", "image/tiff");
      brokenImageURLs.add(attachment.icon.url as string);

      const loadedAttachment = await attachmentsIcon.loadIcon(attachment);

      expect(loadedAttachment).toBe(attachment);
      expect(loadedAttachment.icon).toEqual(IMAGE_ICON);
    });

    it("doesn't load anything when the icon is not an image", async () => {
      const attachment = createAttachment("report.pdf", "application/pdf");

      const loadedAttachment = await attachmentsIcon.loadIcon(attachment);

      expect(loadedAttachment).toBe(attachment);
      expect(loadedAttachment.icon).toEqual(PDF_ICON);
    });
  });
});
