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
import { Logic } from "@/services/logic.js";

/**
 * Map the element to its data object
 * So that each instance of the livedata on the page handle there own data
 */
const instancesMap = new WeakMap();

/**
 * The init function of the logic script
 * For each livedata element on the page, returns its corresponding data / API
 * If the data does not exist yet, create it from the element
 * @param {HTMLElement} element The HTML Element corresponding to the Livedata component
 */
const init = function(element, $) {

  if (!instancesMap.has(element)) {
    // create a new logic object associated to the element
    const logic = new Logic(element, $);
    instancesMap.set(element, logic);
  }

  return instancesMap.get(element);
};

export { init };
