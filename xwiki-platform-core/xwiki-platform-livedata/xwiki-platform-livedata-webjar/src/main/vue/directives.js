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

export const mousedownmove = {
  inserted (el, bindings) {

    // Dispatch mousemove event as mousedownmove event
    // Pass mouseclick event in the detail object of the mouse event
    const dispatchMouseDownMoveEvent = (mouseClickEvent, mouseMoveEvent) => {
      const mouseDownMoveEvent = new MouseEvent("mousedownmove", mouseMoveEvent)
      mouseDownMoveEvent.data = mouseClickEvent.data;
      el.dispatchEvent(mouseDownMoveEvent);
    };

    // On click, bind event listener
    el.addEventListener("mousedown", e => {

      // Object used to store data on click event
      e.data = { clickEvent: e };

      // If a mouse click event handler was given as value of the directive
      // Call it
      if (typeof bindings.value === "function") {
        bindings.value(e);
      }

      // Create a wrapper function to combine both click and move event
      const mousemoveHandler = mouseMoveEvent => {
        dispatchMouseDownMoveEvent(e, mouseMoveEvent);
      };

      // Create remove event listeners handlers
      const removeMousemoveHandler = () => {
        window.removeEventListener("mousemove", mousemoveHandler);
        window.removeEventListener("mouseup", removeMousemoveHandler);
      };

      // If the "immediate" modifiers is set, dispatch the event immediately
      if (bindings.modifiers.immediate) {
        dispatchMouseDownMoveEvent(e, e);
      }

      // Bind event listeners
      window.addEventListener("mousemove", mousemoveHandler);
      window.addEventListener("mouseup", removeMousemoveHandler);
    });
  }
};
