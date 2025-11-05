import { ModelReferenceParserOptions } from './modelReferenceParserOptions';
import { EntityReference } from '@xwiki/cristal-model-api';
/**
 * @since 0.12
 * @beta
 */
interface ModelReferenceParser {
    /**
     * @param reference - an entity reference
     * @param options - (since 0.22) an optional configuration object
     */
    parse(reference: string, options?: ModelReferenceParserOptions): EntityReference;
    /**
     * Parse a reference with additional analysis that can only be performed asynchronously
     * @param reference - an entity reference
     * @param options - an optional configuration object
     * @since 0.22
     * @beta
     */
    parseAsync(reference: string, options?: ModelReferenceParserOptions): Promise<EntityReference>;
}
export type { ModelReferenceParser };
