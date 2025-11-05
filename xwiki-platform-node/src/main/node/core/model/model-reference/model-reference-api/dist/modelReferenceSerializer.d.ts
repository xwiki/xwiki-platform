import { EntityReference } from '@xwiki/cristal-model-api';
/**
 * @since 0.12
 * @beta
 */
interface ModelReferenceSerializer {
    serialize(reference?: EntityReference): string | undefined;
}
export type { ModelReferenceSerializer };
