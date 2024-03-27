/**
 * See the LICENSE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 * This file is part of the Cristal Wiki software prototypenx
 * @copyright  Copyright (c) 2023 XWiki SAS
 * @license    http://opensource.org/licenses/AGPL-3.0 AGPL-3.0
 *
 **/

import { DefaultVueTemplateProvider } from "@cristal/skin";
import type { Component } from "vue";
import { injectable } from "inversify";
import ProsemirrorEditor from "../vue/c-edit-prosemirror.vue";
import { CristalApp } from "@cristal/api";

@injectable()
export class UixProsemirrorEditorProvider extends DefaultVueTemplateProvider {
  public static cname = "cristal.editor.prosemirror";
  public static hint = "";
  public static priority = 1000;
  public static singleton = true;
  public static extensionPoint = "editor";

  registered = false;

  getVueComponent(): Component {
    ProsemirrorEditor.editorname = "prosemirror";
    return ProsemirrorEditor;
  }
  getVueName(): string {
    return "ProsemirrorEditor";
  }
  isGlobal(): boolean {
    return false;
  }
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  isSupported(cristal: CristalApp): boolean {
    return true;
  }
}
