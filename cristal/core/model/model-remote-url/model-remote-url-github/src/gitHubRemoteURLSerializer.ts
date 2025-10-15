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

import { AttachmentReference, EntityType } from "@xwiki/cristal-model-api";
import { inject, injectable } from "inversify";
import type { CristalApp } from "@xwiki/cristal-api";
import type { EntityReference } from "@xwiki/cristal-model-api";
import type { RemoteURLSerializer } from "@xwiki/cristal-model-remote-url-api";

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
        const spaces = reference.names.join("/");
        return `${this.getBaseRestURL()}/contents${spaces}`;
      }
      case EntityType.DOCUMENT: {
        const spaces = reference.space?.names.join("/");
        return `${this.getBaseRestURL()}/contents${spaces}/${reference.name}`;
      }
      case EntityType.ATTACHMENT: {
        return this.serializeAttachmentReference(reference);
      }
    }
  }

  private serializeAttachmentReference(
    attachmentReference: AttachmentReference,
  ) {
    const segments = [
      ...(attachmentReference.document.space?.names ?? []),
      "." + attachmentReference.document.name,
      "attachments",
      attachmentReference.name,
    ]
      .map(encodeURIComponent)
      .join("/");
    return `${this.getBaseURL()}/${segments}`;
  }

  private getBaseURL() {
    return this.cristalApp.getWikiConfig().baseURL;
  }

  private getBaseRestURL() {
    return this.cristalApp.getWikiConfig().baseRestURL;
  }
}

export { GitHubRemoteURLSerializer };
