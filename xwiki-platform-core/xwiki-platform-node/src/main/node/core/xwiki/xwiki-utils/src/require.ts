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

import { getRequire } from "./requirejs";

/**
 * Load RequireJS modules using a promise instead of a callback.
 *
 * @typeParam T - the type of the loaded module, or of the array of loaded modules
 * @param ids - the identifiers of the modules to load
 * @returns the loaded module when a single identifier is given, the array of loaded modules otherwise
 * @since 18.8.0RC1
 * @public
 */
function loadById<T = unknown>(...ids: string[]): Promise<T> {
  return new Promise((resolve, reject) => {
    getRequire()(
      ids,
      (...modules) => resolve((ids.length === 1 ? modules[0] : modules) as T),
      reject,
    );
  });
}

export { loadById };
