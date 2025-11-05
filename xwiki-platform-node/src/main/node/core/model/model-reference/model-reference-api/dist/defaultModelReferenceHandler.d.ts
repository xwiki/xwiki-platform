import { DocumentReference, EntityReference, SpaceReference } from '@xwiki/cristal-model-api';
import { ModelReferenceHandler } from './modelReferenceHandler';
/**
 * Default implementation for {@link ModelReferenceHandler}.
 *
 * @since 0.13
 * @beta
 */
declare class DefaultModelReferenceHandler implements ModelReferenceHandler {
    createDocumentReference(name: string, space: SpaceReference): DocumentReference;
    getTitle(reference: EntityReference): string;
}
export { DefaultModelReferenceHandler };
