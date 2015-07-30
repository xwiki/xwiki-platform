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
// Bridge custom XWiki events between Prototype.js and jQuery.
define(['jquery'], function($) {
  var oldJQueryTrigger = $.event.trigger;
  var oldPrototypeFire = Element.fire;

  var shouldBridgeEvent = function(eventName) {
    return eventName && eventName.substr(0, 6) === 'xwiki:';
  };

  var newJQueryTrigger = function(event, data, element, onlyHandlers) {
    var result = oldJQueryTrigger(event, data, element, onlyHandlers);
    var jQueryEvent, eventName;
    if (event && typeof(event) === 'object' && event.type) {
      jQueryEvent = event;
      eventName = event.type;
    } else if (typeof(event) === 'string') {
      eventName = event;
    }
    var propagationStopped = jQueryEvent && typeof(jQueryEvent.isPropagationStopped) === 'function'
      && jQueryEvent.isPropagationStopped();
    if (!propagationStopped && element && shouldBridgeEvent(eventName)) {
      var memo = $.isArray(data) ? data[0] : data;
      var bubble = !onlyHandlers;
      var prototypeEvent = oldPrototypeFire(element, eventName, memo, bubble);
      // Make sure the jQuery event can be canceled from Prototype.
      if (prototypeEvent.stopped && jQueryEvent && typeof(jQueryEvent.preventDefault) === 'function') {
        jQueryEvent.preventDefault();
      }
    }
    return result;
  }

  var newPrototypeFire = function(element, eventName, memo, bubble) {
    var prototypeEvent = oldPrototypeFire(element, eventName, memo, bubble);
    if (!prototypeEvent.stopped && shouldBridgeEvent(eventName)) {
      var jQueryEvent = $.Event(eventName);
      var data = memo ? [memo] : null;
      var onlyHandlers = bubble === undefined ? false : !bubble;
      oldJQueryTrigger(jQueryEvent, data, element, onlyHandlers);
      // Make sure the Prototype event can be canceled from jQuery.
      if (jQueryEvent.isDefaultPrevented()) {
        prototypeEvent.stop();
      }
    }
    return prototypeEvent;
  };

  $.event.trigger = newJQueryTrigger;
  Element.addMethods({fire: newPrototypeFire});
  Object.extend(Event, {fire: newPrototypeFire});
  Object.extend(document, {fire: newPrototypeFire.methodize()});
});
