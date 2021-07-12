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

function initWrapper({isEditable = true, setEdit = undefined}) {
  global.XWiki = _.merge(global.XWiki, {
    currentWiki: 'xwiki'
  })

  return mount(ActionEdit, {
    propsData: {
      displayer: {isEditable, setEdit},
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
    const wrapper = initWrapper({setEdit});

    expect(wrapper.attributes('title')).toBe('livedata.displayer.actions.edit');
    expect(wrapper.attributes('class')).toBe('livedata-base-action btn');
    expect(wrapper.text()).toBe('');
    expect(wrapper.find('span span').attributes('class')).toBe('icon-placeholder')
  })

  it('Renders when not editable', async () => {
    const wrapper = initWrapper({isEditable: false});
    expect(wrapper.html()).toBe('');
  })
})