import { ParserConfigurationResolver } from './internal-links/parser/parser-configuration-resolver';
import { MarkdownToUniAstConverter } from './markdown-to-uni-ast-converter';
import { ModelReferenceHandlerProvider, ModelReferenceParserProvider } from '@xwiki/platform-model-reference-api';
import { UniAst } from '@xwiki/platform-uniast-api';
/**
 * @since 0.22
 */
export declare class DefaultMarkdownToUniAstConverter implements MarkdownToUniAstConverter {
    private readonly modelReferenceParserProvider;
    private readonly modelReferenceHandlerProvider;
    private readonly parserConfigurationResolver;
    constructor(modelReferenceParserProvider: ModelReferenceParserProvider, modelReferenceHandlerProvider: ModelReferenceHandlerProvider, parserConfigurationResolver: ParserConfigurationResolver);
    parseMarkdown(markdown: string): Promise<UniAst | Error>;
    private convertBlock;
    private convertInline;
    private convertLink;
    private convertImage;
    private convertText;
    private handleLinkOrImage;
    private handleMacro;
    private supportFlexmark;
    private collectInlineContent;
}
