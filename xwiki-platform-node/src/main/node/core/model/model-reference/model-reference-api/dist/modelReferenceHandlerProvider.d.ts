import { ModelReferenceHandler } from './modelReferenceHandler';
/**
 * A ModelReferenceHandlerProvider returns the expected instance of
 * {@link ModelReferenceHandler}.
 *
 * @since 0.13
 * @beta
 */
interface ModelReferenceHandlerProvider {
    /**
     * Returns the instance of {@link ModelReferenceHandler} matching the
     * requested wiki configuration type, or the current one if empty.
     *
     * @param type - the wiki configuration type
     * @returns the instance of model reference handler
     */
    get(type?: string): ModelReferenceHandler | undefined;
}
export { type ModelReferenceHandlerProvider };
