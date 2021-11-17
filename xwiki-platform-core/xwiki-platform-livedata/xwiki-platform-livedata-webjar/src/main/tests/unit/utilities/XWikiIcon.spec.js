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
import flushPromises from 'flush-promises'

function initWrapper(params = {}) {
  return mount(XWikiIcon, _.merge({
    propsData: {
      iconDescriptor: {}
    }
  }, params));
}

/**
 * Initialize the mocks for the icon queries.
 */
function mockIconsFetch() {
  function initPromise(cssClass) {
    return Promise.resolve({
      json: () => {
        return Promise.resolve({
          icons: [{
            cssClass: cssClass
          }]
        });
      },
    });
  }

  const cssClassesMap = {
    'add': 'fa fa-plus',
    'remove': 'fa fa-minus'
  }
  window.fetch = jest.fn((query) => {
    // Extract the name parameter from the query and return the corresponding css classes.
    const nameParameter = query.match(/.+name=(.+)$/)[1];
    const cssClasses = cssClassesMap[nameParameter];
    return initPromise(cssClasses);
  });
}

describe('XWikiIcon.vue', () => {

  beforeEach(() => {
    // Defines the name of the current wiki in the XWiki global object. Needed to resolve the REST URI to call to
    // retrieve the metadata of the icon.
    global.XWiki = {
      currentWiki: 'xwiki'
    }

    // Mock the ajax query to get the metadata of the request icon.
    mockIconsFetch();
  })

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
    const wrapper = initWrapper({
      propsData: {
        iconDescriptor: {
          name: 'add'
        }
      }
    });

    await flushPromises()

    expect(wrapper.attributes('class')).toBe('fa fa-plus');
  })

  it('Refresh the icon after a property change', async () => {
    const wrapper = initWrapper({
      propsData: {
        iconDescriptor: {name: 'add'}
      }
    })

    await flushPromises()

    expect(wrapper.attributes('class')).toBe('fa fa-plus');

    wrapper.setProps({iconDescriptor: {name: 'remove'}});

    await flushPromises();

    expect(wrapper.attributes('class')).toBe('fa fa-minus');
  })
})
