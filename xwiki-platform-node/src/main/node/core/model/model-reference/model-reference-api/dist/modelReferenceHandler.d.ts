import { DocumentReference, EntityReference, SpaceReference } from '@xwiki/cristal-model-api';
/**
 * A ModelReferenceHandler can do backend-specific operations involving
 * {@link @xwiki/cristal-model-api#EntityReference | EntityReferences}.
 *
 * @since 0.13
 * @beta
 */
interface ModelReferenceHandler {
    /**
     * Returns a {@link DocumentReference} with a specific name and a parent
     * {@link SpaceReference}.
     *
     * @param name - the name of the document reference
     * @param space - the parent space of the document reference
     * @returns the document reference
     */
    createDocumentReference(name: string, space: SpaceReference): DocumentReference;
    /**
     * Return the title of a reference
     */
    getTitle(reference: EntityReference): string;
}
export type { ModelReferenceHandler };
