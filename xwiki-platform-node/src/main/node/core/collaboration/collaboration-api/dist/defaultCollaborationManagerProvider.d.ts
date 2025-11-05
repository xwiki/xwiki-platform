import { CollaborationManager } from './collaborationManager';
import { CollaborationManagerProvider } from './collaborationManagerProvider';
import { CristalApp } from '@xwiki/cristal-api';
/**
 * @since 0.20
 * @beta
 */
export declare class DefaultCollaborationManagerProvider implements CollaborationManagerProvider {
    private readonly cristalApp;
    constructor(cristalApp: CristalApp);
    get(): CollaborationManager;
}
