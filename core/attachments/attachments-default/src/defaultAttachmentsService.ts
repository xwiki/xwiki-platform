/**
 * See the LICENSE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 * This file is part of the Cristal Wiki software prototype
 * @copyright  Copyright (c) 2023 XWiki SAS
 * @license    http://opensource.org/licenses/AGPL-3.0 AGPL-3.0
 *
 **/

import type {
  Attachment,
  AttachmentsService,
} from "@xwiki/cristal-attachments-api";
import { Ref } from "vue";
import { inject, injectable } from "inversify";
import { defineStore, Store, StoreDefinition, storeToRefs } from "pinia";
import { CristalApp } from "@xwiki/cristal-api";

type Id = "attachments";
type State = {
  attachments: Attachment[];
  isLoading: boolean;
  unknownPage: boolean;
};
type Getters = Record<string, never>;
type Actions = {
  setLoading(): void;
  updateAttachments(attachments: Attachment[] | undefined): void;
};
type AttachmentsStoreDefinition = StoreDefinition<Id, State, Getters, Actions>;
type AttachmentsStore = Store<Id, State, Getters, Actions>;

const attachmentsStore: AttachmentsStoreDefinition = defineStore(
  "attachments",
  {
    state() {
      return {
        attachments: [],
        isLoading: true,
        unknownPage: false,
      };
    },
    actions: {
      setLoading() {
        this.isLoading = true;
      },
      updateAttachments(attachments) {
        this.isLoading = false;
        if (attachments) {
          this.unknownPage = false;
          this.attachments = attachments;
        } else {
          this.unknownPage = true;
          this.attachments = [];
        }
      },
    },
  },
);

/**
 * @since 0.9
 */
@injectable()
export class DefaultAttachmentsService implements AttachmentsService {
  private readonly refs: {
    attachments: Ref<Attachment[]>;
    isLoading: Ref<boolean>;
  };
  private readonly store: AttachmentsStore;

  constructor(
    @inject<CristalApp>("CristalApp") private readonly cristalApp: CristalApp,
  ) {
    // An internal store is kept to easily provide refs for updatable elements.
    this.store = attachmentsStore();
    this.refs = storeToRefs(this.store);
  }

  list(): Ref<Attachment[]> {
    return this.refs.attachments;
  }

  isLoading(): Ref<boolean> {
    return this.refs.isLoading;
  }

  async refresh(page: string) {
    this.store.setLoading();
    const attachments = await this.cristalApp
      .getWikiConfig()
      .storage.getAttachments(page);
    this.store.updateAttachments(
      attachments?.map(({ id, reference, mimetype, href }) => {
        return { id, name: reference, mimetype, href };
      }),
    );
  }
}
