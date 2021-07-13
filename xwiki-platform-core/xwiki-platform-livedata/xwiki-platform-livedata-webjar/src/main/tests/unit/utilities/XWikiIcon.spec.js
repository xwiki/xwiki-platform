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

import XWikiIcon from "../../../utilities/XWikiIcon";
import {mount} from '@vue/test-utils'
import _ from "lodash";
import Vue from "vue";


function initWrapper(params = {}) {
  return mount(XWikiIcon, _.merge({
    propsData: {
      iconDescriptor: {}
    }
  }, params));
}

describe('XWikiIcon.vue', () => {
  it('Displays an icon using a font', () => {
    const wrapper = initWrapper({
      propsData: {
        iconDescriptor: {
          cssClass: "fa fa-plus"
        }
      }
    });

    expect(wrapper.attributes('class')).toBe('fa fa-plus');
  })

  it('Displays an icon using an image', () => {
    const wrapper = initWrapper({
      propsData: {
        iconDescriptor: {
          url: "http://localhost:8080/myicon.jpg"
        }
      }
    });

    expect(wrapper.attributes('src')).toBe('http://localhost:8080/myicon.jpg');
  })

  it('Displays an icon retrieved from the REST API', async () => {
    // Mock the ajax query to get the metadata of the request icon.
    global.fetch = jest.fn(() =>
      Promise.resolve({
        json: () => Promise.resolve({
          icons: [{
            cssClass: "fa fa-plus"
          }]
        }),
      })
    );

    // Defines the name of the current wiki in the XWiki global object. Needed to resolve the REST URI to call to 
    // retrieve the metadata of the icon.
    global.XWiki = {
      currentWiki: 'xwiki'
    }
    
    const wrapper = initWrapper({
      propsData: {
        iconDescriptor: {
          name: 'add'
        }
      }
    });

    // Wait for the next rendering after the ajax query is resolved (several ticks are needed to account for the
    // resolution of the ajax requests resolution).
    await Vue.nextTick()
    await Vue.nextTick()
    await Vue.nextTick()

    expect(wrapper.attributes('class')).toBe('fa fa-plus');
  })
})