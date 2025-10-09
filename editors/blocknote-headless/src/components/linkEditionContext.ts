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

import { Container } from "inversify";
import type { AttachmentsService } from "@xwiki/cristal-attachments-api";
import type { DocumentService } from "@xwiki/cristal-document-api";
import type { LinkEditionContext } from "@xwiki/cristal-editors-blocknote-react";
import type { LinkSuggestServiceProvider } from "@xwiki/cristal-link-suggest-api";
import type {
  ModelReferenceHandlerProvider,
  ModelReferenceParserProvider,
  ModelReferenceSerializerProvider,
} from "@xwiki/cristal-model-reference-api";
import type {
  RemoteURLParserProvider,
  RemoteURLSerializerProvider,
} from "@xwiki/cristal-model-remote-url-api";

export function createLinkEditionContext(
  container: Container,
): LinkEditionContext {
  const linkSuggestService = container
    .get<LinkSuggestServiceProvider>("LinkSuggestServiceProvider")
    .get();

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
    linkSuggestService: linkSuggestService ?? null,
    modelReferenceParser,
    modelReferenceSerializer,
    modelReferenceHandler,
    remoteURLParser,
    remoteURLSerializer,
    attachmentsService,
    documentService,
  };
}
