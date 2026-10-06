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

import {
  absoluteCristalEntityReference,
  absoluteXWikiEntityReference,
  toCristalEntityReference,
  toXWikiEntityReference,
} from "..";
import {
  AttachmentReference,
  DocumentReference,
  EntityType,
  SpaceReference,
  WikiReference,
} from "@xwiki/platform-model-api";
import {
  EntityReference as XWikiEntityReference,
  EntityType as XWikiEntityType,
  Model,
} from "@xwiki/platform-xwiki-model-api";
import { describe, expect, it } from "vitest";

const wiki = new WikiReference("xwiki");
const space = new SpaceReference(wiki, "Space1", "Space2");
const document = new DocumentReference("WebHome", space);

describe("toXWikiEntityReference", () => {
  it("converts a wiki reference", () => {
    expect(Model.serialize(toXWikiEntityReference(wiki))).toBe("xwiki");
  });

  it("converts a nested space reference", () => {
    const converted = toXWikiEntityReference(space);
    expect(converted.type).toBe(XWikiEntityType.SPACE);
    expect(Model.serialize(converted)).toBe("xwiki:Space1.Space2");
  });

  it("converts a document reference", () => {
    expect(Model.serialize(toXWikiEntityReference(document))).toBe(
      "xwiki:Space1.Space2.WebHome",
    );
  });

  it("converts an attachment reference", () => {
    const attachment = new AttachmentReference("logo.png", document);
    expect(Model.serialize(toXWikiEntityReference(attachment))).toBe(
      "xwiki:Space1.Space2.WebHome@logo.png",
    );
  });

  it("rejects a space reference without any space name", () => {
    expect(() => toXWikiEntityReference(new SpaceReference(wiki))).toThrow(
      "XWiki space references must have at least one space name.",
    );
  });
});

describe("toCristalEntityReference", () => {
  it("returns undefined when there is no reference", () => {
    expect(toCristalEntityReference(undefined)).toBeUndefined();
    expect(toCristalEntityReference(null)).toBeUndefined();
  });

  it("round-trips a document reference", () => {
    const result = toCristalEntityReference(
      toXWikiEntityReference(document),
    ) as DocumentReference;
    expect(result.type).toBe(EntityType.DOCUMENT);
    expect(result.name).toBe("WebHome");
    expect(result.space!.names).toEqual(["Space1", "Space2"]);
    expect(result.space!.wiki!.name).toBe("xwiki");
  });

  it("marks a non-WebHome document as terminal", () => {
    const terminal = new DocumentReference("Page", space);
    const result = toCristalEntityReference(
      toXWikiEntityReference(terminal),
    ) as DocumentReference;
    expect(result.terminal).toBe(true);
  });

  it("marks a WebHome document as non terminal", () => {
    const result = toCristalEntityReference(
      toXWikiEntityReference(document),
    ) as DocumentReference;
    expect(result.terminal).toBe(false);
  });

  it("round-trips an attachment reference", () => {
    const attachment = new AttachmentReference("logo.png", document);
    const result = toCristalEntityReference(
      toXWikiEntityReference(attachment),
    ) as AttachmentReference;
    expect(result.type).toBe(EntityType.ATTACHMENT);
    expect(result.name).toBe("logo.png");
    expect(result.document.name).toBe("WebHome");
  });
});

describe("absoluteXWikiEntityReference", () => {
  it("resolves a relative reference against the base reference", () => {
    const relative = new XWikiEntityReference("Page", XWikiEntityType.DOCUMENT);
    const base = Model.resolve(
      "subwiki:Base.WebHome",
      XWikiEntityType.DOCUMENT,
    )!;
    expect(Model.serialize(absoluteXWikiEntityReference(relative, base))).toBe(
      "subwiki:Base.Page",
    );
  });
});

describe("absoluteCristalEntityReference", () => {
  it("resolves against the base reference and converts back", () => {
    const base = Model.resolve(
      "subwiki:Base.WebHome",
      XWikiEntityType.DOCUMENT,
    )!;
    const result = absoluteCristalEntityReference(
      new DocumentReference("Page"),
      base,
    ) as DocumentReference;
    expect(result.name).toBe("Page");
    expect(result.space!.names).toEqual(["Base"]);
    expect(result.space!.wiki!.name).toBe("subwiki");
  });
});
