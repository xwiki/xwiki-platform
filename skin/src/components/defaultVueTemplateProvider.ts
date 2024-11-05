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

import { injectable } from "inversify";
import type { VueTemplateProvider } from "../api/vueTemplateProvider";
import "reflect-metadata";
import type { Component } from "vue";

@injectable()
export default class DefaultVueTemplateProvider implements VueTemplateProvider {
  public static cname = "cristal.vuejs.component";
  public static hint = "template";
  public static priority = 1000;
  public static singleton = true;

  getVueComponent(): Component {
    throw new Error("Method not implemented.");
  }

  getVueName(): string {
    throw new Error("Method not implemented.");
  }

  isGlobal(): boolean {
    return true;
  }

  registerComponents(): void {}
}
