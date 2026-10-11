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

import { DocumentReference, SpaceReference } from "@xwiki/platform-model-api";
import type { ModelReferenceHandler } from "./modelReferenceHandler";
import type { EntityReference } from "@xwiki/platform-model-api";

/**
 * Shared methods implementation for instances of {@link ModelReferenceHandler}.
 *
 * @since 18.4.0RC1
 * @beta
 */
abstract class AbstractModelReferenceHandler implements ModelReferenceHandler {
  abstract createDocumentReference(
    name: string,
    space: SpaceReference,
  ): DocumentReference;
  abstract getTitle(reference: EntityReference): string;

  getParentDocumentReference(
    reference: DocumentReference,
  ): DocumentReference | undefined {
    const parentSpace = reference.space
      ? this.getParentSpaceReference(reference.space)
      : undefined;

    if (parentSpace) {
      return this.createDocumentReference(
        reference.space!.names.at(-1)!,
        parentSpace,
      );
    }
  }

  getParentSpaceReference(
    reference: SpaceReference,
  ): SpaceReference | undefined {
    if (reference.names.length > 0) {
      return new SpaceReference(
        reference.wiki,
        ...reference.names.slice(0, -1),
      );
    } else {
      return undefined;
    }
  }
}

export { AbstractModelReferenceHandler };
