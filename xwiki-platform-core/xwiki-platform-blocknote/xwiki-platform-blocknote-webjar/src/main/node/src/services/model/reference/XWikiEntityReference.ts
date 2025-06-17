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
import {
  AttachmentReference,
  DocumentReference,
  EntityReference,
  EntityType,
  SpaceReference,
  WikiReference,
} from "@xwiki/cristal-model-api";

type XWikiEntityReference = {
  name: string;
  type: number;
  parent?: XWikiEntityReference;
  locale?: string;
  extractReference: (type: number) => XWikiEntityReference | undefined;
  getReversedReferenceChain: () => XWikiEntityReference[];
};

function toXWikiEntityReference(reference?: EntityReference): XWikiEntityReference | undefined {
  if (!reference) {
    return undefined;
  }
  switch (reference.type) {
    case EntityType.WIKI:
      return toXWikiWikiReference(reference);
    case EntityType.SPACE:
      return toXWikiSpaceReference(reference);
    case EntityType.DOCUMENT:
      return toXWikiDocumentReference(reference);
    case EntityType.ATTACHMENT:
      return toXWikiAttachmentReference(reference);
    default:
      throw new Error(`Unsupported entity type: ${reference.type}`);
  }
}

function toXWikiWikiReference(reference: WikiReference): XWikiEntityReference {
  return new XWiki.EntityReference(reference.name, XWiki.EntityType.WIKI);
}

function toXWikiSpaceReference(reference: SpaceReference): XWikiEntityReference {
  return reference.names.reduce(
    (parent: XWikiEntityReference | undefined, name: string) =>
      new XWiki.EntityReference(name, XWiki.EntityType.SPACE, parent),
    reference.wiki ? toXWikiWikiReference(reference.wiki) : undefined
  );
}

function toXWikiDocumentReference(reference: DocumentReference): XWikiEntityReference {
  return new XWiki.EntityReference(
    reference.name,
    XWiki.EntityType.DOCUMENT,
    reference.space ? toXWikiSpaceReference(reference.space) : undefined
  );
}

function toXWikiAttachmentReference(reference: AttachmentReference): XWikiEntityReference {
  return new XWiki.EntityReference(
    reference.name,
    XWiki.EntityType.ATTACHMENT,
    reference.document ? toXWikiDocumentReference(reference.document) : undefined
  );
}

function toCristalEntityReference(reference?: XWikiEntityReference): EntityReference {
  if (!reference) {
    return undefined;
  }
  switch (reference.type) {
    case XWiki.EntityType.WIKI:
      return new WikiReference(reference.name);
    case XWiki.EntityType.SPACE:
      return new SpaceReference(
        toCristalEntityReference(reference.extractReference(XWiki.EntityType.WIKI)),
        ...reference
          .getReversedReferenceChain()
          .filter((item: XWikiEntityReference) => item.type === XWiki.EntityType.SPACE)
          .map((item: XWikiEntityReference) => item.name)
      );
    case XWiki.EntityType.DOCUMENT:
      return new DocumentReference(
        reference.name,
        toCristalEntityReference(reference.parent),
        reference.name !== "WebHome"
      );
    case XWiki.EntityType.ATTACHMENT:
      return new AttachmentReference(reference.name, toCristalEntityReference(reference.parent));
    default:
      throw new Error(`Unsupported entity type: ${reference.type}`);
  }
}

export { toCristalEntityReference, toXWikiEntityReference, type XWikiEntityReference };
