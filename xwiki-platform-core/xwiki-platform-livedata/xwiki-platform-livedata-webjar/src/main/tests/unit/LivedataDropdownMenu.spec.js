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

import {mount} from "@vue/test-utils";
import LivedataDropdownMenu from "../../LivedataDropdownMenu";

/**
 * Vue component initializer for `LivedataDropdownMenu` components. Calls `mount()` with preconfigured values.
 *
 * @returns a map containing a wrapper for `LivedataDropdownMenu` components and a mock of logic.changeLayout
 */
function initWrapper() {
  global.XWiki = {
    contextPath: ''
  }

  const changeLayout = jest.fn();
  const wrapper = mount(LivedataDropdownMenu, {
    provide: {
      logic: {
        currentLayoutId: 'cards',
        data: {
          meta: {
            layouts: [{id: 'table'}, {id: 'cards'}]
          }
        },
        changeLayout: changeLayout
      },
    },
    mocks: {
      $t: (key) => key
    },
    stubs: {
      XWikiIcon: true
    }
  });
  return {wrapper, changeLayout};
}

// Since the <li> elements does not have distinguishing attributes, we look for the layout items by looking at the
// elements located after the second separator (class dropdown-header).
function findSecondDropdownHeaderIndex(lis) {
  var dropdownHeadersCptr = 0;
  var liIdx = 0;
  for (; liIdx < lis.length; liIdx++) {
    const li = lis.at(liIdx);
    if (li.classes().includes('dropdown-header')) {
      dropdownHeadersCptr++;
    }
    if (dropdownHeadersCptr >= 2) {
      break;
    }
  }
  return liIdx;
}

describe('LivedataDropdownMenu.vue', () => {
  it('Current layout is greyed out', () => {
    const {wrapper} = initWrapper();
    const lis = wrapper.findAll('li');
    const dropDownHeaderIndex = findSecondDropdownHeaderIndex(lis);

    const tableLayout = lis.at(dropDownHeaderIndex + 1);
    const cardsLayout = lis.at(dropDownHeaderIndex + 2);

    expect(tableLayout.classes()).not.toContain('disabled');
    expect(cardsLayout.classes()).toContain('disabled');
  })

  it('Clicking on an enabled layout changes the layout', () => {
    const {wrapper, changeLayout} = initWrapper();
    const lis = wrapper.findAll('li');
    const dropDownHeaderIndex = findSecondDropdownHeaderIndex(lis);
    const tableLayout = lis.at(dropDownHeaderIndex + 1);

    tableLayout.find('a').trigger('click');

    expect(changeLayout.mock.calls.length).toBe(1);
    expect(changeLayout.mock.calls[0][0]).toBe('table');
  })

  it('Clicking on a disabled layout does nothing', () => {
    const {wrapper, changeLayout} = initWrapper();
    const lis = wrapper.findAll('li');
    const dropDownHeaderIndex = findSecondDropdownHeaderIndex(lis);
    const cardsLayout = lis.at(dropDownHeaderIndex + 2);

    cardsLayout.find('a').trigger('click');

    expect(changeLayout.mock.calls.length).toBe(0);
  })

  it('Is not expanded by default', () => {
    const {wrapper} = initWrapper();
    expect(wrapper.find('[data-toggle="dropdown"]').attributes('aria-expanded')).toBe('false')
  })
})
