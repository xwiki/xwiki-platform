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
import type { ResourceReference } from "@xwiki/platform-rendering-api";

/**
 * The parameters of a link resource reference that the link editor edits (the query string and the anchor of a page or
 * attachment link). The link editor leaves them out of the reference it produces when they are empty, so their absence
 * means that they were cleared.
 */
const EDITED_PARAMETERS: readonly string[] = ["queryString", "anchor"];

/**
 * Picks the resource reference to save for an edited link.
 *
 * When the link still points to the same resource, the reference of the edited link is kept, so that what the link
 * editor doesn't edit is preserved (e.g. the parameters it doesn't know about), except for the parameters it edits,
 * which are taken from the reference it produced, so that changing or clearing them is saved. When the link points to
 * another resource, the reference produced by the link editor is saved as is.
 *
 * @param previous - the resource reference of the edited link, if any
 * @param edited - the resource reference produced by the link editor
 * @returns the resource reference to save
 */
function editedLinkResourceReference(
  previous: ResourceReference | undefined,
  edited: ResourceReference,
): ResourceReference {
  if (
    previous?.type !== edited.type ||
    previous.reference !== edited.reference
  ) {
    return edited;
  }
  const parameters: Record<string, string> = Object.fromEntries(
    Object.entries(previous.parameters ?? {}).filter(
      ([name]) => !EDITED_PARAMETERS.includes(name),
    ),
  );
  for (const name of EDITED_PARAMETERS) {
    const value = edited.parameters?.[name];
    if (value !== undefined) {
      parameters[name] = value;
    }
  }
  return { ...previous, parameters };
}

export { editedLinkResourceReference };
