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

import LivedataPagination from "../../LivedataPagination";

import {mount} from '@vue/test-utils'
import _ from "lodash";

/**
 * Vue component initializer for the LivedataPagination component. Calls `mount()` from `@vue/test-utils` with
 * preconfigured values.
 *
 * The default provide objects is:
 * ```javascript
 * {
 *   logic: {
 *     data: {
 *       meta: {
 *         pagination: {
 *           maxShownPages: 10,
 *           showEntryRange: true,
 *           showPageSizeDropdown: true,
 *           pageSizes: [10, 20, 30]
 *         }
 *       },
 *       query: { limit: 20 },
 *       data: { count: 1 }
 *     },
 *     getFirstIndexOfPage() {
 *       return 0;
 *     },
 *     getLastIndexOfPage() {
 *       return 1;
 *     },
 *     getPageCount() {
 *       return 1;
 *     },
 *     getPageIndex() {
 *       return 1;
 *     }
 *   }
 * }
 * ```
 *
 * The default `mocks` has a single key `$t(key)` that returns the passed key as the translation. For instance
 * `$t('a.b')` returns `'a.b'`.
 *
 * @param provide (optional) an object that is merged on top of the default provide parameter.
 * @returns {number|*} a wrapper for the LivedataPagination component
 */
function initWrapper({provide} = {}) {
  return mount(LivedataPagination, {
    provide: _.merge({
      logic: {
        data: {
          meta: {
            pagination: {
              maxShownPages: 10,
              showEntryRange: true,
              showPageSizeDropdown: true,
              pageSizes: [10, 20, 30, 100]
            }
          },
          query: {limit: 20},
          data: {count: 1}
        },
        getFirstIndexOfPage() {
          return 0;
        },
        getLastIndexOfPage() {
          return 1;
        },
        getPageCount() {
          return 1;
        },
        getPageIndex() {
          return 1;
        }
      }
    }, provide),
    mocks: {
      $t: (key) => key
    }
  });
}

describe('LivedataPagination.vue', () => {
  it('Displays the pagination when the limit is an existing page size', () => {
    const wrapper = initWrapper();
    const select = wrapper.find("select");
    expect(select.attributes('title')).toContain('livedata.pagination.selectPageSize')
    const options = wrapper.findAll("select>*");
    expect(options.at(0).html()).toBe('<option value="10">10</option>');
    expect(options.at(1).html()).toBe('<option value="20">20</option>');
    expect(options.at(2).html()).toBe('<option value="30">30</option>');
    expect(options.at(3).html()).toBe('<option value="100">100</option>');
  })

  it('Displays the pagination when the limit is not an existing page size', () => {
    const wrapper = initWrapper({
      provide: {
        logic: {
          data: {
            query: {
              limit: 25
            }
          }
        }
      }
    });
    // The 25 pagination appears in the select options even if it is not part of the default page sizes ([10, 20,
    // 30, 100]).
    const options = wrapper.findAll("select>*");
    expect(options.at(0).html()).toBe('<option value="10">10</option>');
    expect(options.at(1).html()).toBe('<option value="20">20</option>');
    expect(options.at(2).html()).toBe('<option value="25">25</option>');
    expect(options.at(3).html()).toBe('<option value="30">30</option>');
    expect(options.at(4).html()).toBe('<option value="100">100</option>');
  })

  it('Displays the pagination indexes when there is no entries', () => {
    const wrapper = initWrapper({
      provide: {
        logic: {
          getPageCount() {
            return 0;
          }
        }
      }
    });
    expect(wrapper.find('.pagination-indexes').text()).toContain('livedata.pagination.page')
    expect(wrapper.findAll('.pagination-indexes .page-nav').length).toBe(1)
    expect(wrapper.find('.pagination-indexes .page-nav').text()).toBe("1");
  })

  it('Displays the pagination indexes when there is some entries', () => {
    const wrapper = initWrapper({
      provide: {
        logic: {
          getPageCount() {
            return 3;
          }
        }
      }
    });
    expect(wrapper.find('.pagination-indexes').text()).toContain('livedata.pagination.page')
    let pageNavs = wrapper.findAll('.pagination-indexes .page-nav');
    expect(pageNavs.length).toBe(3)
    expect(pageNavs.at(0).text()).toBe("1");
    expect(pageNavs.at(1).text()).toBe("2");
    expect(pageNavs.at(2).text()).toBe("3");
  })
})