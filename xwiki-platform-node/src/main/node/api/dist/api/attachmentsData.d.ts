import { PageAttachment } from './pageAttachment';
/**
 * Return the list of attachments and an optional count if the results are
 * paginated.
 * @since 0.9
 * @beta
 */
export type AttachmentsData = {
    attachments: PageAttachment[];
    count?: number;
};
