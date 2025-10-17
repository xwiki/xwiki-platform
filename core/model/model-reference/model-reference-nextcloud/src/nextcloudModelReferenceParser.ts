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
import type { DocumentService } from "@xwiki/cristal-document-api";
import type { EntityReference } from "@xwiki/cristal-model-api";
import type {
  ModelReferenceParser,
  ModelReferenceParserOptions,
} from "@xwiki/cristal-model-reference-api";

@injectable()
export class NextcloudModelReferenceParser implements ModelReferenceParser {
  private readonly _regExp = RegExp(/^\.attachments\.(?<id>\d+)\/(?<file>.+)$/);

  constructor(
    @inject("CristalApp") private readonly cristalApp: CristalApp,
    @inject("AuthenticationManagerProvider")
    private readonly authenticationManagerProvider: AuthenticationManagerProvider,
    @inject("DocumentService")
    private readonly documentService: DocumentService,
  ) {}

  parse(reference: string): EntityReference {
    if (/^https?:\/\//.test(reference)) {
      throw new Error(`[${reference}] is not a valid entity reference`);
    }
    return this.innerSynchronousParsing(reference);
  }

  async parseAsync(
    reference: string,
    options?: ModelReferenceParserOptions,
  ): Promise<EntityReference> {
    if (/^https?:\/\//.test(reference)) {
      const baseURL = this.cristalApp.getWikiConfig().baseURL;
      if (reference.startsWith(baseURL)) {
        const fileid = reference.substring(`${baseURL}/f/`.length);
        if (parseInt(fileid, 10)) {
          return await this.resolveByFileId(reference);
        } else {
          throw new Error(`[${reference}] is not a valid entity reference`);
        }
      } else {
        throw new Error(`[${reference}] is not a valid entity reference`);
      }
    } else {
      return this.legacyParsing(reference, options?.type);
    }
  }

  private async resolveByFileId(reference: string) {
    const baseURL = this.cristalApp.getWikiConfig().baseURL;
    const ocsUrl = new URL(`${baseURL}/ocs/v2.php/references/resolve`);
    ocsUrl.searchParams.set("reference", reference);
    const request = await fetch(ocsUrl, {
      headers: {
        ...(await this.getCredentials()),
        Accept: "application/json",
        "OCS-APIRequest": "true",
      },
    });
    const json = await request.json();
    const path = json.ocs.data.references[reference].richObject.path as string;
    const dirRoot =
      this.cristalApp
        .getWikiConfig()
        .storageRoot?.substring("/files/${username}".length) || ".cristal/";
    // Remove the root directory from the path
    return this.legacyParsing(
      path.substring(dirRoot.length).replace(/\.md$/, ""),
    );
  }

  private legacyParsing(reference: string, type?: EntityType) {
    if (type === EntityType.ATTACHMENT) {
      return this.relativeAttachmentParsing(reference);
    } else {
      return this.innerSynchronousParsing(reference);
    }
  }

  private innerSynchronousParsing(reference: string) {
    let segments = reference.split("/");
    if (segments[0] == "") {
      segments = segments.slice(1);
    }
    if (segments[segments.length - 2] == "attachments") {
      const documentName = segments[segments.length - 3];
      return new AttachmentReference(
        segments[segments.length - 1],
        new DocumentReference(
          documentName.startsWith(".")
            ? documentName.substring(1)
            : documentName,
          new SpaceReference(
            undefined,
            ...segments.slice(0, segments.length - 2),
          ),
        ),
      );
    } else {
      return new DocumentReference(
        segments[segments.length - 1],
        new SpaceReference(
          undefined,
          ...segments.slice(0, segments.length - 1),
        ),
      );
    }
  }

  private async getCredentials(): Promise<{ Authorization?: string }> {
    const authorizationHeader = await this.authenticationManagerProvider
      .get()
      ?.getAuthorizationHeader();
    const headers: { Authorization?: string } = {};
    if (authorizationHeader) {
      headers["Authorization"] = authorizationHeader;
    }
    return headers;
  }

  // eslint-disable-next-line max-statements
  private async relativeAttachmentParsing(
    reference: string,
  ): Promise<AttachmentReference> {
    const m = this._regExp.exec(reference);
    if (m?.groups?.id && m?.groups?.file) {
      const baseURL = this.cristalApp.getWikiConfig().baseURL;
      const resolved = await this.resolveByFileId(
        `${baseURL}/f/${m.groups.id}`,
      );
      return new AttachmentReference(
        m.groups.file,
        new DocumentReference(
          `.attachments.${m.groups.id}`,
          (resolved as DocumentReference).space,
        ),
        {
          "nextcloud-specific": "true",
        },
      );
    } else {
      const currentDocument =
        this.documentService.getCurrentDocumentReference().value!;

      const references = reference.split("/");
      const spaces = references.slice(0, references.length - 3);
      const currentSegments = [...(currentDocument.space?.names ?? [])];

      while (spaces[0] == "..") {
        currentSegments.pop();
        spaces.shift();
      }

      return new AttachmentReference(
        references[references.length - 1],
        new DocumentReference(
          references[references.length - 3].replace(/^\./, ""),
          new SpaceReference(undefined, ...[...currentSegments, ...spaces]),
        ),
      );
    }
  }
}
