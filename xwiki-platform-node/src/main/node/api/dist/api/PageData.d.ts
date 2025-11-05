import { Document } from './document';
import { UserDetails } from '@xwiki/cristal-authentication-api';
/**
 * @since 0.1
 * @beta
 */
export interface PageData {
    id: string;
    name: string;
    source: string;
    syntax: string;
    html: string;
    headline: string;
    headlineRaw: string;
    document: Document;
    css: Array<string>;
    js: Array<string>;
    version: string | undefined;
    /**
     * Date of the last modification of this page.
     * @since 0.13
     * @beta
     */
    lastModificationDate: Date | undefined;
    /**
     * Name of the last user to edit this page.
     * @since 0.13
     * @beta
     */
    lastAuthor: UserDetails | undefined;
    /**
     * Indicate if the current user can edit this page.
     * @since 0.13
     * @beta
     */
    canEdit: boolean;
    toObject(): any;
    fromObject(object: any): void;
}
