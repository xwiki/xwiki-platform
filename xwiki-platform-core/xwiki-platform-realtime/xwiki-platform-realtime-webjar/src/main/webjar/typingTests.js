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
define('xwiki-realtime-typingTests', function() {
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
        clearTimeout(timeout);
      }
    };
  };

  var testInput = function(rootElement, textNode, offset, callback) {
    var i = 0,
      j = offset,
      textInput = " The quick red fox jumps over the lazy brown dog.",
      errors = 0,
      maxErrors = 15,
      interval;
    var cancel = function() {
      interval.cancel();
    };

    interval = setRandomizedInterval(function() {
      callback();
      try {
        // "Type" the next character from the text input inside the given text node.
        textNode.insertData(Math.min(j, textNode.length), textInput.charAt(i));
      } catch (error) {
        errors++;
        if (errors >= maxErrors) {
          console.log('Max error number exceeded.');
          cancel();
        }

        console.error(error);
        // Continue typing in a new text node.
        textNode = document.createTextNode('');
        rootElement.appendChild(textNode);
        j = -1;
      }
      // Type again the text input when we finish it.
      i = (i + 1) % textInput.length;
      j++;
    }, 200, 50);

    return {cancel};
  };

  return {
    setRandomizedInterval,
    testInput
  };
});
