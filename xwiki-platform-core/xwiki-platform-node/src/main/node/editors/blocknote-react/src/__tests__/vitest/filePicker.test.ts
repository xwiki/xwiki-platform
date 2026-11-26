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
// @vitest-environment jsdom

import { pickFile } from "../../misc/filePicker";
import { describe, expect, test } from "vitest";

function getHiddenFileInput(): HTMLInputElement {
  const input = document.body.querySelector('input[type="file"]');
  expect(input).not.toBeNull();
  return input as HTMLInputElement;
}

describe("pickFile", () => {
  test("appends a hidden file input, resolves with the selected file, and removes the input", async () => {
    const promise = pickFile();

    const input = getHiddenFileInput();
    expect(input.style.display).toBe("none");
    const file = new File(["some content"], "picture.png");
    Object.defineProperty(input, "files", { value: [file] });
    input.dispatchEvent(new Event("change"));

    await expect(promise).resolves.toBe(file);
    expect(document.body.contains(input)).toBe(false);
  });

  test("offers all file types by default", () => {
    pickFile();

    const input = getHiddenFileInput();
    expect(input.hasAttribute("accept")).toBe(false);

    input.dispatchEvent(new Event("cancel"));
  });

  test("offers only the accepted file types when specified", () => {
    pickFile("image/*");

    const input = getHiddenFileInput();
    expect(input.accept).toBe("image/*");

    input.dispatchEvent(new Event("cancel"));
  });

  test("resolves with undefined when the change event carries no file", async () => {
    const promise = pickFile();

    const input = getHiddenFileInput();
    Object.defineProperty(input, "files", { value: [] });
    input.dispatchEvent(new Event("change"));

    await expect(promise).resolves.toBeUndefined();
    expect(document.body.contains(input)).toBe(false);
  });

  test("removes the input and resolves with undefined when the file picker is dismissed", async () => {
    const promise = pickFile();

    const input = getHiddenFileInput();
    input.dispatchEvent(new Event("cancel"));

    await expect(promise).resolves.toBeUndefined();
    expect(document.body.contains(input)).toBe(false);
  });
});
