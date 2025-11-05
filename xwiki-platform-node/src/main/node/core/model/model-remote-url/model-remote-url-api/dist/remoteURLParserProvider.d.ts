import { RemoteURLParser } from './remoteURLParser';
/**
 * @since 0.12
 * @beta
 */
interface RemoteURLParserProvider {
    get(type?: string): RemoteURLParser | undefined;
}
export { type RemoteURLParserProvider };
