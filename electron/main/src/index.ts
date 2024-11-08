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

import { restoreOrCreateWindow } from "./mainWindow";
import { loadFile } from "./reload";
import "./security-restrictions";
// @ts-expect-error shouldn't happen, but we need to generate the types for the whole project once.
import loadBrowser from "@xwiki/cristal-browser-electron/main";
import { load as loadAuthentication } from "@xwiki/cristal-electron-authentication-xwiki-main";
// @ts-expect-error shouldn't happen, but we need to generate the types for the whole project once.
import load from "@xwiki/cristal-electron-storage/main";
import { BrowserWindow, app } from "electron";
import { platform } from "node:process";

/**
 * Prevent electron from running multiple instances.
 */
const isSingleInstance = app.requestSingleInstanceLock();
if (!isSingleInstance) {
  app.quit();
  process.exit(0);
}
app.on("second-instance", restoreOrCreateWindow);

/**
 * Disable Hardware Acceleration to save more system resources.
 */
app.disableHardwareAcceleration();

/**
 * Shout down background process if all windows was closed
 */
app.on("window-all-closed", () => {
  if (platform !== "darwin") {
    app.quit();
  }
});

/**
 * Create the application window when the background process is ready.
 */
app
  .whenReady()
  .then(() => {
    load();
    restoreOrCreateWindow().then((w) => {
      loadBrowser(w);
      loadAuthentication(w, loadFile);
    });
    /**
     * @see https://www.electronjs.org/docs/latest/api/app#event-activate-macos Event: 'activate'.
     */
    app.on("activate", () => {
      if (BrowserWindow.getAllWindows().length === 0) {
        restoreOrCreateWindow();
      }
    });
  })
  .catch((e) => console.error("Failed create window:", e));

/**
 * Install Vue.js or any other extension in development mode only.
 * Note: You must install `electron-devtools-installer` manually
 */
// if (import.meta.env.DEV) {
//   app
//     .whenReady()
//     .then(() => import('electron-devtools-installer'))
//     .then(module => {
//       const {default: installExtension, VUEJS3_DEVTOOLS} =
//         // @ts-expect-error Hotfix for https://github.com/cawa-93/vite-electron-builder/issues/915
//         typeof module.default === 'function' ? module : (module.default as typeof module);
//
//       return installExtension(VUEJS3_DEVTOOLS, {
//         loadExtensionOptions: {
//           allowFileAccess: true,
//         },
//       });
//     })
//     .catch(e => console.error('Failed install extension:', e));
// }

/**
 * Check for app updates, install it in background and notify user that new version was installed.
 * No reason run this in non-production build.
 * @see https://www.electron.build/auto-update.html#quick-setup-guide
 *
 * Note: It may throw "ENOENT: no such file app-update.yml"
 * if you compile production app without publishing it to distribution server.
 * Like `npm run compile` does. It's ok 😅
 */
if (import.meta.env.PROD) {
  app
    .whenReady()
    .then(() =>
      /**
       * Here we forced to use `require` since electron doesn't fully support dynamic import in asar archives
       * @see https://github.com/electron/electron/issues/38829
       * Potentially it may be fixed by this https://github.com/electron/electron/pull/37535
       */
      // eslint-disable-next-line @typescript-eslint/no-require-imports
      require("electron-updater").autoUpdater.checkForUpdatesAndNotify(),
    )
    .catch((e) => console.error("Failed check and install updates:", e));
}
