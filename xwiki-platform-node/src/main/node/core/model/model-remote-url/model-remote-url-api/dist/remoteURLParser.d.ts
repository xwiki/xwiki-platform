import { EntityReference, EntityType } from '@xwiki/cristal-model-api';
/**
 * @since 0.12
 * @beta
 * @throws {@link Error} in case of issue when parsing the url
 */
interface RemoteURLParser {
    /**
     * @param url - the url to parse
     * @param type - an optional expected type of the parsed url
     * @since 0.20
     * @beta
     */
    parse(url: string, type?: EntityType): EntityReference | undefined;
}
export { type RemoteURLParser };
