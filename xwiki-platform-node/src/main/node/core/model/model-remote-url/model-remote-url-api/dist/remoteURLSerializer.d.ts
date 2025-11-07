import { EntityReference } from '@xwiki/platform-model-api';
/**
 * @since 0.12
 * @beta
 */
interface RemoteURLSerializer {
    /**
     * @param reference - the reference to serialize
     */
    serialize(reference?: EntityReference): string | undefined;
}
export { type RemoteURLSerializer };
