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
import LivedataAdvancedPanels from "../../../panels/LivedataAdvancedPanels";
import {mount} from '@vue/test-utils';
import Vue from "vue";

describe('LivedataAdvancedPanels.vue', () => {
  it('Renders the given panel', async () => {
    let component = Vue.component('LdTest', {
      template: '<span>Hello World from {{ panel.id }}!</span>',
      props: {panel: Object}
    });
    const wrapper = await mount(LivedataAdvancedPanels, {
      provide: {
        logic: {
          panels: [
            {
              id: 'test',
              component: 'test'
            }
          ]
        }
      }, stubs: {'test': component}
    });

    expect(wrapper.element).toContainHTML("<span>Hello World from test!</span>");
  });
});