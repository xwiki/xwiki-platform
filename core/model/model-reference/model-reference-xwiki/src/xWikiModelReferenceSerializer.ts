/*
 * See the LICENSE file distributed with this work for additional
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
  EntityReference,
  EntityType,
  SpaceReference,
  WikiReference,
} from "@xwiki/cristal-model-api";
import { ModelReferenceSerializer } from "@xwiki/cristal-model-reference-api";
import { injectable } from "inversify";

@injectable()
export class XWikiModelReferenceSerializer implements ModelReferenceSerializer {
  serialize(reference?: EntityReference): string | undefined {
    if (!reference) {
      return undefined;
    }
    const type = reference.type;
    const { SPACE, ATTACHMENT, DOCUMENT, WIKI } = EntityType;
    switch (type) {
      case WIKI:
        return (reference as WikiReference).name;
      case SPACE: {
        const spaceReference = reference as SpaceReference;
        const wiki = this.serialize(spaceReference.wiki);
        const spaces = spaceReference.names.join(".");
        if (wiki === undefined) {
          return spaces;
        } else {
          return `${wiki}:${spaces}`;
        }
      }
      case DOCUMENT: {
        const documentReference = reference as DocumentReference;
        const spaces = this.serialize(documentReference.space);
        const name = documentReference.name;
        if (spaces === undefined) {
          return name;
        } else {
          return `${spaces}.${name}`;
        }
      }
      case ATTACHMENT: {
        const attachmentReference = reference as AttachmentReference;
        const document = this.serialize(attachmentReference.document);
        const name = attachmentReference.name;
        return `${document}@${name}`;
      }
      default:
        throw new Error(`Unknown reference type [${type}]`);
    }
  }
}
