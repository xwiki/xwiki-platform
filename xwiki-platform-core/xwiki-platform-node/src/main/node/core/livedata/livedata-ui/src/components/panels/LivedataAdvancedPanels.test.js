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
import LivedataAdvancedPanels from "./LivedataAdvancedPanels.vue";
import { mount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";

describe("LivedataAdvancedPanels.vue", () => {
  it("Renders the given panel", async () => {
    const wrapper = await mount(LivedataAdvancedPanels, {
      global: {
        provide: {
          logic: {
            panels: [
              {
                id: "test",
                component: "test",
              },
            ],
          },
        }, stubs: {
          "test": {
            template: "<span>Hello World from {{ panel.id }}!</span>",
            props: { panel: Object },
          },
        },
      },
    });

    expect(wrapper.html()).toBe(
      "<div class=\"livedata-advanced-panels\"><span>Hello World from test!</span></div>");
  });
});
