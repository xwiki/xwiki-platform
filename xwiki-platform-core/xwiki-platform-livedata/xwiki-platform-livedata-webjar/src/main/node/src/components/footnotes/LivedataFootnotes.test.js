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
import LivedataFootnotes from "./LivedataFootnotes.vue";
import { mount } from "@vue/test-utils";
import _ from "lodash";
import { describe, expect, it } from "vitest";

/**
 * Initialize a LivedataFootnotes component using default values vue test-utils `mount` parameters.
 * The default parameters can be overridden using the `mountConfiguration` parameter.
 *
 * The default configuration is:
 * ```
 * {
 *   provide: {
 *     logic: {
 *       footnotes: {
 *         list() {
 *           return [];
 *         }
 *       }
 *     }
 *   },
 *   mocks: {
 *     $t: (key) => key
 *   }
 * }
 * ```
 *
 * @param mountConfiguration mount parameters merged over the default configuration
 * @returns {{options: string}|{}|*} an initialized LivedataFootnotes Vue component
 */
function initWrapper(mountConfiguration = {}) {
  return mount(LivedataFootnotes, _.merge({
    global: {
      provide: {
        logic: {
          footnotes: {
            list() {
              return [];
            },
          },
        },
      },
      mocks: {
        $t: (key) => key,
      },
    },
  }, mountConfiguration));
}

describe("LivedataFootnotes.vue", () => {
  it("Render when no footnote", () => {
    const wrapper = initWrapper();
    expect(wrapper.classes()).contain("footnotes");
    expect(wrapper.text()).toBe("");
  });

  it("Render when one footnote", () => {
    const wrapper = initWrapper({
      global: {
        provide: {
          logic: {
            footnotes: {
              list() {
                return [{ symbol: "1", translationKey: "a.b.c" }];
              },
            },
          },
        },
      },
    });
    expect(wrapper.classes()).contain("footnotes");
    expect(wrapper.find(".box").classes()).toEqual(["box", "infomessage", "footnote"]);
    expect(wrapper.find(".box").text()).toBe("(1) a.b.c");
  });
});
