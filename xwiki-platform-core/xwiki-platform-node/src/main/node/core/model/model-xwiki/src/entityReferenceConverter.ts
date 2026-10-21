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

import {
  AttachmentReference,
  DocumentReference,
  EntityType,
  SpaceReference,
  WikiReference,
} from "@xwiki/platform-model-api";
import {
  EntityReference as XWikiEntityReference,
  EntityType as XWikiEntityType,
  Model,
} from "@xwiki/platform-xwiki-model-api";
import type { EntityReference } from "@xwiki/platform-model-api";

/**
 * Converts a backend agnostic entity reference into its XWiki counterpart.
 *
 * @param reference - the backend agnostic reference to convert
 * @returns the equivalent XWiki entity reference
 * @since 18.8.0RC1
 * @beta
 */
function toXWikiEntityReference(
  reference: EntityReference,
): XWikiEntityReference {
  // Switch on a separate variable so that the default branch, which guards against reference types added later,
  // doesn't narrow the reference itself to never.
  const referenceType = reference.type;
  switch (referenceType) {
    case EntityType.WIKI:
      return toXWikiWikiReference(reference);
    case EntityType.SPACE:
      return toXWikiSpaceReference(reference);
    case EntityType.DOCUMENT:
      return toXWikiDocumentReference(reference);
    case EntityType.ATTACHMENT:
      return toXWikiAttachmentReference(reference);
    default:
      throw new Error(`Unsupported entity type: ${referenceType}`);
  }
}

function toXWikiWikiReference(reference: WikiReference): XWikiEntityReference {
  return new XWikiEntityReference(reference.name, XWikiEntityType.WIKI);
}

function toXWikiSpaceReference(
  reference: SpaceReference,
): XWikiEntityReference {
  if (!reference.names.length) {
    throw new Error(
      "XWiki space references must have at least one space name.",
    );
  }
  const wikiReference = reference.wiki
    ? toXWikiWikiReference(reference.wiki)
    : undefined;
  return reference.names.reduce(
    (parent: XWikiEntityReference | undefined, name: string) =>
      new XWikiEntityReference(name, XWikiEntityType.SPACE, parent),
    wikiReference,
  )!;
}

function toXWikiDocumentReference(
  reference: DocumentReference,
): XWikiEntityReference {
  return new XWikiEntityReference(
    reference.name,
    XWikiEntityType.DOCUMENT,
    reference.space ? toXWikiSpaceReference(reference.space) : undefined,
  );
}

function toXWikiAttachmentReference(
  reference: AttachmentReference,
): XWikiEntityReference {
  return new XWikiEntityReference(
    reference.name,
    XWikiEntityType.ATTACHMENT,
    reference.document
      ? toXWikiDocumentReference(reference.document)
      : undefined,
  );
}

/**
 * Converts an XWiki entity reference into its backend agnostic counterpart.
 *
 * @param reference - the XWiki reference to convert
 * @returns the equivalent backend agnostic reference, or undefined when no reference is given
 * @since 18.8.0RC1
 * @beta
 */
function toCristalEntityReference(
  reference?: XWikiEntityReference | null,
): EntityReference | undefined {
  if (!reference) {
    return undefined;
  }
  switch (reference.type) {
    case XWikiEntityType.WIKI:
      return new WikiReference(reference.name);
    case XWikiEntityType.SPACE:
      return new SpaceReference(
        toCristalEntityReference(
          reference.extractReference(XWikiEntityType.WIKI),
        ) as WikiReference,
        ...reference
          .getReversedReferenceChain()
          .filter((item) => item.type === XWikiEntityType.SPACE)
          .map((item) => item.name),
      );
    case XWikiEntityType.DOCUMENT:
      return new DocumentReference(
        reference.name,
        toCristalEntityReference(reference.parent) as SpaceReference,
        reference.name !== "WebHome",
      );
    case XWikiEntityType.ATTACHMENT:
      return new AttachmentReference(
        reference.name,
        toCristalEntityReference(reference.parent) as DocumentReference,
      );
    default:
      throw new Error(`Unsupported entity type: ${reference.type}`);
  }
}

/**
 * Resolves an XWiki entity reference against a base reference, so that the result no longer depends on it.
 *
 * @param reference - the reference to make absolute
 * @param baseReference - the reference the given one is relative to, typically the current document
 * @returns the absolute reference, or undefined when it cannot be resolved
 * @since 18.8.0RC1
 * @beta
 */
function absoluteXWikiEntityReference(
  reference: XWikiEntityReference,
  baseReference: XWikiEntityReference,
): XWikiEntityReference | null | undefined {
  return Model.resolve(
    Model.serialize(reference),
    reference.type,
    baseReference,
  );
}

/**
 * Resolves a backend agnostic entity reference against a base reference, so that the result no longer depends on it.
 *
 * @param reference - the reference to make absolute
 * @param baseReference - the XWiki reference the given one is relative to, typically the current document
 * @returns the absolute reference, or undefined when it cannot be resolved
 * @since 18.8.0RC1
 * @beta
 */
function absoluteCristalEntityReference(
  reference: EntityReference,
  baseReference: XWikiEntityReference,
): EntityReference | undefined {
  return toCristalEntityReference(
    absoluteXWikiEntityReference(
      toXWikiEntityReference(reference),
      baseReference,
    ),
  );
}

export {
  absoluteCristalEntityReference,
  absoluteXWikiEntityReference,
  toCristalEntityReference,
  toXWikiEntityReference,
};
