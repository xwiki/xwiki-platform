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
import { Ref, onMounted, onUnmounted, ref } from "vue";

interface MouseState {
  readonly x: Ref<number>;
  readonly y: Ref<number>;
}

/**
 * Returns a reactive state that contains the current position of either
 * the mouse cursor or a touch event.
 * @return a reactive state with:
 *  * x: x position of mouse cursor or touch event
 *  * y: y position of mouse cursor or touch event
 *
 * @since 0.8
 **/
export function useMouseCoordinates(): MouseState {
  const x: Ref<number> = ref(0);
  const y: Ref<number> = ref(0);

  function onMouseMove(event: MouseEvent) {
    x.value = event.pageX;
    y.value = event.pageY;
  }

  function onTouchMove(event: TouchEvent) {
    x.value = event.touches[0].clientX;
    y.value = event.touches[0].clientY;
  }

  onMounted(() => {
    window.addEventListener("mousemove", onMouseMove);
    window.addEventListener("touchmove", onTouchMove);
  });
  onUnmounted(() => {
    window.removeEventListener("mousemove", onMouseMove);
    window.removeEventListener("touchmove", onTouchMove);
  });

  return { x, y };
}
