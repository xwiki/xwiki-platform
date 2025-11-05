import { AttachmentsData, Logger, PageAttachment, PageData, Storage, WikiConfig } from '@xwiki/cristal-api';
/**
 * @since 1.0
 * @beta
 */
export declare abstract class AbstractStorage implements Storage {
    protected logger: Logger;
    protected wikiConfig: WikiConfig;
    constructor(logger: Logger, module: string);
    setWikiConfig(wikiConfig: WikiConfig): void;
    getWikiConfig(): WikiConfig;
    abstract getEditField(jsonArticle: object, fieldName: string): Promise<string>;
    abstract getImageURL(page: string, image: string): string;
    abstract getPageContent(page: string, syntax: string, revision?: string): Promise<PageData | undefined>;
    /**
     * Returns the list of attachments of a given page.
     * TODO: this API is missing pagination.
     * @since 0.9
     * @beta
     */
    abstract getAttachments(page: string): Promise<AttachmentsData | undefined>;
    /**
     * @since 0.12
     * @beta
     */
    abstract getAttachment(page: string, name: string): Promise<PageAttachment | undefined>;
    abstract getPageFromViewURL(url: string): string | null;
    abstract getPageRestURL(page: string, syntax: string, revision?: string): string;
    abstract getPanelContent(panel: string, contextPage: string, syntax: string): Promise<PageData>;
    abstract isStorageReady(): Promise<boolean>;
    /**
     * Save a page and its content to the give syntax.
     *
     * @param page - the page to save
     * @param title - the raw page title
     * @param content - the content of the page
     * @param syntax - the syntax of the content
     *
     * @since 0.8
     * @beta
     */
    abstract save(page: string, title: string, content: string, syntax: string): Promise<unknown>;
    /**
     * @param page - the serialized reference of the page
     * @param files - the list of files to upload
     * @returns (since 0.20) an optional list of resolved attachments URL (in the same order as the provided files). This
     *   is useful in the case where the url cannot be resolved from the name of the file and its document reference
     *   alone.
     * @since 0.9
     * @beta
     */
    abstract saveAttachments(page: string, files: File[]): Promise<undefined | (string | undefined)[]>;
    /**
     * Delete a page.
     *
     * @param page - the page to delete
     * @returns true if the delete was successful, false with the reason otherwise
     *
     * @since 0.11
     * @beta
     */
    abstract delete(page: string): Promise<{
        success: boolean;
        error?: string;
    }>;
    /**
     * Move a page.
     *
     * @param page - the page to move
     * @param newPage - the new location for the page
     * @param preserveChildren - whether to move children
     * @returns true if the move was successful, false with the reason otherwise
     *
     * @since 0.14
     * @beta
     */
    abstract move(page: string, newPage: string, preserveChildren: boolean): Promise<{
        success: boolean;
        error?: string;
    }>;
}
