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
import FilterList from "../../../filters/FilterList.vue";
import {mount} from '@vue/test-utils';
import _ from "lodash";
import $ from "jquery";
import flushPromises from "flush-promises";

/**
 * Initialize a FilterList component using default values vue test-utils `mount` parameters.
 * The default parameters can be overridden using the `mountConfiguration` parameter.
 *
 * The default configuration is:
 * ```
 * {
 *   provide: {
 *     logic: {
 *       getQueryFilterGroup() {
 *         return {};
 *       },
 *       onEventWhere() {
 *       },
 *       getFilterDescriptor() {
 *         return {options: ''};
 *       }
 *     }
 *   }
 * }
 * ```
 *
 * @param mountConfiguration mount parameters merged over the default configuration
 * @returns {{options: string}|{}|*} an initialized FilterList Vue component
 */
function initWrapper(mountConfiguration = {}) {
  // Define an empty xwikiSelectize to prevent the component mount to fail.
  $.fn.xwikiSelectize = () => {
  };

  return mount(FilterList, _.merge({
    provide: {
      logic: {
        getQueryFilterGroup() {
          return {};
        },
        onEventWhere() {
        },
        getFilterDescriptor() {
          return {
            options: '',
            operators: []
          };
        },
        translationsLoaded() {
          return Promise.resolve(true);
        }
      }
    },
    mocks: {
      $t: (key) => key
    }
  }, mountConfiguration));
}

describe('FilterList.vue', () => {
  it('Render the filter list when visible', async () => {
    const wrapper = initWrapper();
    // The loader is displayed until the translations are loaded.
    expect(wrapper.classes()).toStrictEqual(['xwiki-loader']);
    await flushPromises()
    expect(wrapper.html()).toBe("<span><input aria-label=\"livedata.filter.list.label\" " +
     "class=\"filter-list livedata-filter\"></span>")
  })

  it('Render the filter list when Empty filter and advanced', async () => {
    const wrapper = initWrapper({
      propsData: {index: 0, isAdvanced: true},
      provide: {
        logic: {
          getQueryFilterGroup() {
            return {
              constraints: [{value: undefined, operator: 'empty'}]
            }
          }
        }
      }
    });
    // The loader is displayed until the translations are loaded.
    expect(wrapper.classes()).toStrictEqual(['xwiki-loader']);
    await flushPromises()
    expect(wrapper.html()).toBe('<span style="display: none;"><input aria-label="livedata.filter.list.label" ' +
     'class="filter-list livedata-filter"></span>')
  })
})
