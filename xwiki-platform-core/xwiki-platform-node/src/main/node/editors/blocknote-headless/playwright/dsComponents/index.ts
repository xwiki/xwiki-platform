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

// The design system's components (e.g. <x-text-field>) are provided at runtime by whichever
// application embeds the editor (they are only typed, not implemented, in `@xwiki/platform-dsapi`).
// These implementations are built against that same typed contract so Playwright component tests
// can interact with real form elements without pulling in a full design system package.

import XBtn from "./XBtn.vue";
import XCheckbox from "./XCheckbox.vue";
import XSelect from "./XSelect.vue";
import XTextField from "./XTextField.vue";
import type { App } from "vue";

export function registerDsComponents(app: App): void {
  app.component("XTextField", XTextField);
  app.component("XSelect", XSelect);
  app.component("XBtn", XBtn);
  app.component("XCheckbox", XCheckbox);
}
