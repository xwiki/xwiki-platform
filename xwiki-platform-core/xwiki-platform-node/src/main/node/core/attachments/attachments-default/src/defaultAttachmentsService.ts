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

import { AttachmentReference } from "@xwiki/platform-model-api";
import { inject, injectable } from "inversify";
import { defineStore, storeToRefs } from "pinia";
import type { PageAttachment } from "@xwiki/platform-api";
import type {
  Attachment,
  AttachmentsService,
} from "@xwiki/platform-attachments-api";
import type { StorageProvider } from "@xwiki/platform-backend-api";
import type { DocumentReference } from "@xwiki/platform-model-api";
import type {
  ModelReferenceParserProvider,
  ModelReferenceSerializerProvider,
} from "@xwiki/platform-model-reference-api";
import type { RemoteURLSerializerProvider } from "@xwiki/platform-model-remote-url-api";
import type { Store, StoreDefinition } from "pinia";
import type { Ref } from "vue";

type Id = "attachments";
type State = {
  attachments: Attachment[];
  count: number;
  isLoading: boolean;
  isUploading: boolean;
  unknownPage: boolean;
  error: string | undefined;
};
/**
 * Take a given type "Type" and wraps each of its fields in a readonly Ref.
 */
type WrappedRefs<Type> = {
  readonly [Property in keyof Type]: Ref<Type[Property]>;
};
type StateRefs = WrappedRefs<State>;
type Getters = Record<string, never>;
type Actions = {
  setLoading(): void;
  /**
   * Update the attachments of the store
   * @param attachments - the list of attachments to store
   * @param count - an optional count, used for the count status if available,
   *  otherwise the size of the attachment list is used
   */
  updateAttachments(
    attachments: Attachment[] | undefined,
    count?: number,
  ): void;
  setError(error: string): void;
  startUploading(): void;
  stopUploading(): void;
};
type AttachmentsStoreDefinition = StoreDefinition<Id, State, Getters, Actions>;
type AttachmentsStore = Store<Id, State, Getters, Actions>;

const attachmentsStore: AttachmentsStoreDefinition = defineStore<
  Id,
  State,
  Getters,
  Actions
>("attachments", {
  state() {
    return {
      attachments: [],
      count: 0,
      isLoading: true,
      isUploading: false,
      error: undefined,
      unknownPage: false,
    };
  },
  actions: {
    setLoading() {
      this.isLoading = true;
    },
    updateAttachments(attachments, count?: number) {
      this.isLoading = false;
      this.error = undefined;
      if (attachments) {
        this.unknownPage = false;
        this.attachments = attachments;
        this.count = count || attachments.length;
      } else {
        this.unknownPage = true;
        this.attachments = [];
        this.count = 0;
      }
    },
    setError(error: string) {
      this.isLoading = false;
      this.error = error;
    },
    startUploading() {
      this.isUploading = true;
    },
    stopUploading() {
      this.isUploading = false;
    },
  },
});

/**
 * @param attachment - an attachment, as provided by the storage
 * @returns the given attachment, as exposed by the attachments service
 */
function toAttachment({
  id,
  reference,
  mimetype,
  href,
  date,
  size,
  author,
}: PageAttachment): Attachment {
  let userDetails = undefined;
  if (author) {
    // TODO: resolve author details
    userDetails = { name: author };
  }
  return {
    id,
    name: reference,
    mimetype,
    href,
    date,
    size,
    author: userDetails,
  };
}

/**
 * @since 18.0.0RC1
 * @beta
 */
@injectable()
export class DefaultAttachmentsService implements AttachmentsService {
  private readonly refs: StateRefs;
  private readonly store: AttachmentsStore;

  constructor(
    @inject("StorageProvider")
    private readonly storageProvider: StorageProvider,
    @inject("ModelReferenceParserProvider")
    private readonly modelReferenceParserProvider: ModelReferenceParserProvider,
    @inject("ModelReferenceSerializerProvider")
    private readonly modelReferenceSerializerProvider: ModelReferenceSerializerProvider,
    @inject("RemoteURLSerializerProvider")
    private readonly remoteURLSerializerProvider: RemoteURLSerializerProvider,
  ) {
    // An internal store is kept to easily provide refs for updatable elements.
    this.store = attachmentsStore();
    this.refs = storeToRefs(this.store);
  }

  list(): StateRefs["attachments"] {
    return this.refs.attachments;
  }

  count(): StateRefs["count"] {
    return this.refs.count;
  }

  isLoading(): StateRefs["isLoading"] {
    return this.refs.isLoading;
  }

  isUploading(): StateRefs["isUploading"] {
    return this.refs.isUploading;
  }

  getError(): StateRefs["error"] {
    return this.refs.error;
  }

  async refresh(page: string): Promise<void> {
    this.store.setLoading();
    try {
      const attachmentData = await this.storageProvider
        .get()
        .getAttachments(page);
      if (attachmentData) {
        const { attachments, count } = attachmentData;
        this.store.updateAttachments(attachments?.map(toAttachment), count);
      } else {
        this.store.updateAttachments([], 0);
      }
    } catch (e) {
      if (e instanceof Error) {
        this.store.setError(e.message);
      }
    }
  }
  async upload(page: string, files: File[]): Promise<Attachment[]> {
    this.store.startUploading();
    let uploadedAttachments;
    try {
      uploadedAttachments = await this.storageProvider
        .get()
        .saveAttachments(page, files);
    } finally {
      this.store.stopUploading();
    }

    await this.refresh(page);
    return files.map((file, index) =>
      toAttachment(
        uploadedAttachments?.[index] ??
          this.getDefaultUploadedAttachment(page, file),
      ),
    );
  }

  /**
   * Describes an uploaded file that the storage didn't describe, assuming the attachment is named after the file.
   *
   * @param page - the page the file was uploaded to
   * @param file - the uploaded file
   * @returns the attachment holding the uploaded file
   */
  private getDefaultUploadedAttachment(
    page: string,
    file: File,
  ): PageAttachment {
    const attachmentReference = new AttachmentReference(
      file.name,
      this.modelReferenceParserProvider
        .get()
        ?.parse(page, { relative: false }) as DocumentReference,
    );
    const href = this.remoteURLSerializerProvider
      .get()
      ?.serialize(attachmentReference);
    if (!href) {
      throw new Error(
        `Failed to compute the URL of the uploaded file [${file.name}].`,
      );
    }
    return {
      id:
        this.modelReferenceSerializerProvider
          .get()
          ?.serialize(attachmentReference) ?? file.name,
      reference: file.name,
      mimetype: file.type,
      href,
      size: file.size,
      date: new Date(),
      author: undefined,
    };
  }
}
