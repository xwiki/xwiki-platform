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

const deepCopy = function(value) {
  return JSON.parse(JSON.stringify(value));
};
const findById = function(id, array) {
  if (id != null) {
    return array.find(item => item.id === id);
  }
};
const mergeArray = function(left, right) {
  // Take the items from the right, merging them with the corresponding item from the left.
  const fromRight = right.map(rightItem => merge(findById(rightItem.id, left), rightItem));
  // Take the items from the left that are identifiable and that are not present on the right.
  const fromLeft = deepCopy(
    left.filter(leftItem => leftItem.id != null && findById(leftItem.id, right) == null));
  return fromRight.concat(fromLeft);
};
const mergeObject = function(left, right) {
  // Don't merge if the right object has an identifier different than the one of the left object.
  if (right.hasOwnProperty("id") && left.id !== right.id) {
    return deepCopy(right);
  }
  Object.keys(right).forEach(key => {
    left[key] = merge(left[key], right[key]);
  });
  return left;
};
const merge = function(left, right) {
  if (left?.constructor === Object && right?.constructor === Object) {
    return mergeObject(left, right);
  } else if (Array.isArray(left) && Array.isArray(right)) {
    return mergeArray(left, right);
  } else if (right == null) {
    // Keep the left value.
    return left;
  } else {
    // Overwrite the left value because either it's null or we cannot merge.
    return deepCopy(right);
  }
};

export function jsonMerge(...objects) {
  return objects?.reduce(merge, null);
}
