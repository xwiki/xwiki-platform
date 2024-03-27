import { contextBridge, ipcRenderer } from "electron";
import { APITypes } from "./apiTypes";

const api: APITypes = {
  readPage: (path: string) => {
    return ipcRenderer.invoke("readPage", { path });
  },
  resolvePath: (page: string, syntax: string) => {
    return ipcRenderer.invoke("resolvePath", { page, syntax });
  },
};
contextBridge.exposeInMainWorld("fileSystemStorage", api);
