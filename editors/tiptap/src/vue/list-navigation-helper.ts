/**
 * See the LICENSE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 * This file is part of the Cristal Wiki software prototype
 * @copyright  Copyright (c) 2023 XWiki SAS
 * @license    http://opensource.org/licenses/AGPL-3.0 AGPL-3.0
 *
 **/

import { nextTick, readonly, Ref, ref, watch } from "vue";

/**
 * Build the Vue operations to easily bind the up/down/enter events to a Vue
 * component template.
 * @param apply an apply operation trigger when a element is called on enter
 * @param length the full length of the list of displayed elements
 * @param container the container of the listed elements
 * @return an object with:
 *  * up: a method to bind to the arrow up event in the template
 *  * down: a method to bind to the arrow down event in the template
 *  * enter: a method to bind to the enter event in the template
 *  * index: a read-only ref containing the index of the currently selected
 *      value
 *
 * @since 0.8
 */
export function listNavigation(
  apply: (index: number) => void,
  length: Ref<number>,
  container: Ref<Element>,
) {
  const index = ref(0);

  function up() {
    index.value = (index.value + length.value - 1) % length.value;
  }

  function down() {
    index.value = (index.value + 1) % length.value;
  }

  function enter() {
    apply(index.value);
  }

  // Make sure the newly selected item is visible on element focus change.
  watch(index, async () => {
    // Wait for the container to be re-pained to run the selector on the newly
    // selected element.
    await nextTick();
    container.value.querySelector(".is-selected")?.scrollIntoView();
  });

  const readOnlyIndex = readonly(index);

  return {
    up,
    down,
    enter,
    index: readOnlyIndex,
  };
}
