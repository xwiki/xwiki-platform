import { RemoteURLParser } from './remoteURLParser';
import { RemoteURLParserProvider } from './remoteURLParserProvider';
import { CristalApp } from '@xwiki/platform-api';
/**
 * @since 0.12
 * @beta
 */
declare class DefaultRemoteURLParserProvider implements RemoteURLParserProvider {
    private readonly cristalApp;
    constructor(cristalApp: CristalApp);
    get(type?: string): RemoteURLParser | undefined;
}
export { DefaultRemoteURLParserProvider };
