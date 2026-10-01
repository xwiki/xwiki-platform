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

import { offset } from "@floating-ui/react";
import type {
  Middleware,
  ReferenceElement,
  VirtualElement,
} from "@floating-ui/react";

/**
 * The element floating-ui positions the side menu against is the block's DOM element wrapped in a virtual element
 * (see `BlockPopover` in `@blocknote/react`), so the element to measure is its `contextElement`, except when a plain
 * `Element` is used as the reference.
 *
 * @param reference - the reference floating-ui is positioning against
 * @returns the block's DOM element, or `undefined` if there is none to measure yet
 */
function resolveBlockElement(reference: ReferenceElement): Element | undefined {
  if (reference instanceof Element) {
    return reference;
  }

  const { contextElement } = reference as VirtualElement;
  return contextElement instanceof Element ? contextElement : undefined;
}

/**
 * The elements that render content of their own rather than through the text they contain.
 */
const REPLACED_ELEMENTS = "img, svg, video, canvas, iframe";

/**
 * Returns the first node that renders something inside `element`, in document order: a non-empty text node or a
 * replaced element. Text alone is not enough, because an image that starts the block has none of its own: a captioned
 * image would then be measured on its caption, which is below it.
 *
 * @param element - the element to search
 * @returns the first node that renders something, or `undefined` if the block renders nothing on its own
 */
function findFirstVisibleNode(element: Element): Node | undefined {
  const walker = document.createTreeWalker(
    element,
    NodeFilter.SHOW_TEXT | NodeFilter.SHOW_ELEMENT,
    {
      acceptNode: (node) => {
        const rendersSomething =
          node instanceof Element
            ? node.matches(REPLACED_ELEMENTS)
            : !!node.textContent?.trim();
        // Skipping an element still walks its children, which is how the text several levels down is reached.
        return rendersSomething
          ? NodeFilter.FILTER_ACCEPT
          : NodeFilter.FILTER_SKIP;
      },
    },
  );
  return walker.nextNode() ?? undefined;
}

/**
 * Returns the rectangle of the block's first line. A range over a text node reports one rectangle per line box, so the
 * first one is the first line, no matter how many lines the block wraps on. A replaced element is measured whole,
 * since it makes up a line on its own.
 *
 * @param element - the block's DOM element to measure
 * @returns the first line's rectangle, or `undefined` if the block has nothing to measure
 */
function measureFirstLineRect(element: Element): DOMRect | undefined {
  const node = findFirstVisibleNode(element);
  if (!node) {
    return undefined;
  }

  if (node instanceof Element) {
    return node.getBoundingClientRect();
  }

  const range = document.createRange();
  range.selectNodeContents(node);
  return range.getClientRects()[0];
}

/**
 * Returns the vertical center, in viewport coordinates, of the block's first line. Blocks with no text of their own,
 * e.g. a divider, fall back to their first child, since that element's box is what is actually visible.
 *
 * @param element - the block's DOM element to measure
 * @returns the vertical center, in viewport coordinates, of the block's first line
 */
function measureFirstLineCenter(element: Element): number {
  const firstLine = measureFirstLineRect(element);
  const rect = firstLine?.height
    ? firstLine
    : (element.firstElementChild ?? element).getBoundingClientRect();
  return rect.top + rect.height / 2;
}

/**
 * Actually measure the bounding box of the first line of text, and use this to offset the menu so that it is vertically
 * centered on that line. We must do this because blocknote's default way of positioning the menu is to offset it by a
 * fixed amount per block type, which only matches its own styling.
 *
 * @returns the middleware to pass to floating-ui, through the side menu's `useFloatingOptions`
 */
export function centerOnFirstLine(): Middleware {
  return offset((state) => {
    const blockElement = resolveBlockElement(state.elements.reference);
    if (!blockElement) {
      return {};
    }

    const blockTop = blockElement.getBoundingClientRect().top;
    const lineCenter = measureFirstLineCenter(blockElement);
    return {
      crossAxis: lineCenter - blockTop - state.rects.floating.height / 2,
    };
  });
}
