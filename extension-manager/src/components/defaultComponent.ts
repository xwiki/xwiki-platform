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

import "reflect-metadata";
import { injectable } from "inversify";
import type { CristalComponent } from "../api/cristalComponent";

@injectable()
export class DefaultComponent implements CristalComponent {
  public name: string;
  public hint: string;
  public priority: number;
  public singleton: boolean;

  constructor() {
    this.name = "component.name";
    this.hint = "default";
    this.priority = 1000;
    this.singleton = false;
  }

  getName(): string {
    return this.name;
  }

  getHint(): string {
    return this.hint;
  }
  getPriority(): number {
    return this.priority;
  }
  isSingleton(): boolean {
    return this.singleton;
  }
}
