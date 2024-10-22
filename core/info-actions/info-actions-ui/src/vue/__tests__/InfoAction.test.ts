/*
 * See the LICENSE file distributed with this work for additional
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
import { describe, expect, it, vi } from "vitest";
import { flushPromises, mount } from "@vue/test-utils";
import { InfoAction as InfoActionAPI } from "@xwiki/cristal-info-actions-api";
import { ref, Ref } from "vue";
import { useRoute } from "vue-router";
import InfoAction from "../InfoAction.vue";
import { wrapInSuspense } from "@xwiki/cristal-dev-test-utils";

describe("info-action", () => {
  it("Takes into account the info action values", async () => {
    vi.mock("vue-router");
    useRoute.mockReturnValue({
      params: {
        page: "testPage",
      },
    });

    const infoActionProp: InfoActionAPI = {
      iconName: "testName",
      id: "testId",
      order: 1,
      async counter(): Promise<Ref<number>> {
        return ref(10);
      },
    };

    const infoAction = mount(
      wrapInSuspense(InfoAction, {
        props: {
          infoAction: infoActionProp,
        },
      }),
      // Exclude suspense and InfoAction from the stubs as we only want to stub
      // components used internally in InfoAction
      {
        shallow: true,
        global: {
          stubs: { InfoAction: false, Suspense: false },
        },
      },
    );
    // Wait for all the asynchronous operations to be terminated before starting
    // to assert the rendered content.
    await flushPromises();
    expect(infoAction.classes()).to.contain("testId");
    expect(infoAction.find("c-icon-stub").attributes("name")).toBe("testName");
    expect(infoAction.find(".counter").text()).toBe("10");
  });
});
