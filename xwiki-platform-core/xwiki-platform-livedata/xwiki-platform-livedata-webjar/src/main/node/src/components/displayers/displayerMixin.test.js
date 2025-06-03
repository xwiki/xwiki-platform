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

import displayerMixin from "./displayerMixin.js";
import { initWrapper } from "./displayerTestsHelper";
import { afterEach, describe, expect, it } from "vitest";
import sinon from "sinon";

const TestComponent = {
  render() {
  },
  title: "test component",
  mixins: [displayerMixin],
};

describe("displayerMixin.js", () => {
  afterEach(function() {
    // completely restore all fakes created through the sandbox
    sinon.restore();
  });

  describe("computed", function() {
    it("value()", () => {
      const wrapper = initWrapper(TestComponent, {});
      expect(wrapper.vm.value).toBe("red");
    });
    it("propertyDescriptor()", () => {
      const wrapper = initWrapper(TestComponent, {
        logic: {
          getPropertyDescriptor(propertyId) {
            return `returnPropertyDescriptor ${propertyId}`;
          },
        },
      });
      expect(wrapper.vm.propertyDescriptor).toBe("returnPropertyDescriptor color");
    });
    it("config()", () => {
      const wrapper = initWrapper(TestComponent, {
        logic: {
          getDisplayerDescriptor(propertyId) {
            return `returnConfig ${propertyId}`;
          },
        },
      });
      expect(wrapper.vm.config).toBe("returnConfig color");
    });
    it("data()", () => {
      const wrapper = initWrapper(TestComponent, {
        logic: { data: "dataTest" },
      });
      expect(wrapper.vm.data).toBe("dataTest");
    });
  });
});
