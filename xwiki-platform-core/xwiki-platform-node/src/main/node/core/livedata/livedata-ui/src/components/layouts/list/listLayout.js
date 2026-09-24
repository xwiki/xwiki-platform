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
 * The id of the List layout.
 */
const LIST_LAYOUT_ID = "list";

/**
 * Return the id of the property displayed by the List layout.
 *
 * The List layout displays a single property per entry. It is kept on the layout descriptor (in its
 * `displayedProperty` field) rather than in the property visibility, so that choosing it does not change the
 * properties displayed by the other layouts. When it is not set, or does not match a property of the query, we fall
 * back on the first visible property, then on the first property.
 *
 * @param logic - the Live Data logic
 * @returns the id of the displayed property, or undefined when there are no properties
 */
function getListPropertyId(logic) {
  const propertyIds = logic.data.query.properties;
  const displayedProperty =
    logic.getLayoutDescriptor(LIST_LAYOUT_ID)?.displayedProperty;
  if (displayedProperty && propertyIds.includes(displayedProperty)) {
    return displayedProperty;
  }
  // Read the visibility from the descriptor rather than with `isPropertyVisible`, which considers every property
  // visible in edit mode.
  return (
    propertyIds.find(
      (propertyId) => logic.getPropertyDescriptor(propertyId)?.visible,
    ) ?? propertyIds[0]
  );
}

/**
 * Set the property displayed by the List layout. Unknown properties are ignored.
 *
 * @param logic - the Live Data logic
 * @param propertyId - the id of the property to display
 */
function setListPropertyId(logic, propertyId) {
  const layoutDescriptor = logic.getLayoutDescriptor(LIST_LAYOUT_ID);
  if (layoutDescriptor && logic.data.query.properties.includes(propertyId)) {
    layoutDescriptor.displayedProperty = propertyId;
  }
}

export { LIST_LAYOUT_ID, getListPropertyId, setListPropertyId };
