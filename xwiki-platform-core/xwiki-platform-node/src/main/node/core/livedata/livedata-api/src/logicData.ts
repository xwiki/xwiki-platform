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

import type { Data } from "./data";
import type { Meta } from "./meta";
import type { Query } from "./query";

/**
 * The data stored in a Logic object. It contains the metadata of a Live Data, the query used to fetch the data, and
 * the data themselves.
 * @since 18.2.0RC1
 * @beta
 */
export interface LogicData {
  /**
   * The id of a livedata
   */
  id: number;
  /**
   * The metadata of a Live Data, stores the descriptor for the displayers, filters, layout, or actions.
   */
  meta: Meta;
  /**
   * Holds the query parameters used to retrieve the Live Data data.
   */
  query: Query;
  /**
   * Holds the data
   */
  data: Data;
}
