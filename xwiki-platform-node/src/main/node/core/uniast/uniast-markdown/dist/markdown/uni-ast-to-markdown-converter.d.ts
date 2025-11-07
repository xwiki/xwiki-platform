import { InlineContent, UniAst } from '@xwiki/platform-uniast-api';
/**
 * Converts Universal AST trees to markdown.
 *
 * @since 0.16
 * @beta
 */
export interface UniAstToMarkdownConverter {
    /**
     * Converts the provided AST to Markdown.
     *
     * @param uniAst - the AST to convert to markdown
     *
     * understand the impacts
     */
    toMarkdown(uniAst: UniAst): Promise<string | Error>;
    /**
     * @since 0.22
     * @internal
     * @param inlineContents - the inline contents to convert to markdown
     * @returns the markdown representation of the inline content
     */
    convertInlineContents(inlineContents: InlineContent[]): Promise<string>;
}
