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
import { tryFallible } from "@xwiki/platform-fn-utils";
import { EntityType } from "@xwiki/platform-model-api";
import type { EntityReference } from "@xwiki/platform-model-api";
import type {
  ModelReferenceParser,
  ModelReferenceSerializer,
} from "@xwiki/platform-model-reference-api";
import type { ResourceReference } from "@xwiki/platform-rendering-api";

/**
 * Build the reference of a linked page or attachment.
 *
 * The model reference serializer produces a typed reference (e.g. "doc:Space.Page") while a resource
 * reference holds the type and the entity reference separately, so the type prefix is stripped. Not
 * all serializers add it though, so its presence is not assumed.
 */
function entityResourceReference(
  type: string,
  ref: EntityReference | null,
  modelReferenceSerializer: ModelReferenceSerializer,
): ResourceReference | undefined {
  const serialized =
    ref === null ? undefined : modelReferenceSerializer.serialize(ref);
  if (!serialized) {
    return undefined;
  }
  const prefix = `${type}:`;
  return typedResourceReference(
    type,
    serialized.startsWith(prefix)
      ? serialized.slice(prefix.length)
      : serialized,
  );
}

function typedResourceReference(
  type: string,
  reference: string,
): ResourceReference {
  return { type, typed: true, reference, parameters: {} };
}

function untypedResourceReference(
  type: string,
  reference: string,
): ResourceReference {
  return { type, typed: false, reference, parameters: {} };
}

function parseEntityReference(
  reference: string,
  type: EntityType,
  modelReferenceParser: ModelReferenceParser,
): EntityReference | null {
  return tryFallible(() => modelReferenceParser.parse(reference, { type }));
}

export {
  entityResourceReference,
  parseEntityReference,
  typedResourceReference,
  untypedResourceReference,
};
