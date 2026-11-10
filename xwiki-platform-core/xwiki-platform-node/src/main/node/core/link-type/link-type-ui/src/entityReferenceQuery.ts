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
import type { LinkContext } from "@xwiki/platform-link-type-api";
import type { EntityReference } from "@xwiki/platform-model-api";
import type { ModelReferenceSerializer } from "@xwiki/platform-model-reference-api";

/**
 * Serialize the reference of a page or an attachment as the text of the search box designating it, i.e. what the user
 * would type: relative to the document holding the link, when it's known, and without the entity type prefix that some
 * serializers add (e.g. "doc:"), since the search box only designates entities of one type.
 *
 * @param ref - the reference of the page or attachment
 * @param serializer - the model reference serializer
 * @param typePrefix - the entity type prefix to remove, if the serializer adds it (e.g. "doc:")
 * @param linkContext - where the link is
 * @returns the text of the search box designating the given entity
 */
function entityReferenceQuery(
  ref: EntityReference,
  serializer: ModelReferenceSerializer,
  typePrefix: string,
  linkContext?: LinkContext,
): string {
  const serialized =
    serializer.serialize(ref, linkContext?.sourceDocument) ?? "";
  return serialized.startsWith(typePrefix)
    ? serialized.slice(typePrefix.length)
    : serialized;
}

export { entityReferenceQuery };
