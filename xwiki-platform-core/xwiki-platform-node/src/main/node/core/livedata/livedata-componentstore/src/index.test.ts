/**
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
import { componentStore } from "./index";
import { describe, expect, it } from "vitest";

describe("componentStore", () => {
  it("returns a component synchronously once it is loaded", async () => {
    const component = { name: "Loaded" };
    componentStore.register("displayer", "loaded", async () => component);

    expect(componentStore.isRegistered("displayer", "loaded")).toBe(true);
    expect(componentStore.get("displayer", "loaded")).toBeUndefined();

    expect(await componentStore.load("displayer", "loaded")).toBe(component);
    expect(componentStore.get("displayer", "loaded")).toBe(component);
  });

  it("does not return a component that is not registered", () => {
    expect(componentStore.isRegistered("displayer", "unknown")).toBe(false);
    expect(componentStore.get("displayer", "unknown")).toBeUndefined();
    expect(componentStore.get("unknownKind", "unknown")).toBeUndefined();
  });

  it("returns a component registered after it was requested", async () => {
    const component = { name: "Late" };
    const loaded = componentStore.load("displayer", "late");

    expect(componentStore.isRegistered("displayer", "late")).toBe(false);

    componentStore.register("displayer", "late", async () => component);

    expect(componentStore.isRegistered("displayer", "late")).toBe(true);
    expect(await loaded).toBe(component);
    expect(componentStore.get("displayer", "late")).toBe(component);
  });
});
