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
import { XWikiStorage } from "../XWikiStorage";
import { EntityType, Model } from "@xwiki/platform-xwiki-model-api";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { Logger } from "@xwiki/platform-api";
import type { XWikiMeta } from "@xwiki/platform-xwiki-utils";

describe("XWikiStorage", () => {
  let storage: XWikiStorage;

  beforeEach(() => {
    vi.stubGlobal("XWiki", {
      currentDocument: {
        documentReference: Model.resolve(
          "wiki:Space.Page",
          EntityType.DOCUMENT,
        ),
      },
      docsyntax: "xwiki/2.1",
      Document: class {
        getURL(action: string, parameters: URLSearchParams) {
          return `/xwiki/bin/${action}/Space/Page?${parameters}`;
        }
      },
    });
    storage = new XWikiStorage(
      { setModule: vi.fn(), debug: vi.fn() } as unknown as Logger,
      { form_token: "token" } as XWikiMeta,
    );
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  function mockUploadResponse(response: unknown) {
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({ ok: true, json: async () => response }),
    );
  }

  describe("saveAttachments", () => {
    it("reports the URL and the reference of the uploaded attachments", async () => {
      mockUploadResponse({
        uploaded: 1,
        url: "/xwiki/bin/download/Space/Page/cat.png",
        fileName: "cat.png",
        message: {
          resourceReference: { type: "attach", reference: "cat.png" },
        },
      });

      const uploadedAttachments = await storage.saveAttachments("Space.Page", [
        new File([""], "cat.png"),
      ]);

      expect(uploadedAttachments).toEqual([
        { url: "/xwiki/bin/download/Space/Page/cat.png", reference: "cat.png" },
      ]);
    });

    it("fails when the upload is rejected", async () => {
      mockUploadResponse({ uploaded: 0, error: "Not allowed" });

      await expect(
        storage.saveAttachments("Space.Page", [new File([""], "cat.png")]),
      ).rejects.toThrow("Upload failed: Not allowed");
    });
  });
});
