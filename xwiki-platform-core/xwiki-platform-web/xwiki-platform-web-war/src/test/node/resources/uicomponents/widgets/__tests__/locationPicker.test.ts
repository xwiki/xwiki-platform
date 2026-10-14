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

// Publish the entity reference API on the global XWiki object, as the corresponding bundle does on every page.
import "xwiki-platform-web-webjar";
import { mockRequireJS } from "@xwiki/platform-test-requirejs";
import jquery from "jquery";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import type { RequireJSMock } from "@xwiki/platform-test-requirejs";

const LOCATION_PICKER_SOURCE = readFileSync(
  resolve(
    import.meta.dirname,
    "../../../../../../main/webapp/resources/uicomponents/widgets/locationPicker.js",
  ),
  "utf-8",
);

// The parent field is not empty, in order to avoid the location preview update performed on initialization.
const LOCATION_PICKER_HTML = `
  <div class="location-picker">
    <input type="text" class="location-title-field" value="" />
    <input type="text" class="location-wiki-field" value="xwiki" />
    <input type="text" class="location-parent-field" value="Initial" />
    <input type="text" class="location-name-field" value="Page" />
    <ol class="breadcrumb" id="locationPickerBreadcrumb"></ol>
    <div class="modal"></div>
  </div>
`;

type Globals = Record<string, unknown>;

const globals = globalThis as unknown as Globals;
const xwiki = globals.XWiki as Globals;

/**
 * The globals the location picker expects to find on the page, besides the XWiki object.
 */
const pageGlobals: Globals = {
  // The location picker is a Velocity template that serializes its translations.
  $jsontool: { serialize: (input: unknown) => input },
  $l10n: {},
  // The client-side validation is not tested here.
  LiveValidation: Object.assign(
    function () {
      return { add: () => {}, remove: () => {}, validate: () => {} };
    },
    { massValidate: () => true },
  ),
  Validate: { Custom: {} },
};

let requirejs: RequireJSMock;
let post: ReturnType<typeof vi.fn>;

function loadLocationPicker(): void {
  document.body.innerHTML = LOCATION_PICKER_HTML;
  Object.assign(globals, pageGlobals);
  xwiki.Document = { URLTemplate: "/xwiki/bin/__action__/__space__/__page__" };
  requirejs = mockRequireJS({
    jquery,
    "xwiki-meta": { wiki: "xwiki", space: "Current", page: "WebHome" },
    "xwiki-events-bridge": {},
    "xwiki-form-validation-async": {},
    // The location tree picker is not tested here.
    "xwiki-tree": jquery,
  });
  // Evaluate the script in the global scope, where the RequireJS mock is installed.
  (0, eval)(LOCATION_PICKER_SOURCE);
}

function unloadLocationPicker(): void {
  requirejs.restore();
  for (const name of Object.keys(pageGlobals)) {
    delete globals[name];
  }
  delete xwiki.Document;
  document.body.innerHTML = "";
}

function getPreviewReference(): string {
  expect(post).toHaveBeenCalledTimes(1);
  return post.mock.calls[0][1].reference;
}

function setParent(wiki: string, parent: string): void {
  jquery(".location-wiki-field").val(wiki);
  jquery(".location-parent-field").val(parent).trigger("input");
}

describe("Location Picker", () => {
  beforeEach(() => {
    // Run the validations right away, without delay, so that the location preview is updated synchronously.
    jquery.fn.validateAsync = function (validation: unknown) {
      return typeof validation === "function" ? validation() : validation;
    };
    post = vi.fn(() => jquery.Deferred().promise());
    vi.spyOn(jquery, "post").mockImplementation(post);
    loadLocationPicker();
  });

  afterEach(() => {
    unloadLocationPicker();
    delete jquery.fn.validateAsync;
    vi.restoreAllMocks();
  });

  describe("location preview", () => {
    it("uses the selected wiki when the parent field is relative to the wiki", () => {
      setParent("subwiki", "Space.Target");
      expect(getPreviewReference()).toEqual("subwiki:Space.Target.WebHome");
    });

    it("doesn't duplicate the wiki when the parent field contains the selected wiki", () => {
      setParent("xwiki", "xwiki:Space.Target");
      expect(getPreviewReference()).toEqual("xwiki:Space.Target.WebHome");
    });

    it("uses the wiki from the parent field when it differs from the selected wiki", () => {
      setParent("subwiki", "otherwiki:Space.Target");
      expect(getPreviewReference()).toEqual("otherwiki:Space.Target.WebHome");
    });

    it("uses the current wiki when no wiki is selected", () => {
      setParent("", "Space.Target");
      expect(getPreviewReference()).toEqual("xwiki:Space.Target.WebHome");
    });

    it("uses the selected wiki when the wiki changes", () => {
      jquery(".location-parent-field").val("Space.Target");
      jquery(".location-wiki-field").val("subwiki").trigger("change");
      expect(getPreviewReference()).toEqual("subwiki:Space.Target.WebHome");
    });
  });
});
