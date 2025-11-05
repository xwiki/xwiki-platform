import { LinkSuggestService } from './linkSuggestService';
/**
 * @since 0.11
 * @beta
 */
interface LinkSuggestServiceProvider {
    /**
     * Resolve the LinkSuggestService for the current config type or for the
     * provided type
     * @param type - an optional type to override the current config type
     */
    get(type?: string): LinkSuggestService | undefined;
}
export type { LinkSuggestServiceProvider };
