import { InternalLinksSerializer } from './internal-links-serializer';
import { UniAstToMarkdownConverter } from '../../uni-ast-to-markdown-converter';
import { Link, LinkTarget } from '@xwiki/cristal-uniast-api';
/**
 * @since 0.22
 */
export declare class XWikiInternalLinkSerializer implements InternalLinksSerializer {
    serialize(content: Link["content"], target: Extract<LinkTarget, {
        type: "internal";
    }>, uniAstToMarkdownConverter: UniAstToMarkdownConverter): Promise<string>;
    serializeImage(target: Extract<LinkTarget, {
        type: "internal";
    }>, alt?: string): Promise<string>;
}
