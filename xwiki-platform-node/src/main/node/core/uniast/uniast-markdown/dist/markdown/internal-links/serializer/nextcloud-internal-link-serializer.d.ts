import { InternalLinksSerializer } from './internal-links-serializer';
import { UniAstToMarkdownConverter } from '../../uni-ast-to-markdown-converter';
import { CristalApp } from '@xwiki/platform-api';
import { DocumentService } from '@xwiki/platform-document-api';
import { RemoteURLSerializerProvider } from '@xwiki/platform-model-remote-url-api';
import { Link, LinkTarget } from '@xwiki/platform-uniast-api';
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
