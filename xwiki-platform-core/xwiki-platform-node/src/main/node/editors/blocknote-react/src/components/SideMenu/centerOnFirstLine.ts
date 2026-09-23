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
 * Returns the first non-empty text node inside `element`, in document order. The element floating-ui positions the
 * menu against is the block's outer wrapper, so the text that makes up the block's first line is several levels down
 * from it.
 *
 * @param element - the element to search
 * @returns the first non-empty text node, or `undefined` if the block has no text at all
 */
function findFirstNonEmptyTextNode(element: Element): Text | undefined {
  const walker = document.createTreeWalker(element, NodeFilter.SHOW_TEXT);
  let textNode = walker.nextNode() as Text | null;
  while (textNode && !textNode.textContent?.trim()) {
    textNode = walker.nextNode() as Text | null;
  }
  return textNode ?? undefined;
}

/**
 * Returns the rectangle of the block's first line of text. A range over a text node reports one rectangle per line
 * box, so the first one is the first line, no matter how many lines the block wraps on.
 *
 * @param element - the block's DOM element to measure
 * @returns the first line's rectangle, or `undefined` if the block has no text to measure
 */
function measureFirstLineRect(element: Element): DOMRect | undefined {
  const textNode = findFirstNonEmptyTextNode(element);
  if (!textNode) {
    return undefined;
  }

  const range = document.createRange();
  range.selectNodeContents(textNode);
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
 * A floating-ui middleware that keeps the block side menu vertically centered on the block's first line, however tall
 * that line renders: its own font size, and whatever margin or padding a skin puts around it. BlockNote's own
 * middleware instead offsets the menu by a fixed amount per block type, which only matches its own styling. Both the
 * first line's position and the menu's own height are measured from the actual render, so this stays correct across
 * skins, themes and block content without replicating anything about how either is styled.
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
    // The menu is placed with the "left-start" placement, on which the cross axis is the vertical one.
    return {
      crossAxis: lineCenter - blockTop - state.rects.floating.height / 2,
    };
  });
}
