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
 * Return the id of the property displayed by the List layout.
 *
 * The List layout displays a single property per entry: the first visible property, following the order of the
 * properties configured for the Live Data. When no property is visible, we fall back on the first property.
 *
 * @param logic - the Live Data logic
 * @returns the id of the displayed property, or undefined when there are no properties
 */
function getListPropertyId(logic) {
  const propertyIds = logic.data.query.properties;
  // Read the visibility from the descriptor rather than with `isPropertyVisible`, which considers every property
  // visible in edit mode.
  return (
    propertyIds.find(
      (propertyId) => logic.getPropertyDescriptor(propertyId)?.visible,
    ) ?? propertyIds[0]
  );
}

/**
 * Wrap the Live Data logic so that nothing can be edited through it. The List layout is read-only: its displayers use
 * this logic, which prevents them from offering the edit action or switching to their editor.
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

export { createReadOnlyLogic, getListPropertyId };
