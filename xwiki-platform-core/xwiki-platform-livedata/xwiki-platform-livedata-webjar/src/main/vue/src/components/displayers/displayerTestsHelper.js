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

import { mount } from "@vue/test-utils";
import $ from "jquery";
import sinon from "sinon";

/**
 * Generic Vue component initializer for the displayers tests. Calls `mount()` from
 * `@vue/test-utils` with preconfigured values. All the preconfigured properties can be overloaded
 * using the named optional parameters (`props`, `logic`, and `mocks`) described below.
 *
 * The default `propData` parameter of `mount()` is:
 * ```javascript
 * { viewOnly: false, isView: true, propertyId: 'color', entry: { color: 'red', age: 13 } }
 * ```
 * and is merged with the `props` parameter.
 *
 * The default `provide` parameter of `mount()` has one key: `logic`, merged with the `logic`
 * parameter.
 *
 * `provide.logic` is an object with the following keys:
 * * `isEditable()`, returns the constant `true`
 * * `getDisplayerDescriptor()` returns the constant `{ actions: ['jump', 'dance'] }`
 * * `isActionAllowed(action)` returns `true` when `action` is equals to `"jump"`, `false`
 * otherwise
 * * `getActionDescriptor(action)` returns
 * ```
 * { name: 'jump', icon: { iconSetName: 'Font Awesome', cssClass: 'fa fa-table', iconSetType:
 * 'FONT', url: '' } }
 * ```
 * * `getEditBus` returns the edit bus.
 * when `action` is equals to `"jump"`, `undefined` otherwise.
 *
 * The `getEditBus()` method returns an object with the following keys:
 * * `start()` does nothing
 * * `isEditable()` returns the constant `true`
 * The returns object is merged with the `editBus` parameter.
 *
 * The default `mock` parameter of `mount()` is an object with a single `$t()` key which returns
 * the parameter key as the result.
 *
 *
 *
 * @param displayer the Vue displayer component to initialize
 * @param props the props value to pass to the component (merged to the default props parameter)
 * @param logic a mock of `Logic.js`
 * @param editBus a mock of the editBus Vue component
 * @param mocks additional mocks such as the `$t` translation operation.
 * @returns an initialized Vue component
 */
export function initWrapper(displayer, { props, logic, editBus, mocks }) {
  global.xcontext = { locale: "en" };
  global.XWiki = {
    EntityType: {},
    Model: {
      resolve() {
      },
    },
    Document: class {
      getURL() {
        return "http://localhost/";
      }
    },
  };

  // Defines jQUery globally if it is not already done.
  if (!global.$) {
    global.$ = global.jQuery = $;
  }

  // Mocks daterangepicker
  global.$.fn.daterangepicker = sinon.stub();
  global.$.fn.daterangepicker.resolves({
    show: () => {
    },
  });

  // Mock $.data
  global.$.fn.data = sinon.stub(global.$.fn, "data");
  global.$.fn.data.resolves({
    show: () => {
    },
  });

  // Mock fetch.
  global.fetch = sinon.stub(global, "fetch");
  global.fetch.resolves({
    json: async () => {
      return { icons: [] };
    },
  });

  // Creates a div in the document body. It will be used as the attach point when mounting the Vue
  // component. This is useful for some assertions, for instance when testing which element is on
  // focus.
  const elem = document.createElement("div");
  if (document.body) {
    document.body.appendChild(elem);
  }

  return mount(displayer, {
    attachTo: elem,
    props: {
      viewOnly: false,
      isView: true,
      propertyId: "color",
      entry: {
        color: "red",
        age: "13",
      }, ...props,
    }, global: {
      provide: {
        jQuery: $,
        logic: {
          isEditable() {
            return true;
          }, getDisplayerDescriptor() {
            return {
              actions: ["jump", "dance"],
            };
          }, isActionAllowed(action) {
            return action === "jump" || action === "view";
          }, getActionDescriptor(action) {
            return {
              jump: {
                name: "jump", icon: {
                  iconSetName: "Font Awesome",
                  cssClass: "fa fa-table",
                  iconSetType: "FONT",
                  url: "",
                },
              },
            }[action];
          }, getEditBus() {
            return {
              start() {
              }, isEditable() {
                return true;
              }, onAnyEvent: () => {
              }, ...editBus,
            };
          }, footnotes: {
            put() {
            }, reset() {
            }, list() {
              return [];
            },
          }, isContentTrusted: () => true, ...logic,
        },
      }, mocks: { $t: (key) => key, ...mocks },
    },
  });
}
