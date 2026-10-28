/**
 * See the NOTICE file distributed with this work for additional
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

import { inject } from "vue";
import type { ToolbarEnvironment, ToolbarSession, ToolbarState } from "./types";
import type { XWikiDocument } from "@xwiki/platform-document-xwiki";
import type { InjectionKey } from "vue";

const environmentKey: InjectionKey<ToolbarEnvironment> = Symbol("environment");
const formKey: InjectionKey<HTMLElement> = Symbol("form");
const documentKey: InjectionKey<XWikiDocument> = Symbol("document");
const sessionKey: InjectionKey<ToolbarSession> = Symbol("session");
const stateKey: InjectionKey<ToolbarState> = Symbol("state");
const toolbarKey: InjectionKey<HTMLElement> = Symbol("toolbar");

function injectStrict<T>(key: InjectionKey<T>): T {
  const value = inject(key);
  if (value === undefined) {
    throw new Error(`Missing ${key.description} for the realtime toolbar.`);
  }
  return value;
}

function useEnvironment(): ToolbarEnvironment {
  return injectStrict(environmentKey);
}

/**
 * @returns the edit form the toolbar belongs to
 */
function useForm(): HTMLElement {
  return injectStrict(formKey);
}

/**
 * @returns the document edited with the edit form
 */
function useDocument(): XWikiDocument {
  return injectStrict(documentKey);
}

function useSession(): ToolbarSession {
  return injectStrict(sessionKey);
}

function useState(): ToolbarState {
  return injectStrict(stateKey);
}

/**
 * @returns the toolbar element, that is placed inside the edit form (unlike the modals)
 */
function useToolbarElement(): HTMLElement {
  return injectStrict(toolbarKey);
}

export {
  documentKey,
  environmentKey,
  formKey,
  sessionKey,
  stateKey,
  toolbarKey,
  useDocument,
  useEnvironment,
  useForm,
  useSession,
  useState,
  useToolbarElement,
};
