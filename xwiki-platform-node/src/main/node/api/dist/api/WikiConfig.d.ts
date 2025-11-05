import { Storage } from './storage';
/**
 * @since 0.18
 * @beta
 */
interface WikiConfig {
    name: string;
    baseURL: string;
    baseRestURL: string;
    /**
     * Realtime endpoint URL.
     * Defaults to http://localhost:15681/collaboration when undefined.
     * @since 0.11
     * @beta
     */
    realtimeURL?: string;
    /**
     * Realtime provider hint. When undefined, the default hocuspocus provider is used.
     * @since 0.20
     * @beta
     */
    realtimeHint?: string;
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
    offline: boolean;
    /**
     * Root location to store pages.
     * @since 0.16
     * @beta
     */
    storageRoot?: string;
    /**
     * The (optional) type of the editor. The default is "blocknote".
     *
     * @since 0.16
     * @beta
     */
    editor?: string;
    setConfig(name: string, baseURL: string, baseRestURL: string, homePage: string, serverRendering: boolean, designSystem: string, offline: boolean, editor: string): void;
    setConfigFromObject(configObject: any): void;
    isSupported(format: string): boolean;
    initialize(): void;
    /**
     * The default page name for the current configuration.
     * For instance, "README" for Github, or "Main.WebHome" for XWiki.
     */
    defaultPageName(): string;
    /**
     * Returns the type of the WikiConfig implementation.
     *
     * @returns the type of the implementation
     * @since 0.9
     * @beta
     */
    getType(): string;
    /**
     * Returns the default name for a newly created page.
     *
     * @returns the default name
     * @since 0.10
     * @beta
     */
    getNewPageDefaultName(): string;
}
export type { WikiConfig };
