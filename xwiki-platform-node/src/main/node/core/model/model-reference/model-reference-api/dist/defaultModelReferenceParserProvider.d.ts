import { ModelReferenceParser } from './modelReferenceParser';
import { ModelReferenceParserProvider } from './modelReferenceParserProvider';
import { CristalApp } from '@xwiki/cristal-api';
/**
 * @since 0.12
 * @beta
 */
declare class DefaultModelReferenceParserProvider implements ModelReferenceParserProvider {
    private readonly cristalApp;
    constructor(cristalApp: CristalApp);
    get(type?: string): ModelReferenceParser | undefined;
}
export { DefaultModelReferenceParserProvider };
