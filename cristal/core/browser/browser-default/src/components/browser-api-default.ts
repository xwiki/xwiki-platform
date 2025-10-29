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
import { onBeforeMount, onBeforeUnmount } from "vue";
import type { WikiConfig } from "@xwiki/cristal-api";
import type { BrowserApi } from "@xwiki/cristal-browser-api";

/**
 * Default implementation for the browser. Set the window location, and the
 * browser reloads as a consequence
 */
@injectable()
export class BrowserApiDefault implements BrowserApi {
  switchLocation(wikiConfig: WikiConfig): void {
    window.location.href = `/${wikiConfig.name}/#/${wikiConfig.homePage}/`;
  }

  reload(): void {
    window.location.reload();
  }

  onClose(callback: () => boolean): void {
    const listener = (e: Event) => {
      e.preventDefault();
      return callback();
    };
    onBeforeMount(() => {
      window.addEventListener("beforeunload", listener);
    });
    onBeforeUnmount(() => {
      window.removeEventListener("beforeunload", listener);
    });
  }

  setLocation(): void {
    // At this point, we need to change the current URL on configuration
    // switching, but this operation is already handled during app loading for
    // the initial configuration (which is when this method is called).
  }
}
