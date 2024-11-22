import { Attachment } from "./attachment";
import { Ref } from "vue";

/**
 * @since 0.9
 */
interface AttachmentsService {
  list(): Ref<Attachment[]>;

  count(): Ref<number>;

  isLoading(): Ref<boolean>;

  /**
   * True while an attachment is uploading.
   */
  isUploading(): Ref<boolean>;

  getError(): Ref<string | undefined>;

  /**
   * Load the initial state of the attachments.
   */
  refresh(page: string): Promise<void>;

  /**
   * Upload the provided list of files to a given page
   * @param page - the page where to save the files
   * @param files - the list of files to upload
   */
  upload(page: string, files: File[]): Promise<void>;
}

export { type AttachmentsService };
