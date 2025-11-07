import { InternalLinksSerializer } from './internal-links-serializer';
import { CristalApp } from '@xwiki/platform-api';
/**
 * @since 0.22
 */
export declare class InternalLinksSerializerResolver {
    private readonly cristalApp;
    constructor(cristalApp: CristalApp);
    get(): Promise<InternalLinksSerializer>;
}
