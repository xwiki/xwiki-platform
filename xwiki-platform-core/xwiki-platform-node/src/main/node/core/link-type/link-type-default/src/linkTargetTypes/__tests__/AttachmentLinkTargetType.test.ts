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
import { AttachmentLinkTargetType } from "../AttachmentLinkTargetType";
import { EntityType } from "@xwiki/platform-model-api";
import { beforeEach, describe, expect, it } from "vitest";
import { mock, mockReset } from "vitest-mock-extended";
import type { AttachmentReference } from "@xwiki/platform-model-api";
import type {
  ModelReferenceParser,
  ModelReferenceSerializer,
} from "@xwiki/platform-model-reference-api";
import type {
  RemoteURLParser,
  RemoteURLSerializer,
} from "@xwiki/platform-model-remote-url-api";

const attachmentRef = {
  type: EntityType.ATTACHMENT,
} as unknown as AttachmentReference;

// The link target type resolves its services through the providers each time it uses them, so the mocks can be reset
// between the tests.
const remoteURLParser = mock<RemoteURLParser>();
const remoteURLSerializer = mock<RemoteURLSerializer>();
const modelReferenceParser = mock<ModelReferenceParser>();
const modelReferenceSerializer = mock<ModelReferenceSerializer>();
const targetType = new AttachmentLinkTargetType(
  { get: () => remoteURLParser },
  { get: () => remoteURLSerializer },
  { get: () => modelReferenceParser },
  { get: () => modelReferenceSerializer },
);

describe("AttachmentLinkTargetType", () => {
  beforeEach(() => {
    mockReset(remoteURLParser);
    mockReset(remoteURLSerializer);
    mockReset(modelReferenceParser);
    mockReset(modelReferenceSerializer);
  });

  it("has the expected identity", async () => {
    expect(targetType.type).toBe("attachment");
    await expect(targetType.getLabel("en")).resolves.toBe("Attachment");
  });

  describe("URL", () => {
    it("parses the query string of a relative attachment URL", () => {
      remoteURLParser.parse.mockReturnValue(attachmentRef);

      const config = targetType.fromURL(
        "/xwiki/bin/download/Some/Page/file.png?width=100",
      );

      expect(config).toEqual({ ref: attachmentRef, queryString: "width=100" });
    });

    it("parses an attachment URL, including its query string, without its URL separator", () => {
      remoteURLParser.parse.mockReturnValue(attachmentRef);

      const config = targetType.fromURL(
        "https://xwiki.org/xwiki/bin/download/Some/Page/file.png?foo=bar",
      );

      expect(config).toEqual({ ref: attachmentRef, queryString: "foo=bar" });
    });

    it("does not match a non-attachment URL", () => {
      remoteURLParser.parse.mockReturnValue(undefined);

      expect(targetType.fromURL("https://example.org")).toBeNull();
    });

    it("serializes an attachment config back into a URL, including its query string", () => {
      remoteURLSerializer.serialize.mockReturnValue(
        "https://xwiki.org/xwiki/bin/download/Some/Page/file.png",
      );

      const url = targetType.toURL({
        ref: attachmentRef,
        queryString: "foo=bar",
      });

      expect(url).toBe(
        "https://xwiki.org/xwiki/bin/download/Some/Page/file.png?foo=bar",
      );
    });

    it("serializes an attachment config into a URL without query string when it is empty", () => {
      remoteURLSerializer.serialize.mockReturnValue(
        "https://xwiki.org/xwiki/bin/download/Some/Page/file.png",
      );

      expect(targetType.toURL({ ref: attachmentRef, queryString: " " })).toBe(
        "https://xwiki.org/xwiki/bin/download/Some/Page/file.png",
      );
    });
  });

  describe("reference", () => {
    it("parses an attachment reference, including its query string", () => {
      modelReferenceParser.parse.mockReturnValue(attachmentRef);

      const config = targetType.fromReference?.({
        type: "attach",
        typed: true,
        reference: "Some.Page@file.png",
        parameters: { queryString: "foo=bar" },
      });

      expect(config).toEqual({ ref: attachmentRef, queryString: "foo=bar" });
    });

    it("does not match a non-attachment reference", () => {
      expect(
        targetType.fromReference?.({
          type: "doc",
          typed: true,
          reference: "Some.Page",
          parameters: {},
        }),
      ).toBeNull();
    });

    it("converts an attachment config back into a reference", () => {
      modelReferenceSerializer.serialize.mockReturnValue(
        "attach:Some.Page@file.png",
      );

      const reference = targetType.toReference?.({ ref: attachmentRef });

      expect(reference).toEqual({
        type: "attach",
        typed: true,
        reference: "Some.Page@file.png",
        parameters: {},
      });
    });

    it("keeps the query string in the parameters of the reference, without its URL separator", () => {
      modelReferenceSerializer.serialize.mockReturnValue(
        "attach:Some.Page@file.png",
      );

      const reference = targetType.toReference?.({
        ref: attachmentRef,
        queryString: "?foo=bar",
      });

      expect(reference?.parameters).toEqual({ queryString: "foo=bar" });
    });

    it("returns undefined when converting to a reference with no attachment selected", () => {
      expect(targetType.toReference?.({ ref: null })).toBeUndefined();
    });
  });

  it("labels the link with the name of the attached file", () => {
    expect(
      targetType.getDefaultLabel({
        ref: { ...attachmentRef, name: "file.png" } as AttachmentReference,
        queryString: "foo=bar",
      }),
    ).toBe("file.png");
    expect(targetType.getDefaultLabel({ ref: null })).toBe("");
  });
});
