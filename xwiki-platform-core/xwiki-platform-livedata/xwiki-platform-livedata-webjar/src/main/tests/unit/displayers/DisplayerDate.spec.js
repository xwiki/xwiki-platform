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

jest.mock("daterangepicker", function () {
});
jest.mock("moment", function () {
  return function () {
    return {
      format() {
        return "formatted date"
      }
    }
  }
});

import DisplayerDate from "../../../displayers/DisplayerDate.vue";
import {initWrapper} from "./displayerTestsHelper";


describe('DisplayerDate.vue', () => {
  it('Renders an entry in view mode', () => {
    const wrapper = initWrapper(DisplayerDate, {});
    expect(wrapper.text()).toMatch('formatted date')
  })

  it('Switch to edit mode', async () => {
    const wrapper = initWrapper(DisplayerDate, {});

    await wrapper.setData({isView: false});

    const input = wrapper.find('.editor-date');
    expect(input.element.value).toMatch('formatted date')
    expect(input.element).toHaveFocus()
  })
})