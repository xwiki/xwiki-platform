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
import { toXWikiEntityReference } from "@xwiki/platform-model-xwiki";
import { Model } from "@xwiki/platform-xwiki-model-api";
import { injectable } from "inversify";
import type { EntityReference } from "@xwiki/platform-model-api";
import type { ModelReferenceSerializer } from "@xwiki/platform-model-reference-api";

/**
 * Serializes entity references in the format of the default server-side entity reference serializer (e.g.
 * `wiki:Space.Page@file.png`), without any type prefix: the type of the entity is not part of its reference, so it
 * must be passed to the parser when it's not a document (see `ModelReferenceParserOptions.type`).
 */
@injectable()
class XWikiModelReferenceSerializer implements ModelReferenceSerializer {
  public serialize(
    reference?: EntityReference,
    base?: EntityReference,
  ): string | undefined {
    if (!reference) {
      return undefined;
    }
    return Model.serialize(
      toXWikiEntityReference(reference),
      base && toXWikiEntityReference(base),
    );
  }
}

export { XWikiModelReferenceSerializer };
