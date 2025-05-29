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
import { Factory } from "./factory";
import { describe, expect, it, vi } from "vitest";

describe("factory.js", () => {
  vi.mock("@/services/logic.js", () => {
    return {
      Logic: class MockLogic {
        constructor(host) {
          this.host = host;
          this.name = host.name;
          this.ready = Promise.resolve(this);
        }

        destroy() {
          // Do nothing.
        }
      },
    };
  });

  it("should create a BlockNote instance", async () => {
    const factory = new Factory();
    const host = document.createElement("div");
    host.name = "test";

    expect(factory.get(host)).toBeUndefined();
    expect(factory.get("test")).toBeUndefined();

    const logic = await factory.create(host);

    expect(factory.get(host)).toBe(logic);
    expect(factory.get("test")).toBe(logic);

    expect(factory.destroy("test")).toBe(true);
    expect(factory.destroy(host)).toBe(false);

    expect(factory.get(host)).toBeUndefined();
    expect(factory.get("test")).toBeUndefined();
  });
});
