// Bridge custom XWiki events fired from Prototype.js to jQuery.
require(['jquery'], function($) {
  var triggerJQueryEvent = function(event) {
    if (event.eventName.substr(0, 6) === 'xwiki:') {
      var jQueryEvent = $.Event(event.eventName);
      // This function is executed in the context of the target element.
      $(this).trigger(jQueryEvent, event.memo);
      if (jQueryEvent.isDefaultPrevented()) {
        event.stop();
      }
    }
    return event;
  };

  var bridge = function(oldPrototypeFire) {
    return function() {
      // Prototype doesn't extend the event object if there are no registered event listeners. In IE8 the event object is
      // missing the target element for custom events, if the event object is not extended. As a consequence calling
      // Event#element() throws an exception. We need to know the target element in this case, and for this we execute
      // triggerJQueryEvent() in the context of the target element.
      //
      // If this is an element or the document then use it as target, otherwise use the first method argument.
      var target = (this.nodeType === 1 || this.nodeType === 9) ? this : arguments[0];
      return triggerJQueryEvent.call(target, oldPrototypeFire.apply(this, arguments));
    };
  };

  $.each([Event, Element, document], function() {
    this.fire = bridge(this.fire);
  });
});

