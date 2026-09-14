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

import type {
  EntityReference,
  EntityTypeApi,
  ModelApi,
} from "@xwiki/platform-xwiki-model-api";

/**
 * A document from the global XWiki object, able to compute its own URLs. It is the legacy counterpart of the
 * `XWikiDocument` class from `@xwiki/platform-document-xwiki`, which forwards its URL computation to it.
 *
 * @since 18.8.0RC1
 * @beta
 */
interface LegacyXWikiDocument {
  /**
   * @param action - the action to perform on this document, e.g. `view` or `edit`
   * @param parameters - the query string parameters to add to the URL
   * @param fragment - the fragment identifier to add to the URL, without the leading `#`
   * @returns the URL that performs the given action on this document
   */
  getURL(
    action?: string,
    parameters?: URLSearchParams,
    fragment?: string,
  ): string;

  /**
   * @param entity - the path of the sub-resource to target, relative to this document, e.g. `objects` or
   *   `translations/fr`
   * @param parameters - the query string parameters to add to the URL
   * @returns the URL of this document, or of one of its sub-resources, in the XWiki REST API
   */
  getRestURL(entity?: string, parameters?: URLSearchParams): string;
}

/**
 * An attachment from the global XWiki object, able to compute its own URL.
 *
 * @since 18.8.0RC1
 * @beta
 */
interface LegacyXWikiAttachment {
  getURL(): string;
}

/**
 * The global `XWiki` object exposed by the XWiki web pages. Its entity reference API comes from
 * `@xwiki/platform-xwiki-model-api`, which the web WebJar assigns onto the global object on page load; the rest is
 * contributed by the legacy `xwiki.js` script.
 *
 * Declare the global in the modules that need it with:
 *
 * ```ts
 * declare global {
 *   const XWiki: import("@xwiki/platform-xwiki-utils").XWikiGlobal;
 * }
 * export {};
 * ```
 *
 * @since 18.8.0RC1
 * @beta
 */
interface XWikiGlobal {
  EntityReference: new (
    name: string,
    type: number,
    parent?: EntityReference | null,
    locale?: string,
  ) => EntityReference;

  EntityType: EntityTypeApi;

  Model: ModelApi;

  Document: new (reference: EntityReference) => LegacyXWikiDocument;

  Attachment: new (reference: EntityReference) => LegacyXWikiAttachment;

  /**
   * The document being displayed by the current page.
   */
  currentDocument: LegacyXWikiDocument & { documentReference: EntityReference };

  /**
   * The name of the wiki the current page belongs to.
   */
  currentWiki: string;

  /**
   * The path the XWiki instance is served from, e.g. `/xwiki`.
   */
  contextPath: string;

  /**
   * The syntax identifier of the current document, e.g. `xwiki/2.1`.
   */
  docsyntax: string;
}

export type { LegacyXWikiAttachment, LegacyXWikiDocument, XWikiGlobal };
