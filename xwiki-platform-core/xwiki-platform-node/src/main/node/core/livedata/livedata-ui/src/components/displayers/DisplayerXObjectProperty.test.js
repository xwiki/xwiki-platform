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
import { edit, view } from "../displayerXObjectPropertyHelper.js";
import { mockRequireJS } from "@xwiki/platform-test-requirejs";
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
    view: vi.fn(),
  };
});

describe("DisplayerXObjectProperty.vue", () => {
  let requireJS;

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
    vi.mocked(view).mockReset();
    editConfirmationStub.parseConfirmationResponse.mockClear();
    editConfirmationStub.showConfirmationModal.mockClear();
    // The edit confirmation modal and the XWiki context are loaded as RequireJS modules; register stubs so that
    // the tests don't need the real modules.
    requireJS = mockRequireJS({
      "xwiki-edit-confirmation": editConfirmationStub,
      "xwiki-meta": { locale: "en", form_token: "token" },
    });
  });

  afterEach(function () {
    requireJS.restore();
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
      logic: {
        isEditMode: () => false,
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

  describe("Rendering a row that has no entry yet", () => {
    const draftEntry = {
      color: "red",
      _new: true,
      _displayReference: "NewRows.placeholder",
    };

    const draftLogic = {
      getEntryId: () => undefined,
      isEditMode: () => false,
      data: { query: { source: { className: "XWiki.TestClass" } } },
    };

    it("renders its content from the values it holds", async () => {
      vi.mocked(view).mockResolvedValue("<strong>Red</strong>");

      const wrapper = initWrapper(DisplayerXObjectProperty, {
        props: { entry: draftEntry },
        logic: draftLogic,
      });
      await flushPromises();

      // No query result holds the content of a row that is not an entry yet, so it is rendered
      // from the values held on the row instead of showing them raw.
      expect(vi.mocked(view)).toHaveBeenCalledWith(
        "NewRows.placeholder",
        "XWiki.TestClass",
        "color",
        { color: "red" },
      );
      expect(wrapper.find(".html-wrapper").html()).toContain(
        "<strong>Red</strong>",
      );
    });

    it("loads its edit field from the values it holds", async () => {
      vi.mocked(edit).mockResolvedValue(EDIT_FORM);
      vi.mocked(view).mockResolvedValue(undefined);

      const wrapper = initWrapper(DisplayerXObjectProperty, {
        props: { entry: draftEntry },
        logic: draftLogic,
      });
      await wrapper.setProps({ isView: false });
      await wrapper.setData({ isView: false });
      await flushPromises();

      // The wiki stores nothing for that row, so the field would come back empty unless what was
      // edited is sent along with it.
      expect(vi.mocked(edit)).toHaveBeenCalledWith(
        "NewRows.placeholder",
        "XWiki.TestClass",
        "color",
        undefined,
        { color: "red" },
      );
    });

    it("falls back on the value it holds when the rendering fails", async () => {
      vi.mocked(view).mockRejectedValue(new Error("nope"));

      const wrapper = initWrapper(DisplayerXObjectProperty, {
        props: { entry: draftEntry },
        logic: draftLogic,
      });
      await flushPromises();

      expect(wrapper.find(".html-wrapper").html()).toContain("red");
    });

    it("displays the value it holds when its location is unknown", async () => {
      const wrapper = initWrapper(DisplayerXObjectProperty, {
        props: { entry: { color: "red", _new: true } },
        logic: draftLogic,
      });
      await flushPromises();

      // There is no document to render the content against.
      expect(vi.mocked(view)).not.toHaveBeenCalled();
      expect(wrapper.find(".html-wrapper").html()).toContain("red");
    });

    it("drops a rendering that arrives after the row got its entry", async () => {
      let resolveView;
      vi.mocked(view).mockReturnValue(
        new Promise((resolve) => {
          resolveView = resolve;
        }),
      );
      const wrapper = initWrapper(DisplayerXObjectProperty, {
        props: { entry: draftEntry },
        logic: draftLogic,
      });

      // The entry is created before the content of the row comes back, and has its own content.
      await wrapper.setProps({ entry: { color: "<em>red</em>" } });
      resolveView("<strong>Red</strong>");
      await flushPromises();

      expect(wrapper.find(".html-wrapper").html()).toContain("<em>red</em>");
      expect(wrapper.find(".html-wrapper").html()).not.toContain("<strong>");
    });

    it("displays the rendering of the latest value", async () => {
      const resolvers = [];
      vi.mocked(view).mockImplementation(
        () => new Promise((resolve) => resolvers.push(resolve)),
      );
      const wrapper = initWrapper(DisplayerXObjectProperty, {
        props: { entry: draftEntry },
        logic: draftLogic,
      });
      await wrapper.setProps({ entry: { ...draftEntry, color: "blue" } });

      // The second content comes back before the first one.
      resolvers[1]("<strong>Blue</strong>");
      resolvers[0]("<strong>Red</strong>");
      await flushPromises();

      expect(wrapper.find(".html-wrapper").html()).toContain(
        "<strong>Blue</strong>",
      );
    });
  });

  describe("Rendering an entry", () => {
    it("displays the content of the query results and loads its field from the store", async () => {
      vi.mocked(edit).mockResolvedValue(EDIT_FORM);
      vi.mocked(view).mockReset();

      const wrapper = initWrapper(DisplayerXObjectProperty, {
        props: { entry: { color: "<strong>Red</strong>" } },
        logic: {
          getEntryId: () => "NewRows.My first entry",
          isEditMode: () => false,
          data: { query: { source: { className: "XWiki.TestClass" } } },
        },
      });
      await flushPromises();

      // An entry is in the store, so the wiki renders it on its own.
      expect(vi.mocked(view)).not.toHaveBeenCalled();
      expect(wrapper.find(".html-wrapper").html()).toContain(
        "<strong>Red</strong>",
      );

      await wrapper.setProps({ isView: false });
      await wrapper.setData({ isView: false });
      await flushPromises();

      expect(vi.mocked(edit)).toHaveBeenCalledWith(
        "NewRows.My first entry",
        "XWiki.TestClass",
        "color",
        undefined,
        undefined,
      );
    });
  });
});
