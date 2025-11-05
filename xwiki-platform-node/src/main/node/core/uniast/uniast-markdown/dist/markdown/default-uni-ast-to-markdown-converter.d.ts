import { InternalLinksSerializerResolver } from './internal-links/serializer/internal-links-serializer-resolver';
import { UniAstToMarkdownConverter } from './uni-ast-to-markdown-converter';
import { InlineContent, UniAst } from '@xwiki/cristal-uniast-api';
/**
 * @since 0.22
 */
export declare class DefaultUniAstToMarkdownConverter implements UniAstToMarkdownConverter {
    private readonly internalLinksSerializerResolver;
    constructor(internalLinksSerializerResolver: InternalLinksSerializerResolver);
    /**
     * Converts the provided AST to Markdown.
     *
     * @param uniAst - the AST to convert to markdown
     *
     * understand the impacts
     */
    toMarkdown(uniAst: UniAst): Promise<string | Error>;
    private blockToMarkdown;
    private convertListItem;
    private convertImage;
    private convertTable;
    private convertTableCell;
    convertInlineContents(inlineContents: InlineContent[]): Promise<string>;
    convertInlineContent(inlineContent: InlineContent): Promise<string>;
    private convertLink;
    private convertMacro;
    private convertText;
}
