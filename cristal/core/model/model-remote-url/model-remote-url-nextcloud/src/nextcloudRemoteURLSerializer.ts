/**
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
  EntityType,
  SpaceReference,
} from "@xwiki/cristal-model-api";
import { inject, injectable } from "inversify";
import type { CristalApp } from "@xwiki/cristal-api";
import type { AuthenticationManagerProvider } from "@xwiki/cristal-authentication-api";
import type { EntityReference } from "@xwiki/cristal-model-api";
import type { RemoteURLSerializer } from "@xwiki/cristal-model-remote-url-api";

@injectable()
class NextcloudRemoteURLSerializer implements RemoteURLSerializer {
  constructor(
    @inject("CristalApp") private readonly cristalApp: CristalApp,
    @inject("AuthenticationManagerProvider")
    private readonly authenticationManagerProvider: AuthenticationManagerProvider,
  ) {}

  serialize(reference?: EntityReference): string | undefined {
    if (!reference) {
      return undefined;
    }
    switch (reference.type) {
      case EntityType.WIKI:
        throw new Error("Not implemented");
      case EntityType.SPACE:
        return this.serializeSpace(reference);
      case EntityType.DOCUMENT:
        return `${this.serializeDocument(reference)}.md`;
      case EntityType.ATTACHMENT:
        return this.serializeAttachment(reference);
    }
  }

  private serializeSpace(spaceReference: SpaceReference) {
    const spaces =
      spaceReference.names.length > 0
        ? "/" + spaceReference.names.join("/")
        : "";
    const userId = this.authenticationManagerProvider.get()?.getUserId?.();
    return `${this.getRootURL(spaceReference.wiki?.name ?? userId ?? "")}${spaces}`;
  }

  private serializeDocument(documentReference: DocumentReference) {
    return `${this.serializeSpace(documentReference.space!)}/${documentReference.name}`;
  }

  private serializeMeta(documentReference: DocumentReference) {
    return `${this.serializeSpace(documentReference.space!)}/.${documentReference.name}`;
  }

  private serializeAttachment(attachmentReference: AttachmentReference) {
    if (attachmentReference.metadata["nextcloud-specific"] === "true") {
      const spaceSegments = attachmentReference.document.space!
        ? this.serializeSpace(attachmentReference.document.space) + "/"
        : "";
      return `${spaceSegments}${attachmentReference.document.name}/${attachmentReference.name}`;
    } else {
      return `${this.serializeMeta(attachmentReference.document)}/attachments/${attachmentReference.name}`;
    }
  }

  private getRootURL(username: string) {
    const config = this.cristalApp.getWikiConfig();
    const url = (config.storageRoot ?? "/files/${username}/.cristal").replace(
      "${username}",
      username,
    );
    return `${config.baseRestURL}${url}`;
  }
}

export { NextcloudRemoteURLSerializer };
