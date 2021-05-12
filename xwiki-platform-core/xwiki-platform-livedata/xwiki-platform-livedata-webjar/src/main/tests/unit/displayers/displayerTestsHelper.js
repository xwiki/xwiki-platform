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

/**
 * Generic Vue component initializer for the displayers tests. Calls `mount()` from `@vue/test-utils` with
 * preconfigured
 * values. All the preconfigured properties can be overloaded using the named optional parameters (`props`, `logic`,
 * `editBus`, and `mocks`) described below.
 *
 * The default `propData` parameter of `mount()` is:
 * ```javascript
 * { viewOnly: false, isView: true, propertyId: 'propertyIdTest', entry: { propertyIdTest: 'entryA1', propertyIdTest2:
 * 'entryB1' } }
 * ```
 * and is merged with the `props` parameter.
 *
 * The default `provide` parameter of `mount()` has two keys: `logic` and `editBus`, respectively merged with the
 * `logic` and `editBus` parameters.
 *
 * `provide.logic` is an object with the following keys:
 * * `isEditable()`, returns the constant `true`
 * * `getDisplayerDescriptor()` returns the constant `{ actions: ['action1', 'action2'] }`
 * * `isActionAllowed(action)` returns `true` when `action` is equals to `"action1"`, `false` otherwise
 * * `getActionDescriptor(action)` returns 
 * ```
 * { name: 'action1', icon: { iconSetName: 'Font Awesome', cssClass: 'fa fa-table', iconSetType: 'FONT', url: '' } }
 * ```
 * when `action` is equals to `"action1"`, `undefined` otherwise.
 *
 * `provide.editBus` is an object with the following keys:
 * * `start()` does nothing
 * * `isEditable()` returns the constant `true`
 *
 * The default `mock` parameter of `mount()` is an object with a single `$t()` key which returns the constant 
 * `"default test translation value"`
 *
 * 
 * @param displayer the Vue displayer component to initialize
 * @param props the props value to pass to the component (merged to the default propsData parameter)/
 * @param logic a mock of `Logic.js`
 * @param editBus a mock of the editBus Vue component
 * @param mocks additional mocks such as the `$t` translation operation.
 * @returns an initialized Vue component
 */
export function initWrapper(displayer, {props, logic, editBus, mocks})
{
  return mount(displayer, {
    propsData: Object.assign({
      viewOnly: false,
      isView: true,
      propertyId: 'propertyIdTest',
      entry: {
        propertyIdTest: 'entryA1',
        propertyIdTest2: 'entryB1'
      }
    }, props),
    provide: {
      logic: Object.assign({
        isEditable()
        {
          return true;
        },
        getDisplayerDescriptor()
        {
          return {
            actions: ['action1', 'action2']
          };
        },
        isActionAllowed(action)
        {
          return action === 'action1';
        },
        getActionDescriptor(action)
        {
          return {
            action1: {
              name: 'action1',
              icon: {
                iconSetName: 'Font Awesome',
                cssClass: 'fa fa-table',
                iconSetType: 'FONT',
                url: ''
              }
            }
          }[action];
        },
      }, logic),
      editBus: Object.assign({
        start()
        {
        },
        isEditable()
        {
          return true;
        }
      }, editBus),
    },
    mocks: Object.assign({
      $t: () => 'default test translation value'
    }, mocks)
  });
}
