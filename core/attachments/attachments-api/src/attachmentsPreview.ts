import { Attachment } from "./attachment";
import { AttachmentReference } from "@xwiki/cristal-model-api";
import { Ref } from "vue";

/**
 * @since 0.12
 */
interface AttachmentPreview {
  preview(attachment: AttachmentReference): Promise<void>;
  attachment(): Ref<Attachment | undefined>;
  error(): Ref<string | undefined>;
  loading(): Ref<boolean>;
}

export { type AttachmentPreview };
