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

  // Prevent Prototype.js from overriding the Array.from function.
  // See https://github.com/prototypejs/prototype/issues/338.
  let arrayFrom = Array.from;
  Object.defineProperty(Array, "from", {
    get: () => arrayFrom,
    set: function(newArrayFrom) {
      if (window.Prototype && newArrayFrom === window.$A) {
        // Prototype.js tries to override the Array.from function.
        // We delete the defined property because we don't want to to prevent anyone from overriding Array.from, we just
        // want to prevent Prototype.js from doing it because we know that Prototype's implementation is outdated and
        // causes new code to fail.
        delete this.from;
        this.from = arrayFrom;
      } else {
        // Allow others to override Array.from (their need may be valid, we don't know).
        arrayFrom = newArrayFrom;
      }
    }
  });
})();
