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
import { Logic } from "@/services/Logic";

/**
 * Factory class to create and manage BlockNote instances.
 */
export class Factory {
  constructor() {
    // Maps BlockNote instances to their host elements.
    this._instancesByHost = new WeakMap();

    // Map BlockNote instances to their form field names.
    this._instancesByName = new Map();
  }

  /**
   * Creates (if it doesn't exist yet) and returns the BlockNote instance associated with the given host element.
   *
   * @param {HTMLElement} element the HTML Element that will host the created BlockNote editor instance
   * @returns {Promise<Logic>} a promise that resolves to the BlockNote instance associated with the given host element
   */
  async create(element, ...args) {
    if (!this._instancesByHost.has(element)) {
      const logic = new Logic(element, ...args);
      this._instancesByHost.set(element, logic);
      if (logic.name) {
        this._instancesByName.set(logic.name, logic);
      }
    }

    return await this._instancesByHost.get(element).ready;
  }

  /**
   * @param {String | HTMLElement} hostOrName the name of a form field or the host element for which to get the
   *          BlockNote instance
   * @returns {Logic} the BlockNote instance associated with the given host element or form field name
   */
  get(hostOrName) {
    if (typeof hostOrName === "string") {
      return this._instancesByName.get(hostOrName);
    } else {
      return this._instancesByHost.get(hostOrName);
    }
  }

  /**
   * Destroys the BlockNote instance associated with the given host element or form field name.
   *
   * @param {String | HTMLElement} hostOrName the name of a form field or the host element for which to destroy the
   *          BlockNote instance
   * @return {boolean} true if the BlockNote instance was destroyed, false otherwise
   */
  destroy(hostOrName) {
    const instance = this.get(hostOrName);
    if (instance) {
      instance.destroy();
      this._instancesByName.delete(instance.name);
      this._instancesByHost.delete(instance.host);
      return true;
    }
    return false;
  }

  /**
   * @returns {Object} the syntax used by BlockNote
   */
  get syntax() {
    return {
      type: "markdown",
      version: "1.2",
    };
  }
}
