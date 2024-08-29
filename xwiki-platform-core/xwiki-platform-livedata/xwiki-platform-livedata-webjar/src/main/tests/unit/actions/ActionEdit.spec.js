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
import ActionEdit from "../../../displayers/actions/ActionEdit";
import _ from "lodash";

function initWrapper({setEdit = undefined}) {
  global.XWiki = _.merge(global.XWiki, {
    currentWiki: 'xwiki'
  })

  // Mock fetch.
  global.fetch = jest.fn(() => ({
    json: jest.fn()
  }));

  return mount(ActionEdit, {
    propsData: {
      displayer: {setEdit},
      closePopover: () => {}
    },
    mocks: {
      $t: (key) => key
    }
  });
}

describe('ActionEdit.vue', () => {
  it('Renders and edit on click', async () => {
    const setEdit = jest.fn();
    const stopPropagation = jest.fn();

    const wrapper = initWrapper({setEdit});
    expect(wrapper.attributes('title')).toBe('livedata.displayer.actions.edit');
    expect(wrapper.attributes('class')).toBe('livedata-base-action btn');
    expect(wrapper.text()).toBe('');

    // Overriding the stopPropagation method to be able to assert it after the event.
    await wrapper.trigger('click', {stopPropagation});

    expect(setEdit.mock.calls.length).toBe(1)
    expect(stopPropagation.mock.calls.length).toBe(1)
  })
})