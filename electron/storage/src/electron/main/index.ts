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

import { dirname, join, relative } from "node:path";
import { app, ipcMain, shell } from "electron";
import fs from "node:fs";
import { PageAttachment, PageData } from "@xwiki/cristal-api";

const HOME_PATH = ".cristal";
const HOME_PATH_FULL = join(app.getPath("home"), HOME_PATH);

function resolvePath(page: string, ...lastSegments: string[]) {
  const homedir = app.getPath("home");
  return join(homedir, HOME_PATH, page, ...lastSegments);
}

function resolvePagePath(page: string): string {
  return resolvePath(page, "page.json");
}

function resolveAttachmentsPath(page: string): string {
  return resolvePath(page, "attachments");
}
function resolveAttachmentPath(page: string, filename: string): string {
  return resolvePath(page, "attachments", filename);
}

async function isFile(path: string) {
  let stat = undefined;
  try {
    stat = await fs.promises.lstat(path);
  } catch (e) {
    console.debug(e);
  }
  return stat?.isFile();
}
async function isDirectory(path: string) {
  let stat = undefined;
  try {
    stat = await fs.promises.lstat(path);
  } catch (e) {
    console.debug(e);
  }
  return stat?.isDirectory();
}

async function pathExists(path: string) {
  try {
    await fs.promises.lstat(path);
  } catch {
    return false;
  }
  return true;
}

async function readPage(path: string): Promise<PageData | undefined> {
  if (!(await isWithin(HOME_PATH_FULL, path))) {
    throw new Error(`[${path}] is not in in [${HOME_PATH_FULL}]`);
  }
  if (await isFile(path)) {
    const pageContent = await fs.promises.readFile(path);
    return JSON.parse(pageContent.toString("utf8"));
  } else {
    return undefined;
  }
}

async function readAttachments(
  path: string,
): Promise<PageAttachment[] | undefined> {
  if (!(await isWithin(HOME_PATH_FULL, path))) {
    throw new Error(`[${path}] is not in in [${HOME_PATH_FULL}]`);
  }
  if (await isDirectory(path)) {
    const pageContent = await fs.promises.readdir(path);
    return pageContent.map((path) => {
      return {
        id: path,
        mimetype: "",
        reference: path,
        href: path,
      };
    });
  } else {
    return undefined;
  }
}

async function isWithin(root: string, path: string) {
  const rel = relative(root, path);
  return !rel.startsWith("../") && rel !== "..";
}

/**
 * Note: currently this is more a method to update the content of a page than
 * an actual page save.
 *
 * @param path - the path of the page on the file system
 * @param content - the content of the page
 * @param title - the title of the page
 * @since 0.8
 */
async function savePage(
  path: string,
  content: string,
  title: string,
): Promise<PageData | undefined> {
  if (!(await isWithin(HOME_PATH_FULL, path))) {
    throw new Error(`[${path}] is not in in [${HOME_PATH_FULL}]`);
  }
  let jsonContent: {
    source: string;
    name: string;
    syntax?: string;
  };
  if ((await pathExists(path)) && (await isFile(path))) {
    const fileContent = await fs.promises.readFile(path);
    const textDecoder = new TextDecoder("utf-8");
    jsonContent = JSON.parse(textDecoder.decode(fileContent));
    jsonContent.source = content;
    jsonContent.name = title;
  } else {
    jsonContent = {
      name: title,
      source: content,
      syntax: "markdown/1.2",
    };
  }

  const newJSON = JSON.stringify(jsonContent, null, 2);
  const parentDirectory = dirname(path);
  // Create the parent directories in case they do not exist.
  await fs.promises.mkdir(parentDirectory, { recursive: true });
  // Set the flag to w+ so that the file is created if it does not already
  // exist, or fully replaced when it does.
  await fs.promises.writeFile(path, newJSON, { flag: "w+" });
  return readPage(path);
}

async function saveAttachment(path: string, filePath: string) {
  if (!(await isWithin(HOME_PATH_FULL, path))) {
    throw new Error(`[${path}] is not in in [${HOME_PATH_FULL}]`);
  }
  const parentDirectory = dirname(path);

  // Create the parent directories in case they do not exist.
  await fs.promises.mkdir(parentDirectory, { recursive: true });
  await fs.promises.copyFile(filePath, path);
}

/**
 * Get the ids of the children pages for a given page.
 *
 * @param page - the id of the page
 * @returns a list of page ids
 * @since 0.10
 */
async function listChildren(page: string): Promise<Array<string>> {
  const folderPath = resolvePath(page).replace(/\/page.json$/, "");

  const children = [];
  const files = await fs.promises.readdir(folderPath);

  for (const file of files) {
    const path = `${folderPath}/${file}`;
    if (await isDirectory(path)) {
      children.push(file);
    }
  }

  return children;
}

/**
 * Delete a page.
 *
 * @param path - the path to the page to delete
 * @since 0.11
 */
async function deletePage(path: string): Promise<void> {
  await shell.trashItem(path.replace(/\/page.json$/, ""));
}

export default function load(): void {
  ipcMain.handle("resolvePath", (event, { page }) => {
    return resolvePagePath(page);
  });
  ipcMain.handle("resolveAttachmentsPath", (event, { page }) => {
    return resolveAttachmentsPath(page);
  });
  ipcMain.handle("readPage", (event, { path }) => {
    return readPage(path);
  });
  ipcMain.handle("readAttachments", (event, { path }) => {
    return readAttachments(path);
  });
  ipcMain.handle("savePage", (event, { path, content, title }) => {
    return savePage(path, content, title);
  });
  ipcMain.handle("resolveAttachmentPath", (event, { page, filename }) => {
    return resolveAttachmentPath(page, filename);
  });
  ipcMain.handle("saveAttachment", (event, { path, filePath }) => {
    return saveAttachment(path, filePath);
  });
  ipcMain.handle("listChildren", (event, { page }) => {
    return listChildren(page);
  });
  ipcMain.handle("deletePage", (event, { path }) => {
    return deletePage(path);
  });
}
