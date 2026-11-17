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
import {
  toCristalEntityReference,
  toXWikiEntityReference,
} from "@xwiki/platform-model-xwiki";
import {
  EntityType as XWikiEntityType,
  Model,
} from "@xwiki/platform-xwiki-model-api";
import { injectable } from "inversify";
import type { EntityReference } from "@xwiki/platform-model-api";
import type {
  ModelReferenceParser,
  ModelReferenceParserOptions,
} from "@xwiki/platform-model-reference-api";

const XWIKI_ENTITY_TYPES: Record<EntityType, number> = {
  [EntityType.WIKI]: XWikiEntityType.WIKI,
  [EntityType.SPACE]: XWikiEntityType.SPACE,
  [EntityType.DOCUMENT]: XWikiEntityType.DOCUMENT,
  [EntityType.ATTACHMENT]: XWikiEntityType.ATTACHMENT,
};

/**
 * Parses entity references in the format of the default server-side entity reference resolver (e.g.
 * `wiki:Space.Page@file.png`), the format produced by the XWiki model reference serializer. The reference holds no
 * entity type, so it's parsed as a document reference unless another type is specified.
 */
@injectable()
class XWikiModelReferenceParser implements ModelReferenceParser {
  public parse(
    reference: string,
    options?: ModelReferenceParserOptions,
  ): EntityReference {
    const base = options?.base && toXWikiEntityReference(options.base);
    return toCristalEntityReference(
      Model.resolve(
        reference,
        XWIKI_ENTITY_TYPES[options?.type ?? EntityType.DOCUMENT],
        base,
      ),
    )!;
  }

  public async parseAsync(
    reference: string,
    options?: ModelReferenceParserOptions,
  ): Promise<EntityReference> {
    return this.parse(reference, options);
  }
}

export { XWikiModelReferenceParser };
