import { RemoteURLSerializer } from './remoteURLSerializer';
import { RemoteURLSerializerProvider } from './remoteURLSerializerProvider';
import { CristalApp } from '@xwiki/cristal-api';
declare class DefaultRemoteURLSerializerProvider implements RemoteURLSerializerProvider {
    private readonly cristalApp;
    constructor(cristalApp: CristalApp);
    get(type?: string): RemoteURLSerializer | undefined;
}
export { DefaultRemoteURLSerializerProvider };
