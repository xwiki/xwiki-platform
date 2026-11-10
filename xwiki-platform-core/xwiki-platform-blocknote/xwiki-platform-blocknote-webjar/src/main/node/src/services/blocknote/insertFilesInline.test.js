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
import { takeOverFileTransfers } from "./insertFilesInline";
import { beforeEach, describe, expect, it, vi } from "vitest";

function file(name, type) {
  return new File(["content"], name, { type });
}

function link(label) {
  return { type: "link", href: `/download/${label}`, content: label };
}

describe("takeOverFileTransfers", () => {
  let insertInlineContent, moveCaretTo, linkFor, takeOver;

  function transfer(files, coordinates) {
    return { paste: !coordinates, files, coordinates };
  }

  async function take(...files) {
    const taken = takeOver(transfer(files, { left: 5, top: 9 }));
    await vi.waitFor(() => expect(linkFor).toHaveBeenCalledTimes(files.length));
    return taken;
  }

  beforeEach(() => {
    insertInlineContent = vi.fn();
    moveCaretTo = vi.fn(() => true);
    linkFor = vi.fn(async (f) => link(f.name));
    takeOver = takeOverFileTransfers(insertInlineContent, moveCaretTo, linkFor);
  });

  describe("the files it takes over", () => {
    it("takes over a transfer of files that are not images", async () => {
      expect(await take(file("report.pdf", "application/pdf"))).toBe(true);
    });

    it("moves the caret under the pointer while the drop event is live, since dropping leaves it behind", () => {
      // Not once the files are uploaded: the coordinates are in the viewport, so they rot as the page scrolls.
      takeOver(
        transfer([file("report.pdf", "application/pdf")], { left: 5, top: 9 }),
      );

      expect(moveCaretTo).toHaveBeenCalledWith({ left: 5, top: 9 });
    });

    it("links a dropped file at the caret it moved under the pointer", async () => {
      await take(file("report.pdf", "application/pdf"));

      expect(insertInlineContent).toHaveBeenCalledWith([link("report.pdf")]);
    });

    it("links a pasted file at the caret, which a paste doesn't move", async () => {
      takeOver(transfer([file("report.pdf", "application/pdf")]));
      await vi.waitFor(() => expect(insertInlineContent).toHaveBeenCalled());

      expect(moveCaretTo).not.toHaveBeenCalled();
      expect(insertInlineContent).toHaveBeenCalledWith([link("report.pdf")]);
    });

    it("links the files of one transfer in order, after the first one", async () => {
      await take(
        file("first.pdf", "application/pdf"),
        file("second.pdf", "application/pdf"),
      );

      // The caret follows the first link, so later files go after it rather than back at the drop point, each
      // preceded by a space.
      expect(insertInlineContent.mock.calls).toEqual([
        [[link("first.pdf")]],
        [[" ", link("second.pdf")]],
      ]);
    });

    it("tells the link builder how the files arrived, so that a paste is renamed", () => {
      takeOver(transfer([file("report.pdf", "application/pdf")]));

      expect(linkFor.mock.calls[0][1]).toBe(true);
    });

    it("inserts nothing for a file that couldn't be attached", async () => {
      linkFor.mockResolvedValue(undefined);

      await take(file("report.pdf", "application/pdf"));

      expect(insertInlineContent).not.toHaveBeenCalled();
    });
  });

  describe("transfers left to BlockNote", () => {
    // An image belongs in a block of its own, which is what BlockNote gives it.
    it("leaves a transfer of images alone", () => {
      expect(takeOver(transfer([file("photo.png", "image/png")]))).toBe(false);
      expect(linkFor).not.toHaveBeenCalled();
    });

    // A drop with nowhere to put the caret is on the chrome around the content, e.g. a dialog over the editor.
    it("leaves a drop alone when the caret can't follow the pointer", () => {
      moveCaretTo.mockReturnValue(false);

      expect(
        takeOver(
          transfer([file("report.pdf", "application/pdf")], {
            left: 5,
            top: 9,
          }),
        ),
      ).toBe(false);
      expect(linkFor).not.toHaveBeenCalled();
    });

    it("leaves a transfer mixing an image with another file alone", () => {
      const files = [
        file("photo.png", "image/png"),
        file("report.pdf", "application/pdf"),
      ];

      expect(takeOver(transfer(files))).toBe(false);
      expect(linkFor).not.toHaveBeenCalled();
    });
  });
});
