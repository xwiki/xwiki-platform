import { contextBridge, ipcRenderer } from "electron";
import { APITypes } from "./apiTypes";
import { PageData } from "@cristal/api";

const api: APITypes = {
  readPage: (path: string) => {
    return ipcRenderer.invoke("readPage", { path });
  },
  resolvePath: (page: string, syntax: string) => {
    return ipcRenderer.invoke("resolvePath", { page: page || "", syntax });
  },
  savePage(path: string, content: string): Promise<PageData> {
    return ipcRenderer.invoke("savePage", { path, content });
  },
};
contextBridge.exposeInMainWorld("fileSystemStorage", api);
