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
import {shallowMount} from '@vue/test-utils';
import _ from 'lodash';
import LayoutTableRow from "../../../../layouts/table/LayoutTableRow";

/**
 * Vue Component initializer for LayoutTableRow component. Since this component creates a deep hierarchy of
 * sub-components, we use `shallowMount` to initialize the wrapper.
 *
 * The default option object is merged with the `option` parameter.
 *
 * @param options an optional option object use to customize the initialized component
 * @returns {*} a shallow wrapper for the LayoutTableRow component
 */
function initWrapper(options = {}) {
  return shallowMount(LayoutTableRow, _.merge({
    provide: {
      logic: {
        getEntryId: (e) => e.id,
        isSelectionEnabled: () => false,
        isPropertyVisible: () => false,
        getPropertyDescriptors: () => {
          return [];
        },
        data: {
          query: {
            properties: []
          }
        }
      }
    },
    mocks: {
      $t: (key) => key
    }
  }, options))
}

describe('LayoutTableRow.vue', () => {
  it('Root element had an index and an id', async () => {
    const wrapper = initWrapper({
      propsData: {
        entry: {
          id: "idA",
          title: "title"
        },
        entryIdx: 1,
      }
    });
    expect(wrapper.attributes("data-livedata-entry-index")).toBe("1")
    expect(wrapper.attributes("data-livedata-entry-id")).toBe("idA")
  })
})