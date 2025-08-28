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
import { reactive } from "vue";

const store = {};

const componentStore = {

  register(kind, key, componentProvider) {
    if (!store[kind]) {
      store[kind] = {};
    }
    store[kind][key] = componentProvider;
  },

  async load(kind, key) {
    if (!store[kind] || !store[kind][key]) {
      return undefined;
    }

    // On the first access, we resolve the component provider and save its result instead.
    // On the next access, the cached result is returned without a lookup.
    // This is fine since the result of the componentProvider function is expected to be stable.
    if(typeof store[kind][key] === "function") {
      store[kind][key] = await store[kind][key]()
    }

    return store[kind][key];
  },
};

export { componentStore };
