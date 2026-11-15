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
import { LinkType } from "@xwiki/platform-link-suggest-api";
import { ResourceType } from "@xwiki/platform-rendering-api";
import type {
  ResourceReference,
  ResourceReferenceParser,
} from "@xwiki/platform-rendering-api";

/**
 * Builds the resource reference of a page or an attachment from its serialized entity reference, rather than parsing
 * it back from its URL.
 *
 * @param resourceReferenceParser - the parser used to build the resource reference
 * @param reference - the serialized entity reference, e.g. of a link suggestion or of an uploaded attachment
 * @param type - the type of the referenced entity, used as the resource type when the reference isn't typed
 * @returns the resource reference of the specified entity
 */
function toResourceReference(
  resourceReferenceParser: ResourceReferenceParser,
  reference: string,
  type: LinkType,
): ResourceReference {
  return resourceReferenceParser.parse(reference, {
    type:
      type === LinkType.ATTACHMENT
        ? ResourceType.ATTACHMENT
        : ResourceType.DOCUMENT,
  });
}

export { toResourceReference };
