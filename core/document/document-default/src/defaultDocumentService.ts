import { inject, injectable } from "inversify";
import { Store, StoreDefinition, defineStore, storeToRefs } from "pinia";
import { Ref } from "vue";
import type { CristalApp, PageData } from "@xwiki/cristal-api";
import type { DocumentService } from "@xwiki/cristal-document-api";

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
      },
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
    // this.store.setLoading();
    this.store.update(documentReference, true, revision);
  }

  refreshCurrentDocument(): void {
    const documentReference = this.store.lastDocumentReference;
    if (documentReference) {
      // this.store.setLoading();
      this.store.update(documentReference, false, this.store.lastRevision);
    }
  }
}
