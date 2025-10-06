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
(function () {
  // We expect this code to be loaded right before Prototype.js code.

  function preventAssignment(target, property, forbiddenValueProvider) {
    let value = target[property];
    Object.defineProperty(target, property, {
      get: () => value,
      set: function(newValue) {
        if (window.Prototype && newValue === forbiddenValueProvider()) {
          // We assume there is a single try to assign the forbidden value. We remove the protection (getter/setter) once
          // this happens, keeping the current value, i.e. preventing the forbidden value.
          delete this[property];
          this[property] = value;
        } else {
          // Allowed value.
          value = newValue;
        }
      }
    });
  }

  // Prevent Prototype.js from overriding the Array.from function.
  // See https://github.com/prototypejs/prototype/issues/338.
  preventAssignment(Array, "from", () => window.$A);

  // Prototype.js first overwrites Array.entries with its own Enumerable.entries only to delete Array.entries shortly
  // after, breaking any code that relies on Array.entries (such as the Inversify library).
  preventAssignment(Array.prototype, "entries", () => window.Enumerable.entries);
})();
