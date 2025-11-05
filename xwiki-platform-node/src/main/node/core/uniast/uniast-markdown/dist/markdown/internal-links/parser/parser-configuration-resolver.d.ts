import { MarkdownParserConfiguration } from './markdown-parser-configuration';
import { CristalApp } from '@xwiki/cristal-api';
/**
 * @since 0.22
 */
export declare class ParserConfigurationResolver {
    private readonly cristalApp;
    constructor(cristalApp: CristalApp);
    get(): MarkdownParserConfiguration;
}
