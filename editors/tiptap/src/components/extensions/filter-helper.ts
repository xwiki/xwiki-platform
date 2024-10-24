/*
 * See the LICENSE file distributed with this work for additional
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
 * Produces an equality operator based on the current query.
 * The equality operation currently returns true of the query is a sub-string
 * of the provided value, without taking into account the case.
 *
 * @param query - the query to apply on the provided value
 * @returns a lamba taking a string and returning a true when the value matches
 * the query filter, and false otherwise
 * @since 0.8
 */
function queryEqualityOperator(query: string) {
  const queryNoCase = query.toLowerCase();
  return (value: string): boolean => {
    return value.toLowerCase().includes(queryNoCase);
  };
}

export { queryEqualityOperator };
