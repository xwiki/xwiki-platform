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

import type { EntityReference } from "@xwiki/platform-xwiki-model-api";

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
 * The save button behaviour of the legacy `actionbuttons.js` script, which the editors override in order to stay on
 * the page after a save.
 *
 * @since 18.8.0RC1
 * @beta
 */
interface LegacyAjaxSaveAndContinue {
  /**
   * Called after a save, to load the saved content back into the editor. The merge of concurrent changes happens
   * server side, so when a save ran into a merge conflict that was resolved automatically the editor has no way of
   * knowing the merged content and has to fetch it back.
   */
  reloadEditor(): void;

  /**
   * Called after a successful save, to leave the edit mode when the user asked for it.
   *
   * @param continueEditing - whether the user asked to stay in the edit mode
   * @returns whether the redirect was handled
   */
  maybeRedirect(continueEditing: boolean): boolean;
}

/**
 * The global `XWiki` object exposed by the XWiki web pages, contributed by the legacy `xwiki.js` and
 * `actionbuttons.js` scripts.
 *
 * The entity reference API that the web WebJar also assigns onto the global object is deliberately left out: import
 * it from `@xwiki/platform-xwiki-model-api` instead, which is the same code, without the load order dependency and
 * without needing a global to be stubbed in the tests.
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

  /**
   * The behaviour of the save buttons. Available only in edit mode.
   */
  actionButtons?: {
    AjaxSaveAndContinue: { prototype: LegacyAjaxSaveAndContinue };
  };
}

export type {
  LegacyAjaxSaveAndContinue,
  LegacyXWikiAttachment,
  LegacyXWikiDocument,
  XWikiGlobal,
};
