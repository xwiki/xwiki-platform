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
import { labelFromTranslations } from "./labels";
import {
  entityResourceReference,
  parseEntityReference,
} from "./referenceHelpers";
import { tryFallible } from "@xwiki/platform-fn-utils";
import { EntityType } from "@xwiki/platform-model-api";
import { ResourceType } from "@xwiki/platform-rendering-api";
import { inject, injectable } from "inversify";
import type { PageLinkTargetConfig } from "../data/linkType";
import type { LinkTargetType } from "@xwiki/platform-link-type-api";
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
 * Built-in link target type for links to an internal wiki page.
 *
 * @since 18.10.0RC1
 * @beta
 */
@injectable()
class PageLinkTargetType implements LinkTargetType<PageLinkTargetConfig> {
  readonly type = "page";
  readonly order = 0;

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

  getLabel = labelFromTranslations("link-type.target-types.page.label");

  createDefaultConfig(): PageLinkTargetConfig {
    return { ref: null };
  }

  tryParseURL(url: string): PageLinkTargetConfig | null {
    if (url.trim() === "") {
      return { ref: null };
    }

    const ref = tryFallible(() =>
      this.remoteURLParserProvider.get()?.parse(url),
    );

    if (ref?.type !== EntityType.DOCUMENT) {
      return null;
    }

    const parsedURL = tryFallible(() => new URL(url));

    return {
      ref,
      anchor: parsedURL?.hash,
      queryString: parsedURL?.search,
    };
  }

  serializeURL({ ref, queryString, anchor }: PageLinkTargetConfig): string {
    const base = this.remoteURLSerializerProvider
      .get()
      ?.serialize(ref ?? undefined);

    return `${base ?? ""}${queryString ?? ""}${anchor ?? ""}`;
  }

  tryParseReference(reference: ResourceReference): PageLinkTargetConfig | null {
    if (reference.type !== ResourceType.DOCUMENT) {
      return null;
    }

    const ref = parseEntityReference(
      reference.reference,
      EntityType.DOCUMENT,
      this.modelReferenceParserProvider,
    );

    return ref?.type === EntityType.DOCUMENT ? { ref } : null;
  }

  configToReference({
    ref,
  }: PageLinkTargetConfig): ResourceReference | undefined {
    return entityResourceReference(
      ResourceType.DOCUMENT,
      ref,
      this.modelReferenceSerializerProvider,
    );
  }
}

export { PageLinkTargetType };
