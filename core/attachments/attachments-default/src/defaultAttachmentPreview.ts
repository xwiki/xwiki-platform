/*
 * See the LICENSE file distributed with this work for additional
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

import { Attachment, AttachmentPreview } from "@xwiki/cristal-attachments-api";
import { UserDetails } from "@xwiki/cristal-authentication-api";
import { AttachmentReference } from "@xwiki/cristal-model-api";
import { inject, injectable } from "inversify";
import { Store, StoreDefinition, defineStore, storeToRefs } from "pinia";
import { Ref } from "vue";
import type { StorageProvider } from "@xwiki/cristal-backend-api";
import type { ModelReferenceSerializerProvider } from "@xwiki/cristal-model-reference-api";

type Id = "attachment-preview";
type State = {
  loading: boolean;
  attachment: Attachment | undefined;
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
  startLoading(): void;
  stopLoading(): void;
  setAttachment(attachment: Attachment): void;
  setError(message: string): void;
};
type AttachmentPreviewStoreDefinition = StoreDefinition<
  Id,
  State,
  Getters,
  Actions
>;
type AttachmentPreviewStore = Store<Id, State, Getters, Actions>;

const attachmentPreviewStore: AttachmentPreviewStoreDefinition = defineStore<
  Id,
  State,
  Getters,
  Actions
>("attachment-preview", {
  state() {
    return {
      loading: false,
      attachment: undefined,
      error: undefined,
    };
  },
  actions: {
    startLoading() {
      this.loading = true;
      // The attachment and error are also reset so that the state of a preview attachment is not key when an error
      // occurs during the load of a new attachment.
      this.attachment = undefined;
      this.error = undefined;
    },
    setAttachment(attachment: Attachment): void {
      this.attachment = attachment;
      this.loading = false;
    },
    setError(message: string): void {
      this.error = message;
      this.loading = false;
    },
    stopLoading(): void {
      this.loading = false;
    },
  },
});

@injectable()
class DefaultAttachmentPreview implements AttachmentPreview {
  private readonly refs: StateRefs;
  private readonly store: AttachmentPreviewStore;
  constructor(
    @inject<StorageProvider>("StorageProvider")
    private readonly storageProvider: StorageProvider,
    @inject("ModelReferenceSerializerProvider")
    private readonly modelReferenceSerializerProvider: ModelReferenceSerializerProvider,
  ) {
    this.store = attachmentPreviewStore();
    this.refs = storeToRefs(this.store);
  }

  // TODO: reduce the number of statements in the following method and reactivate the disabled eslint rule.
  // eslint-disable-next-line max-statements
  async preview(attachmentReference: AttachmentReference): Promise<void> {
    this.store.startLoading();
    try {
      const serializer = this.modelReferenceSerializerProvider.get();
      if (serializer) {
        const attachment = await this.storageProvider
          .get()
          .getAttachment(
            serializer.serialize(attachmentReference.document)!,
            attachmentReference.name,
          );
        if (attachment) {
          let author: UserDetails | undefined = undefined;
          if (attachment.author) {
            // TODO: resolve author details
            author = { name: attachment.author };
          }
          this.store.setAttachment({
            author,
            name: attachmentReference.name,
            href: attachment.href,
            date: attachment.date,
            size: attachment.size,
            mimetype: attachment.mimetype,
            id: attachment.id,
          });
        }
      } else {
        console.error("Failed to find a model reference serializer component.");
      }
    } finally {
      this.store.stopLoading();
    }
  }

  attachment(): StateRefs["attachment"] {
    return this.refs.attachment;
  }

  loading(): StateRefs["loading"] {
    return this.refs.loading;
  }

  error(): StateRefs["error"] {
    return this.refs.error;
  }
}

export { DefaultAttachmentPreview };
