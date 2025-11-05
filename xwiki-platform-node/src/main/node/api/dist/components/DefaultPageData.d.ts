import { PageData } from '../api/PageData';
import { Document } from '../api/document';
import { UserDetails } from '@xwiki/cristal-authentication-api';
/**
 * @beta
 */
export declare class DefaultPageData implements PageData {
    id: string;
    name: string;
    mode: string;
    source: string;
    syntax: string;
    html: string;
    document: Document;
    css: Array<string>;
    js: Array<string>;
    version: string;
    headline: string;
    headlineRaw: string;
    lastModificationDate: Date | undefined;
    lastAuthor: UserDetails | undefined;
    canEdit: boolean;
    constructor(id?: string, name?: string, source?: string, syntax?: string);
    toObject(): any;
    fromObject(object: any): void;
}
