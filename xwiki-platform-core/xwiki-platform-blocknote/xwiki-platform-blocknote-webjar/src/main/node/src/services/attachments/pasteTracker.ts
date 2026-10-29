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

type FileTransfer = {
  // Whether the files were pasted rather than dropped.
  paste: boolean;

  files: File[];

  // Where the files were dropped, since a drop leaves the caret where it was. Undefined for a paste.
  coordinates?: { left: number; top: number };
};

type PasteTracker = {
  isPaste(): boolean;
  dispose(): void;
};

/**
 * Remembers whether the last interaction was a paste, because BlockNote doesn't pass the originating event to its
 * upload callback. Capture phase, so both listeners run before BlockNote starts the upload. The flag stays set until
 * the opposite event rather than being cleared once the upload starts, because BlockNote uploads the pasted files one
 * after another, so all but the first are read after the paste event is over. Dispose with the editor.
 *
 * @param target - the editor element, so that two editors on the same page don't see each other's transfers
 * @param takeOver - offered every transfer that carries files; returning true stops the event, which keeps it from
 *   reaching BlockNote, so the caller inserts those files itself
 * @returns the tracker
 */
function trackPaste(
  target: EventTarget,
  takeOver: (transfer: FileTransfer) => boolean = () => false,
): PasteTracker {
  let paste = false;
  const offer = (event: Event, transfer: Omit<FileTransfer, "paste">) => {
    if (transfer.files.length > 0 && takeOver({ ...transfer, paste })) {
      event.preventDefault();
      event.stopPropagation();
    }
  };
  const onPaste = (event: Event) => {
    paste = true;
    const files = (event as ClipboardEvent).clipboardData?.files;
    offer(event, { files: Array.from(files ?? []) });
  };
  const onDrop = (event: Event) => {
    paste = false;
    const { dataTransfer, clientX, clientY } = event as DragEvent;
    offer(event, {
      files: Array.from(dataTransfer?.files ?? []),
      // A drop leaves the caret where it was, so where it landed is the only clue to where its files belong.
      coordinates: { left: clientX, top: clientY },
    });
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

export { type FileTransfer, type PasteTracker, trackPaste };
