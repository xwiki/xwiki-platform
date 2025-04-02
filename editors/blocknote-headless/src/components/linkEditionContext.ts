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

import { LinkSuggestor, createLinkSuggestor } from "./linkSuggest";
import { Container } from "inversify";
import type { LinkSuggestServiceProvider } from "@xwiki/cristal-link-suggest-api";
import type {
  ModelReferenceParser,
  ModelReferenceParserProvider,
  ModelReferenceSerializer,
  ModelReferenceSerializerProvider,
} from "@xwiki/cristal-model-reference-api";
import type {
  RemoteURLParser,
  RemoteURLParserProvider,
  RemoteURLSerializer,
  RemoteURLSerializerProvider,
} from "@xwiki/cristal-model-remote-url-api";

export type LinkEditionContext = {
  suggestLink: LinkSuggestor;
  modelReferenceParser: ModelReferenceParser;
  modelReferenceSerializer: ModelReferenceSerializer;
  remoteURLParser: RemoteURLParser;
  remoteURLSerializer: RemoteURLSerializer;
};

export function createLinkEditionContext(
  container: Container,
): LinkEditionContext {
  const linkSuggestService = container
    .get<LinkSuggestServiceProvider>("LinkSuggestServiceProvider")
    .get()!;

  const modelReferenceParser = container
    .get<ModelReferenceParserProvider>("ModelReferenceParserProvider")
    .get()!;

  const modelReferenceSerializer = container
    .get<ModelReferenceSerializerProvider>("ModelReferenceSerializerProvider")
    .get()!;

  const remoteURLParser = container
    .get<RemoteURLParserProvider>("RemoteURLParserProvider")
    .get()!;

  const remoteURLSerializer = container
    .get<RemoteURLSerializerProvider>("RemoteURLSerializerProvider")
    .get()!;

  return {
    suggestLink: createLinkSuggestor(linkSuggestService, modelReferenceParser),
    modelReferenceParser,
    modelReferenceSerializer,
    remoteURLParser,
    remoteURLSerializer,
  };
}
