import { Status } from './status';
import { CollaborationInitializer } from './collaborationInitializer';
import { User } from './user';
import { Ref } from 'vue';
/**
 * The manager for a given collaboration provider (e.g., hocuspocus, or y-websocket).
 * @since 0.20
 * @beta
 */
interface CollaborationManager {
    /**
     * A lazy initializer for a collaboration initializer. We proceed that way to allow the collaboration provider to
     * be resolved and assigned outside the editor, but to be effectively initialized inside the editor, where
     * access to the internal editor structure is available.
     */
    get(): Promise<() => CollaborationInitializer>;
    /**
     * The current status.
     */
    status(): Ref<Status>;
    /**
     * The list of currently connected users.
     */
    users(): Ref<User[]>;
}
export type { CollaborationManager };
