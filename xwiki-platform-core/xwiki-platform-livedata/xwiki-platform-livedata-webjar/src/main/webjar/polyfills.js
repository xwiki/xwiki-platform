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

/**
 * Polyfills that are not yet provided by Closure Compiler.
 * See https://github.com/google/closure-compiler/blob/master/src/com/google/javascript/jscomp/js/polyfills.txt
 */
define('xwiki-livedata-polyfills', function () {
  /**
   * Polyfill for the custom event function for IE 11
   * Taken from https://developer.mozilla.org/en-US/docs/Web/API/CustomEvent/CustomEvent
   */
  (function () {
    if (typeof window.CustomEvent === "function") return false;
    function CustomEvent (event, params) {
      params = params || {bubbles: false, cancelable: false, detail: null};
      var evt = document.createEvent('CustomEvent');
      evt.initCustomEvent(event, params.bubbles, params.cancelable, params.detail);
      return evt;
    }
    window.CustomEvent = CustomEvent;
  })();
});
