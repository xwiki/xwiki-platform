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

import { PageAttachment, PageData } from "@xwiki/cristal-api";
import { LinkType } from "@xwiki/cristal-link-suggest-api";
import { EntityType } from "@xwiki/cristal-model-api";
import { protocol as cristalFSProtocol } from "@xwiki/cristal-model-remote-url-filesystem-api";
import { app, ipcMain, net, protocol, shell } from "electron";
import mime from "mime";
import fs from "node:fs";
import { readdir } from "node:fs/promises";
import os from "node:os";
import { basename, dirname, join, relative } from "node:path";

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

function hasMimetype(path: string, mimetype: string) {
  const mimetypeFile = mime.getType(path);
  if (!mimetypeFile) {
    return false;
  }

  const [p1, p2] = mimetypeFile.split("/");
  const [q1, q2] = mimetype.split("/");
  return (q1 == "*" || q1 == p1) && (q2 == "*" || q2 == p2);
}

async function isAttachment(path: string, mimetype?: string) {
  if (!(await isFile(path))) {
    return false;
  }

  const isInAttachmentsDirectory = basename(dirname(path)) == "attachments";
  if (isInAttachmentsDirectory && mimetype) {
    return hasMimetype(path, mimetype);
  }
  return isInAttachmentsDirectory;
}

async function isPage(path: string) {
  if (!(await isFile(path))) {
    return false;
  }

  return basename(path) == "page.json";
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

async function readPage(
  path: string,
): Promise<{ type: EntityType.DOCUMENT; value: PageData } | undefined> {
  if (!(await isWithin(HOME_PATH_FULL, path))) {
    throw new Error(`[${path}] is not in in [${HOME_PATH_FULL}]`);
  }
  if (await isFile(path)) {
    const pageContent = await fs.promises.readFile(path);
    const pageStats = await fs.promises.stat(path);
    const parse = JSON.parse(pageContent.toString("utf8"));
    if (!parse.name) {
      // Fallback to the current directory name if the name is not explicitly defined.
      parse.name = basename(dirname(path));
    }
    return {
      type: EntityType.DOCUMENT,
      value: {
        ...parse,
        lastAuthor: { name: os.userInfo().username },
        lastModificationDate: new Date(pageStats.mtimeMs),
        id: relative(HOME_PATH_FULL, dirname(path)),
      },
    };
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
    const attachments = await fs.promises.readdir(path);
    return (
      await Promise.all(
        attachments.map((attachment) => readAttachment(join(path, attachment))),
      )
    )
      .filter((it) => it !== undefined)
      .map((it) => it.value);
  } else {
    return undefined;
  }
}

async function readAttachment(
  path: string,
): Promise<{ type: EntityType.ATTACHMENT; value: PageAttachment } | undefined> {
  if (!(await isWithin(HOME_PATH_FULL, path))) {
    throw new Error(`[${path}] is not in in [${HOME_PATH_FULL}]`);
  }

  if (await isFile(path)) {
    const stats = await fs.promises.stat(path);
    const mimetype = mime.getType(path) || "";
    return {
      type: EntityType.ATTACHMENT,
      value: {
        id: basename(path),
        mimetype,
        reference: relative(HOME_PATH_FULL, path),
        href: `${cristalFSProtocol}://${relative(HOME_PATH_FULL, path)}`,
        date: stats.mtime,
        size: stats.size,
        author: undefined,
      },
    };
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
// TODO: reduce the number of statements in the following method and reactivate the disabled eslint rule.
// eslint-disable-next-line max-statements
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
  return (await readPage(path))?.value;
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
  if (await isDirectory(folderPath)) {
    const files = await fs.promises.readdir(folderPath);

    for (const file of files) {
      const path = `${folderPath}/${file}`;
      if (await isDirectory(path)) {
        children.push(file);
      }
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

async function asyncFilter<T>(arr: T[], predicate: (i: T) => Promise<boolean>) {
  // First compute the async result for each element
  const results = await Promise.all(arr.map(predicate));

  // The filter out on the async results, retrieving the async results by index
  return arr.filter((_v, index) => results[index]);
}

async function search(
  query: string,
  type?: LinkType,
  mimetype?: string,
): Promise<
  (
    | { type: EntityType.ATTACHMENT; value: PageAttachment }
    | { type: EntityType.DOCUMENT; value: PageData }
  )[]
> {
  const attachments = (await readdir(HOME_PATH_FULL, { recursive: true })).map(
    (it) => join(HOME_PATH_FULL, it),
  );
  const allEntities = await asyncFilter(attachments, async (path: string) => {
    if (type == LinkType.ATTACHMENT) {
      return isAttachment(path, mimetype);
    } else if (type == LinkType.PAGE) {
      return isPage(path);
    } else {
      return (await isAttachment(path, mimetype)) || (await isPage(path)); // TODO: filter out
    }
  });

  const searchedEntities = await asyncFilter(allEntities, async (it) => {
    if (await isAttachment(it)) {
      return basename(it).includes(query);
    } else if (await isPage(it)) {
      return basename(dirname(it)).includes(query);
    } else {
      return false;
    }
  });
  return (
    await Promise.all(
      searchedEntities.map(async (it) => {
        if (await isPage(it)) {
          return readPage(it);
        } else {
          return readAttachment(it);
        }
      }),
    )
  ).filter((it) => it !== undefined);
}

// TODO: reduce the number of statements in the following method and reactivate the disabled eslint rule.
// eslint-disable-next-line max-statements
export default function load(): void {
  protocol.handle(cristalFSProtocol, async (request) => {
    const path = join(
      HOME_PATH_FULL,
      request.url.substring(`${cristalFSProtocol}://`.length),
    );
    if (!(await isWithin(HOME_PATH_FULL, path))) {
      throw new Error(`[${path}] is not in in [${HOME_PATH_FULL}]`);
    }
    return net.fetch(`file://${path}`);
  });

  ipcMain.handle("resolvePath", (event, { page }) => {
    return resolvePagePath(page);
  });
  ipcMain.handle("resolveAttachmentsPath", (event, { page }) => {
    return resolveAttachmentsPath(page);
  });
  ipcMain.handle("readPage", async (event, { path }) => {
    return (await readPage(path))?.value;
  });
  ipcMain.handle("readAttachments", (event, { path }) => {
    return readAttachments(path);
  });
  ipcMain.handle("readAttachment", async (event, { path }) => {
    return (await readAttachment(path))?.value;
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
  ipcMain.handle("search", (event, { query, type, mimetype }) => {
    return search(query, type, mimetype);
  });
}
