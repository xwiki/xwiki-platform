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
  function setRandomizedInterval(func, target, range) {
    let timeout;
    const again = function() {
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
  }

  function testInput(getSelection, callback) {
    let i = 0, textInput = " The quick red fox jumps over the lazy brown dog.";

    let interval = setRandomizedInterval(() => {
      const selection = getSelection();
      if (selection?.rangeCount) {
        const range = selection.getRangeAt(0);
        if (range.collapsed && range.startContainer.nodeType === Node.TEXT_NODE) {
          // "Type" the next character from the input string.
          const textNode = range.startContainer;
          textNode.insertData(range.startOffset, textInput.charAt(i));

          // Type again the text input when we finish it.
          i = (i + 1) % textInput.length;

          // Update the caret position.
          range.setStart(textNode, range.startOffset + 1);
          range.collapse();
          selection.removeAllRanges();
          selection.addRange(range);

          // Notify about the change.
          callback();
        }
      }
    }, 200, 50);

    return {
      cancel: () => {
        interval.cancel();
      }
    };
  }

  return {
    setRandomizedInterval,
    testInput
  };
});
