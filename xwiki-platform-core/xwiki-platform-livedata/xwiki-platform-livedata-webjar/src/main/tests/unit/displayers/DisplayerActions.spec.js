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
import DisplayerActions from "../../../displayers/DisplayerActions";

describe('DisplayerActions.vue', () => {
  it('Renders an entry in view mode', () => {

    const descriptors = {
      action1: {
        name: 'action1',
        icon: {
          iconSetName: 'Font Awesome',
          cssClass: 'fa fa-table',
          iconSetType: 'FONT',
          url: ''
        }
      }
    }

    const mockLogic = {
      isEditable() {
        return true;
      },
      getDisplayerDescriptor(propertyId) {
        return {
          actions: ['action1', 'action2']
        };
      },
      isActionAllowed(action) {
        return action === 'action1';
      },
      getActionDescriptor(action) {
        return descriptors[action];
      }
    };

    const wrapper = mount(DisplayerActions, {
      propsData: {
        viewOnly: false,
        isView: true,
        propertyId: 'propertyIdTest',
        entry: {
          propertyIdTest: 'entryA1',
          propertyIdTest2: 'entryB1'
        }
      },
      provide: {logic: mockLogic}
    })
    expect(wrapper.text()).toMatch('action1')
  })
})