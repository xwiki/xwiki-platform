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
 * @param logic - the Live Data logic
 * @returns the ids of the visible properties, following the order of the properties configured for the Live Data
 */
function getVisiblePropertyIds(logic) {
  // Read the visibility from the descriptor rather than with `isPropertyVisible`, which considers every property
  // visible in edit mode.
  return logic.data.query.properties.filter(
    (propertyId) => logic.getPropertyDescriptor(propertyId)?.visible,
  );
}

/**
 * Return the id of the main property displayed by the List layout.
 *
 * The List layout displays the first visible property of each entry as its main property, following the order of the
 * properties configured for the Live Data. When no property is visible, we fall back on the first property.
 *
 * @param logic - the Live Data logic
 * @returns the id of the main property, or undefined when there are no properties
 */
function getListPropertyId(logic) {
  return getVisiblePropertyIds(logic)[0] ?? logic.data.query.properties[0];
}

/**
 * Return the ids of the secondary properties displayed by the List layout: the other visible properties, following the
 * order of the properties configured for the Live Data.
 *
 * @param logic - the Live Data logic
 * @returns the ids of the secondary properties
 */
function getListSecondaryPropertyIds(logic) {
  const mainPropertyId = getListPropertyId(logic);
  return getVisiblePropertyIds(logic).filter(
    (propertyId) => propertyId !== mainPropertyId,
  );
}

export { getListPropertyId, getListSecondaryPropertyIds };
