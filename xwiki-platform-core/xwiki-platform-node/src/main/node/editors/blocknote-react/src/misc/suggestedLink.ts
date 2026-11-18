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
import { toLinkResourceReference } from "./linkResourceReference";
import { getSourceDocument } from "./sourceDocument";
import type { EditorInstance } from "../editorInstance";
import type { LinkEditionData } from "../services/linkEditor";
import type { Link } from "@xwiki/platform-link-suggest-api";

/**
 * A suggested or uploaded page or attachment to link to.
 */
type SuggestedLink = Pick<Link, "label" | "url" | "type"> & {
  /** The absolute serialized reference of the page or attachment, when known. */
  reference?: string;
};

/**
 * The label of a link to a suggested or uploaded page or attachment is generated from its target (the title of the
 * page, the name of the attachment), which is the label of the suggestion.
 *
 * @param instance - the editor instance where the link is inserted
 * @param link - the suggested or uploaded page or attachment to link to
 * @returns the link to insert
 */
function suggestedLinkData(
  instance: EditorInstance,
  link: SuggestedLink,
): LinkEditionData {
  return {
    title: link.label,
    generatedTitle: true,
    url: link.url,
    // Without a reference, the link is saved from its URL.
    reference:
      link.reference === undefined
        ? undefined
        : toLinkResourceReference(
            instance.serviceContainer,
            link.reference,
            link.type,
            getSourceDocument(instance),
          ),
  };
}

export { suggestedLinkData };
export type { SuggestedLink };
