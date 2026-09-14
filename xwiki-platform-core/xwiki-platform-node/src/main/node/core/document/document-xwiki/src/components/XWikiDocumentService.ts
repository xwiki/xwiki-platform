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
import { XWikiDocument } from "../XWikiDocument";
import { toCristalEntityReference } from "@xwiki/platform-model-xwiki";
import { inject, injectable } from "inversify";
import { ref } from "vue";
import type { PageData } from "@xwiki/platform-api";
import type {
  DocumentChange,
  DocumentService,
} from "@xwiki/platform-document-api";
import type { DocumentReference } from "@xwiki/platform-model-api";
import type { XWikiMeta } from "@xwiki/platform-xwiki-utils";
import type { Ref } from "vue";

@injectable()
class XWikiDocumentService implements DocumentService {
  constructor(@inject("XWikiMeta") private readonly xwikiMeta: XWikiMeta) {}

  /**
   * @returns the document displayed by the current page. It is read again on each call, so that it stays accurate
   *   when the page edits a different document in place, without being reloaded.
   */
  private getCurrentXWikiDocument(): XWikiDocument {
    return XWikiDocument.currentDocument(this.xwikiMeta);
  }

  public getCurrentDocument(): Ref<PageData | undefined> {
    // TODO
    throw new Error("Method not implemented.");
  }

  public getCurrentDocumentReference(): Ref<DocumentReference | undefined> {
    const currentDocument = this.getCurrentXWikiDocument();
    const documentReference = toCristalEntityReference(
      currentDocument.documentReference,
    ) as DocumentReference;
    documentReference.locale = currentDocument.language;
    return ref(documentReference);
  }

  public getCurrentDocumentReferenceString(): Ref<string | undefined> {
    return ref(
      XWiki.Model.serialize(this.getCurrentXWikiDocument().documentReference),
    );
  }

  public getCurrentDocumentRevision(): Ref<string | undefined> {
    // TODO
    throw new Error("Method not implemented.");
  }

  public getCurrentDocumentAction(): Ref<string | undefined> {
    // TODO
    throw new Error("Method not implemented.");
  }

  public getDisplayTitle(): Ref<string> {
    // TODO
    throw new Error("Method not implemented.");
  }

  public getTitle(): Ref<string | undefined> {
    // TODO
    throw new Error("Method not implemented.");
  }

  public isLoading(): Ref<boolean> {
    // TODO
    throw new Error("Method not implemented.");
  }

  public getError(): Ref<Error | undefined> {
    // TODO
    throw new Error("Method not implemented.");
  }

  public setCurrentDocument(
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    documentReference: string,
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    revision?: string,
  ): Promise<void> {
    // TODO
    throw new Error("Method not implemented.");
  }

  public refreshCurrentDocument(): Promise<void> {
    // TODO
    throw new Error("Method not implemented.");
  }

  public registerDocumentChangeListener(
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    change: DocumentChange,
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    listener: (page: DocumentReference) => Promise<void>,
  ): void {
    // TODO
    throw new Error("Method not implemented.");
  }

  public notifyDocumentChange(
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    change: DocumentChange,
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    page: DocumentReference,
  ): Promise<void> {
    // TODO
    throw new Error("Method not implemented.");
  }

  removeDocumentChangeListener(): void {
    // TODO implement along with registerDocumentChangeListener
    throw new Error("Method not implemented.");
  }
}

export { XWikiDocumentService };
