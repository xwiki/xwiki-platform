import { ModelReferenceParser } from './modelReferenceParser';
/**
 * @since 0.12
 * @beta
 */
interface ModelReferenceParserProvider {
    get(type?: string): ModelReferenceParser | undefined;
}
export { type ModelReferenceParserProvider };
