import { InternalLinksSerializer } from './internal-links-serializer';
import { UniAstToMarkdownConverter } from '../../uni-ast-to-markdown-converter';
import { CristalApp } from '@xwiki/cristal-api';
import { DocumentService } from '@xwiki/cristal-document-api';
import { RemoteURLSerializerProvider } from '@xwiki/cristal-model-remote-url-api';
import { Link, LinkTarget } from '@xwiki/cristal-uniast-api';
/**
 * @since 0.22
 */
export declare class NextcloudInternalLinkSerializer implements InternalLinksSerializer {
    private readonly remoteURLSerializerProvider;
    private readonly cristalApp;
    private readonly documentService;
    constructor(remoteURLSerializerProvider: RemoteURLSerializerProvider, cristalApp: CristalApp, documentService: DocumentService);
    serialize(content: Link["content"], target: Extract<LinkTarget, {
        type: "internal";
    }>, uniAstToMarkdownConverter: UniAstToMarkdownConverter): Promise<string>;
    serializeImage(target: Extract<LinkTarget, {
        type: "internal";
    }>, alt?: string): Promise<string>;
}
