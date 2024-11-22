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

import { inject, injectable } from "inversify";
import { Store, StoreDefinition, defineStore, storeToRefs } from "pinia";
import { Ref } from "vue";
import type { CristalApp } from "@xwiki/cristal-api";
import type {
  Attachment,
  AttachmentsService,
} from "@xwiki/cristal-attachments-api";

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
 * @since 0.9
 */
@injectable()
export class DefaultAttachmentsService implements AttachmentsService {
  private readonly refs: StateRefs;
  private readonly store: AttachmentsStore;

  constructor(
    @inject<CristalApp>("CristalApp") private readonly cristalApp: CristalApp,
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
      const attachmentData = await this.getStorage().getAttachments(page);
      if (attachmentData) {
        const { attachments, count } = attachmentData;
        this.store.updateAttachments(
          attachments?.map(
            ({ id, reference, mimetype, href, date, size, author }) => {
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
            },
          ),
          count,
        );
      }
    } catch (e) {
      if (e instanceof Error) {
        this.store.setError(e.message);
      }
    }
  }

  private getStorage() {
    return this.cristalApp.getWikiConfig().storage;
  }

  async upload(page: string, files: File[]): Promise<void> {
    this.store.startUploading();
    try {
      await this.getStorage().saveAttachments(page, files);
    } finally {
      this.store.stopUploading();
    }

    await this.refresh(page);
    return;
  }
}
