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

global.$ = global.jQuery = $;

jest.mock("xwiki-livedata-xObjectPropertyHelper", function () {
  return {
    setLocalization() {
    },
    edit() {
      return new Promise(resolve => {
        resolve('<form id="editForm"><input type="text" id="editField" /></form>');
      })
    }
  }
})

describe('DisplayerXObjectProperty.vue', () => {
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
      logic: {
        getEntryId() {
          return 'testProperty'
        }, data: {
          query: {
            source: {
              className: 'XWiki.TestClass'
            }
          }
        }
      }
    })

    // The :visible filter does not work in the context of jest. To mitigate that issue we spy on jquery filter
    // method and return the expected result (the #editField input).
    const jQuerySpy = jest.spyOn($.fn, "filter", );
    jQuerySpy.mockImplementation(() => {
      return $(wrapper.element).find('#editField')
    })

    // Switch to edit mode and manually call updateEdit, instead of using the actions because accessing the actions
    // of the popover is not currently possible.
    await wrapper.setData({isView: false})
    wrapper.vm.updateEdit()
    await flushPromises();

    // Checks that the edition form is properly retrieved and displayed.
    expect(wrapper.find('#editForm').exists()).toBe(true);
    expect(wrapper.find('#editField').element).toHaveFocus();
  })
})
