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

import type { PageData } from "@xwiki/cristal-api";
import type { DocumentReference } from "@xwiki/cristal-model-api";
import type { Ref } from "vue";

/**
 * @since 0.12
 */
type DocumentChange = "update" | "delete";

/**
 * Provide the operation to access a document.
 *
 * @since 0.11
 */
interface DocumentService {
  /**
   * @returns the reference to the current document, the current document changes when setCurrentDocument is called
   */
  getCurrentDocument(): Ref<PageData | undefined>;

  /**
   * Returns a reference the document reference for the current document.
   *
   * @since 0.13
   */
  getCurrentDocumentReference(): Ref<DocumentReference | undefined>;

  /**
   * Returns a serialized string of {@link getCurrentDocumentReference}.
   *
   * @since 0.13
   */
  getCurrentDocumentReferenceString(): Ref<string | undefined>;

  /**
   * @returns the revision of the current document, or undefined if it's the last one
   * @since 0.12
   */
  getCurrentDocumentRevision(): Ref<string | undefined>;

  /**
   * @returns a ref to the loading state. true when the page is loading, false otherwise
   */
  isLoading(): Ref<boolean>;

  /**
   * @returns a ref to the error for the loading of the current document. undefined if no error happened
   */
  getError(): Ref<Error | undefined>;

  /**
   * Update the reference of the latest document.
   * @param documentReference - the current document reference
   * @param revision - the revision of the document, undefined for latest
   */
  setCurrentDocument(documentReference: string, revision?: string): void;

  /**
   * Force reloading the content of the document without changing the current document reference
   */
  refreshCurrentDocument(): void;

  /**
   * Register a change listener that will be executed on any document change
   * made on the whole Cristal instance.
   * @param change - the kind of change
   * @param listener - the listener to register
   * @since 0.12
   */
  registerDocumentChangeListener(
    change: DocumentChange,
    listener: (page: PageData) => Promise<void>,
  ): void;

  /**
   * Notify that a document change happened. This will execute all registered
   * listeners for the given kind of change.
   * @param change - the kind of change
   * @param page - the document changed
   * @since 0.12
   */
  notifyDocumentChange(change: DocumentChange, page: PageData): Promise<void>;
}

const name: string = "DocumentService";

export { type DocumentChange, type DocumentService, name };
