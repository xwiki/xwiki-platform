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
import { EntityType } from "@xwiki/cristal-model-api";
import { Container } from "inversify";
import type { EntityReference } from "@xwiki/cristal-model-api";
import type {
  ModelReferenceHandlerProvider,
  ModelReferenceParserProvider,
  ModelReferenceSerializerProvider,
} from "@xwiki/cristal-model-reference-api";
import type {
  RemoteURLParserProvider,
  RemoteURLSerializerProvider,
} from "@xwiki/cristal-model-remote-url-api";

/**
 * Set of tools used by converters
 *
 * @since 0.16
 */
export type ConverterContext = {
  /**
   * Try to parse a reference from a string
   * This function must **NOT** throw
   *
   * @param reference - The reference string to parse
   * @param type - Optional type of reference, introduced in 0.17
   *
   * @returns The entity reference or `null` if the input as invalid. Must be inversible with `serializeReference`
   */
  parseReference(
    reference: string,
    type: EntityType | null,
  ): EntityReference | null;

  /**
   * Serialize a reference to a string
   * This function must **NOT** throw
   *
   * @since 0.17
   *
   * @param reference - The reference to serialize
   *
   * @returns The serialized reference. Must be inversible with `parseReference`
   */
  serializeReference(reference: EntityReference): string;

  /**
   * Parse the URL of a reference to that reference
   * This function must **NOT** throw
   *
   * @since 0.17
   *
   * @param url - The reference URL to parse
   *
   * @returns The reference or `null` if the input is invalid. Must be inversable with `getUrlFromReference`
   */
  parseReferenceFromUrl(url: string): EntityReference | null;

  /**
   * Get the URL a reference is pointing to
   * This function must **NOT** throw
   *
   * @since 0.17
   *
   * @param reference - The reference to get an URL from
   *
   * @returns The URL for the reference. Must be inversible with `getReferenceFromUrl`
   */
  getUrlFromReference(reference: EntityReference): string;

  /**
   * Get the display name of a reference
   * This function must **NOT** throw
   *
   * @since 0.17
   *
   * @param reference - The reference to get the name of
   *
   * @returns The display name for this reference
   */
  getDisplayName(reference: EntityReference): string;
};

/**
 * Automatically create a converter context from a container
 * This works by extracting the required providers from the container
 *
 * @since 0.16
 *
 * @param container - Cristal application's Inversify container
 *
 * @returns The container containing everything required by the various converters
 */
export function createConverterContext(container: Container): ConverterContext {
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
