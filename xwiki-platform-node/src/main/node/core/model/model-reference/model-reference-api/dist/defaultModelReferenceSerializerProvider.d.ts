import { ModelReferenceSerializer } from './modelReferenceSerializer';
import { ModelReferenceSerializerProvider } from './modelReferenceSerializerProvider';
import { CristalApp } from '@xwiki/platform-api';
/**
 * @since 0.12
 * @beta
 */
declare class DefaultModelReferenceSerializerProvider implements ModelReferenceSerializerProvider {
    private readonly cristalApp;
    constructor(cristalApp: CristalApp);
    get(type?: string): ModelReferenceSerializer | undefined;
}
export { DefaultModelReferenceSerializerProvider };
