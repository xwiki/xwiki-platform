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

// Guards against setAttribute throwing on an invalid attribute name; it is not an escaping mechanism (the DOM escapes
// attribute values when the element is serialized).
const ATTRIBUTE_NAME = /^[a-zA-Z_:][-a-zA-Z0-9_:.]*$/;

/**
 * Copies XWiki parameters (`class`, `style` and any other attributes) onto the given element as HTML attributes. The
 * parameters come from the server renderer, either as a plain object (e.g. inside macro output, where they are kept
 * verbatim) or as a JSON string (e.g. a text style value in the top-level document, which the processor serializes so
 * BlockNote can store it as a primitive). Both forms are accepted.
 *
 * @param element - the element to apply the parameters to
 * @param params - the parameters as an object or a JSON-serialized object; anything else is ignored
 */
function applyXWikiParameters(element: HTMLElement, params: unknown): void {
  let map = params;
  if (typeof map === "string") {
    try {
      map = JSON.parse(map);
    } catch {
      return;
    }
  }
  if (!map || typeof map !== "object") {
    return;
  }
  for (const [name, value] of Object.entries(map)) {
    if (value != null && ATTRIBUTE_NAME.test(name)) {
      element.setAttribute(name, String(value));
    }
  }
}

export { applyXWikiParameters };
