import { AttachmentsService } from '@xwiki/platform-attachments-api';
import { DocumentService } from '@xwiki/platform-document-api';
import { LinkSuggestService, LinkType } from '@xwiki/platform-link-suggest-api';
import { ModelReferenceHandler, ModelReferenceParser, ModelReferenceSerializer } from '@xwiki/platform-model-reference-api';
import { RemoteURLParser, RemoteURLSerializer } from '@xwiki/platform-model-remote-url-api';
/**
 * @since 18.0.0RC1
 * @beta
 */
type LinkEditionContext = {
    linkSuggestService: LinkSuggestService | null;
    modelReferenceParser: ModelReferenceParser;
    modelReferenceSerializer: ModelReferenceSerializer;
    modelReferenceHandler: ModelReferenceHandler;
    remoteURLParser: RemoteURLParser;
    remoteURLSerializer: RemoteURLSerializer;
    attachmentsService: AttachmentsService;
    documentService: DocumentService;
};
export type { LinkEditionContext, LinkType };
