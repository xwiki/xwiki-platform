/*
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
import { type Container, injectable } from "inversify";
import type { SkinManager } from "@xwiki/cristal-api";
import type { App, Component } from "vue";

@injectable("Singleton")
export class DefaultSkinManager implements SkinManager {
  private designSystem: string = "xwiki";
  private readonly templates: Map<string, Component> = new Map<string, Component>();

  public static bind(container: Container): void {
    container.bind<SkinManager>("SkinManager").to(DefaultSkinManager).inSingletonScope();
  }

  constructor() {}

  public getTemplate(name: string): Component | null {
    return this.getDefaultTemplate(name);
  }

  public getDefaultTemplate(name: string): Component | null {
    try {
      return this.templates.get(name) as object;
    } catch (e) {
      console.error("Error loading default template ", name, e);
      return null;
    }
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  public loadDesignSystem(app: App, container: Container): void {
    // Do nothing for now.
  }

  public setDesignSystem(designSystem: string): void {
    this.designSystem = designSystem;
  }

  public getDesignSystem(): string {
    return this.designSystem;
  }
}
