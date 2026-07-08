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

// This file is required as it is the base import for Playwright

import { registerDsComponentsMock } from "./dsComponentsMock";
import { beforeMount } from "@playwright/experimental-ct-vue/hooks";
import { createI18n } from "vue-i18n";

// Components under test call useI18n(), which requires the vue-i18n plugin to
// be installed on the app, even when each component then provides its own
// local messages.
beforeMount(async ({ app }) => {
  app.use(createI18n({ legacy: false, locale: "en", messages: {} }));

  // The link modal renders design system components (e.g. <x-text-field>) that are only provided
  // at runtime by the application embedding the editor. Register stand-ins so tests can interact
  // with them.
  registerDsComponentsMock(app);
});
