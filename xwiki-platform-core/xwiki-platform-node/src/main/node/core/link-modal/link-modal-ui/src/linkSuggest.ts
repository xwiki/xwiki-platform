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

import { EntityType } from "@xwiki/platform-model-api";
import type {
  Link,
<<<<<<< HEAD:xwiki-platform-core/xwiki-platform-node/src/main/node/editors/blocknote-react/src/misc/linkSuggest.ts
=======
  LinkSuggestService,
>>>>>>> b6b7f1704e (XWIKI-24269: Improvements for the link insertion dialog):xwiki-platform-core/xwiki-platform-node/src/main/node/core/link-modal/link-modal-ui/src/linkSuggest.ts
  LinkSuggestServiceProvider,
  LinkType,
} from "@xwiki/platform-link-suggest-api";
import type {
  AttachmentReference,
  DocumentReference,
} from "@xwiki/platform-model-api";
<<<<<<< HEAD:xwiki-platform-core/xwiki-platform-node/src/main/node/editors/blocknote-react/src/misc/linkSuggest.ts
import type { ModelReferenceParserProvider } from "@xwiki/platform-model-reference-api";
import type { Container } from "inversify";
=======
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
 * @since 18.5.0RC1
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
>>>>>>> b6b7f1704e (XWIKI-24269: Improvements for the link insertion dialog):xwiki-platform-core/xwiki-platform-node/src/main/node/core/link-modal/link-modal-ui/src/linkSuggest.ts

/**
 * Describe a link suggestion action (i.e., a search result entry).
 *
 * @since 18.5.0RC1
 * @beta
 */
type LinkSuggestion = {
  title: string;
  segments: string[];
  reference: string;
  url: string;
  type: LinkType;
};

/**
 * Shape of a function providing a list of link suggestions for a given query
 *
 * @since 18.5.0RC1
 * @beta
 */
type LinkSuggestor = (params: { query: string }) => Promise<LinkSuggestion[]>;

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

/**
 * Build a function returning an array of link suggestions from a string.
 *
 * @returns `null` if the context does not contain a link suggestion service
 *
 * @since 18.0.0RC1
 * @beta
 */
function createLinkSuggestor(depsContainer: Container): LinkSuggestor | null {
  const linkSuggestService = depsContainer
    .get<LinkSuggestServiceProvider>("LinkSuggestServiceProvider")
    .get()!;

  if (!linkSuggestService) {
    return null;
  }

  const modelReferenceParser = depsContainer
    .get<ModelReferenceParserProvider>("ModelReferenceParserProvider")
    .get()!;

  // Return an array of suggestions from a query

  return async ({ query }) => {
    // TODO: add upload attachment action
    // TODO: add create new page action
    // TODO: add links suggestions
    let links: Link[];

    try {
      links = await linkSuggestService.getLinks(query);
    } catch (e) {
      console.group("Failed to fetch remote links");
      console.error(e);
      console.groupEnd();
      links = [];
    }

    const equalityOperator = queryEqualityOperator(query);

    return links
      .filter((link) => equalityOperator(link.label))
      .map((link) => {
        // FIXME: relate to link management is reference management, here too we
        // need to think me precisely of the architecture we want for this.
        const entityReference = modelReferenceParser?.parse(link.reference, {
          relative: false,
        });

        const documentReference =
          entityReference?.type == EntityType.ATTACHMENT
            ? (entityReference as AttachmentReference).document
            : (entityReference as DocumentReference);

        const segments = documentReference.space?.names.slice(0) ?? [];

        // TODO: replace with an actual construction of segments from a reference
        if (documentReference.terminal) {
          segments.push(documentReference.name);
        }

        return {
          title: documentReference.name,
          segments,
          reference: link.reference,
          url: link.url,
          type: link.type,
        };
      });
  };
}

function queryEqualityOperator(query: string) {
  const queryNoCase = query.toLowerCase();
  return (value: string): boolean => {
    return value.toLowerCase().includes(queryNoCase);
  };
}

<<<<<<< HEAD:xwiki-platform-core/xwiki-platform-node/src/main/node/editors/blocknote-react/src/misc/linkSuggest.ts
export { createLinkSuggestor };
export type { LinkSuggestion, LinkSuggestor, LinkType };
=======
export { createLinkEditionContext, createLinkSuggestor };
export type { LinkEditionContext, LinkSuggestion, LinkSuggestor };
>>>>>>> b6b7f1704e (XWIKI-24269: Improvements for the link insertion dialog):xwiki-platform-core/xwiki-platform-node/src/main/node/core/link-modal/link-modal-ui/src/linkSuggest.ts
