import path from "node:path";
import { app, ipcMain } from "electron";
import fs from "node:fs";
import { PageData } from "@cristal/api";

const HOME_PATH = ".cristal";

function resolvePath(wikiName: string, id: string): string {
  // TODO: currently a mess, the wikiName is actually the page path
  // and the id is the syntax
  // We need to move to the syntax being saved as a property of the json,
  // and the wikiname being the path (or the actual wiki name, but in this case,
  // we also need to provide the actual page name).
  // We also need to decide if we define some sort of default page name (e.g., index).
  const homedir = app.getPath("home");
  console.trace("resolvePath", "wikiName", wikiName, "id", id);
  let paths = wikiName;
  if (wikiName === "index") {
    paths = "";
  }
  return path.join(homedir, HOME_PATH, paths, id + ".json");
}

async function readPage(path: string): Promise<PageData> {
  const pageContent = await fs.promises.readFile(path);
  return JSON.parse(pageContent.toString("utf8"));
}

async function savePage(path: string, content: string): Promise<PageData> {
  // TODO: currently expects an existing page, need to handle new page creation.
  // TODO: first read the page, then update the html field, then save and return the updated version
  const fileContent = await fs.promises.readFile(path);
  const textDecoder = new TextDecoder("utf-8");
  const jsonContent: { source?: string } = JSON.parse(
    textDecoder.decode(fileContent),
  );
  jsonContent.source = content;
  const newJSON = JSON.stringify(jsonContent, null, 2);
  console.log("neJSON", newJSON);
  console.log("path", path);
  await fs.promises.writeFile(path, newJSON);
  return readPage(path);
}

export default function load() {
  console.log("inside load");
  ipcMain.handle("resolvePath", (event, { page, syntax }) => {
    return resolvePath(page, syntax);
  });
  ipcMain.handle("readPage", (event, { path }) => {
    return readPage(path);
  });
  ipcMain.handle("savePage", (event, { path, content }) => {
    return savePage(path, content);
  });
}
