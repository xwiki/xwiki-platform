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

import type { SkinManager } from "./api/skinManager.js";
import type { CristalApp } from "./api/cristalApp.js";
import type { WikiConfig } from "./api/WikiConfig.js";
import type { Storage } from "./api/storage.js";
import type { PageData } from "./api/PageData.js";
import type { PageAttachment } from "./api/pageAttachment";
import type { Document } from "./api/document.js";
import type { Logger } from "./api/logger.js";
import type { LoggerConfig } from "./api/loggerConfig.js";
import {
  type DesignSystemLoader,
  registerAsyncComponent,
} from "./api/designSystemLoader.js";
import { DefaultPageData } from "./components/DefaultPageData.js";
import { DefaultWikiConfig } from "./components/defaultWikiConfig.js";
import { JSONLDDocument } from "./components/JSONLDDocument.js";
import { DefaultLogger } from "./components/defaultLogger.js";
import { DefaultLoggerConfig } from "./components/defaultLoggerConfig.js";
import type { WrappingStorage } from "./api/wrappingStorage.js";
import ComponentInit from "./components/componentsInit.js";

export type {
  SkinManager,
  CristalApp,
  WikiConfig,
  Storage,
  WrappingStorage,
  PageData,
  Logger,
  LoggerConfig,
  Document,
  DesignSystemLoader,
  PageAttachment,
};
export {
  ComponentInit,
  DefaultWikiConfig,
  DefaultPageData,
  DefaultLogger,
  DefaultLoggerConfig,
  JSONLDDocument,
  registerAsyncComponent,
};
