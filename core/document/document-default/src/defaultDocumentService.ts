import type { DocumentService } from "@xwiki/cristal-document-api";
import { Ref } from "vue";
import { inject, injectable } from "inversify";
import { defineStore, Store, StoreDefinition, storeToRefs } from "pinia";
import type { CristalApp, PageData } from "@xwiki/cristal-api";

type Id = "document";
type State = {
  lastDocumentReference: string | undefined;
  document: PageData | undefined;
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
   * @param documentReference the reference o the document to update
   * @param requeue true in case of offline refresh required
   */
  update(documentReference: string, requeue: boolean): Promise<void>;
};
type DocumentStoreDefinition = StoreDefinition<Id, State, Getters, Actions>;
type DocumentStore = Store<Id, State, Getters, Actions>;

function createStore(cristal: CristalApp): DocumentStoreDefinition {
  return defineStore<Id, State, Getters, Actions>("document", {
    state() {
      return {
        lastDocumentReference: undefined,
        document: undefined,
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
      async update(documentReference: string, requeue: boolean) {
        this.lastDocumentReference = documentReference;
        this.setLoading();
        try {
          const doc = await cristal.getPage(documentReference, {
            requeue,
          });
          // Only store the result if the current document reference is equal to
          // the most recently requested one.
          if (this.lastDocumentReference === documentReference) {
            this.document = doc;
            this.error = undefined;
            this.loading = false;
          }
        } catch (e) {
          if (this.lastDocumentReference === documentReference) {
            this.document = undefined;
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

  isLoading(): Ref<boolean> {
    return this.refs.loading;
  }

  getError(): Ref<Error | undefined> {
    return this.refs.error;
  }

  setCurrentDocument(documentReference: string): void {
    // this.store.setLoading();
    this.store.update(documentReference, true);
  }

  refreshCurrentDocument(): void {
    const documentReference = this.store.lastDocumentReference;
    if (documentReference) {
      // this.store.setLoading();
      this.store.update(documentReference, false);
    }
  }
}
