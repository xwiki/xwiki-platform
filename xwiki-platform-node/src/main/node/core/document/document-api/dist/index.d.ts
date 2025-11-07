import { PageData } from '@xwiki/platform-api';
import { DocumentReference } from '@xwiki/platform-model-api';
import { Ref } from 'vue';
/**
 * @since 0.12
 * @beta
 */
type DocumentChange = "update" | "delete";
/**
 * Provide the operation to access a document.
 *
 * @since 0.11
 * @beta
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
     * @beta
     */
    getCurrentDocumentReference(): Ref<DocumentReference | undefined>;
    /**
     * Returns a serialized string of {@link getCurrentDocumentReference}.
     *
     * @since 0.13
     * @beta
     */
    getCurrentDocumentReferenceString(): Ref<string | undefined>;
    /**
     * @returns the revision of the current document, or undefined if it's the last one
     * @since 0.12
     * @beta
     */
    getCurrentDocumentRevision(): Ref<string | undefined>;
    /**
     * @returns the current document action
     * @since 0.23
     */
    getCurrentDocumentAction(): Ref<string | undefined>;
    /**
     * Return the document title, either with the defined title (from {@link getTitle}), or by using the name of the
     * document reference.
     *
     * @since 0.14
     * @beta
     */
    getDisplayTitle(): Ref<string>;
    /**
     * Return the defined document title, or undefined if no title is defined. To get a never empty title to display,
     * see {@link getDisplayTitle}.
     *
     * @since 0.14
     * @beta
     */
    getTitle(): Ref<string | undefined>;
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
     * @param action - the current document action (default: "view")
     * @param revision - the revision of the document, undefined for latest
     *
     * @since 0.23
     * @beta
     */
    setCurrentDocument(documentReference: string, action?: string, revision?: string): Promise<void>;
    /**
     * Force reloading the content of the document without changing the current document reference
     *
     * @since 0.18
     * @beta
     */
    refreshCurrentDocument(): Promise<void>;
    /**
     * Register a change listener that will be executed on any document change
     * made on the whole Cristal instance.
     * @param change - the kind of change
     * @param listener - the listener to register
     * @since 0.15
     * @beta
     */
    registerDocumentChangeListener(change: DocumentChange, listener: (page: DocumentReference) => Promise<void>): void;
    /**
     * Notify that a document change happened. This will execute all registered
     * listeners for the given kind of change.
     * @param change - the kind of change
     * @param page - the reference to the changed document
     * @since 0.15
     * @beta
     */
    notifyDocumentChange(change: DocumentChange, page: DocumentReference): Promise<void>;
}
/**
 * @beta
 */
declare const name: string;
export { type DocumentChange, type DocumentService, name };
