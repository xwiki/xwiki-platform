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

import { DefaultWikiConfig } from "@xwiki/cristal-api";
import { inject, injectable, named } from "inversify";
import type { CristalApp, Logger, Storage } from "@xwiki/cristal-api";

/**
 * Configuration instance for the Nextcloud backend.
 *
 * @since 0.9
 */
@injectable()
export class NextcloudWikiConfig extends DefaultWikiConfig {
  override storage: Storage;
  override cristal: CristalApp;

  constructor(
    @inject<Logger>("Logger") logger: Logger,
    @inject("Storage") @named("Nextcloud") storage: Storage,
    @inject("CristalApp") cristal: CristalApp,
  ) {
    super(logger);
    this.cristal = cristal;
    this.storage = storage;
    this.storage.setWikiConfig(this);
  }

  override isSupported(format: string): boolean {
    return format == "html";
  }

  override defaultPageName(): string {
    return "home";
  }

  override getType(): string {
    return "Nextcloud";
  }
}
