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
import { XWikiModelReferenceParser } from "../XWikiModelReferenceParser";
import { XWikiModelReferenceSerializer } from "../XWikiModelReferenceSerializer";
import {
  AttachmentReference,
  DocumentReference,
  EntityType,
  SpaceReference,
  WikiReference,
} from "@xwiki/platform-model-api";
import { describe, expect, it } from "vitest";
import type { ResourceReferenceParser } from "@xwiki/platform-rendering-api";

// Reads the resource type prefix (e.g. "doc:"), like the XWiki resource reference parser does for the entity types.
const resourceReferenceParser = {
  parse(reference: string, { type }: { type: string }) {
    const match = /^(doc|attach|space):(.*)$/.exec(reference);
    return {
      type: match?.[1] ?? type,
      typed: !!match,
      reference: match?.[2] ?? reference,
      parameters: {},
    };
  },
} as unknown as ResourceReferenceParser;

const parser = new XWikiModelReferenceParser(resourceReferenceParser);
const serializer = new XWikiModelReferenceSerializer();

const page = new DocumentReference(
  "Page",
  new SpaceReference(new WikiReference("xwiki"), "Space"),
  true,
);
const otherPage = new DocumentReference(
  "Other",
  new SpaceReference(new WikiReference("xwiki"), "Space"),
  true,
);

describe("XWikiModelReferenceParser", () => {
  it("resolves a relative reference against the base reference", () => {
    expect(parser.parse("Other", { base: page })).toEqual(otherPage);
    expect(
      parser.parse("cat.png", { type: EntityType.ATTACHMENT, base: page }),
    ).toEqual(new AttachmentReference("cat.png", page));
  });

  it("ignores the base reference for an absolute reference", () => {
    expect(parser.parse("dev:A.B", { base: page })).toEqual(
      new DocumentReference(
        "B",
        new SpaceReference(new WikiReference("dev"), "A"),
        true,
      ),
    );
  });
});

describe("XWikiModelReferenceSerializer", () => {
  it("serializes a reference fully without a base reference", () => {
    expect(serializer.serialize(otherPage)).toBe("doc:xwiki:Space.Other");
  });

  it("serializes a reference relative to the base reference", () => {
    expect(serializer.serialize(otherPage, page)).toBe("doc:Other");
    expect(
      serializer.serialize(new AttachmentReference("cat.png", page), page),
    ).toBe("attach:cat.png");
  });

  it("serializes a reference that parses back to itself against the same base", () => {
    const childPage = new DocumentReference(
      "WebHome",
      new SpaceReference(new WikiReference("xwiki"), "Space", "Page", "Child"),
      false,
    );
    const nestedPage = new DocumentReference(
      "WebHome",
      new SpaceReference(new WikiReference("xwiki"), "Space", "Page"),
      false,
    );
    for (const reference of [childPage, otherPage, page]) {
      expect(
        parser.parse(serializer.serialize(reference, nestedPage)!, {
          base: nestedPage,
        }),
      ).toEqual(reference);
    }
  });
});
