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
import { trackPaste } from "./pasteTracker";
import { afterEach, describe, expect, it, vi } from "vitest";

describe("trackPaste", () => {
  let tracker;

  afterEach(() => {
    tracker?.dispose();
  });

  function dispatch(type) {
    document.dispatchEvent(new Event(type, { bubbles: true }));
  }

  it("reports a drop until a paste happens", () => {
    tracker = trackPaste(document);

    expect(tracker.isPaste()).toBe(false);

    dispatch("paste");
    expect(tracker.isPaste()).toBe(true);

    dispatch("drop");
    expect(tracker.isPaste()).toBe(false);
  });

  it("offers a transfer carrying files, and stops the event when it is taken over", () => {
    const files = [new File(["content"], "notes.txt")];
    const takeOver = vi.fn(() => true);
    tracker = trackPaste(document, takeOver);

    const event = new Event("drop", { bubbles: true, cancelable: true });
    Object.defineProperty(event, "dataTransfer", { value: { files } });
    Object.assign(event, { clientX: 12, clientY: 34 });
    document.dispatchEvent(event);

    expect(takeOver).toHaveBeenCalledWith({
      paste: false,
      files,
      coordinates: { left: 12, top: 34 },
    });
    expect(event.defaultPrevented).toBe(true);
  });

  it("lets through a transfer nobody takes over, and one without files", () => {
    const files = [new File(["content"], "notes.txt")];
    tracker = trackPaste(document, () => false);

    const ignored = new Event("drop", { bubbles: true, cancelable: true });
    Object.defineProperty(ignored, "dataTransfer", { value: { files } });
    document.dispatchEvent(ignored);

    expect(ignored.defaultPrevented).toBe(false);
    expect(tracker.isPaste()).toBe(false);
  });

  it("stops tracking once disposed of", () => {
    tracker = trackPaste(document);
    tracker.dispose();

    dispatch("paste");

    expect(tracker.isPaste()).toBe(false);
  });
});
