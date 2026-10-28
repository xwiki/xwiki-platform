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

type PasteTracker = {
  isPaste(): boolean;
  dispose(): void;
};

/**
 * Remembers whether the last interaction was a paste, because BlockNote doesn't pass the originating event to its
 * upload callback. Capture phase, so both listeners run before BlockNote starts the upload. The flag stays set until
 * the opposite event rather than being cleared once the upload starts, because BlockNote uploads the pasted files one
 * after another, so all but the first are read after the paste event is over. Dispose with the editor.
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
