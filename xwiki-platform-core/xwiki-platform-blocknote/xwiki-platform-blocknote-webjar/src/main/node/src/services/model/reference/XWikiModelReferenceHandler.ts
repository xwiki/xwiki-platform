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

import { absoluteCristalEntityReference } from "@/services/model/reference/XWikiEntityReference";
import {
  AttachmentReference,
  DocumentReference,
  EntityReference,
  EntityType,
  SpaceReference,
  WikiReference,
} from "@xwiki/cristal-model-api";
import { ModelReferenceHandler } from "@xwiki/cristal-model-reference-api";
import { Container, injectable } from "inversify";

@injectable("Singleton")
export class XWikiModelReferenceHandler implements ModelReferenceHandler {
  public static bind(container: Container): void {
    container.bind("ModelReferenceHandler").to(XWikiModelReferenceHandler).inSingletonScope().whenNamed("XWiki");
  }

  public createDocumentReference(name: string, space: SpaceReference): DocumentReference {
    const newSpace = new SpaceReference(space.wiki, ...space.names, name);
    return new DocumentReference("WebHome", newSpace);
  }

  public getTitle(reference: EntityReference): string {
    const absoluteReference = absoluteCristalEntityReference(reference);
    switch (absoluteReference.type) {
      case EntityType.WIKI:
        return (absoluteReference as WikiReference).name;
      case EntityType.SPACE:
        return [...(absoluteReference as SpaceReference).names].pop()!;
      case EntityType.DOCUMENT: {
        const name = (absoluteReference as DocumentReference).name;
        if (name === "WebHome") {
          return this.getTitle((absoluteReference as DocumentReference).space!);
        } else {
          return name;
        }
      }
      case EntityType.ATTACHMENT:
        return (absoluteReference as AttachmentReference).name;
    }
    return "";
  }
}
