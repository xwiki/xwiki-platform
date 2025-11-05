import { Storage } from '@xwiki/cristal-api';
/**
 * Resolves a {@link Storage} based on the current configuration.
 * @since 0.13
 * @beta
 */
export interface StorageProvider {
    /**
     * @returns a {@link Storage} based on the current configuration
     */
    get(): Storage;
}
