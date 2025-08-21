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

import { getStorageRoot } from "@xwiki/cristal-electron-state";
import { LinkType } from "@xwiki/cristal-link-suggest-api";
import { EntityType } from "@xwiki/cristal-model-api";
import { protocol as cristalFSProtocol } from "@xwiki/cristal-model-remote-url-filesystem-api";
import {
  DefaultPageReader,
  DefaultPageWriter,
} from "@xwiki/cristal-page-default";
import { app, ipcMain, net, protocol, shell } from "electron";
import mime from "mime";
import fs from "node:fs";
import { readdir } from "node:fs/promises";
import os from "node:os";
import { basename, dirname, join, relative } from "node:path";
import type { PageAttachment, PageData } from "@xwiki/cristal-api";

const HOME_PATH = ".cristal";
const HOME_PATH_FULL = join(app.getPath("home"), HOME_PATH);

function getHomePathFull(): string {
  // If no custom storage root was provided in the current configuration, we
  // fallback to the folder `${HOME}/.cristal`.
  return getStorageRoot() ?? HOME_PATH_FULL;
}

function resolvePath(page: string, ...lastSegments: string[]) {
  return join(getHomePathFull(), page, ...lastSegments);
}

function resolvePagePath(page: string): string {
  return resolvePath(`${page}.md`);
}

function makeLastSegmentHidden(page: string) {
  const segments = page.split("/");
  // Add a dot after the last segment of the page path
  if (segments.length == 1) {
    return `.${page}`;
  } else {
    const joined = segments.slice(0, segments.length - 1).join("/");
    return `${joined}/.${segments[segments.length - 1]}`;
  }
}

function resolveMetaDirectoryPath(page: string) {
  return resolvePath(makeLastSegmentHidden(page));
}

function resolveAttachmentsPath(page: string): string {
  return join(resolveMetaDirectoryPath(page), "attachments");
}

