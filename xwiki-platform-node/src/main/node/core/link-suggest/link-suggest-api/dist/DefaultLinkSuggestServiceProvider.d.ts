import { LinkSuggestServiceProvider } from './LinkSuggestServiceProvider';
import { LinkSuggestService } from './linkSuggestService';
import { CristalApp } from '@xwiki/cristal-api';
/**
 * @since 0.11
 * @beta
 */
declare class DefaultLinkSuggestServiceProvider implements LinkSuggestServiceProvider {
    private cristalApp;
    constructor(cristalApp: CristalApp);
    get(type?: string): LinkSuggestService | undefined;
}
export { DefaultLinkSuggestServiceProvider };
