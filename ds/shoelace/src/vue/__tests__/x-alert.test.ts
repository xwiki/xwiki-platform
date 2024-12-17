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

import XAlert from "../x-alert.vue";
import { shallowMount } from "@vue/test-utils";
import { describe, expect, it } from "vitest";

describe("x-alert", () => {
  // TODO: reduce the number of statements in the following method and reactivate the disabled eslint rule.
  // eslint-disable-next-line max-statements
  it("has a description", () => {
    const xAlert = shallowMount(XAlert, {
      props: {
        title: "My Title",
        type: "warning",
        description: "My description",
        actions: [{ name: "My Action", callback: () => {} }],
      },
    });
    expect(xAlert.text()).match(/^My Title.*My description.*My Action$/);
    const xTitle = xAlert.find("strong");
    expect(xTitle.text()).eq("My Title");
    const xIcon = xAlert.find("c-icon-stub");
    expect(xIcon.attributes("name")).eq("exclamation-triangle");
    expect(xIcon.attributes("slot")).eq("icon");
    const xBtn = xAlert.find("x-btn");
    expect(xBtn.attributes("variant")).eq("text");
    expect(xBtn.attributes("size")).eq("small");
    expect(xBtn.text()).eq("My Action");
  });
});
