import { PageData } from './PageData';
import { WikiConfig } from './WikiConfig';
import { Logger } from './logger';
import { LoggerConfig } from './loggerConfig';
import { SkinManager } from './skinManager';
import { Configurations } from '@xwiki/cristal-configuration-api';
import { Container } from 'inversify';
import { App, Component, Ref } from 'vue';
import { Router } from 'vue-router';
/**
 * @since 0.1
 * @beta
 */
export interface CristalApp {
    getApp(): App;
    getRouter(): Router;
    getContainer(): Container;
    setContainer(container: Container): void;
    getWikiConfig(): WikiConfig;
    setWikiConfig(wikiConfig: WikiConfig): void;
    getSkinManager(): SkinManager;
    switchConfig(configName: string): void;
    setAvailableConfigurations(config: Configurations): void;
    getAvailableConfigurations(): Map<string, WikiConfig>;
    /**
     * Delete a configuration from the set of available configurations.
     *
     * @param configName - the name of the configuration to delete
     * @since 0.18
     * @beta
     */
    deleteAvailableConfiguration(configName: string): void;
    run(): Promise<void>;
    /**
     * The method existed before 0.15, but wasn't allowing for components to be
     * resolved asynchronously.
     * This is useful to avoid loading all the components of a UIX during the first page load, and instead wait for the
     * extension point to be actually required first.
     *
     * @param extensionPoint - id of the extension point to resolve
     * @since 0.15
     * @beta
     */
    getUIXTemplates(extensionPoint: string): Promise<Array<Component>>;
    getMenuEntries(): Array<string>;
    getCurrentPage(): string;
    getCurrentContent(): string;
    getCurrentSource(): string;
    /**
     * Return the syntax of the current page.
     * @since 0.7
     * @beta
     */
    getCurrentSyntax(): string;
    /** @since 0.18
     * @beta
     */
    setCurrentPage(page: string, mode?: string): Promise<void>;
    /**
     * @deprecated use the document-api instead
     * @param ref - the reactive reference holding the reference to a page data.
     */
    setContentRef(ref: Ref): void;
    /**
     * @deprecated since 0.12, use ClickListener instead
     */
    loadPageFromURL(url: string): Promise<void>;
    loadPage(action?: string, options?: {
        requeue: boolean;
    }): Promise<void>;
    /**
     * Return the requested page
     * @param page - a page identifier (e.g., a document reference for the XWiki
     *  backend, or a filename for the filesystem backend)
     * @param revision - the revision requested, undefined will default to latest
     * @returns the page data, or undefined if the page is not found
     *
     * @since 0.7
     * @beta
     */
    getPage(page: string, options?: {
        requeue?: boolean;
        revision?: string;
    }): Promise<PageData | undefined>;
    getLogger(module: string): Logger;
    getLoggerConfig(): LoggerConfig;
}
