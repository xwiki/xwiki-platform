import { UniAst } from '@xwiki/platform-uniast-api';
/**
 * Convert Markdown string to a Universal AST.
 *
 * @since 0.16
 * @beta
 */
export interface MarkdownToUniAstConverter {
    /**
     * Parse a markdown document to a universal AST
     *
     * @param markdown - The markdown content to parse
     *
     * @returns The Universal Ast
     */
    parseMarkdown(markdown: string): Promise<UniAst | Error>;
}
