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
import XSelect from "../XSelect.vue";
import XTextField from "../XTextField.vue";
import { flushPromises, mount } from "@vue/test-utils";
import { mockRequireJS } from "@xwiki/platform-test-requirejs";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import type { RequireJSMock } from "@xwiki/platform-test-requirejs";

const TRANSLATIONS_ID =
  "xwiki-l10n!xwiki-flamingo-design-system-translation-keys";

describe("required fields", () => {
  let requireJS: RequireJSMock;

  beforeEach(() => {
    requireJS = mockRequireJS();
  });

  afterEach(() => {
    requireJS.restore();
  });

  it("aren't marked while the translation of the mark is not loaded, then are", async () => {
    // The first load fails, e.g. because the server is not reachable.
    const failing = mount(XTextField, {
      props: { label: "Name", required: true },
    });
    await flushPromises();
    expect(failing.get("label").text()).toBe("Name");

    requireJS.set(TRANSLATIONS_ID, { "core.validation.required": "(Needed)" });
    const textField = mount(XTextField, {
      props: { label: "Name", required: true },
    });
    const select = mount(XSelect, {
      props: { label: "Type", items: ["a"], required: true },
    });
    await flushPromises();

    expect(textField.get("label .xRequired").text()).toBe("(Needed)");
    expect(select.get("label .xRequired").text()).toBe("(Needed)");
    // The fields share the loaded translation.
    expect(failing.get("label .xRequired").text()).toBe("(Needed)");
  });

  it("aren't the optional fields", async () => {
    requireJS.set(TRANSLATIONS_ID, { "core.validation.required": "(Needed)" });
    const textField = mount(XTextField, { props: { label: "Name" } });
    const select = mount(XSelect, { props: { label: "Type", items: ["a"] } });
    await flushPromises();

    expect(textField.find(".xRequired").exists()).toBe(false);
    expect(select.find(".xRequired").exists()).toBe(false);
  });
});
