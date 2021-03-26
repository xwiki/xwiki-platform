
export const mousedownmove = {
  inserted (el, bindings) {

    // Dispatch mousemove event as mousedownmove event
    // Pass mouseclick event in the detail object of the mouse event
    const dispatchMouseDownMoveEvent = (mouseClickEvent, mouseMoveEvent) => {
      const mouseDownMoveEvent = new MouseEvent("mousedownmove", mouseMoveEvent)
      mouseDownMoveEvent.data = mouseClickEvent.data;
      el.dispatchEvent(mouseDownMoveEvent);
    };

    // On clik, bind event listener
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
        window.removeEventListener("focusout", removeMousemoveHandler);
      };

      // If the "immediate" modifiers is set, dispatch the event immediately
      if (bindings.modifiers.immediate) {
        dispatchMouseDownMoveEvent(e, e);
      }

      // Bind event listeners
      window.addEventListener("mousemove", mousemoveHandler);
      window.addEventListener("mouseup", removeMousemoveHandler);
      window.addEventListener("focusout", removeMousemoveHandler);
    });
  }
};
