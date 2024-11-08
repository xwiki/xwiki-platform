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

import DefaultUIXTemplateProvider from "./defaultUIXTemplateProvider";
import { UIXEditorProvider } from "../api/uixEditorProvider";
import { CristalApp } from "@xwiki/cristal-api";
import { injectable } from "inversify";
import "reflect-metadata";

@injectable()
export default class DefaultUIXEditorProvider
  extends DefaultUIXTemplateProvider
  implements UIXEditorProvider
{
  public static override cname = "cristal.editor.component";
  public static override hint = "uix";
  public static override priority = 1000;
  public static override singleton = true;

  constructor() {
    super();
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  isSupported(cristal: CristalApp): boolean {
    return true;
  }
}
