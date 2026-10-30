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

import { createImageBlockSpec } from "@blocknote/core";

type ImageBlockSpec = ReturnType<typeof createImageBlockSpec>;

/**
 * Derives a label for an image from the URL it is loaded from, so that an image the browser can't display is still
 * identifiable. The last path segment is the file name for an attachment URL and the best guess for any other URL.
 *
 * @param url - the image URL, which can be relative, absolute or empty
 * @returns the label, empty when the URL holds nothing usable
 */
function getLabelFromURL(url: string): string {
  // A base is needed to parse a relative URL, and is ignored for an absolute one.
  const path = URL.canParse(url, document.baseURI)
    ? new URL(url, document.baseURI).pathname
    : url;
  // Already decoded by the URL parser, but a URL we failed to parse isn't.
  const lastSegment = path.split("/").pop() ?? "";
  try {
    return decodeURIComponent(lastSegment);
  } catch {
    // Malformed escape sequence, so the segment is the best we have.
    return lastSegment;
  }
}

/**
 * The image block of BlockNote, displaying the file name when the image itself can't be displayed. BlockNote leaves
 * the alt text empty unless the user sets one, which marks the image as decorative: right for a reader, but in the
 * editor it makes a broken image indistinguishable from an empty block. The fallback is only displayed, never stored:
 * the alt text is saved only when the user sets it explicitly.
 *
 * @returns the image block specification
 */
function XWikiImageBlock(): ImageBlockSpec {
  const spec = createImageBlockSpec();

  const render = spec.implementation.render;
  if (render) {
    spec.implementation.render = function (block, editor) {
      const nodeView = render.call(this, block, editor);
      if (!block.props.name) {
        const image = nodeView.dom.querySelector("img");
        image?.setAttribute("alt", getLabelFromURL(String(block.props.url)));
      }
      return nodeView;
    };
  }

  return spec;
}

export { XWikiImageBlock, getLabelFromURL };
