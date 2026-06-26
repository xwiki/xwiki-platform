/*
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

import DisplayerXObjectProperty from "../../../displayers/DisplayerXObjectProperty.vue";
import {initWrapper} from "./displayerTestsHelper";
import $ from 'jquery';
import flushPromises from "flush-promises";
import xObjectPropertyHelper from "xwiki-livedata-xObjectPropertyHelper";
import editConfirmation from "xwiki-edit-confirmation";

global.$ = global.jQuery = $;

const EDIT_FORM = '<form id="editForm"><input type="text" id="editField" /></form>';

// The edit form is loaded through this helper; mock it so that the tests don't need a server.
jest.mock("xwiki-livedata-xObjectPropertyHelper", function () {
  return {
    edit: jest.fn()
  };
});

// The edit confirmation modal is loaded as a RequireJS module; mock it so that the tests don't need the real modules.
jest.mock("xwiki-edit-confirmation", function () {
  return {
    parseConfirmationResponse: jest.fn(error => {
      // Re-throws the error if it's not an edit confirmation (423) response.
      if (error && error.status === 423) {
        return error.responseJSON;
      }
      throw error;
    }),
    showConfirmationModal: jest.fn(() => Promise.resolve()),
    isConfirmationDismissed: jest.fn(() => false)
  };
});

jest.mock("xwiki-meta", function () {
  return {
    locale: 'en',
    // eslint-disable-next-line camelcase
    form_token: 'token'
  };
});

const editModeLogic = {
  getEntryId() {
    return 'testProperty'
  },
  data: {
    query: {
      source: {
        className: 'XWiki.TestClass'
      }
    }
  }
};

describe('DisplayerXObjectProperty.vue', () => {
  beforeEach(() => {
    xObjectPropertyHelper.edit.mockReset();
    xObjectPropertyHelper.edit.mockResolvedValue(EDIT_FORM);
    editConfirmation.parseConfirmationResponse.mockClear();
    editConfirmation.showConfirmationModal.mockClear();
    editConfirmation.isConfirmationDismissed.mockClear();
  })

  it('Renders an entry in view mode', () => {
    const wrapper = initWrapper(DisplayerXObjectProperty, {
      props: {
        entry: {
          color: '<strong>some content</strong>'
        }
      }
    })
    expect(wrapper.find('.html-wrapper').html())
      .toBe('<div class="html-wrapper"><strong>some content</strong></div>')
  })

  it('Renders an entry in edit mode', async () => {
    const wrapper = initWrapper(DisplayerXObjectProperty, {
      props: {
        entry: {
          color: '<strong>some content</strong>'
        }
      },
      logic: editModeLogic
    })

    // Switch to edit mode, the isView watcher then calls updateEdit which loads the edit form.
    await wrapper.setData({isView: false})
    await flushPromises();

    // Checks that the edition form is properly retrieved and displayed.
    expect(wrapper.find('#editForm').exists()).toBe(true);
  })

  it('Shows the edit confirmation modal and forces the edit once confirmed', async () => {
    // The first edit request requires a confirmation (the server returns a 423 with the confirmation as JSON), the
    // second one (forced) returns the editor.
    const confirmationError = Object.assign(new Error('confirmation required'), {
      status: 423,
      responseJSON: {
        title: 'Warning',
        message: '<p>Required rights warning.</p>',
        reject: 'Cancel',
        confirm: 'Force'
      }
    });
    xObjectPropertyHelper.edit.mockReset();
    xObjectPropertyHelper.edit
      .mockRejectedValueOnce(confirmationError)
      .mockResolvedValueOnce(EDIT_FORM);

    const wrapper = initWrapper(DisplayerXObjectProperty, {
      props: {
        entry: {
          color: '<strong>some content</strong>'
        }
      },
      logic: editModeLogic
    })

    await wrapper.setData({isView: false})
    await flushPromises();

    // The confirmation modal was shown and, once confirmed, the editor was forced and displayed.
    expect(editConfirmation.showConfirmationModal).toHaveBeenCalledTimes(1);
    // One of the edit requests was retried with the force parameters once the confirmation was confirmed.
    expect(xObjectPropertyHelper.edit.mock.calls.some(call => call[3] && call[3].force === 1)).toBe(true);
    expect(wrapper.find('#editForm').exists()).toBe(true);
  })
})
