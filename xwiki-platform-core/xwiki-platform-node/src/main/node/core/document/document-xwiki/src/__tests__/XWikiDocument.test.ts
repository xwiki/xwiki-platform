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

import { XWikiDocument } from "../XWikiDocument";
import { EntityType, Model } from "@xwiki/platform-xwiki-model-api";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { EntityReference } from "@xwiki/platform-xwiki-model-api";
import type { XWikiMeta } from "@xwiki/platform-xwiki-utils";

function reference(representation: string): EntityReference {
  return Model.resolve(representation, EntityType.DOCUMENT)!;
}

function fakeMeta(overrides: Partial<XWikiMeta> = {}): XWikiMeta {
  return {
    documentReference: reference("xwiki:Space.Page"),
    version: "3.1",
    restURL: "/xwiki/rest/wikis/xwiki/spaces/Space/pages/Page",
    form_token: "token",
    isNew: false,
    locale: "",
    realLocale: "en",
    action: "edit",
    setVersion: vi.fn(),
    refreshVersion: vi.fn(),
    ...overrides,
  };
}

// A stand-in for the legacy API of the global XWiki object, recording the arguments it is called with.
const legacyAPI = {
  getURL: vi.fn(
    (action?: string, parameters?: URLSearchParams, fragment?: string) =>
      `/xwiki/bin/${action}/Space/Page?${parameters ?? ""}#${fragment ?? ""}`,
  ),
  getRestURL: vi.fn(
    (entity?: string, parameters?: URLSearchParams) =>
      `/xwiki/rest/wikis/xwiki/spaces/Space/pages/Page/${entity ?? ""}?${parameters ?? ""}`,
  ),
};

beforeEach(() => {
  vi.stubGlobal("XWiki", {
    Document: vi.fn(() => legacyAPI),
    currentDocument: legacyAPI,
    Model,
    EntityType,
  });
  document.body.innerHTML = `
    <input type="hidden" id="editingVersionDate" value="1700000000000"/>
    <input type="hidden" id="isNew" value="false"/>`;
});

afterEach(() => {
  vi.unstubAllGlobals();
  vi.clearAllMocks();
  document.body.innerHTML = "";
});

describe("currentDocument", () => {
  it("takes its fields from the meta information", () => {
    const meta = fakeMeta({ locale: "fr", isNew: true });
    const doc = XWikiDocument.currentDocument(meta);

    expect(doc.documentReference).toBe(meta.documentReference);
    expect(doc.language).toBe("fr");
    expect(doc.version).toBe("3.1");
    expect(doc.isNew).toBe(true);
    // The raw locale is not empty, so the real locale is not needed.
    expect(doc.translations).toBeUndefined();
  });

  it("takes the real locale from the meta information for the original translation", () => {
    const doc = XWikiDocument.currentDocument(fakeMeta({ realLocale: "de" }));

    expect(doc.language).toBe("");
    expect(doc.defaultLocale).toBe("de");
    expect(doc.realLocale).toBe("de");
  });

  it("instantiates the class it is called on", () => {
    class SubDocument extends XWikiDocument {}

    expect(SubDocument.currentDocument(fakeMeta())).toBeInstanceOf(SubDocument);
  });
});

describe("realLocale", () => {
  it("falls back on the default locale when the raw locale is empty", () => {
    const doc = new XWikiDocument({
      language: "",
      translations: { default: "en" },
    });

    expect(doc.realLocale).toBe("en");
  });

  it("is the root locale for a technical document", () => {
    const doc = new XWikiDocument({
      language: "",
      translations: { default: "" },
    });

    expect(doc.realLocale).toBe("");
  });

  it("is the raw locale for a translation", () => {
    const doc = new XWikiDocument({
      language: "fr",
      translations: { default: "en" },
    });

    expect(doc.realLocale).toBe("fr");
  });
});

