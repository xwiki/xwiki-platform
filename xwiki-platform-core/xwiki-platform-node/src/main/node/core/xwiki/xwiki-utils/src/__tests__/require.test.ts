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

import { loadById } from "..";
import { mockRequireJS } from "@xwiki/platform-test-requirejs";
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import type { RequireJSMock } from "@xwiki/platform-test-requirejs";

describe("loadById", () => {
  let requireJS: RequireJSMock;

  beforeEach(() => {
    requireJS = mockRequireJS({
      jquery: "jQuery module",
      moment: "moment module",
    });
  });

  afterEach(() => {
    requireJS.restore();
  });

  it("resolves with the module when a single identifier is given", async () => {
    await expect(loadById("jquery")).resolves.toBe("jQuery module");
  });

  it("resolves with the array of modules when several identifiers are given", async () => {
    await expect(loadById("jquery", "moment")).resolves.toStrictEqual([
      "jQuery module",
      "moment module",
    ]);
  });

  it("rejects when a module fails to load", async () => {
    await expect(loadById("missing")).rejects.toThrow(
      "No RequireJS module registered with identifier [missing].",
    );
  });
});
