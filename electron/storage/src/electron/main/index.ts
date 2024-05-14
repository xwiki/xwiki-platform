import { dirname, join, relative } from "node:path";
import { app, ipcMain } from "electron";
import fs from "node:fs";
import { PageData } from "@cristal/api";

const HOME_PATH = ".cristal";
const HOME_PATH_FULL = join(app.getPath("home"), HOME_PATH);

function resolvePath(wikiName: string, id: string): string {
  // TODO: currently a mess, the wikiName is actually the page path
  // and the id is the syntax
  // We need to move to the syntax being saved as a property of the json,
  // and the wikiname being the path (or the actual wiki name, but in this case,
  // we also need to provide the actual page name).
  // We also need to decide if we define some sort of default page name (e.g., index).
  const homedir = app.getPath("home");
  let paths = wikiName;
  if (wikiName === "index") {
    paths = "";
  }
  return join(homedir, HOME_PATH, paths, id + ".json");
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

async function pathExists(path: string) {
  try {
    await fs.promises.lstat(path);
  } catch (e) {
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
async function isWithin(root: string, path: string) {
  const rel = relative(root, path);
  return !rel.startsWith("../") && rel !== "..";
}

/**
 * Note: currently this is more a method to update the content of a page than
 * an actual page save.
 *
 * @param path the path of the page on the file system
 * @param content the content of the page
 * @param title the title of the page
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
  // TODO: currently expects an existing page, need to handle new page creation.
  // TODO: first read the page, then update the html field, then save and return the updated version
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

export default function load() {
  ipcMain.handle("resolvePath", (event, { page, syntax }) => {
    return resolvePath(page, syntax);
  });
  ipcMain.handle("readPage", (event, { path }) => {
    return readPage(path);
  });
  ipcMain.handle("savePage", (event, { path, content, title }) => {
    return savePage(path, content, title);
  });
}
