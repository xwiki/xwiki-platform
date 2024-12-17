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

import { inject, injectable } from "inversify";
import { Store, StoreDefinition, defineStore, storeToRefs } from "pinia";
import { Ref } from "vue";
import type { CristalApp, PageData } from "@xwiki/cristal-api";
import type {
  DocumentChange,
  DocumentService,
} from "@xwiki/cristal-document-api";

type Id = "document";
type State = {
  lastDocumentReference: string | undefined;
  lastRevision: string | undefined;
  document: PageData | undefined;
  revision: string | undefined;
  loading: boolean;
  error: Error | undefined;
};
/**
 * Take a given type "Type" and wraps each of its fields in a readonly Ref.
 */
type WrappedRefs<Type> = {
  readonly [Property in keyof Type]: Ref<Type[Property]>;
};
type StateRefs = WrappedRefs<State>;
type Getters = Record<string, never>;
type Actions = {
  /**
   * Switch the loading state to true;
   */
  setLoading(): void;

  /**
   * Update the page data for the provided document reference
   * @param documentReference - the reference of the document to update
   * @param requeue - true in case of offline refresh required
   * @param revision - the revision of the document, undefined for latest
   */
  update(
    documentReference: string,
    requeue: boolean,
    revision?: string,
  ): Promise<void>;
};
type DocumentStoreDefinition = StoreDefinition<Id, State, Getters, Actions>;
type DocumentStore = Store<Id, State, Getters, Actions>;

function createStore(cristal: CristalApp): DocumentStoreDefinition {
  return defineStore<Id, State, Getters, Actions>("document", {
    state() {
      return {
        lastDocumentReference: undefined,
        lastRevision: undefined,
        document: undefined,
        revision: undefined,
        loading: false,
        error: undefined,
      };
    },
    actions: {
      // Loading must be in its own separate action because action changes are
      // only applied to the store at the end of an action.
      setLoading() {
        this.loading = true;
        this.document = undefined;
        this.error = undefined;
      },
      // TODO: reduce the number of statements in the following method and reactivate the disabled eslint rule.
      // eslint-disable-next-line max-statements
      async update(
        documentReference: string,
        requeue: boolean,
        revision?: string,
      ) {
        this.lastDocumentReference = documentReference;
        this.setLoading();
        try {
          const doc = await cristal.getPage(documentReference, {
            requeue,
            revision,
          });
          // Only store the result if the current document reference is equal to
          // the most recently requested one.
          if (this.lastDocumentReference === documentReference) {
            this.document = doc;
            // We clean up the revision before storing if it was empty.
            this.revision = revision === "" ? undefined : revision;
            this.error = undefined;
            this.loading = false;
          }
        } catch (e) {
          if (this.lastDocumentReference === documentReference) {
            this.document = undefined;
            this.revision = undefined;
            this.loading = false;
            if (e instanceof Error) {
              this.error = e;
            }
          }
        }
      },
    },
  });
}

@injectable()
export class DefaultDocumentService implements DocumentService {
  private readonly refs: StateRefs;
  private readonly store: DocumentStore;
  private readonly documentChangeListeners: Map<
    DocumentChange,
    Array<(page: PageData) => Promise<void>>
  > = new Map();

  constructor(@inject("CristalApp") private cristal: CristalApp) {
    this.store = createStore(this.cristal)();
    this.refs = storeToRefs(this.store);
  }

  getCurrentDocument(): Ref<PageData | undefined> {
    return this.refs.document;
  }

  getCurrentDocumentRevision(): Ref<string | undefined> {
    return this.refs.revision;
  }

  isLoading(): Ref<boolean> {
    return this.refs.loading;
  }

  getError(): Ref<Error | undefined> {
    return this.refs.error;
  }

  setCurrentDocument(documentReference: string, revision?: string): void {
    this.store.update(documentReference, true, revision);
  }

  refreshCurrentDocument(): void {
    const documentReference = this.store.lastDocumentReference;
    if (documentReference) {
      this.store.update(documentReference, false, this.store.lastRevision);
    }
  }

  registerDocumentChangeListener(
    change: DocumentChange,
    listener: (page: PageData) => Promise<void>,
  ): void {
    if (this.documentChangeListeners.has(change)) {
      this.documentChangeListeners.get(change)!.push(listener);
    } else {
      this.documentChangeListeners.set(change, [listener]);
    }
  }

  async notifyDocumentChange(
    change: DocumentChange,
    page: PageData,
  ): Promise<void> {
    if (this.documentChangeListeners.has(change)) {
      await Promise.all(
        this.documentChangeListeners.get(change)!.map((l) => l(page)),
      );
    }
  }
}
