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
import { DefaultBlockNoteIterator } from "./DefaultBlockNoteIterator";
import { XWikiBlockNoteProcessor } from "./XWikiBlockNoteProcessor";
import { extractLinkId } from "./linkId";
import { beforeEach, describe, expect, it } from "vitest";

describe("XWikiBlockNoteProcessor", () => {
  let processor;

  beforeEach(() => {
    processor = new XWikiBlockNoteProcessor(new DefaultBlockNoteIterator());
  });

  // Builds a document with a single paragraph holding a single link with the given props.
  function documentWithLink(linkProps) {
    return [
      {
        id: "p1",
        type: "paragraph",
        props: {},
        content: [
          {
            type: "link",
            props: linkProps,
            href: "/xwiki/bin/view/Space/Page",
            content: [{ type: "text", text: "label", styles: {} }],
          },
        ],
        children: [],
      },
    ];
  }

  function firstLink(content) {
    return content[0].content[0];
  }

  describe("load", () => {
    it("backs up the link metadata and binds it to a synthetic id in the href", () => {
      const reference = {
        type: "doc",
        typed: true,
        reference: "Space.Page",
        parameters: {},
      };
      const document = processor.load(
        JSON.stringify(
          documentWithLink({
            xwikiReference: reference,
            xwikiParameters: { class: "myclass" },
            xwikiFreestanding: true,
          }),
        ),
      );

      const link = firstLink(document.content);
      // The BlockNote link schema has no props: they must be removed.
      expect(link.props).toBeUndefined();
      // A synthetic id is injected in the href.
      const id = extractLinkId(link.href);
      expect(id).toBeTruthy();
      // The metadata is backed up, mapped to that synthetic id.
      expect(document.getMetadata(id)).toEqual({
        xwikiReference: reference,
        xwikiParameters: { class: "myclass" },
        xwikiFreestanding: true,
      });
    });

    it("normalizes bare string elements in link content into text objects", () => {
      const doc = documentWithLink({});
      firstLink(doc).content = ["label"];
      const document = processor.load(JSON.stringify(doc));
      expect(firstLink(document.content).content).toEqual([
        { type: "text", text: "label", styles: {} },
      ]);
    });

    it("does not inject a synthetic id when the link has no metadata", () => {
      const document = processor.load(JSON.stringify(documentWithLink({})));
      const link = firstLink(document.content);
      expect(link.props).toBeUndefined();
      expect(extractLinkId(link.href)).toBeUndefined();
      expect(link.href).toBe("/xwiki/bin/view/Space/Page");
    });
  });

  describe("load then save round-trip", () => {
    it("restores the full link metadata and the original href", () => {
      const linkProps = {
        xwikiReference: {
          type: "doc",
          typed: true,
          reference: "Space.Page",
          parameters: {},
        },
        xwikiParameters: { class: "myclass" },
        xwikiFreestanding: true,
      };
      const document = processor.load(
        JSON.stringify(documentWithLink(linkProps)),
      );

      const saved = JSON.parse(processor.save(document));
      const link = firstLink(saved);
      expect(link.props).toEqual(linkProps);
      expect(link.href).toBe("/xwiki/bin/view/Space/Page");
    });

    it("restores a link that only has a resource reference", () => {
      const linkProps = {
        xwikiReference: {
          type: "url",
          typed: false,
          reference: "http://example.com",
          parameters: {},
        },
      };
      const document = processor.load(
        JSON.stringify(documentWithLink(linkProps)),
      );

      const saved = JSON.parse(processor.save(document));
      const link = firstLink(saved);
      expect(link.props).toEqual(linkProps);
      expect(link.href).toBe("/xwiki/bin/view/Space/Page");
    });

    it("leaves a link without metadata unchanged", () => {
      const document = processor.load(JSON.stringify(documentWithLink({})));
      const saved = JSON.parse(processor.save(document));
      const link = firstLink(saved);
      expect(link.props).toBeUndefined();
      expect(link.href).toBe("/xwiki/bin/view/Space/Page");
    });
  });

  describe("generated label", () => {
    // A link without a label is rendered with an empty content and its generated label stored in the
    // xwikiGeneratedLabel property.
    function documentWithGeneratedLabel(generatedLabel) {
      return [
        {
          id: "p1",
          type: "paragraph",
          props: {},
          content: [
            {
              type: "link",
              props: {
                xwikiReference: {
                  type: "doc",
                  typed: true,
                  reference: "Space.Page",
                  parameters: {},
                },
                xwikiGeneratedLabel: generatedLabel,
              },
              href: "/xwiki/bin/view/Space/Page",
              content: [],
            },
          ],
          children: [],
        },
      ];
    }

    it("shows the generated label as the link content on load", () => {
      const document = processor.load(
        JSON.stringify(documentWithGeneratedLabel("Page")),
      );
      const link = firstLink(document.content);
      expect(link.content).toEqual([
        { type: "text", text: "Page", styles: {} },
      ]);
    });

    it("keeps the marker on save when the generated label is not changed", () => {
      const document = processor.load(
        JSON.stringify(documentWithGeneratedLabel("Page")),
      );
      const saved = JSON.parse(processor.save(document));
      const link = firstLink(saved);
      expect(link.props.xwikiGeneratedLabel).toBe("Page");
      expect(link.props.xwikiReference.reference).toBe("Space.Page");
    });

    it("drops the marker on save when the user changed the generated label", () => {
      const document = processor.load(
        JSON.stringify(documentWithGeneratedLabel("Page")),
      );
      // Simulate the user editing the label.
      firstLink(document.content).content = [
        { type: "text", text: "Custom", styles: {} },
      ];
      const saved = JSON.parse(processor.save(document));
      const link = firstLink(saved);
      expect(link.props.xwikiGeneratedLabel).toBeUndefined();
      expect(link.props.xwikiReference.reference).toBe("Space.Page");
      expect(link.content).toEqual([
        { type: "text", text: "Custom", styles: {} },
      ]);
    });
  });
});
