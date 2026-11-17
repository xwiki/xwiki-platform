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
import { moveCaretTo } from "@xwiki/platform-editors-blocknote-headless";
import { beforeEach, describe, expect, it, vi } from "vitest";

vi.mock("@xwiki/platform-editors-blocknote-headless", () => ({
  moveCaretTo: vi.fn(),
}));

function file(name, type) {
  return new File(["content"], name, { type });
}

function link(label) {
  return { type: "link", href: `/download/${label}`, content: label };
}

describe("takeOverFileTransfers", () => {
  let editor, linkFor, takeOver;

  function transfer(files, coordinates) {
    return { paste: !coordinates, files, coordinates };
  }

  async function take(...files) {
    const taken = takeOver(transfer(files, { left: 5, top: 9 }));
    await vi.waitFor(() => expect(linkFor).toHaveBeenCalledTimes(files.length));
    return taken;
  }

  beforeEach(() => {
    // Only what inserting the links touches.
    editor = { insertInlineContent: vi.fn(), isFocused: vi.fn(() => true) };
    // The module mock is shared by the whole file, so the calls of the previous test have to go.
    moveCaretTo.mockReset();
    moveCaretTo.mockReturnValue(true);
    linkFor = vi.fn(async (f) => link(f.name));
    takeOver = takeOverFileTransfers(() => editor, linkFor);
  });

  describe("the files it takes over", () => {
    it("takes over a transfer of files that are not images", async () => {
      expect(await take(file("report.pdf", "application/pdf"))).toBe(true);
    });

    it("places the caret before any upload, while the drop coordinates still point at the text they were dropped on", () => {
      // Resolved after the upload they would point at whatever the page had scrolled to meanwhile.
      takeOver(
        transfer([file("report.pdf", "application/pdf")], { left: 5, top: 9 }),
      );

      expect(moveCaretTo).toHaveBeenCalledWith(editor, { left: 5, top: 9 });
    });

    it("inserts a paste at the caret the editor already holds, since there is no pointer to go by", () => {
      takeOver(transfer([file("report.pdf", "application/pdf")]));

      expect(moveCaretTo).not.toHaveBeenCalled();
      expect(editor.isFocused).toHaveBeenCalled();
    });

    it("links a dropped file at the caret it moved under the pointer", async () => {
      await take(file("report.pdf", "application/pdf"));

      expect(editor.insertInlineContent).toHaveBeenCalledWith(
        [link("report.pdf")],
        { updateSelection: true },
      );
    });

    it("links a pasted file at the caret", async () => {
      takeOver(transfer([file("report.pdf", "application/pdf")]));
      await vi.waitFor(() =>
        expect(editor.insertInlineContent).toHaveBeenCalled(),
      );

      expect(editor.insertInlineContent).toHaveBeenCalledWith(
        [link("report.pdf")],
        { updateSelection: true },
      );
    });

    it("links the files of one transfer in order, after the first one", async () => {
      await take(
        file("first.pdf", "application/pdf"),
        file("second.pdf", "application/pdf"),
      );

      // The caret follows the first link, so later files go after it rather than back at the drop point, each
      // preceded by a space.
      expect(editor.insertInlineContent.mock.calls).toEqual([
        [[link("first.pdf")], { updateSelection: true }],
        [[" ", link("second.pdf")], { updateSelection: true }],
      ]);
    });

    it("tells the link builder how the files arrived, so that a paste is renamed", () => {
      takeOver(transfer([file("report.pdf", "application/pdf")]));

      expect(linkFor.mock.calls[0][1]).toBe(true);
    });

    it("inserts nothing for a file that couldn't be attached", async () => {
      linkFor.mockResolvedValue(undefined);

      await take(file("report.pdf", "application/pdf"));

      expect(editor.insertInlineContent).not.toHaveBeenCalled();
    });
  });

  describe("transfers left to BlockNote", () => {
    // An image belongs in a block of its own, which is what BlockNote gives it.
    it("leaves a transfer of images alone", () => {
      expect(takeOver(transfer([file("photo.png", "image/png")]))).toBe(false);
      expect(linkFor).not.toHaveBeenCalled();
    });

    // A transfer with nowhere to put the caret never landed on the content: a drop on a dialog over the editor,
    // or a paste into one of its fields.
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

    it("leaves a paste alone when the editor doesn't hold the caret", () => {
      editor.isFocused.mockReturnValue(false);

      expect(takeOver(transfer([file("report.pdf", "application/pdf")]))).toBe(
        false,
      );
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
