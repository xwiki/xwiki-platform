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

import type {
  DocumentReference,
  EntityReference,
  SpaceReference,
} from "@xwiki/platform-model-api";

/**
 * A ModelReferenceHandler can do backend-specific operations involving
 * {@link @xwiki/platform-model-api#EntityReference | EntityReferences}.
 *
 * @since 0.13
 * @beta
 */
interface ModelReferenceHandler {
  /**
   * Returns a {@link DocumentReference} with a specific name and a parent
   * {@link SpaceReference}.
   *
   * @param name - the name of the document reference
   * @param space - the parent space of the document reference
   * @returns the document reference
   */
  createDocumentReference(
    name: string,
    space: SpaceReference,
  ): DocumentReference;

  /**
   * Return the title of a reference
   */
  getTitle(reference: EntityReference): string;
}

export type { ModelReferenceHandler };
