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
import { translateLabel } from "./labels";
import {
  entityResourceReference,
  linkURLPartsFromReference,
  linkURLPartsFromURL,
  linkURLSuffix,
  parseEntityReference,
} from "./referenceHelpers";
import { tryFallible } from "@xwiki/platform-fn-utils";
import { EntityType } from "@xwiki/platform-model-api";
import { ResourceType } from "@xwiki/platform-rendering-api";
import { inject, injectable } from "inversify";
import type { LinkTargetType } from "@xwiki/platform-link-type-api";
import type { AttachmentReference } from "@xwiki/platform-model-api";
import type {
  ModelReferenceParserProvider,
  ModelReferenceSerializerProvider,
} from "@xwiki/platform-model-reference-api";
import type {
  RemoteURLParserProvider,
  RemoteURLSerializerProvider,
} from "@xwiki/platform-model-remote-url-api";
import type { ResourceReference } from "@xwiki/platform-rendering-api";

/**
 * Configuration for an attachment link
 *
 * @since 18.10.0RC1
 * @beta
 */
type AttachmentLinkTargetConfig = {
  ref: AttachmentReference | null;

  /**
   * The query string of the link, without the "?" URL separator (e.g. "key1=value1&key2=value2")
   */
  queryString?: string;
};

/**
 * Built-in link target type for links to an attachment.
 */
@injectable()
class AttachmentLinkTargetType
  implements LinkTargetType<AttachmentLinkTargetConfig>
{
  readonly type = "attachment";
  readonly order = 100;

  constructor(
    @inject("RemoteURLParserProvider")
    private readonly remoteURLParserProvider: RemoteURLParserProvider,
    @inject("RemoteURLSerializerProvider")
    private readonly remoteURLSerializerProvider: RemoteURLSerializerProvider,
    @inject("ModelReferenceParserProvider")
    private readonly modelReferenceParserProvider: ModelReferenceParserProvider,
    @inject("ModelReferenceSerializerProvider")
    private readonly modelReferenceSerializerProvider: ModelReferenceSerializerProvider,
  ) {}

  getLabel(locale: string): Promise<string> {
    return translateLabel("link-type.target-types.attachment.label", locale);
  }

  createDefaultConfig(): AttachmentLinkTargetConfig {
    return { ref: null };
  }

  fromURL(url: string): AttachmentLinkTargetConfig | null {
    const ref = tryFallible(() =>
      this.remoteURLParserProvider.get()?.parse(url),
    );

    if (ref?.type !== EntityType.ATTACHMENT) {
      return null;
    }

    return { ref, queryString: linkURLPartsFromURL(url).queryString };
  }

  toURL({ ref, queryString }: AttachmentLinkTargetConfig): string {
    const base = this.remoteURLSerializerProvider
      .get()
      ?.serialize(ref ?? undefined);

    return `${base ?? ""}${linkURLSuffix({ queryString })}`;
  }

  /**
   * @returns the name of the attached file
   */
  getDefaultLabel({ ref }: AttachmentLinkTargetConfig): string {
    return ref?.name ?? "";
  }

  fromReference(
    reference: ResourceReference,
  ): AttachmentLinkTargetConfig | null {
    if (reference.type !== ResourceType.ATTACHMENT) {
      return null;
    }

    const ref = parseEntityReference(
      reference.reference,
      EntityType.ATTACHMENT,
      this.modelReferenceParserProvider,
    );

    return ref?.type === EntityType.ATTACHMENT
      ? { ref, queryString: linkURLPartsFromReference(reference).queryString }
      : null;
  }

  toReference({
    ref,
    queryString,
  }: AttachmentLinkTargetConfig): ResourceReference | undefined {
    return entityResourceReference(
      ResourceType.ATTACHMENT,
      ref,
      this.modelReferenceSerializerProvider,
      { queryString },
    );
  }
}

export { AttachmentLinkTargetType };
export type { AttachmentLinkTargetConfig };
