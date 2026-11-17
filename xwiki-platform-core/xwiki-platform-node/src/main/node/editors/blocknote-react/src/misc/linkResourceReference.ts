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
import { getCurrentPage } from "./fileUpload";
import { tryFallible } from "@xwiki/platform-fn-utils";
import { LinkType } from "@xwiki/platform-link-suggest-api";
import { EntityType } from "@xwiki/platform-model-api";
import { ResourceType } from "@xwiki/platform-rendering-api";
import type {
  ModelReferenceParserProvider,
  ModelReferenceSerializerProvider,
} from "@xwiki/platform-model-reference-api";
import type { ResourceReference } from "@xwiki/platform-rendering-api";
import type { Container } from "inversify";

/**
 * Builds the resource reference of a link to a page or an attachment, as the link editor does: typed, since a link
 * designates a page by default (an untyped attachment reference would be taken for a page reference), and relative to
 * the edited page, so that the link keeps pointing to the same entity when the edited page is moved together with it
 * (e.g. along with its attachments or its child pages).
 *
 * @param depsContainer - the container providing the model reference services
 * @param reference - the absolute serialized reference of the linked page or attachment
 * @param type - the type of the linked entity
 * @param page - the serialized reference of the edited page, the current page when not specified
 * @returns the resource reference of the link, or `undefined` when the given reference can't be parsed or serialized
 *   (the link is then saved from its URL)
 */
function toLinkResourceReference(
  depsContainer: Container,
  reference: string,
  type: LinkType,
  page?: string,
): ResourceReference | undefined {
  const parser = depsContainer
    .get<ModelReferenceParserProvider>("ModelReferenceParserProvider")
    .get();
  const serializer = depsContainer
    .get<ModelReferenceSerializerProvider>("ModelReferenceSerializerProvider")
    .get();
  const isAttachment = type === LinkType.ATTACHMENT;
  const entityReference = tryFallible(() =>
    parser?.parse(reference, {
      type: isAttachment ? EntityType.ATTACHMENT : EntityType.DOCUMENT,
      relative: false,
    }),
  );
  const base = tryFallible(() =>
    parser?.parse(page ?? getCurrentPage(depsContainer), {
      type: EntityType.DOCUMENT,
      relative: false,
    }),
  );
  const serialized =
    entityReference &&
    tryFallible(() =>
      serializer?.serialize(entityReference, base ?? undefined),
    );
  if (!serialized) {
    return undefined;
  }
  return {
    type: isAttachment ? ResourceType.ATTACHMENT : ResourceType.DOCUMENT,
    typed: true,
    reference: serialized,
    parameters: {},
  };
}

export { toLinkResourceReference };
