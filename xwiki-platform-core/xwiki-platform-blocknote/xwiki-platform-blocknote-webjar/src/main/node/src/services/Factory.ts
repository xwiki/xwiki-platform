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
import { Logic } from "./Logic";

/**
 * Factory class to create and manage BlockNote instances.
 */
export class Factory {
  /**
   * Maps BlockNote instances to their host elements.
   */
  private readonly instancesByHost: WeakMap<HTMLElement, Logic> = new WeakMap();

  /**
   * Map BlockNote instances to their form field names.
   */
  private readonly instancesByName: Map<string, Logic> = new Map();

  /**
   * Creates (if it doesn't exist yet) and returns the BlockNote instance associated with the given host element.
   *
   * @param element - the HTML Element that will host the created BlockNote editor instance
   * @returns a promise that resolves to the BlockNote instance associated with the given host element
   */
  async create(element: HTMLElement) {
    let logic = this.instancesByHost.get(element);
    if (!logic) {
      logic = new Logic(element);
      this.instancesByHost.set(element, logic);
      if (logic.name) {
        this.instancesByName.set(logic.name, logic);
      }
    }

    return await logic.ready;
  }

  /**
   * @param hostOrName - the name of a form field or the host element for which to get the BlockNote instance
   * @returns the BlockNote instance associated with the given host element or form field name
   */
  get(hostOrName: string | HTMLElement): Logic | undefined {
    if (typeof hostOrName === "string") {
      return this.instancesByName.get(hostOrName);
    } else {
      return this.instancesByHost.get(hostOrName);
    }
  }

  /**
   * @returns an array of all BlockNote editor instances created by this factory
   */
  getAll(): Logic[] {
    return Array.from(this.instancesByName.values());
  }

  /**
   * Destroys the BlockNote instance associated with the given host element or form field name.
   *
   * @param hostOrName - the name of a form field or the host element for which to destroy the BlockNote instance
   * @returns true if the BlockNote instance was destroyed, false otherwise
   */
  destroy(hostOrName: string | HTMLElement): boolean {
    const instance = this.get(hostOrName);
    if (instance) {
      instance.destroy();
      if (instance.name) {
        this.instancesByName.delete(instance.name);
      }
      this.instancesByHost.delete(instance.host);
      return true;
    }
    return false;
  }

  /**
   * @returns the syntax used by BlockNote
   */
  get syntax(): { type: string; version: string } {
    return {
      type: "uniast",
      version: "1.0",
    };
  }
}