function resolveAttachmentPath(page: string, filename: string): string {
  return join(resolveAttachmentsPath(page), filename);
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

async function isPage(pathFull: string) {
  if (!(await isFile(pathFull))) {
    return false;
  }

  const homePathFull = getHomePathFull();
  const path = relative(homePathFull, pathFull);

  const parentPath = dirname(path);
  const grandParentPath = dirname(parentPath);
  const parentName = basename(parentPath);
  const grandParentName = basename(grandParentPath);

  return (
    parentName != "attachments" &&
    (!grandParentName.startsWith(".") ||
      parentName === "." ||
      grandParentName === ".")
  );
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

async function isEmpty(path: string) {
  const readdir = await fs.promises.readdir(path);
  return readdir.length == 0;
}

async function pathExists(path: string) {
  try {
    await fs.promises.lstat(path);
  } catch {
    return false;
  }
  return true;
}

// eslint-disable-next-line max-statements
async function readPage(
  path: string,
): Promise<{ type: EntityType.DOCUMENT; value: PageData } | undefined> {
  const homePathFull = getHomePathFull();
  if (!(await isWithin(homePathFull, path))) {
    throw new Error(`[${path}] is not in in [${homePathFull}]`);
  }
  if (await isFile(path)) {
    const pageContent = await fs.promises.readFile(path);
    const pageStats = await fs.promises.stat(path);
    const content = pageContent.toString("utf8");

    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const data = new DefaultPageReader().readPage(content) as any;

    const id = relative(homePathFull, path).replace(/.md$/, "");
    return {
      type: EntityType.DOCUMENT,
      value: {
        ...data,
        source: data.content,
        syntax: "markdown/1.2",
        lastAuthor: { name: os.userInfo().username },
        lastModificationDate: new Date(pageStats.mtimeMs),
        id: id,
        canEdit: true,
      },
    };
  } else {
    return undefined;
  }
}

async function readAttachments(
  path: string,
): Promise<PageAttachment[] | undefined> {
  const homePathFull = getHomePathFull();
  if (!(await isWithin(homePathFull, path))) {
    throw new Error(`[${path}] is not in in [${homePathFull}]`);
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

// eslint-disable-next-line max-statements
async function readAttachment(
  path: string,
): Promise<{ type: EntityType.ATTACHMENT; value: PageAttachment } | undefined> {
  const homePathFull = getHomePathFull();
  if (!(await isWithin(homePathFull, path))) {
    throw new Error(`[${path}] is not in in [${homePathFull}]`);
  }

  if (await isFile(path)) {
    const stats = await fs.promises.stat(path);
    const mimetype = mime.getType(path) ?? "";
    const grandParentPath = dirname(dirname(path)); // .something
    const grandParentName = basename(grandParentPath);
    const grandGrandParentPath = dirname(grandParentPath); // space

    const id = basename(path);
    return {
      type: EntityType.ATTACHMENT,
      value: {
        id: id,
        mimetype,
        reference: relative(
          homePathFull,
          join(
            grandGrandParentPath,
            grandParentName.slice(1),
            "attachments",
            id,
          ),
        ),
        href: `${cristalFSProtocol}://${relative(
          homePathFull,
          join(grandGrandParentPath, grandParentName, "attachments", id),
        )}`,
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
  const homePathFull = getHomePathFull();
  if (!(await isWithin(homePathFull, path))) {
    throw new Error(`[${path}] is not in in [${homePathFull}]`);
  }
  let page: { content: string; [key: string]: unknown };
  if ((await pathExists(path)) && (await isFile(path))) {
    const fileContent = await fs.promises.readFile(path);
    const textDecoder = new TextDecoder("utf-8");
    page = new DefaultPageReader().readPage(textDecoder.decode(fileContent));
    page.content = content;
    page.name = title;
  } else {
    page = {
      name: title,
      content: content,
    };
  }

  const newContent = new DefaultPageWriter().writePage(page);
  const parentDirectory = dirname(path);
  // Create the parent directories in case they do not exist.
  await fs.promises.mkdir(parentDirectory, { recursive: true });
  // Set the flag to w+ so that the file is created if it does not already
  // exist, or fully replaced when it does.
  await fs.promises.writeFile(path, newContent, { flag: "w+" });
  return (await readPage(path))?.value;
}

async function saveAttachment(path: string, filePath: string) {
  const homePathFull = getHomePathFull();
  if (!(await isWithin(homePathFull, path))) {
    throw new Error(`[${path}] is not in in [${homePathFull}]`);
  }
  const parentDirectory = dirname(path);

  // Create the parent directories in case they do not exist.
  await fs.promises.mkdir(parentDirectory, { recursive: true });
  await fs.promises.copyFile(filePath, path);
}

function removeExtension(file: string): string {
  if (!file.includes(".")) {
    return file;
  }
  return file.slice(0, file.lastIndexOf("."));
}

async function listSingleChild(folderPath: string, children: Set<string>) {
  const files = await fs.promises.readdir(folderPath);

  for (const file of files) {
    // Ignore all hidden files.
    if (file.startsWith(".")) {
      continue;
    }
    const path = join(folderPath, file);
    if (await isFile(path)) {
      children.add(removeExtension(file));
    } else if (await isDirectory(path)) {
      children.add(file);
    }
  }
}

/**
 * Get the ids of the children pages for a given page.
 *
 * @param page - the id of the page
 * @returns a list of page ids
 * @since 0.10
 */
async function listChildren(page: string): Promise<Array<string>> {
  const folderPath = resolvePath(page);

  const children = new Set<string>();
  if (await isDirectory(folderPath)) {
    await listSingleChild(folderPath, children);
  }

  return Array.from(children);
}

/**
 * Delete a page and its attachments.
 *
 * @param reference - the reference of the page to delete
 * @since 0.19
 */
async function deletePage(reference: string): Promise<void> {
  // Remove the page and its attachments
  const values = [];

  const path = `${resolvePath(reference)}.md`;
  if (await isFile(path)) {
    values.push(shell.trashItem(path));
  }
  const metaDirectoryPath = resolveMetaDirectoryPath(reference);
  if (await isDirectory(metaDirectoryPath)) {
    values.push(shell.trashItem(metaDirectoryPath));
  }

  await Promise.all(values);
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
  const homePathFull = getHomePathFull();
  const attachments = (await readdir(homePathFull, { recursive: true })).map(
    (it) => join(homePathFull, it),
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

/**
 * Initialize the wiki with a minimal content.
 *
 * @since 0.14
 */
async function createMinimalContent() {
  await savePage(
    join(getHomePathFull(), "index.md"),
    "# Welcome\n" +
      "\n" +
      "This is a new **Cristal** wiki.\n" +
      "\n" +
      "You can use it to take your *own* notes.\n" +
      "\n" +
      "You can also create new [[pages|index/newpage]].\n" +
      "\n" +
      "Enjoy!",
    "",
  );
}

async function movePageSubSpaces(
  reference: string,
  newReference: string,
  actions: Promise<void>[],
) {
  const pathSpace = `${resolvePath(reference)}`;
  const newSpacePath = `${resolvePath(newReference)}`;

  if (await isDirectory(pathSpace)) {
    actions.push(fs.promises.rename(pathSpace, newSpacePath));
  }
}

// eslint-disable-next-line max-statements
async function movePage(
  reference: string,
  newReference: string,
  preserveChildren: boolean,
): Promise<void> {
  const path = `${resolvePath(reference)}.md`;
  const metaPath = resolveMetaDirectoryPath(reference);
  const newPath = `${resolvePath(newReference)}.md`;
  const newMetaPath = resolveMetaDirectoryPath(newReference);

  const actions = [];

  if (await isFile(path)) {
    actions.push(fs.promises.rename(path, newPath));
  }

  if (await isDirectory(metaPath)) {
    actions.push(fs.promises.rename(metaPath, newMetaPath));
  }

  if (preserveChildren) {
    await movePageSubSpaces(reference, newReference, actions);
  }

  await Promise.all(actions);
}

// TODO: reduce the number of statements in the following method and reactivate the disabled eslint rule.
// eslint-disable-next-line max-statements
export default async function load(): Promise<void> {
  ipcMain.on("initRootDirectory", async () => {
    // Check if the root directory does not exist, or exists and is empty.
    // If that's the case, create it and populate it with a default minimal content.
    const homePathFull = getHomePathFull();
    if (
      !(await pathExists(homePathFull)) ||
      ((await isDirectory(homePathFull)) && (await isEmpty(homePathFull)))
    ) {
      await createMinimalContent();
    }
  });

  protocol.handle(cristalFSProtocol, async (request) => {
    const homePathFull = getHomePathFull();
    const path = join(
      homePathFull,
      request.url.substring(`${cristalFSProtocol}://`.length),
    );
    if (!(await isWithin(homePathFull, path))) {
      throw new Error(`[${path}] is not in in [${homePathFull}]`);
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
  ipcMain.handle("deletePage", (event, { path: reference }) => {
    return deletePage(reference);
  });
  ipcMain.handle("search", (event, { query, type, mimetype }) => {
    return search(query, type, mimetype);
  });
  ipcMain.handle(
    "movePage",
    (event, { reference, newReference, preserveChildren }) => {
      return movePage(reference, newReference, preserveChildren);
    },
  );
}
