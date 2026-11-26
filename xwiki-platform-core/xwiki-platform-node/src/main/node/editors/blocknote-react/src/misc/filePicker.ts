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

/**
 * Opens the system file picker.
 *
 * @param accept - the file types the file picker offers, e.g. "image/*" (all types when not specified)
 * @returns the file selected by the user, or `undefined` when the user closes the file picker without selecting one
 */
function pickFile(accept?: string): Promise<File | undefined> {
  return new Promise((resolve) => {
    // Creates a hidden file input element and temporarily adds it to the DOM, so we can open the system file picker by
    // clicking it. See
    // https://developer.mozilla.org/en-US/docs/Web/API/File_API/Using_files_from_web_applications#using_hidden_file_input_elements_using_the_click_method
    const input = document.createElement("input");
    input.type = "file";
    if (accept) {
      input.accept = accept;
    }
    input.style.display = "none";

    input.addEventListener("change", () => {
      input.remove();
      resolve(input.files?.[0]);
    });

    // Removes the input from the DOM again if the user closes the file picker without selecting a file.
    input.addEventListener("cancel", () => {
      input.remove();
      resolve(undefined);
    });

    document.body.appendChild(input);
    input.click();
  });
}

export { pickFile };
