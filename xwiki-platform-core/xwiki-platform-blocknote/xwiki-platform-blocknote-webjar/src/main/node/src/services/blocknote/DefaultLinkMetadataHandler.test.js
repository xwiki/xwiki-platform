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
import { DefaultLinkMetadataHandler } from "./DefaultLinkMetadataHandler";
import {
  getLinkMetadata,
  setLinkMetadata,
  stripLinkMetadata,
} from "@xwiki/platform-editors-blocknote-react";
import { beforeEach, describe, expect, it } from "vitest";

function pageReference(reference) {
  return { type: "doc", typed: true, reference, parameters: {} };
}

describe("DefaultLinkMetadataHandler", () => {
  const handler = new DefaultLinkMetadataHandler();
  let instance;

  beforeEach(() => {
    instance = { options: {} };
  });

  /**
   * @returns the URL of a link to Users.Alice whose label is generated, as loaded from the server
   */
  function linkWithGeneratedLabel(metadata = {}) {
    return setLinkMetadata("/xwiki/bin/view/Users/Alice", {
      xwikiReference: pageReference("Users.Alice"),
      xwikiGeneratedLabel: "Alice",
      ...metadata,
    });
  }

  describe("beforeEdit", () => {
    it("tells the link editor that the label of a link is generated", () => {
      expect(
        handler.beforeEdit(instance, {
          title: "Alice",
          url: linkWithGeneratedLabel(),
        }),
      ).toEqual({
        title: "Alice",
        url: "/xwiki/bin/view/Users/Alice",
        reference: pageReference("Users.Alice"),
        generatedTitle: true,
      });
    });

    it("doesn't take a generated label changed in the edited content for a generated one", () => {
      expect(
        handler.beforeEdit(instance, {
          title: "Alize",
          url: linkWithGeneratedLabel(),
        }),
      ).toMatchObject({ title: "Alize", generatedTitle: false });
    });

    it("leaves the links without metadata untouched", () => {
      const linkData = { title: "XWiki", url: "https://xwiki.org/" };

      expect(handler.beforeEdit(instance, linkData)).toBe(linkData);
    });
  });

  describe("beforeUpdate", () => {
    it("keeps the label of a link generated when the link editor reports it as such", () => {
      const previous = { title: "Alice", url: linkWithGeneratedLabel() };

      const updated = handler.beforeUpdate(
        instance,
        {
          title: "Bob",
          generatedTitle: true,
          url: "/xwiki/bin/view/Users/Bob",
          reference: pageReference("Users.Bob"),
        },
        previous,
      );

      expect(stripLinkMetadata(updated.url)).toBe("/xwiki/bin/view/Users/Bob");
      expect(getLinkMetadata(updated.url)).toEqual({
        xwikiReference: pageReference("Users.Bob"),
        xwikiGeneratedLabel: "Bob",
      });
    });

    it("keeps the metadata that the link editor doesn't edit", () => {
      const previous = {
        title: "Alice",
        url: linkWithGeneratedLabel({
          xwikiParameters: { rel: "__blank" },
        }),
      };

      const updated = handler.beforeUpdate(
        instance,
        {
          title: "Bob",
          url: "/xwiki/bin/view/Users/Bob",
          reference: pageReference("Users.Bob"),
        },
        previous,
      );

      expect(getLinkMetadata(updated.url)).toEqual({
        xwikiReference: pageReference("Users.Bob"),
        xwikiParameters: { rel: "__blank" },
      });
      // The edited link is left untouched, so that undo restores it.
      expect(getLinkMetadata(previous.url)).toMatchObject({
        xwikiReference: pageReference("Users.Alice"),
      });
    });

    it("saves the label typed in the link editor, even when it's the same as the generated one", () => {
      const previous = { title: "Alice", url: linkWithGeneratedLabel() };

      const updated = handler.beforeUpdate(
        instance,
        {
          title: "Alice",
          generatedTitle: false,
          url: "/xwiki/bin/view/Users/Alice",
          reference: pageReference("Users.Alice"),
        },
        previous,
      );

      expect(getLinkMetadata(updated.url)).toEqual({
        xwikiReference: pageReference("Users.Alice"),
      });
    });

    it("marks the label of a new link as generated when the link editor reports it as such", () => {
      const updated = handler.beforeUpdate(instance, {
        title: "Alice",
        generatedTitle: true,
        url: "/xwiki/bin/view/Users/Alice",
        reference: pageReference("Users.Alice"),
      });

      expect(getLinkMetadata(updated.url)).toEqual({
        xwikiReference: pageReference("Users.Alice"),
        xwikiGeneratedLabel: "Alice",
      });
    });

    it("marks the generated label of a link without reference", () => {
      const updated = handler.beforeUpdate(instance, {
        title: "https://xwiki.org/",
        generatedTitle: true,
        url: "https://xwiki.org/",
      });

      expect(getLinkMetadata(updated.url)).toEqual({
        xwikiGeneratedLabel: "https://xwiki.org/",
      });
    });

    it("leaves the url of a link without reference and with a label of its own untouched", () => {
      const linkData = { title: "XWiki", url: "https://xwiki.org" };

      expect(handler.beforeUpdate(instance, linkData)).toEqual(linkData);
    });
  });
});
