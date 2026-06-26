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

import DisplayerXObjectProperty from "./DisplayerXObjectProperty.vue";
import { initWrapper } from "./displayerTestsHelper";
import { loadById } from "../../services/require.js";
import { edit } from "../displayerXObjectPropertyHelper.js";
import flushPromises from "flush-promises";
import $ from "jquery";
import { restore } from "sinon";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

global.$ = global.jQuery = $;

const EDIT_FORM =
  '<form id="editForm"><input type="text" id="editField" /></form>';

// The edit form is loaded through this helper; mock it so that the tests don't need a server.
vi.mock("../displayerXObjectPropertyHelper.js", () => {
  return {
    edit: vi.fn(),
  };
});

// The edit confirmation modal is loaded as a RequireJS module through loadById; mock the require service so that
// loadById resolves with stubs instead of trying to load the real modules.
vi.mock("../../services/require.js", () => {
  return {
    loadById: vi.fn(),
  };
});

describe("DisplayerXObjectProperty.vue", () => {
  const editConfirmationStub = {
    parseConfirmationResponse: vi.fn((error) => {
      if (error && error.status === 423) {
        return error.responseJSON;
      }
      throw error;
    }),
    showConfirmationModal: vi.fn(() => Promise.resolve()),
    isConfirmationDismissed: vi.fn(() => false),
  };

  beforeEach(function () {
    vi.mocked(edit).mockResolvedValue(EDIT_FORM);
    editConfirmationStub.parseConfirmationResponse.mockClear();
    editConfirmationStub.showConfirmationModal.mockClear();
    vi.mocked(loadById).mockImplementation((id) => {
      if (id === "xwiki-edit-confirmation") {
        return Promise.resolve(editConfirmationStub);
      }
      // xwiki-meta
      return Promise.resolve({ locale: "en", form_token: "token" });
    });
  });

  afterEach(function () {
    // completely restore all fakes created through the sandbox
    restore();
  });

  it("Renders an entry in view mode", () => {
    const wrapper = initWrapper(DisplayerXObjectProperty, {
      props: {
        entry: {
          color: "<strong>some content</strong>",
        },
      },
    });
    expect(wrapper.find(".html-wrapper").html()).toBe(
      '<div class="html-wrapper"><strong>some content</strong></div>',
    );
  });

  it("Renders an entry in edit mode", async () => {
    const wrapper = initWrapper(DisplayerXObjectProperty, {
      props: {
        entry: {
          color: "<strong>some content</strong>",
        },
      },
      logic: {
        getEntryId() {
          return "testProperty";
        },
        data: {
          query: {
            source: {
              className: "XWiki.TestClass",
            },
          },
        },
      },
    });

    // Switch to edit mode and manually call updateEdit, instead of using the actions because
    // accessing the actions of the popover is not currently possible.
    await wrapper.setProps({ isView: false });
    await wrapper.setData({ isView: false });
    await flushPromises();

    // Checks that the edition form is properly retrieved and displayed.
    const editForm = wrapper.find("#editForm");
    expect(editForm.exists()).toBe(true);
  });

  it("Shows the edit confirmation modal and forces the edit once confirmed", async () => {
    // The first edit request requires a confirmation (the server returns a 423 with the confirmation as JSON), the
    // second one (forced) returns the editor.
    const confirmationError = Object.assign(
      new Error("confirmation required"),
      {
        status: 423,
        responseJSON: {
          title: "Warning",
          message: "<p>Required rights warning.</p>",
          reject: "Cancel",
          confirm: "Force",
        },
      },
    );
    vi.mocked(edit)
      .mockRejectedValueOnce(confirmationError)
      .mockResolvedValueOnce(EDIT_FORM);

    const wrapper = initWrapper(DisplayerXObjectProperty, {
      props: {
        entry: {
          color: "<strong>some content</strong>",
        },
      },
      logic: {
        getEntryId() {
          return "testProperty";
        },
        isEditMode: () => false,
        data: {
          query: {
            source: {
              className: "XWiki.TestClass",
            },
          },
        },
      },
    });

    await wrapper.setProps({ isView: false });
    await wrapper.setData({ isView: false });
    await flushPromises();

    // The confirmation modal was shown and, once confirmed, the editor was forced and displayed.
    expect(editConfirmationStub.showConfirmationModal).toHaveBeenCalledOnce();
    // One of the edit requests was retried with the force parameters once the confirmation was confirmed.
    expect(
      vi.mocked(edit).mock.calls.some((call) => call[3] && call[3].force === 1),
    ).toBe(true);
    expect(wrapper.find("#editForm").exists()).toBe(true);
  });
});
