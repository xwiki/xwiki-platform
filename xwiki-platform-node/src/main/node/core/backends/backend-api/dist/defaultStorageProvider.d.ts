import { StorageProvider } from './storageProvider';
import { CristalApp, Storage } from '@xwiki/platform-api';
/**
 * Provide the current storage.
 * @since 0.13
 * @beta
 */
declare class DefaultStorageProvider implements StorageProvider {
    private readonly cristalApp;
    constructor(cristalApp: CristalApp);
    get(): Storage;
}
export { DefaultStorageProvider };
