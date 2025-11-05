import { CollaborationManager } from './collaborationManager';
/**
 * Dynamically resolves a CollaborationManager based on the configuration.
 *
 * @since 0.20
 * @beta
 */
export interface CollaborationManagerProvider {
    /**
     * @returns the resolved collaboration manager
     */
    get(): CollaborationManager;
}
