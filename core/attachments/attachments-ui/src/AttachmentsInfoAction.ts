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

import { InfoAction } from "@xwiki/cristal-info-actions-api";
import { inject, injectable } from "inversify";
import { Ref } from "vue";
import type { CristalApp } from "@xwiki/cristal-api";
import type { AttachmentsService } from "@xwiki/cristal-attachments-api";

/**
 * Display the total attachments count of the current page.
 * @since 0.9
 */
@injectable()
export class AttachmentsInfoAction implements InfoAction {
  iconName = "paperclip";
  id = "attachments";
  order = 3000;

  constructor(
    @inject<AttachmentsService>("AttachmentsService")
    private readonly attachmentsService: AttachmentsService,
    @inject<CristalApp>("CristalApp") private readonly cristalApp: CristalApp,
  ) {}

  async counter(): Promise<Ref<number>> {
    return this.attachmentsService.count();
  }

  async refresh(page?: string): Promise<void> {
    await this.attachmentsService.refresh(
      page || this.cristalApp.getCurrentPage(),
    );
  }
}
