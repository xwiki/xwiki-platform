import path from "node:path";
import { app, ipcMain } from "electron";
import fs from "node:fs";
import { PageData } from "@cristal/api";

const HOME_PATH = ".cristal";

function resolvePath(wikiName: string, id: string): string {
  const homedir = app.getPath("home");
  return path.join(homedir, HOME_PATH, wikiName, id + ".json");
}

function readPage(path: string): Promise<PageData> {
  return fs.promises
    .readFile(path)
    .then((pageContent: Buffer) => JSON.parse(pageContent.toString("utf8")));
}

export default function load() {
  console.log("inside load");
  ipcMain.handle("resolvePath", (event, { page, syntax }) => {
    return resolvePath(page, syntax);
  });
  ipcMain.handle("readPage", (event, { path }) => {
    return readPage(path);
  });
}
