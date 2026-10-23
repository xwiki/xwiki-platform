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
import { withUniqueName } from "./uniqueFileName";
import { describe, expect, it } from "vitest";

describe("withUniqueName", () => {
  // <timestamp>-<random 100..999>.
  const generatedName = /^\d+-[1-9]\d\d/;

  function file(name, type = "image/png") {
    return new File(["content"], name, { type, lastModified: 1234567890 });
  }

  it("renames the file after the current timestamp", () => {
    expect(withUniqueName(file("image.png")).name).toMatch(
      new RegExp(`${generatedName.source}\\.png$`),
    );
  });

  it("keeps every extension of a multi-extension name", () => {
    expect(withUniqueName(file("archive.tar.gz")).name).toMatch(
      new RegExp(`${generatedName.source}\\.tar\\.gz$`),
    );
  });

  it("generates a name without extension when the file has none", () => {
    expect(withUniqueName(file("README")).name).toMatch(
      new RegExp(`${generatedName.source}$`),
    );
  });

  it("generates a name for a file that has none", () => {
    expect(withUniqueName(file("")).name).toMatch(
      new RegExp(`${generatedName.source}$`),
    );
  });

  it("keeps the media type, the last modified date and the content", async () => {
    const original = file("image.png", "image/webp");
    const renamed = withUniqueName(original);

    expect(renamed.type).toBe("image/webp");
    expect(renamed.lastModified).toBe(1234567890);
    expect(await renamed.text()).toBe("content");
  });

  it("generates a different name for each call", () => {
    const names = new Set(
      Array.from({ length: 50 }, () => withUniqueName(file("image.png")).name),
    );

    // The timestamp alone doesn't change fast enough, so this covers the random suffix too.
    expect(names.size).toBeGreaterThan(1);
  });
});
