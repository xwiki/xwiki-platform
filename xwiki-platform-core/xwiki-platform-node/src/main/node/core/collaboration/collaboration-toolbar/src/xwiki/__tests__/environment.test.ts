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

// Test suites and test scenarios are naturally made of many statements.
/* eslint-disable max-statements */

import { createXWikiToolbarEnvironment } from "../environment";
import { mockRequireJS } from "@xwiki/platform-test-requirejs";
import { EntityType, Model } from "@xwiki/platform-xwiki-model-api";
import { loadById } from "@xwiki/platform-xwiki-utils";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { XWikiDocument } from "@xwiki/platform-document-xwiki";
import type { RequireJSMock } from "@xwiki/platform-test-requirejs";
import type { EntityReference } from "@xwiki/platform-xwiki-model-api";

/**
 * @returns the edited document, with only the fields used by the environment
 */
function createDocument(): XWikiDocument {
  return {
    realLocale: "fr",
    getURL: (action: string, parameters?: URLSearchParams) =>
      `/${action}${parameters ? "?" + parameters : ""}`,
  } as XWikiDocument;
}

function createJQuery() {
  const calls: { element: Element; method: string; args: unknown[] }[] = [];
  const $ = (element: Element) => {
    const record =
      (method: string) =>
      (...args: unknown[]) =>
        calls.push({ element, method, args });
    return {
      trigger: record("trigger"),
    };
  };
  return { $, calls };
}

function createIcon(name: string) {
  return {
    render() {
      const icon = document.createElement("span");
      icon.className = `icon-${name}`;
      return icon;
    },
  };
}

function createMoment() {
  return (date?: number) => ({
    format: (format: string) => `${date} as ${format}`,
    toMomentFormatString: (javaDateFormat: string) =>
      `moment(${javaDateFormat})`,
  });
}

