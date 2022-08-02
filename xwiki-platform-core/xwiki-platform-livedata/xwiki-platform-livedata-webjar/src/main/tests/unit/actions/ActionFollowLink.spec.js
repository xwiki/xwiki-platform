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
import _ from "lodash"
import ActionFollowLink from "../../../displayers/actions/ActionFollowLink";

function initWrapper() {
  // Define a name for the current wiki.
  global.XWiki = _.merge(global.XWiki, {
    currentWiki: 'xwiki'
  })

  // Mock fetch.
  global.fetch = jest.fn(() => ({
    json: jest.fn()
  }));

  return mount(ActionFollowLink, {
    propsData: {
      displayer: {
        href: 'https://link/'
      },
      closePopover: () => {}
    },
    mocks: {
      $t: (key) => key
    }
  });
}


describe('ActionFollowLink.vue', () => {
  it('Renders and click', async () => {
    const wrapper = initWrapper();
    const actionSpan = wrapper.find('a > span');
    expect(actionSpan.attributes('title')).toBe('livedata.displayer.actions.followLink')
    expect(actionSpan.attributes('class')).toBe('livedata-base-action btn')
    expect(wrapper.attributes('href')).toBe('https://link/')
  })
})