import { EntityType } from '@xwiki/platform-model-api';
/**
 * @since 0.22
 * @beta
 */
export type ModelReferenceParserOptions = {
    /**
     * an optional type, helping to remove ambiguity when parsing the reference
     */
    type?: EntityType;
    /**
     * When false, the model reference is parsed as an absolute reference.
     * When true, the model reference is parsed relatively to the current document.
     * The default value is true.
     */
    relative?: boolean;
};