describe("createXWikiToolbarEnvironment", () => {
  let requireJS: RequireJSMock;
  let jQuery: ReturnType<typeof createJQuery>;
  let documentReferences: EntityReference[];

  beforeEach(() => {
    jQuery = createJQuery();
    requireJS = mockRequireJS({
      jquery: jQuery.$,
      moment: createMoment(),
      "moment-jdateformatparser": {},
      "xwiki-meta": { dateFormat: "dd/MM/yyyy" },
      "xwiki-icon!xwiki-realtime-toolbar-icons": {
        check: createIcon("check"),
        clock: createIcon("clock"),
        warning: createIcon("warning"),
      },
    });
    documentReferences = [];
    vi.stubGlobal("XWiki", {
      contextPath: "/xwiki",
      Document: class {
        constructor(private readonly reference: EntityReference) {
          documentReferences.push(reference);
        }
        getURL() {
          return `/profile/${Model.serialize(this.reference)}`;
        }
      },
    });
    document.body.innerHTML = `
      <form id="edit" action="/save">
        <input name="title" value="Title" />
        <div class="buttons">
          <input name="comment" value="" data-xwiki-edit-comment-mandatory="true" />
          <input type="checkbox" name="minorEdit" />
          <div class="realtime-edit-toolbar"></div>
        </div>
      </form>`;
  });

  afterEach(() => {
    requireJS.restore();
    vi.unstubAllGlobals();
  });

  function getForm(): HTMLElement {
    return document.querySelector("form#edit")!;
  }

  function getToolbar(): HTMLElement {
    return document.querySelector(".realtime-edit-toolbar")!;
  }

  it("loads the icons, the default avatar and the date format", async () => {
    const environment = await createXWikiToolbarEnvironment();
    const iconQuery = await loadById<{ icons: string[] }>(
      "xwiki-realtime-toolbar-icons",
    );
    expect(iconQuery.icons).toEqual(["check", "clock", "warning"]);
    expect(environment.icons).toEqual({
      check: '<span class="icon-check"></span>',
      clock: '<span class="icon-clock"></span>',
      warning: '<span class="icon-warning"></span>',
    });
    expect(environment.defaultAvatarURL).toBe(
      "/xwiki/bin/skin/resources/icons/xwiki/noavatar.png",
    );
    expect(environment.formatDate(123)).toBe("123 as moment(dd/MM/yyyy)");
  });

  it("falls back on defaults when the icons and the date format are missing", async () => {
    requireJS.set("xwiki-meta", {});
    requireJS.set("xwiki-icon!xwiki-realtime-toolbar-icons", {});
    const environment = await createXWikiToolbarEnvironment();
    expect(environment.icons).toEqual({ check: "", clock: "", warning: "" });
    expect(environment.formatDate(1)).toBe("1 as moment(yyyy/MM/dd HH:mm)");
  });

  it("reads the save options from the edit form", async () => {
    const environment = await createXWikiToolbarEnvironment();
    expect(environment.getSaveOptions(getForm())).toEqual({
      editComment: { enabled: true, mandatory: true },
      minorEdit: true,
    });

    // The version summary is optional.
    getForm()
      .querySelector("input[name=comment]")!
      .removeAttribute("data-xwiki-edit-comment-mandatory");
    expect(environment.getSaveOptions(getForm())).toEqual({
      editComment: { enabled: true, mandatory: false },
      minorEdit: true,
    });

    // The version summary and the minor edit fields are disabled.
    getForm()
      .querySelectorAll("input[name=comment], input[name=minorEdit]")
      .forEach((input) => input.remove());
    expect(environment.getSaveOptions(getForm())).toEqual({
      editComment: { enabled: false, mandatory: false },
      minorEdit: false,
    });
  });

  it("computes the URLs", async () => {
    const environment = await createXWikiToolbarEnvironment();
    expect(environment.getUserURL("xwiki:XWiki.Alice")).toBe(
      "/profile/xwiki:XWiki.Alice",
    );
    expect(documentReferences[0].type).toBe(EntityType.DOCUMENT);
    const xwikiDocument = createDocument();
    expect(environment.getVersionURL(xwikiDocument, "1.2")).toBe(
      "/view?rev=1.2&language=fr",
    );
    expect(environment.getVersionContentURL(xwikiDocument, "1.2")).toBe(
      "/view?xpage=displaycontent&htmlHeaderAndFooter=true&outputTitle=true&language=fr&rev=1.2",
    );
  });

  it("fetches the changes", async () => {
    const fetch = vi.fn(async () => ({
      ok: true,
      text: async () => "changes",
    }));
    vi.stubGlobal("fetch", fetch);
    const environment = await createXWikiToolbarEnvironment();

    expect(
      await environment.fetchChanges(
        getForm(),
        createDocument(),
        "1.1",
        getToolbar(),
      ),
    ).toBe("changes");
    expect(jQuery.calls).toContainEqual({
      element: getToolbar(),
      method: "trigger",
      args: ["xwiki:actions:beforePreview"],
    });
    expect(fetch).toHaveBeenCalledWith("/edit", {
      method: "POST",
      headers: {
        "Content-Type": "application/x-www-form-urlencoded",
        "X-Requested-With": "XMLHttpRequest",
      },
      body: expect.any(URLSearchParams),
    });
    const body = (fetch.mock.calls[0] as unknown[])[1] as {
      body: URLSearchParams;
    };
    expect(body.body.get("title")).toBe("Title");
    expect(body.body.get("rev1")).toBe("1.1");
    expect(body.body.get("xpage")).toBe("changesinline");
  });

  it("reports failures to fetch the changes", async () => {
    vi.stubGlobal(
      "fetch",
      vi.fn(async () => ({ ok: false, status: 500 })),
    );
    const environment = await createXWikiToolbarEnvironment();
    await expect(
      environment.fetchChanges(
        getForm(),
        createDocument(),
        undefined,
        getToolbar(),
      ),
    ).rejects.toThrow("Response status: 500");
  });

  it("puts the change summary in the edit form", async () => {
    const environment = await createXWikiToolbarEnvironment();
    const comment = document.querySelector<HTMLInputElement>(
      "input[name=comment]",
    )!;
    const minorEdit = document.querySelector<HTMLInputElement>(
      "input[name=minorEdit]",
    )!;

    environment.setChangeSummary(getForm(), "summary", true);
    expect(comment.value).toBe("summary");
    expect(minorEdit.checked).toBe(true);

    // The minor edit flag is left untouched when not specified.
    environment.setChangeSummary(getForm(), "other");
    expect(comment.value).toBe("other");
    expect(minorEdit.checked).toBe(true);
  });
});
