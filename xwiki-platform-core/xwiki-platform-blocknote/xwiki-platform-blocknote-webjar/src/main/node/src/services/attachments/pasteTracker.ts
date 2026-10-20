/**
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
 * Tells whether the files currently being uploaded were pasted or dropped, and stops tracking.
 */
type PasteTracker = {
  isPaste(): boolean;
  dispose(): void;
};

/**
 * Remembers whether the last clipboard or drag and drop interaction was a paste, because BlockNote doesn't pass the
 * originating event to its upload callback and we rename pasted files only (dropped files have a meaningful name).
 *
 * Both listeners are registered in the capture phase, so they run before BlockNote handles the event and starts the
 * uploads it triggers.
 *
 * @param target - the event target to listen on, typically the document
 * @returns the tracker, which must be disposed of when the editor is destroyed
 */
function trackPaste(target: EventTarget): PasteTracker {
  let paste = false;
  const onPaste = () => {
    paste = true;
  };
  const onDrop = () => {
    paste = false;
  };
  target.addEventListener("paste", onPaste, true);
  target.addEventListener("drop", onDrop, true);
  return {
    isPaste: () => paste,
    dispose: () => {
      target.removeEventListener("paste", onPaste, true);
      target.removeEventListener("drop", onDrop, true);
    },
  };
}

export { type PasteTracker, trackPaste };
