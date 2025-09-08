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
import { tryFallible } from "@xwiki/cristal-fn-utils";
import { Container } from "inversify";
import type {
  ModelReferenceHandlerProvider,
  ModelReferenceParserProvider,
  ModelReferenceSerializerProvider,
} from "@xwiki/cristal-model-reference-api";
import type {
  RemoteURLParserProvider,
  RemoteURLSerializerProvider,
} from "@xwiki/cristal-model-remote-url-api";
import type { ConverterContext } from "@xwiki/cristal-uniast-api";

/**
 * Automatically create a converter context from a container
 * This works by extracting the required providers from the container
 *
 * @since 0.16
 * @beta
 *
 * @param container - Cristal application's Inversify container
 *
 * @returns The container containing everything required by the various converters
 */
export function createConverterContext(container: Container): ConverterContext {
  const modelReferenceParserProvider =
    container.get<ModelReferenceParserProvider>("ModelReferenceParserProvider");
  const modelReferenceParser = modelReferenceParserProvider.get()!;

  const modelReferenceSerializer = container
    .get<ModelReferenceSerializerProvider>("ModelReferenceSerializerProvider")
    .get()!;

  const remoteURLParser = container
    .get<RemoteURLParserProvider>("RemoteURLParserProvider")
    .get()!;

  const remoteURLSerializer = container
    .get<RemoteURLSerializerProvider>("RemoteURLSerializerProvider")
    .get()!;

  const modelReferenceHandler = container
    .get<ModelReferenceHandlerProvider>("ModelReferenceHandlerProvider")
    .get()!;

  return {
    parseReference: (reference, type) =>
      tryFallible(() =>
        modelReferenceParser.parse(reference, type ?? undefined),
      ),

    serializeReference: (reference) =>
      modelReferenceSerializer.serialize(reference)!,

    parseReferenceFromUrl: (url) =>
      tryFallible(() => remoteURLParser.parse(url)) ?? null,

    getUrlFromReference: (reference) =>
      remoteURLSerializer.serialize(reference)!,

    getDisplayName: (reference) => modelReferenceHandler.getTitle(reference),
  };
}
