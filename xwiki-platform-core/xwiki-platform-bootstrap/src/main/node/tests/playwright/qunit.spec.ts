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

import { test, expect } from "@playwright/test";
import { fileURLToPath, pathToFileURL } from "node:url";
import path from "node:path";

// The unchanged Bootstrap QUnit harness lives one level up from this spec.
const indexHtmlUrl = pathToFileURL(
  path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..", "index.html"),
).href;

test("Bootstrap QUnit plugin suite passes", async ({ page }) => {
  await page.goto(indexHtmlUrl);

  // index.html sets window.global_test_results from its QUnit.done() callback.
  await page.waitForFunction(
    () => (window as unknown as { global_test_results?: unknown }).global_test_results !== undefined,
    undefined,
    { timeout: 120_000 },
  );

  const results = await page.evaluate(
    () => (window as unknown as { global_test_results: { passed: number; failed: number; total: number } }).global_test_results,
  );

  expect(
    results.failed,
    `QUnit: ${results.passed}/${results.total} assertions passed, ${results.failed} failed`,
  ).toBe(0);
  expect(results.total).toBeGreaterThan(0);
});
