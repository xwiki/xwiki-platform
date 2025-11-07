import { EntityReference } from '@xwiki/platform-model-api';
/**
 * @since 0.12
 * @beta
 */
interface ModelReferenceSerializer {
    serialize(reference?: EntityReference): string | undefined;
}
export type { ModelReferenceSerializer };
