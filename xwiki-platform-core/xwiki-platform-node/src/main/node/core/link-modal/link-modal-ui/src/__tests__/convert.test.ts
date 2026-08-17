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
  linkTargetToResourceReference,
  resourceReferenceToLinkTarget,
} from "../convert";
import {
  AttachmentReference,
  DocumentReference,
  EntityType,
  SpaceReference,
  WikiReference,
} from "@xwiki/platform-model-api";
import { describe, expect, it } from "vitest";
import type { LinkTarget } from "../data/linkType";
import type { EntityReference } from "@xwiki/platform-model-api";
import type {
  ModelReferenceParser,
  ModelReferenceSerializer,
} from "@xwiki/platform-model-reference-api";

const space = new SpaceReference(new WikiReference("xwiki"), "Space");
const pageReference = new DocumentReference("Page", space, true);
const attachmentReference = new AttachmentReference(
  "image.gif",
  pageReference,
  {},
);

/**
 * Serializes like the XWiki model reference serializer, i.e. prefixing the reference with its type.
 */
const typedSerializer: ModelReferenceSerializer = {
  serialize(reference?: EntityReference): string | undefined {
    switch (reference?.type) {
      case EntityType.DOCUMENT:
        return "doc:xwiki:Space.Page";
      case EntityType.ATTACHMENT:
        return "attach:xwiki:Space.Page@image.gif";
      default:
        return undefined;
    }
  },
};

/**
 * Serializes without prefixing the reference with its type, which the serializer interface allows.
 */
const untypedSerializer: ModelReferenceSerializer = {
  serialize(reference?: EntityReference): string | undefined {
    switch (reference?.type) {
      case EntityType.DOCUMENT:
        return "xwiki:Space.Page";
      case EntityType.ATTACHMENT:
        return "xwiki:Space.Page@image.gif";
      default:
        return undefined;
    }
  },
};

const parser: ModelReferenceParser = {
  parse(reference: string): EntityReference {
    if (reference === "xwiki:Space.Page") {
      return pageReference;
    } else if (reference === "xwiki:Space.Page@image.gif") {
      return attachmentReference;
    }
    throw new Error(`[${reference}] is not an entity reference.`);
  },
  async parseAsync(reference: string): Promise<EntityReference> {
    return this.parse(reference);
  },
};

describe("linkTargetToResourceReference", () => {
  it("converts a page target, dropping the type prefix added by the serializer", () => {
    expect(
      linkTargetToResourceReference(
        { type: "page", config: { ref: pageReference } },
        typedSerializer,
      ),
    ).toEqual({
      type: "doc",
      typed: true,
      reference: "xwiki:Space.Page",
      parameters: {},
    });
  });

  it("converts a page target serialized without a type prefix", () => {
    expect(
      linkTargetToResourceReference(
        { type: "page", config: { ref: pageReference } },
        untypedSerializer,
      ),
    ).toEqual({
      type: "doc",
      typed: true,
      reference: "xwiki:Space.Page",
      parameters: {},
    });
  });

  it("converts an attachment target", () => {
    expect(
      linkTargetToResourceReference(
        { type: "attachment", config: { ref: attachmentReference } },
        typedSerializer,
      ),
    ).toEqual({
      type: "attach",
      typed: true,
      reference: "xwiki:Space.Page@image.gif",
      parameters: {},
    });
  });

  it("converts an URL target into an untyped reference", () => {
    expect(
      linkTargetToResourceReference(
        { type: "url", config: { url: "https://xwiki.org" } },
        typedSerializer,
      ),
    ).toEqual({
      type: "url",
      typed: false,
      reference: "https://xwiki.org",
      parameters: {},
    });
  });

  it("converts an e-mail target into a typed mailto reference", () => {
    expect(
      linkTargetToResourceReference(
        { type: "email", config: { address: "alice@xwiki.org" } },
        typedSerializer,
      ),
    ).toEqual({
      type: "mailto",
      typed: true,
      reference: "alice@xwiki.org",
      parameters: {},
    });
  });

  it("returns nothing when the target doesn't designate a resource yet", () => {
    const targets: LinkTarget[] = [
      { type: "page", config: { ref: null } },
      { type: "attachment", config: { ref: null } },
      { type: "url", config: { url: "  " } },
      { type: "email", config: { address: "" } },
    ];
    targets.forEach((target) => {
      expect(linkTargetToResourceReference(target, typedSerializer)).toBe(
        undefined,
      );
    });
  });
});

describe("resourceReferenceToLinkTarget", () => {
  it("converts a document reference", () => {
    expect(
      resourceReferenceToLinkTarget(
        {
          type: "doc",
          typed: true,
          reference: "xwiki:Space.Page",
          parameters: {},
        },
        parser,
      ),
    ).toEqual({ type: "page", config: { ref: pageReference } });
  });

  it("converts an attachment reference", () => {
    expect(
      resourceReferenceToLinkTarget(
        {
          type: "attach",
          typed: true,
          reference: "xwiki:Space.Page@image.gif",
          parameters: {},
        },
        parser,
      ),
    ).toEqual({ type: "attachment", config: { ref: attachmentReference } });
  });

  it("converts a mailto reference", () => {
    expect(
      resourceReferenceToLinkTarget(
        {
          type: "mailto",
          typed: true,
          reference: "alice@xwiki.org",
          parameters: {},
        },
        parser,
      ),
    ).toEqual({ type: "email", config: { address: "alice@xwiki.org" } });
  });

  it("converts the references that have no dedicated configuration into an URL", () => {
    ["url", "path", "unc", "unknown"].forEach((type) => {
      expect(
        resourceReferenceToLinkTarget(
          { type, typed: false, reference: "some/target", parameters: {} },
          parser,
        ),
      ).toEqual({ type: "url", config: { url: "some/target" } });
    });
  });

  it("returns nothing when the resource type has no matching link target", () => {
    [
      "page",
      "pageAttach",
      "space",
      "interwiki",
      "icon",
      "data",
      "user",
    ].forEach((type) => {
      expect(
        resourceReferenceToLinkTarget(
          { type, typed: true, reference: "Space.Page", parameters: {} },
          parser,
        ),
      ).toBe(undefined);
    });
  });

  it("returns nothing when the entity reference can't be parsed", () => {
    expect(
      resourceReferenceToLinkTarget(
        { type: "doc", typed: true, reference: "@@@", parameters: {} },
        parser,
      ),
    ).toBe(undefined);
  });

  it("is the reverse of linkTargetToResourceReference", () => {
    const targets: LinkTarget[] = [
      { type: "page", config: { ref: pageReference } },
      { type: "attachment", config: { ref: attachmentReference } },
      { type: "url", config: { url: "https://xwiki.org" } },
      { type: "email", config: { address: "alice@xwiki.org" } },
    ];
    targets.forEach((target) => {
      const reference = linkTargetToResourceReference(
        target,
        untypedSerializer,
      );
      expect(resourceReferenceToLinkTarget(reference!, parser)).toEqual(target);
    });
  });
});
