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

import { Ref, onMounted, onUnmounted, ref } from "vue";

enum ViewportType {
  Mobile,
  Desktop,
}

/**
 * Returns a reactive state that contains the type of the current viewport.
 * @returns a reactive state with either of these values:
 *  * ViewportType.Mobile (&lt;= 600px)
 *  * ViewportType.Desktop (&gt; 600px)
 *
 * @since 0.8
 **/
function useViewportType(): Ref<ViewportType> {
  const viewportType: Ref<ViewportType> = ref(getNewViewportType());

  function onResize() {
    viewportType.value = getNewViewportType();
  }

  function getNewViewportType(): ViewportType {
    if (window.innerWidth <= 600) {
      return ViewportType.Mobile;
    } else {
      return ViewportType.Desktop;
    }
  }

  onMounted(() => window.addEventListener("resize", onResize));
  onUnmounted(() => window.removeEventListener("resize", onResize));

  return viewportType;
}

export { ViewportType, useViewportType };
