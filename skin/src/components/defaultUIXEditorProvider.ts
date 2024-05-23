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
 * This file is part of the Cristal Wiki software prototype
 * @copyright  Copyright (c) 2023 XWiki SAS
 * @license    http://opensource.org/licenses/AGPL-3.0 AGPL-3.0
 *
 **/

import { UIXEditorProvider } from "../api/uixEditorProvider";
import { injectable } from "inversify";
import "reflect-metadata";
import DefaultUIXTemplateProvider from "./defaultUIXTemplateProvider";
import { CristalApp } from "@xwiki/cristal-api";

@injectable()
export default class DefaultUIXEditorProvider
  extends DefaultUIXTemplateProvider
  implements UIXEditorProvider
{
  public static cname = "cristal.editor.component";
  public static hint = "uix";
  public static priority = 1000;
  public static singleton = true;

  constructor() {
    super();
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  isSupported(cristal: CristalApp): boolean {
    return true;
  }
}
