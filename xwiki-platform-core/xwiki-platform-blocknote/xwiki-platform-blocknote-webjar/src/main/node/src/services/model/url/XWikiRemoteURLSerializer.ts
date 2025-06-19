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
import { absoluteXWikiEntityReference, toXWikiEntityReference } from "@/services/model/reference/XWikiEntityReference";
import { DocumentReference, EntityReference, EntityType, SpaceReference } from "@xwiki/cristal-model-api";
import { RemoteURLSerializer } from "@xwiki/cristal-model-remote-url-api";
import { Container, injectable } from "inversify";

@injectable("Singleton")
export class XWikiRemoteURLSerializer implements RemoteURLSerializer {
  public static bind(container: Container): void {
    container.bind("RemoteURLSerializer").to(XWikiRemoteURLSerializer).inSingletonScope().whenNamed("XWiki");
  }

  public serialize(reference?: EntityReference): string | undefined {
    if (!reference) {
      return undefined;
    }
    switch (reference.type) {
      case EntityType.WIKI:
        // URL of the wiki home page.
        return this.serialize(new SpaceReference(reference, "Main"));
      case EntityType.SPACE:
        // URL of the space home page.
        return this.getDocumentURL(new DocumentReference("WebHome", reference));
      case EntityType.DOCUMENT: {
        return this.getDocumentURL(reference);
      }
      case EntityType.ATTACHMENT: {
        return this.getAttachmentURL(reference);
      }
    }
  }

  private getDocumentURL(reference: EntityReference): string {
    return new XWiki.Document(absoluteXWikiEntityReference(toXWikiEntityReference(reference))).getURL();
  }

  private getAttachmentURL(reference: EntityReference): string {
    return new XWiki.Attachment(absoluteXWikiEntityReference(toXWikiEntityReference(reference))).getURL();
  }
}
