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
define(function () {
  'use strict';
  var setRandomizedInterval = function(func, target, range) {
    var timeout;
    var again = function() {
      timeout = setTimeout(function() {
        again();
        func();
      }, target - (range / 2) + Math.random() * range);
    };
    again();
    return {
      cancel: function() {
        if (timeout) {
          clearTimeout(timeout);
          timeout = undefined;
        }
      }
    };
  };

  var testInput = function(doc, el, offset, cb) {
    var i = 0,
      j = offset,
      input = " The quick red fox jumps over the lazy brown dog.",
      l = input.length,
      errors = 0,
      maxErrors = 15,
      interval;
    var cancel = function() {
      if (interval) {interval.cancel();}
    };

    interval = setRandomizedInterval(function() {
      cb();
      try {
        el.replaceData(Math.min(j, el.length), 0, input.charAt(i));
      } catch (err) {
        errors++;
        if (errors >= maxErrors) {
          console.log("Max error number exceeded");
          cancel();
        }

        console.error(err);
        var next = document.createTextNode("");
        doc.appendChild(next);
        el = next;
        j = -1;
      }
      i = (i + 1) % l;
      j++;
    }, 200, 50);

    return {
      cancel: cancel
    };
  };

  return {
    testInput: testInput,
    setRandomizedInterval: setRandomizedInterval
  };
});
