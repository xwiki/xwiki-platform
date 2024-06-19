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

import { BrowserApi } from "@xwiki/cristal-browser-api";
import { injectable } from "inversify";
import { APITypes } from "../electron/preload/apiTypes";
import { WikiConfig } from "@xwiki/cristal-api";

declare const browserElectron: APITypes;

/**
 * Remembers the current location in the locale storage, then reload the
 * browser.
 */
@injectable()
export class BrowserApiElectron implements BrowserApi {
  switchLocation(wikiConfig: WikiConfig) {
    window.localStorage.setItem("currentApp", wikiConfig.name);
    browserElectron.reloadBrowser();
  }
}
