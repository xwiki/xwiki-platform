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
import type {
  LegacyXWikiDocument,
  XWikiMeta,
} from "@xwiki/platform-xwiki-utils";

/**
 * The fields of an XWiki document. Only the ones this API relies on are listed: the others, such as the ones returned
 * by the REST API, are merged as they are by {@link XWikiDocument.update}.
 *
 * @since 18.8.0RC1
 * @beta
 */
type XWikiDocumentData = {
  /**
   * The reference of the wiki page this document belongs to. Note that it doesn't identify the translation.
   */
  documentReference?: EntityReference;

  /**
   * This document's raw locale. It is empty for the original translation, whose actual locale is given by
   * {@link XWikiDocument.realLocale}.
   */
  language?: string;

  /**
   * The locale of each of this document's translations, indexed by translation name. Only the `default` entry, holding
   * the locale of the original translation, is used by this API.
   */
  translations?: Record<string, string>;

  version?: string;

  /**
   * The date of the last modification, as a number of milliseconds since the epoch.
   */
  modified?: number;

  isNew?: boolean;

  content?: string;

  [field: string]: unknown;
};

/**
 * An entry from an XWiki document's history, as returned by the REST API.
 *
 * @since 18.8.0RC1
 * @beta
 */
type XWikiDocumentRevision = {
  version: string;
  modified: number;
  author: string;
  authorName: string;
  [field: string]: unknown;
};

/**
 * The error thrown when an XWiki REST API request fails, carrying the response status so that the caller can tell a
 * missing document from an actual failure.
 */
class RestError extends Error {
  public readonly status: number;

  public constructor(url: string, status: number) {
    super(`Failed to fetch [${url}]. Response status: ${status}`);
    this.status = status;
  }
}

/**
 * @returns the legacy API of the given document, which is the one computing the URLs
 */
function getLegacyAPI(xwikiDocument: XWikiDocument): LegacyXWikiDocument {
  return (
    (xwikiDocument.documentReference &&
      new XWiki.Document(xwikiDocument.documentReference)) ||
    XWiki.currentDocument
  );
}

function removeNullProperties(
  object: Record<string, unknown>,
): Record<string, unknown> {
  return Object.fromEntries(
    Object.entries(object).filter(([, value]) => value != null),
  );
}

/**
 * Update the value of a hidden field of the edit form, when that field is present.
 */
function setFieldValue(id: string, value: unknown): void {
  const field = document.getElementById(id) as HTMLInputElement | null;
  if (field) {
    field.value = String(value);
  }
}

/**
 * Generic client-side API for an XWiki document. Nothing specific to a given editor should go here.
 *
 * @since 18.8.0RC1
 * @beta
 */
class XWikiDocument {
  [field: string]: unknown;

  public documentReference?: EntityReference;

  public language?: string;

  public translations?: Record<string, string>;

  public version?: string;

  public modified?: number;

  public isNew?: boolean;

  public content?: string;

  /**
   * The metadata of the document displayed by the current page, when this document was created from it. It is left
   * unset when this document targets another one, in which case the page state is not this document's to update.
   */
  protected meta?: XWikiMeta;

  /**
   * @param meta - the metadata exposed by the `xwiki-meta` RequireJS module, which the caller is responsible for
   *   loading
   * @returns the document currently displayed by the web page, with the fields exposed by the meta information
   */
  public static currentDocument<T extends XWikiDocument>(
    this: new (data?: XWikiDocumentData) => T,
    meta: XWikiMeta,
  ): T {
    // Instantiate "this" rather than XWikiDocument, so that a class extending it gets its own type out of this
    // inherited factory. Note that a subclass overriding this factory must not call it through "super", because the
    // Closure Compiler, which minifies the code of the WebJars, compiles a super call made from a static method into
    // a plain call on the parent class, dropping the "this" binding this relies on.
    const currentDocument = new this({
      documentReference: meta.documentReference,
      language: meta.locale,
      version: meta.version,
      isNew: meta.isNew,
    });
    if (!currentDocument.language) {
      // We know this is the original document translation, whose raw locale is empty, so we take its actual (real)
      // locale from the meta information.
      currentDocument.translations = { default: meta.realLocale };
    }
    currentDocument.meta = meta;
    return currentDocument;
  }

  /**
   * @param data - the fields to initialize this document with
   */
  public constructor(data?: XWikiDocumentData) {
    Object.assign(this, data);
  }

