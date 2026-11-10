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
import type { DocumentReference } from "@xwiki/platform-model-api";
import type {
  ModelReferenceHandlerProvider,
  ModelReferenceParserProvider,
  ModelReferenceSerializerProvider,
} from "@xwiki/platform-model-reference-api";
import type {
  RemoteURLParserProvider,
  RemoteURLSerializerProvider,
} from "@xwiki/platform-model-remote-url-api";
import type { ResourceReference } from "@xwiki/platform-rendering-api";

/**
 * Configuration for a page link
 *
 * @since 18.10.0RC1
 * @beta
 */
type PageLinkTargetConfig = {
  ref: DocumentReference | null;

  /**
   * The query string of the link, without the "?" URL separator (e.g. "key1=value1&key2=value2")
   */
  queryString?: string;

  /**
   * The identifier of the element of the page targeted by the link, without the "#" URL separator
   */
  anchor?: string;

  /**
   * The title of the page, when it's known (e.g. from the link suggestion that the user picked), used as the default
   * label of the link. It's not part of the link itself: it's left out when the link is converted into a URL or a
   * reference, and unset when the link is read from one.
   */
  title?: string;
};

/**
 * Built-in link target type for links to an internal wiki page.
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
    @inject("ModelReferenceHandlerProvider")
    private readonly modelReferenceHandlerProvider: ModelReferenceHandlerProvider,
  ) {}

  getLabel(locale: string): Promise<string> {
    return translateLabel("link-type.target-types.page.label", locale);
  }

  createDefaultConfig(): PageLinkTargetConfig {
    return { ref: null };
  }

  fromURL(url: string): PageLinkTargetConfig | null {
    if (url.trim() === "") {
      return { ref: null };
    }

    const ref = tryFallible(() =>
      this.remoteURLParserProvider.get()?.parse(url),
    );

    if (ref?.type !== EntityType.DOCUMENT) {
      return null;
    }

    return { ref, ...linkURLPartsFromURL(url) };
  }

  toURL({ ref, queryString, anchor }: PageLinkTargetConfig): string {
    const base = this.remoteURLSerializerProvider
      .get()
      ?.serialize(ref ?? undefined);

    return `${base ?? ""}${linkURLSuffix({ queryString, anchor })}`;
  }

  /**
   * @returns the title of the page when it's known, its name otherwise, as the backend displays it (e.g. the name of
   *   the parent space for the home page of a nested page in XWiki, rather than "WebHome")
   */
  getDefaultLabel({ ref, title }: PageLinkTargetConfig): string {
    const knownTitle = title?.trim();
    if (knownTitle) {
      return knownTitle;
    }
    return ref === null
      ? ""
      : (this.modelReferenceHandlerProvider.get()?.getTitle(ref) ?? "");
  }

  fromReference(reference: ResourceReference): PageLinkTargetConfig | null {
    if (reference.type !== ResourceType.DOCUMENT) {
      return null;
    }

    const ref = parseEntityReference(
      reference.reference,
      EntityType.DOCUMENT,
      this.modelReferenceParserProvider,
    );

    return ref?.type === EntityType.DOCUMENT
      ? { ref, ...linkURLPartsFromReference(reference) }
      : null;
  }

  toReference({
    ref,
    queryString,
    anchor,
  }: PageLinkTargetConfig): ResourceReference | undefined {
    return entityResourceReference(
      ResourceType.DOCUMENT,
      ref,
      this.modelReferenceSerializerProvider,
      { queryString, anchor },
    );
  }
}

export { PageLinkTargetType };
export type { PageLinkTargetConfig };
