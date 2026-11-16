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

/**
 * Wrap the Live Data logic so that nothing can be edited through it. Layouts provide this logic to the displayers of
 * the values that must be read-only, which prevents them from offering the edit action or switching to their editor.
 *
 * @param logic - the Live Data logic
 * @returns a logic behaving like the given one, except that no entry or property is editable
 */
function createReadOnlyLogic(logic) {
  return new Proxy(logic, {
    get(target, property) {
      if (property === "isEditable") {
        return () => false;
      }
      const value = Reflect.get(target, property, target);
      // Call the methods on the actual logic, so that they keep working with its internal state.
      return typeof value === "function" ? value.bind(target) : value;
    },
  });
}

export { createReadOnlyLogic };
