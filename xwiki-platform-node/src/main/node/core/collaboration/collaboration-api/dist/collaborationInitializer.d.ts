import { Doc } from 'yjs';
/**
 * Holds properties of an collaboration. It's provider, the document held by the provider, and a provide resolved on
 * the initialized is ready.
 * @since 0.20
 * @beta
 */
type CollaborationInitializer = {
    /**
     * The provider, can be of arbitrary type.
     */
    provider: any;
    /**
     * The yjs document held by the provider.
     */
    doc: Doc;
    /**
     * The promise must be resolved once the provider is connected and ready.
     */
    initialized: Promise<unknown>;
};
export { type CollaborationInitializer };
