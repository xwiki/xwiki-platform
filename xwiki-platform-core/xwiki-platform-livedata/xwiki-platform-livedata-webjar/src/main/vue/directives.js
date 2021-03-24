
export const mousedownmove = {
  inserted (el, bindings) {
    const mousemoveHandler = e => {
      el.dispatchEvent(new MouseEvent("mousedownmove", e));
    };
    const removeMousemoveHandler = () => {
      window.removeEventListener("mousemove", mousemoveHandler);
      window.removeEventListener("mouseup", removeMousemoveHandler);
      window.removeEventListener("focusout", removeMousemoveHandler);
    };
    el.addEventListener("mousedown", e => {
      if (bindings.modifiers.immediate) {
        el.dispatchEvent(new MouseEvent("mousedownmove", e));
      }
      window.addEventListener("mousemove", mousemoveHandler);
      window.addEventListener("mouseup", removeMousemoveHandler);
      window.addEventListener("focusout", removeMousemoveHandler);
    });
  }
};
