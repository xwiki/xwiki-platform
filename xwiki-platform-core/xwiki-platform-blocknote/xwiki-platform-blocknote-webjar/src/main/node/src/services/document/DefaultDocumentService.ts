/*
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
import type { PageData } from "@xwiki/cristal-api";
import { DocumentChange, DocumentService } from "@xwiki/cristal-document-api";
import type { DocumentReference } from "@xwiki/cristal-model-api";
import { Container, injectable } from "inversify";
import { type Ref, ref } from "vue";
import { toCristalEntityReference, XWiki } from "../model/reference/XWikiEntityReference";

@injectable("Singleton")
export class DefaultDocumentService implements DocumentService {
  public static bind(container: Container): void {
    container.bind("DocumentService").to(DefaultDocumentService).inSingletonScope();
  }

  public getCurrentDocument(): Ref<PageData | undefined> {
    // TODO
    throw new Error("Method not implemented.");
  }

  public getCurrentDocumentReference(): Ref<DocumentReference | undefined> {
    return ref(toCristalEntityReference(XWiki.currentDocument.documentReference) as DocumentReference);
  }

  public getCurrentDocumentReferenceString(): Ref<string | undefined> {
    return ref(XWiki.Model.serialize(XWiki.currentDocument.documentReference));
  }

  public getCurrentDocumentRevision(): Ref<string | undefined> {
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

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  public setCurrentDocument(documentReference: string, revision?: string): Promise<void> {
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
    listener: (page: DocumentReference) => Promise<void>
  ): void {
    // TODO
    throw new Error("Method not implemented.");
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  public notifyDocumentChange(change: DocumentChange, page: DocumentReference): Promise<void> {
    // TODO
    throw new Error("Method not implemented.");
  }
}
