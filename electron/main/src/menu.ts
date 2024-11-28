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

import { BrowserWindow, Menu } from "electron";
import type { MenuItem } from "electron";

/**
 * Scan recursively the menus and sub-menus to find the interesting items.
 *
 * @param items - the item of the current menu
 * @param actions - the action we are interested to keep
 * @param newMenu - the new menu built from interesting actions
 * @since 0.13
 */
function scan(items: MenuItem[], actions: string[], newMenu: Menu): void {
  for (const item of items) {
    if (actions.includes(`${item.role}`)) {
      newMenu.insert(0, item);
    } else {
      if (item.submenu != undefined) {
        scan(item.submenu.items, actions, newMenu);
      }
    }
  }
}

/**
 * Initialize the menu for the provided browser window. The menu is hidden, and we only keep entries with useful
 * keyboard shortcuts.
 * @param browserWindow - the provided browser window.
 * @since 0.13
 */
function initializeMenu(browserWindow: BrowserWindow) {
  const menu = Menu.getApplicationMenu();
  const newMenu = new Menu();

  // The list of actions we wish to preserve from the default application menu.
  const actions = [
    "toggledevtools",
    "resetzoom",
    "zoomin",
    "zoomout",
    "togglefullscreen",
  ];
  if (menu?.items) {
    scan(menu?.items, actions, newMenu);
  }

  // Sets the menu on windows and linux.
  browserWindow.setMenu(newMenu);
  // Set the menu on MacOS.
  Menu.setApplicationMenu(menu);
  browserWindow.setMenuBarVisibility(false);
}

export { initializeMenu };
