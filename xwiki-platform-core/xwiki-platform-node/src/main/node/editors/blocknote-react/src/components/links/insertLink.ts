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
import type { EditorType } from "../../blocknote";
import type { LinkEditionData } from "./linkEdition";
import type { LinkEditionHooks } from "./linkEditionHooks";

/**
 * Writes a new link into the content, at the editor's selection, then focuses the editor back, so that the user can
 * keep typing after the link.
 *
 * The integration intercepts the link (including the reference of the linked resource and whether its label is
 * generated) before it is written into the content, e.g. to store that reference aside. Throwing from the hook cancels
 * the insertion.
 *
 * @param editor - the editor to insert the link into
 * @param linkData - the link to insert
 * @param linkEditionHooks - the hooks intercepting the link editions, if any
 * @param selectedText - the text selected when the link was configured: it is linked in place, without being replaced
 *   (which would strip the inline formatting it doesn't share), as long as the link label is that same text. When not
 *   specified, or when the label differs (the user changed the display text, or emptied it to have the label generated
 *   from the target), the selection is replaced with the link label.
 */
function insertLink(
  editor: EditorType,
  linkData: LinkEditionData,
  linkEditionHooks?: LinkEditionHooks,
  selectedText?: string,
): void {
  const { url, title } = linkEditionHooks?.beforeUpdate?.(linkData) ?? linkData;
  editor.createLink(url, title === selectedText ? undefined : title);
  editor.focus();
}

export { insertLink };