  /**
   * Fetch this document's fields from the REST API. A document that doesn't exist anymore, e.g. because it was
   * deleted, is reset to a new document. Any other failure leaves the current fields untouched.
   *
   * @returns this document
   */
  public async reload(): Promise<this> {
    try {
      const updatedDocument = await this.getJSON<Record<string, unknown>>(
        this.getRestURL(),
      );
      return this.update({
        // The REST API response includes some properties with null values, that would otherwise overwrite the
        // properties of this document that have a value set.
        ...removeNullProperties(updatedDocument),
        // We were able to load the document so it's not new.
        isNew: false,
      });
    } catch (error) {
      if (error instanceof RestError && error.status === 404) {
        // The document doesn't exist anymore. Maybe it was deleted?
        return this.update({
          version: "1.1",
          modified: 0,
          content: "",
          isNew: true,
        });
      }
      // Otherwise the reload failed and we continue using the current data.
      return this.update();
    }
  }

  /**
   * @param data - the fields to merge into this document
   * @returns this document
   */
  public update(data?: XWikiDocumentData): this {
    Object.assign(this, data);
    this.syncCurrentDocumentState();
    return this;
  }

  /**
   * @returns whether this document is the one currently displayed by the web page
   */
  public isCurrentDocument(): boolean {
    return !!this.documentReference?.equals(this.meta?.documentReference);
  }

  /**
   * Keep the meta and the hidden fields used by the edit form in sync, in order to ensure a proper merge on save.
   */
  public syncCurrentDocumentState(): void {
    if (
      this.meta &&
      this.isCurrentDocument() &&
      this.version !== this.meta.version
    ) {
      this.meta.setVersion(this.version!);
      setFieldValue("editingVersionDate", this.modified);
      setFieldValue("isNew", this.isNew);
    }
  }

  /**
   * This document's real locale. It differs from its (raw) locale only for the original translation, whose raw locale
   * is empty.
   */
  public get realLocale(): string | undefined {
    const locale = this.language;
    if (typeof locale !== "string" || locale === "") {
      return this.defaultLocale;
    }
    return locale;
  }

  /**
   * The locale of this document's original translation. Note that it is the empty string for a technical document,
   * whose default locale is the root locale.
   */
  public get defaultLocale(): string | undefined {
    return this.translations?.["default"];
  }

  /**
   * @param action - the action to perform on this document, e.g. `view` or `edit`
   * @param parameters - the query string parameters to add to the URL
   * @param fragment - the fragment identifier to add to the URL, without the leading `#`
   * @returns the URL that performs the given action on this document
   */
  public getURL(
    action?: string,
    parameters?: URLSearchParams,
    fragment?: string,
  ): string {
    return getLegacyAPI(this).getURL(action, parameters, fragment);
  }

  /**
   * @param entity - the path of the sub-resource to target, relative to the wiki page
   * @param parameters - the query string parameters to add to the URL
   * @returns the REST URL of the wiki page this document belongs to, without taking its translation into account
   */
  public getPageRestURL(entity?: string, parameters?: URLSearchParams): string {
    return getLegacyAPI(this).getRestURL(entity, parameters);
  }

  /**
   * @param entity - the path of the sub-resource to target, relative to this document
   * @param parameters - the query string parameters to add to the URL
   * @returns the REST URL of this document. Note that a document translation is exposed through a different REST URL
   *   than the original translation.
   */
  public getRestURL(entity?: string, parameters?: URLSearchParams): string {
    const translationEntity =
      this.language && `translations/${encodeURIComponent(this.language)}`;
    return this.getPageRestURL(
      [translationEntity, entity].filter((segment) => segment).join("/"),
      parameters,
    );
  }

  /**
   * @param version - the version to look for, e.g. `2.1`
   * @returns the entry this document's history holds for the given version
   */
  public getRevision(version: string): Promise<XWikiDocumentRevision> {
    return this.getJSON<XWikiDocumentRevision>(
      this.getRestURL(
        `history/${encodeURIComponent(version)}`,
        new URLSearchParams({ prettyNames: "true" }),
      ),
    );
  }

  /**
   * Fetch JSON from the given URL. The error thrown when the request fails carries the response status.
   *
   * @param url - the URL to fetch
   * @returns the parsed response body
   */
  protected async getJSON<T>(url: string): Promise<T> {
    const response = await fetch(url, {
      // The XWiki REST API doesn't specify how its responses should be cached, so we ask for a fresh one.
      cache: "no-store",
      headers: {
        // Without this the XWiki REST API answers with XML.
        Accept: "application/json",
        // Some server side code answers differently when the request is made from JavaScript.
        "X-Requested-With": "XMLHttpRequest",
      },
    });
    if (!response.ok) {
      throw new RestError(url, response.status);
    }
    return response.json();
  }

  /**
   * @param id - the identifier of a hidden field of the edit form
   * @returns the value of that field, when it is present
   */
  protected static getFieldValue(id: string): string | undefined {
    return (document.getElementById(id) as HTMLInputElement | null)?.value;
  }
}

export { XWikiDocument };
export type { XWikiDocumentData, XWikiDocumentRevision };
