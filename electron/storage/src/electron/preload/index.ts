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

import { APITypes } from "./apiTypes";
import { PageAttachment, PageData } from "@xwiki/cristal-api";
import { contextBridge, ipcRenderer, webUtils } from "electron";

const api: APITypes = {
  readPage: (path: string) => {
    return ipcRenderer.invoke("readPage", { path });
  },
  readAttachments(path: string): Promise<PageAttachment[]> {
    return ipcRenderer.invoke("readAttachments", { path });
  },
  readAttachment(path: string): Promise<PageAttachment> {
    return ipcRenderer.invoke("readAttachment", { path });
  },
  resolvePath: (page: string) => {
    return ipcRenderer.invoke("resolvePath", { page: page || "index" });
  },
  resolveAttachmentsPath: (page: string) => {
    return ipcRenderer.invoke("resolveAttachmentsPath", {
      page: page || "index",
    });
  },
  savePage(path: string, content: string, title: string): Promise<PageData> {
    return ipcRenderer.invoke("savePage", { path, content, title });
  },
  resolveAttachmentPath(page: string, filename: string): Promise<string> {
    return ipcRenderer.invoke("resolveAttachmentPath", {
      page: page || "index",
      filename,
    });
  },
  saveAttachment(path: string, file: File): Promise<PageData> {
    const filePath = webUtils.getPathForFile(file);
    return ipcRenderer.invoke("saveAttachment", { path, filePath: filePath });
  },
  listChildren(page: string): Promise<Array<string>> {
    return ipcRenderer.invoke("listChildren", { page });
  },
  deletePage(path: string): Promise<void> {
    return ipcRenderer.invoke("deletePage", { path });
  },
};
contextBridge.exposeInMainWorld("fileSystemStorage", api);
