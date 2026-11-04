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
import { suggestLinks } from "../linkSuggestions";
import { LinkType } from "@xwiki/platform-link-suggest-api";
import { describe, expect, it, vi } from "vitest";
import type { LinkEditionContext } from "@xwiki/platform-link-modal-ui";
import type {
  Link,
  LinkSuggestService,
} from "@xwiki/platform-link-suggest-api";

function link(reference: string, label: string, type = LinkType.PAGE): Link {
  return {
    id: reference,
    url: `https://wiki.example/${reference}`,
    reference,
    label,
    hint: `Location of ${label}`,
    type,
  };
}

// suggestLinks only uses the link suggestion service of the link edition context.
function linkEditionContext(
  getLinks: LinkSuggestService["getLinks"] | null,
): LinkEditionContext {
  return {
    linkSuggestService: getLinks ? { getLinks } : null,
  } as LinkEditionContext;
}

describe("suggestLinks", () => {
  it("returns false when there is no link suggestion service", async () => {
    await expect(
      suggestLinks(linkEditionContext(null), "query", LinkType.PAGE, () => 1),
    ).resolves.toBe(false);
  });

  it("lists the links returned by the service, as matched by the service", async () => {
    const getLinks = vi.fn(async () => [
      link("Space.Page", "Some Page"),
      // The label doesn't contain the query, but the service matched the link (e.g. on its location).
      link("Space.Other", "Other"),
    ]);

    const items = await suggestLinks(
      linkEditionContext(getLinks),
      "some",
      LinkType.PAGE,
      (link) => `value of ${link.reference}`,
    );

    expect(getLinks).toHaveBeenCalledWith("some", LinkType.PAGE);
    expect(items).toEqual([
      {
        key: "https://wiki.example/Space.Page",
        value: "value of Space.Page",
        renderingData: link("Space.Page", "Some Page"),
        equivalentQuery: "Space.Page",
      },
      {
        key: "https://wiki.example/Space.Other",
        value: "value of Space.Other",
        renderingData: link("Space.Other", "Other"),
        equivalentQuery: "Space.Other",
      },
    ]);
  });

  it("leaves out the links of another type and the links without a value", async () => {
    const items = await suggestLinks(
      linkEditionContext(async () => [
        link("Space.Page@file.png", "file.png", LinkType.ATTACHMENT),
        link("Space.Invalid", "Invalid"),
        link("Space.Page", "Page"),
      ]),
      "",
      LinkType.PAGE,
      (link) => (link.reference === "Space.Invalid" ? null : link.reference),
    );

    expect(items).toEqual([expect.objectContaining({ value: "Space.Page" })]);
  });

  it("returns no link when the service fails", async () => {
    const error = new Error("Search failed");
    const consoleError = vi
      .spyOn(console, "error")
      .mockImplementation(() => {});

    try {
      const items = await suggestLinks(
        linkEditionContext(async () => {
          throw error;
        }),
        "query",
        LinkType.PAGE,
        () => 1,
      );

      expect(items).toEqual([]);
      expect(consoleError).toHaveBeenCalledWith(
        "Failed to fetch the link suggestions",
        error,
      );
    } finally {
      consoleError.mockRestore();
    }
  });
});
