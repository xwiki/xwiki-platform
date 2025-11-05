import { LinkType } from './linkType';
import { Link } from './link';
/**
 * Provide the operation to get links suggestions.
 * @since 0.8
 * @beta
 */
interface LinkSuggestService {
    /**
     * Returns a list of page links from a text query.
     * @param query - a textual search value (e.g., PageName)
     * @param linkType - when provided, only results matching the provided type are returned
     * @param mimetype - when provided, only results matching the provided mimetype are returned. Only used when
     *     linkType is {@link LinkType.ATTACHMENT} (e.g., "image/*" or "application/pdf")
     */
    getLinks(query: string, linkType?: LinkType, mimetype?: string): Promise<Link[]>;
}
export { type LinkSuggestService };
