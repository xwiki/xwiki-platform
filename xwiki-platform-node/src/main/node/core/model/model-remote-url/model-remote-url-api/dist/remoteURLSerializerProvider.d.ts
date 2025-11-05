import { RemoteURLSerializer } from './remoteURLSerializer';
/**
 * @since 0.12
 * @beta
 */
interface RemoteURLSerializerProvider {
    get(type?: string): RemoteURLSerializer | undefined;
}
export { type RemoteURLSerializerProvider };
