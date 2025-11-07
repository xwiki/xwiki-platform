import { ModelReferenceHandler } from './modelReferenceHandler';
import { ModelReferenceHandlerProvider } from './modelReferenceHandlerProvider';
import { CristalApp } from '@xwiki/platform-api';
/**
 * Default implementation for {@link ModelReferenceHandlerProvider}.
 * Will provide an instance of
 * {@link ./defaultModelReferenceHandler#DefaultModelReferenceHandler} as a
 * fallback if no better fit was registered.
 *
 * @since 0.13
 * @beta
 */
declare class DefaultModelReferenceHandlerProvider implements ModelReferenceHandlerProvider {
    private cristalApp;
    constructor(cristalApp: CristalApp);
    get(type?: string): ModelReferenceHandler | undefined;
}
export { DefaultModelReferenceHandlerProvider };
