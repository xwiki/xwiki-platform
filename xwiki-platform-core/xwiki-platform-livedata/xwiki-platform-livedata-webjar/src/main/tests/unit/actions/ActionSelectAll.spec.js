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
import {mount} from '@vue/test-utils'
import _ from "lodash";
import ActionSelectAll from "../../../displayers/actions/ActionSelectAll";

function initWrapper(target) {
  global.XWiki = _.merge(global.XWiki, {
    currentWiki: 'xwiki'
  })

  return mount(ActionSelectAll, {
    // attachTo: root,
    propsData: {
      target
    },
    mocks: {
      $t: (key) => key
    },
    provide: {
      logic: {}
    }
  });
}

describe('ActionSelectAll.vue', () => {
  it('Renders and select all on click', async () => {

    global.XWiki = _.merge(global.XWiki, {widgets: {Notification: jest.fn()}})

    const removeAllRanges = jest.fn();
    const addRange = jest.fn();

    window.getSelection = jest.fn(() => {
      return {removeAllRanges, addRange};
    });

    const selectNodeContents = jest.fn();
    document.createRange = jest.fn(() => {
      return {selectNodeContents};
    })


    document.execCommand = jest.fn();

    const wrapper = initWrapper('target');

    await wrapper.trigger('click')

    expect(window.getSelection).toHaveBeenCalled();
    expect(document.createRange).toHaveBeenCalled();
    expect(selectNodeContents).toHaveBeenCalledWith('target');
    expect(removeAllRanges).toHaveBeenCalled();
    expect(addRange).toHaveBeenCalledWith({selectNodeContents});
    expect(document.execCommand).toHaveBeenCalledWith('copy');
    expect(XWiki.widgets.Notification).toHaveBeenCalledWith('livedata.displayer.actions.selectAll.success', 'done');
    expect(wrapper.attributes('title')).toBe('livedata.displayer.actions.selectAll');
    expect(wrapper.text()).toBe('livedata.displayer.actions.selectAll');
  })
})