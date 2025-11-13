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

import {
  AttachmentReference,
  DocumentReference,
  EntityType,
} from "@xwiki/platform-model-api";
import { injectable } from "inversify";
import type { ModelReferenceHandler } from "./modelReferenceHandler";
import type {
  EntityReference,
  SpaceReference,
  WikiReference,
} from "@xwiki/platform-model-api";

/**
 * Default implementation for {@link ModelReferenceHandler}.
 *
 * @since 0.13
 * @beta
 */
@injectable()
class DefaultModelReferenceHandler implements ModelReferenceHandler {
  createDocumentReference(
    name: string,
    space: SpaceReference,
  ): DocumentReference {
    return new DocumentReference(name, space);
  }

  getTitle(reference: EntityReference): string {
    switch (reference.type) {
      case EntityType.WIKI:
        return (reference as WikiReference).name;
      case EntityType.SPACE:
        return [...(reference as SpaceReference).names].pop()!;
      case EntityType.DOCUMENT: {
        return (reference as DocumentReference).name;
      }
      case EntityType.ATTACHMENT:
        return (reference as AttachmentReference).name;
    }
    return "";
  }
}

export { DefaultModelReferenceHandler };