describe("getRestURL", () => {
  it("targets the wiki page for the original translation", () => {
    new XWikiDocument({ language: "" }).getRestURL("objects");

    expect(legacyAPI.getRestURL).toHaveBeenCalledWith("objects", undefined);
  });

  it("targets the translation when the raw locale is set", () => {
    new XWikiDocument({ language: "fr" }).getRestURL("objects");

    expect(legacyAPI.getRestURL).toHaveBeenCalledWith(
      "translations/fr/objects",
      undefined,
    );
  });

  it("ignores the translation in getPageRestURL", () => {
    new XWikiDocument({ language: "fr" }).getPageRestURL("channels");

    expect(legacyAPI.getRestURL).toHaveBeenCalledWith("channels", undefined);
  });
});

describe("reload", () => {
  it("merges the REST response, ignoring its null properties", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () =>
        Response.json({ version: "4.1", title: "Page", content: null }),
      ),
    );
    const doc = new XWikiDocument({
      version: "3.1",
      content: "before",
      isNew: true,
    });

    await doc.reload();

    expect(doc.version).toBe("4.1");
    expect(doc.title).toBe("Page");
    // The null content of the response must not overwrite the one we have.
    expect(doc.content).toBe("before");
    expect(doc.isNew).toBe(false);
  });

  it("resets the document when it doesn't exist anymore", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => new Response("", { status: 404 })),
    );
    const doc = new XWikiDocument({ version: "3.1", content: "before" });

    await doc.reload();

    expect(doc.version).toBe("1.1");
    expect(doc.modified).toBe(0);
    expect(doc.content).toBe("");
    expect(doc.isNew).toBe(true);
  });

  it("keeps the current data when the request fails", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => new Response("", { status: 500 })),
    );
    const doc = new XWikiDocument({ version: "3.1", content: "before" });

    await doc.reload();

    expect(doc.version).toBe("3.1");
    expect(doc.content).toBe("before");
  });
});

describe("syncCurrentDocumentState", () => {
  it("updates the meta version and the hidden fields of the edit form", () => {
    const meta = fakeMeta();
    const doc = XWikiDocument.currentDocument(meta);

    doc.update({ version: "4.1", modified: 1700000009999, isNew: false });

    expect(meta.setVersion).toHaveBeenCalledWith("4.1");
    expect(
      (document.getElementById("editingVersionDate") as HTMLInputElement).value,
    ).toBe("1700000009999");
    expect((document.getElementById("isNew") as HTMLInputElement).value).toBe(
      "false",
    );
  });

  it("does nothing when the version didn't change", () => {
    const meta = fakeMeta();
    const doc = XWikiDocument.currentDocument(meta);

    doc.update({ modified: 1700000009999 });

    expect(meta.setVersion).not.toHaveBeenCalled();
    expect(
      (document.getElementById("editingVersionDate") as HTMLInputElement).value,
    ).toBe("1700000000000");
  });

  it("leaves the page state alone for a document that is not the current one", () => {
    const meta = fakeMeta();
    const doc = XWikiDocument.currentDocument(meta);
    doc.documentReference = reference("xwiki:Space.Other");

    doc.update({ version: "4.1", modified: 1700000009999 });

    expect(doc.isCurrentDocument()).toBe(false);
    expect(meta.setVersion).not.toHaveBeenCalled();
    expect(
      (document.getElementById("editingVersionDate") as HTMLInputElement).value,
    ).toBe("1700000000000");
  });

  it("leaves the page state alone for a document built without the meta information", () => {
    const doc = new XWikiDocument({
      documentReference: reference("xwiki:Space.Page"),
      version: "3.1",
    });

    doc.update({ version: "4.1", modified: 1700000009999 });

    expect(doc.isCurrentDocument()).toBe(false);
    expect(
      (document.getElementById("editingVersionDate") as HTMLInputElement).value,
    ).toBe("1700000000000");
  });
});

describe("getRevision", () => {
  it("asks the history of the edited translation for pretty names", async () => {
    const fetchMock = vi.fn(async () =>
      Response.json({ version: "3.1", modified: 1, author: "XWiki.Admin" }),
    );
    vi.stubGlobal("fetch", fetchMock);

    await new XWikiDocument({ language: "fr" }).getRevision("3.1");

    expect(legacyAPI.getRestURL).toHaveBeenCalledWith(
      "translations/fr/history/3.1",
      new URLSearchParams({ prettyNames: "true" }),
    );
  });
});
