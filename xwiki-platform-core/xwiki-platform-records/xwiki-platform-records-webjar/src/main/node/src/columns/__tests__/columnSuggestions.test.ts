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
import {
  givenPickerDialog,
  values,
} from "../../common/__tests__/suggestions.mock";
import { columnSuggestions } from "../columnSuggestions";
import { afterEach, describe, expect, it, vi } from "vitest";

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("columnSuggestions", () => {
  it("suggests every field matching the query", async () => {
    const { element, fetchJson } = givenPickerDialog();
    expect(
      values(await columnSuggestions.load(element, "co", fetchJson, [])),
    ).toEqual(["count", "code"]);
  });

  it("shows a stored column back under its label", async () => {
    const { element, fetchJson } = givenPickerDialog();
    const [option] = await columnSuggestions.resolve(
      element,
      "count",
      fetchJson,
    );
    expect(option).toMatchObject({ value: "count", label: "Count" });
  });
});
