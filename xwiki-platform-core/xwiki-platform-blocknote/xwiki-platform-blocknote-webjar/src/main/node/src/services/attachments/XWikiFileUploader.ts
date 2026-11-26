/**
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
import { getTemporaryUploads } from "./temporaryUploads";
import { ConnectionStatus } from "@xwiki/platform-collaboration-api";
import { fileUploaderName } from "@xwiki/platform-editors-blocknote-react";
import { toXWikiEntityReference } from "@xwiki/platform-model-xwiki";
import { AttachmentReference, Model } from "@xwiki/platform-xwiki-model-api";
import { Container, inject, injectable } from "inversify";
import type { StorageProvider } from "@xwiki/platform-backend-api";
import type {
  EditorInstance,
  FileUploader,
  UploadedFile,
} from "@xwiki/platform-editors-blocknote-react";
import type { EntityReference as XWikiEntityReference } from "@xwiki/platform-xwiki-model-api";

/**
 * Attaches the uploaded files to the source document of the edited content (the current document when not specified),
 * either right away, or through the temporary store of the user session, from where the save request attaches them, as
 * the CKEditor integration does. The files uploaded to the temporary store are recorded in the
 * {@link TemporaryUploads} of the editor instance, so that the save request asks for them.
 *
 * @since 18.10.0RC1
 */
@injectable("Singleton")
export class XWikiFileUploader implements FileUploader {
  public static bind(container: Container): void {
    container
      .bind<FileUploader>(fileUploaderName)
      .to(XWikiFileUploader)
      .inSingletonScope();
  }

  constructor(
    @inject("StorageProvider")
    private readonly storageProvider: StorageProvider,
  ) {}

  public async upload(
    instance: EditorInstance,
    file: File,
  ): Promise<UploadedFile> {
    const documentReference = this.getDocumentReference(instance);
    const temporary = this.isTemporary(instance);
    const result = await this.storageProvider
      .get()
      .saveAttachments(Model.serialize(documentReference), [file], {
        temporary,
      });
    const attachment = result?.[0];
    if (!attachment) {
      throw new Error(`Failed to attach [${file.name}].`);
    }
    // The storage identifies the attachment by its name, which the upload end-point can change, so it is the one to
    // report, rather than the name of the uploaded file.
    const name = attachment.reference;
    if (temporary) {
      getTemporaryUploads(instance)?.add(name);
    }
    return {
      url: attachment.href,
      name,
      reference: Model.serialize(
        new AttachmentReference(name, documentReference),
      ),
    };
  }

  private getDocumentReference(instance: EditorInstance): XWikiEntityReference {
    const { sourceDocumentReference } = instance.options;
    return sourceDocumentReference
      ? toXWikiEntityReference(sourceDocumentReference)
      : XWiki.currentDocument.documentReference;
  }

  /**
   * A file in the temporary store is visible only to the session that uploaded it, and only a save made from that
   * session attaches it to the document. In a realtime session any of the collaborators can be the one that saves, so
   * the file is attached right away instead, like the CKEditor integration does. The same goes when the editor
   * instance has no name, because then nothing it holds is submitted and the file would never be attached.
   *
   * @param instance - the editor instance where the file is uploaded
   * @returns whether to upload the file to the temporary store
   */
  private isTemporary(instance: EditorInstance): boolean {
    const { name, collaboration } = instance.options;
    return (
      !!name &&
      collaboration?.connectionStatus.value !== ConnectionStatus.Connected
    );
  }
}
