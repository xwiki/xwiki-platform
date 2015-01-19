// Bridge custom XWiki events fired from Prototype.js to jQuery.
require(['jquery'], function($) {
  var triggerJQueryEvent = function(event) {
    if (event.eventName.substr(0, 6) === 'xwiki:') {
      var jQueryEvent = $.Event(event.eventName);
      $(event.element()).trigger(jQueryEvent, event.memo);
      if (jQueryEvent.isDefaultPrevented()) {
        event.stop();
      }
    }
    return event;
  };

  var bridge = function(oldPrototypeFire) {
    return function() {
      return triggerJQueryEvent(oldPrototypeFire.apply(this, arguments));
    };
  };

  $.each([Event, Element, document], function() {
    this.fire = bridge(this.fire);
  });
});

