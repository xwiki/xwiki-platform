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

import { edit, view } from "./displayerXObjectPropertyHelper.js";
import { mockRequireJS } from "@xwiki/platform-test-requirejs";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const CLASS_NAME = "Space.Class";

describe("displayerXObjectPropertyHelper.js", () => {
  let requireJS;
  let jQuery;

  beforeEach(function () {
    // Minimal model API: the URL holds the action, and a reference serializes to its names joined by dots.
    global.XWiki = {
      EntityType: { DOCUMENT: 0, OBJECT: 1, OBJECT_PROPERTY: 2 },
      EntityReference: class {
        constructor(name, type, parent) {
          this.name = name;
          this.type = type;
          this.parent = parent;
        }
      },
      Model: {
        resolve: (reference) => reference,
        serialize: (reference) =>
          reference.parent
            ? `${reference.parent.name}.${reference.name}`
            : reference.name,
      },
      Document: class {
        constructor(reference) {
          this.reference = reference;
        }

        getURL(action) {
          return `/${action}/${this.reference}`;
        }
      },
    };
    jQuery = {
      get: vi.fn().mockResolvedValue("stored"),
      ajax: vi.fn().mockResolvedValue("pending"),
    };
    requireJS = mockRequireJS({
      "xwiki-meta": { locale: "en", form_token: "token" },
      jquery: jQuery,
    });
  });

  afterEach(function () {
    requireJS.restore();
    delete global.XWiki;
  });

  it("Renders a stored entry from its document", async () => {
    expect(await view("Space.Page", CLASS_NAME, "color")).toBe("stored");

    expect(jQuery.ajax).not.toHaveBeenCalled();
    expect(jQuery.get).toHaveBeenCalledWith("/get/Space.Page", {
      xpage: "display",
      mode: "view",
      property: "Space.Class[0].color",
      type: "object",
      language: "en",
      objectPolicy: "updateOrCreate",
    });
  });

  it("Renders a pending entry from its values with the preview action", async () => {
    const pendingValues = {
      color: "red",
      size: ["small", "large"],
      "doc.title": "Some title",
      RequiresHTMLConversion: "Space.Class_0_description",
    };

    expect(await view("Space.Page", CLASS_NAME, "color", pendingValues)).toBe(
      "pending",
    );

    expect(jQuery.get).not.toHaveBeenCalled();
    expect(jQuery.ajax).toHaveBeenCalledWith({
      type: "POST",
      url: "/preview/Space.Page",
      data: {
        xpage: "display",
        mode: "view",
        property: "Space.Class[0].color",
        type: "object",
        language: "en",
        objectPolicy: "updateOrCreate",
        "Space.Class_0_color": "red",
        "Space.Class_0_size": ["small", "large"],
        RequiresHTMLConversion: "Space.Class_0_description",
        form_token: "token",
      },
      traditional: true,
    });
  });

  it("Renders a pending entry from its template", async () => {
    await view("Space.Page", CLASS_NAME, "color", {}, "Space.Template");

    expect(jQuery.ajax).toHaveBeenCalledWith({
      type: "POST",
      url: "/preview/Space.Page",
      data: {
        xpage: "display",
        mode: "view",
        property: "Space.Class[0].color",
        type: "object",
        language: "en",
        objectPolicy: "updateOrCreate",
        template: "Space.Template",
        form_token: "token",
      },
      traditional: true,
    });
  });

  it("Edits a pending document property with the extra parameters", async () => {
    await edit(
      "Space.Page",
      CLASS_NAME,
      "doc.title",
      { force: 1, force_token: "force" },
      { "doc.title": "Some title" },
    );

    expect(jQuery.ajax).toHaveBeenCalledWith({
      type: "POST",
      url: "/preview/Space.Page",
      data: {
        xpage: "display",
        mode: "edit",
        property: "title",
        type: "document",
        language: "en",
        objectPolicy: "updateOrCreate",
        force: 1,
        force_token: "force",
        form_token: "token",
      },
      traditional: true,
    });
  });
});
