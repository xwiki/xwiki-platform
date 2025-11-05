import { WikiConfig } from '../api/WikiConfig';
import { CristalApp } from '../api/cristalApp';
import { Logger } from '../api/logger';
import { Storage } from '../api/storage';
/**
 * @since 0.16
 * @beta
 */
export type ConfigObjectType = {
    name: string;
    baseURL: string;
    baseRestURL: string;
    homePage: string;
    serverRendering: boolean;
    designSystem: string;
    offline: boolean;
    realtimeURL?: string;
    authenticationBaseURL?: string;
    authenticationManager?: string;
    storageRoot?: string;
    editor?: string;
};
/**
 * @since 0.1
 * @beta
 */
export declare class DefaultWikiConfig implements WikiConfig {
    name: string;
    baseURL: string;
    baseRestURL: string;
    /**
     * Realtime endpoint URL.
     * @since 0.11
     * @beta
     */
    realtimeURL?: string;
    /**
     * Authentication server base URL.
     * @since 0.15
     * @beta
     */
    authenticationBaseURL?: string;
    /**
     * Authentication Manager component to use.
     * By default, resolves to configuration type.
     * @since 0.16
     * @beta
     */
    authenticationManager?: string;
    homePage: string;
    storage: Storage;
    serverRendering: boolean;
    designSystem: string;
    editor: string;
    offline: boolean;
    offlineSetup: boolean;
    /**
     * Root location to store pages.
     * @since 0.16
     * @beta
     */
    storageRoot?: string;
    cristal: CristalApp;
    logger: Logger;
    constructor(logger: Logger);
    setConfig(name: string, baseURL: string, baseRestURL: string, homePage: string, serverRendering: boolean, designSystem: string, offline: boolean, editor: string, optional?: {
        realtimeURL?: string;
        authenticationBaseURL?: string;
        authenticationManager?: string;
        storageRoot?: string;
    }): void;
    setConfigFromObject(configObject: ConfigObjectType): void;
    setupOfflineStorage(): void;
    isSupported(format: string): boolean;
    initialize(): void;
    defaultPageName(): string;
    getType(): string;
    getNewPageDefaultName(): string;
}
