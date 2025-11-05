import { Storage } from './storage';
/**
 * @since 0.1
 * @beta
 */
export interface WrappingStorage extends Storage {
    setStorage(storage: Storage): void;
    getStorage(): Storage;
    updatePageContent(page: string, syntax: string): Promise<boolean>;
}
