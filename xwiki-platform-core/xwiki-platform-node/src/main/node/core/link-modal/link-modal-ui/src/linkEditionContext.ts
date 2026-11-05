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

import type { AttachmentsService } from "@xwiki/platform-attachments-api";
import type { DocumentService } from "@xwiki/platform-document-api";
import type {
  LinkSuggestService,
  LinkSuggestServiceProvider,
} from "@xwiki/platform-link-suggest-api";
import type {
  ModelReferenceHandler,
  ModelReferenceHandlerProvider,
  ModelReferenceParser,
  ModelReferenceParserProvider,
  ModelReferenceSerializer,
  ModelReferenceSerializerProvider,
} from "@xwiki/platform-model-reference-api";
import type {
  RemoteURLParser,
  RemoteURLParserProvider,
  RemoteURLSerializer,
  RemoteURLSerializerProvider,
} from "@xwiki/platform-model-remote-url-api";
import type { Container } from "inversify";

/**
 * Link edition context
 *
 * Contains various services needed by multiple components, avoids duplication. Exposed so that link target types
 * defined in other packages (e.g., `@xwiki/platform-link-type-default`) can access these services from their own
 * configuration component via `inject("linkEditionCtx")`.
 *
 * @since 18.10.0RC1
 * @beta
 */
type LinkEditionContext = {
  linkSuggestService: LinkSuggestService | null;
  modelReferenceParser: ModelReferenceParser;
  modelReferenceSerializer: ModelReferenceSerializer;
  modelReferenceHandler: ModelReferenceHandler;
  remoteURLParser: RemoteURLParser;
  remoteURLSerializer: RemoteURLSerializer;
  attachmentsService: AttachmentsService;
  documentService: DocumentService;
};

/**
 * Create a link edition context from a Cristal container
 *
 * @param container - The container to provide from
 * @returns The link edition context
 *
 * @since 18.5.0RC1
 * @beta
 */
function createLinkEditionContext(container: Container): LinkEditionContext {
  const linkSuggestServiceProvider = container.get<LinkSuggestServiceProvider>(
    "LinkSuggestServiceProvider",
  );

  const modelReferenceParser = container
    .get<ModelReferenceParserProvider>("ModelReferenceParserProvider")
    .get()!;

  const modelReferenceSerializer = container
    .get<ModelReferenceSerializerProvider>("ModelReferenceSerializerProvider")
    .get()!;

  const modelReferenceHandler = container
    .get<ModelReferenceHandlerProvider>("ModelReferenceHandlerProvider")
    .get()!;

  const remoteURLParser = container
    .get<RemoteURLParserProvider>("RemoteURLParserProvider")
    .get()!;

  const remoteURLSerializer = container
    .get<RemoteURLSerializerProvider>("RemoteURLSerializerProvider")
    .get()!;

  const attachmentsService =
    container.get<AttachmentsService>("AttachmentsService");

  const documentService = container.get<DocumentService>("DocumentService")!;

  return {
    linkSuggestService: linkSuggestServiceProvider.get() ?? null,
    modelReferenceParser,
    modelReferenceSerializer,
    modelReferenceHandler,
    remoteURLParser,
    remoteURLSerializer,
    attachmentsService,
    documentService,
  };
}

export { createLinkEditionContext };
export type { LinkEditionContext };
