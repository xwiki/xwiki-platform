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
'use strict';
(function () {
  // We expect this code to be loaded right before Prototype.js code.

  function preventAssignment(target, property, isForbiddenValue, onlyOnce) {
    let value = target[property];
    Object.defineProperty(target, property, {
      configurable: true,
      get: () => value,
      set: function(newValue) {
        if (!window.Prototype || !isForbiddenValue(newValue)) {
          // Allowed value.
          value = newValue;
        } else if (onlyOnce) {
          // We expect a single try to assign the forbidden value. We remove the protection (getter/setter) once
          // this happens, keeping the current value (if any), i.e. preventing the forbidden value.
          delete this[property];
          if (value !== undefined) {
            this[property] = value;
          }
        }
      }
    });
  }

  // Prevent Prototype.js from overriding the Array.from function.
  // See https://github.com/prototypejs/prototype/issues/338.
  preventAssignment(Array, "from", newValue => newValue === window.$A, true);

  // Prototype.js first overwrites Array.entries with its own Enumerable.entries only to delete Array.entries shortly
  // after, breaking any code that relies on Array.entries (such as the Inversify library).
  preventAssignment(Array.prototype, "entries", newValue => newValue === window.Enumerable.entries, true);

  // Prototype.js' Element#remove() throws an exception when called on a detached element, while the native
  // implementation does nothing, which is what the caller expects.
  // Note that we can't prevent the assignment only once because Prototype.js calls Element.addMethods() multiple times.
  preventAssignment(HTMLElement.prototype, "remove",
    newValue => newValue === Element?.Methods?.remove?._methodized);

  // Prototype.js' String#stripTags() is vulnerable to ReDoS (CVE-2020-27511) and String#unescapeHTML() calls it. We
  // prevent Prototype.js from adding them to the String prototype since they are not safe. The first assignment made
  // after Prototype.js starts loading is Prototype.js' own; later assignments (e.g. an extension providing a safe
  // implementation) are allowed.
  ["stripTags", "unescapeHTML"].forEach(method => preventAssignment(String.prototype, method, () => true, true));
})();
