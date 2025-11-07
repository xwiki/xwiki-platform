import { PageData } from '@xwiki/platform-api';
/**
 * @since 0.1
 * @beta
 */
export default interface OfflineStorage {
    getPage(wikiName: string, id: string): Promise<PageData | undefined>;
    savePage(wikiName: string, id: string, page: PageData): void;
    updatePage(wikiName: string, id: string, page: PageData): void;
}
