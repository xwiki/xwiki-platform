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
} from "@xwiki/cristal-model-api";
import { RemoteURLSerializer } from "@xwiki/cristal-model-remote-url-api";
import { inject, injectable } from "inversify";
import type { CristalApp } from "@xwiki/cristal-api";

@injectable()
class GitHubRemoteURLSerializer implements RemoteURLSerializer {
  constructor(@inject("CristalApp") private readonly cristalApp: CristalApp) {}

  serialize(reference?: EntityReference): string | undefined {
    if (!reference) {
      return undefined;
    }
    switch (reference.type) {
      case EntityType.WIKI:
        throw new Error("Not implemented");
      case EntityType.SPACE: {
        const spaceReference = reference as SpaceReference;
        const spaces = spaceReference.names.join("/");
        return `${this.getBaseRestURL()}/${spaces}`;
      }
      case EntityType.DOCUMENT: {
        const documentReference = reference as DocumentReference;
        const spaces = documentReference.space?.names.join("/");
        return `${this.getBaseRestURL()}/${spaces}/${documentReference.name}`;
      }
      case EntityType.ATTACHMENT: {
        return this.serializeAttachmentReference(
          reference as AttachmentReference,
        );
      }
    }
  }

  private serializeAttachmentReference(
    attachmentReference: AttachmentReference,
  ) {
    const spaces = attachmentReference.document.space?.names.join("/");
    return `${this.getBaseURL()}/${spaces}/${attachmentReference.document.name}/${attachmentReference.name}`;
  }

  private getBaseURL() {
    return this.cristalApp.getWikiConfig().baseURL;
  }

  private getBaseRestURL() {
    return this.cristalApp.getWikiConfig().baseRestURL;
  }
}

export { GitHubRemoteURLSerializer };
