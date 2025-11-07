/**
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

import { injectable } from "inversify";
import type { APITypes } from "../electron/preload/apiTypes";
import type { WikiConfig } from "@xwiki/cristal-api";
import type { BrowserApi } from "@xwiki/cristal-browser-api";

declare const browserElectron: APITypes;

/**
 * Remembers the current location in the locale storage, then reload the
 * browser.
 */
@injectable()
export class BrowserApiElectron implements BrowserApi {
  switchLocation(wikiConfig: WikiConfig): void {
    this.setLocation(wikiConfig);
    browserElectron.reloadBrowser();
  }

  reload(): void {
    browserElectron.reloadBrowser();
  }

  onClose(): void {
    // TODO: CRISTAL-429 we did not find a viable implementation to intercept the close action and display a confirm
    // dialog to ask for the user if the confirm closing the page.
  }

  setLocation(wikiConfig: WikiConfig): void {
    window.localStorage.setItem("currentApp", wikiConfig.name);
    browserElectron.setStorageRoot(wikiConfig.storageRoot);
  }
}
