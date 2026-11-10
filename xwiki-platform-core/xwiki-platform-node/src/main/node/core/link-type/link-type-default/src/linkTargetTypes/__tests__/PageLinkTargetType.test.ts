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
import { PageLinkTargetType } from "../PageLinkTargetType";
import { EntityType } from "@xwiki/platform-model-api";
import { beforeEach, describe, expect, it } from "vitest";
import { mock, mockReset } from "vitest-mock-extended";
import type { LinkContext } from "@xwiki/platform-link-type-api";
import type { DocumentReference } from "@xwiki/platform-model-api";
import type {
  ModelReferenceHandler,
  ModelReferenceParser,
  ModelReferenceSerializer,
} from "@xwiki/platform-model-reference-api";
import type {
  RemoteURLParser,
  RemoteURLSerializer,
} from "@xwiki/platform-model-remote-url-api";

const documentRef = {
  type: EntityType.DOCUMENT,
} as unknown as DocumentReference;

// The link target type resolves its services through the providers each time it uses them, so the mocks can be reset
// between the tests.
const remoteURLParser = mock<RemoteURLParser>();
const remoteURLSerializer = mock<RemoteURLSerializer>();
const modelReferenceParser = mock<ModelReferenceParser>();
const modelReferenceSerializer = mock<ModelReferenceSerializer>();
const modelReferenceHandler = mock<ModelReferenceHandler>();
const targetType = new PageLinkTargetType(
  { get: () => remoteURLParser },
  { get: () => remoteURLSerializer },
  { get: () => modelReferenceParser },
  { get: () => modelReferenceSerializer },
  { get: () => modelReferenceHandler },
);

describe("PageLinkTargetType", () => {
  beforeEach(() => {
    mockReset(remoteURLParser);
    mockReset(remoteURLSerializer);
    mockReset(modelReferenceParser);
    mockReset(modelReferenceSerializer);
    mockReset(modelReferenceHandler);
  });

  it("has the expected identity", async () => {
    expect(targetType.type).toBe("page");
    await expect(targetType.getLabel("en")).resolves.toBe("Page");
  });

  describe("URL", () => {
    it("parses an empty URL as a page link with no reference selected yet", () => {
      expect(targetType.fromURL("")).toEqual({ ref: null });
    });

    it("parses a document URL, including its query string and anchor, without their URL separators", () => {
      remoteURLParser.parse.mockReturnValue(documentRef);

      const config = targetType.fromURL(
        "https://xwiki.org/xwiki/bin/view/Some/Page?foo=bar#Anchor",
      );

      expect(config).toEqual({
        ref: documentRef,
        queryString: "foo=bar",
        anchor: "Anchor",
      });
    });

    it("parses the query string and the anchor of a relative document URL", () => {
      remoteURLParser.parse.mockReturnValue(documentRef);

      const config = targetType.fromURL(
        "/xwiki/bin/view/Some/Page?color=red&age=13#HTest",
      );

      expect(config).toEqual({
        ref: documentRef,
        queryString: "color=red&age=13",
        anchor: "HTest",
      });
    });

    it("decodes the anchor of a document URL", () => {
      remoteURLParser.parse.mockReturnValue(documentRef);

      const config = targetType.fromURL(
        "https://xwiki.org/xwiki/bin/view/Some/Page#H%C3%89t%C3%A9",
      );

      expect(config?.anchor).toBe("HÉté");
    });

    it("does not match a non-document URL", () => {
      remoteURLParser.parse.mockReturnValue(undefined);

      expect(targetType.fromURL("https://example.org")).toBeNull();
    });

    it("serializes a page config back into a URL, including query string and anchor", () => {
      remoteURLSerializer.serialize.mockReturnValue(
        "https://xwiki.org/xwiki/bin/view/Some/Page",
      );

      const url = targetType.toURL({
        ref: documentRef,
        queryString: "foo=bar",
        anchor: "HÉté",
      });

      expect(url).toBe(
        "https://xwiki.org/xwiki/bin/view/Some/Page?foo=bar#H%C3%89t%C3%A9",
      );
    });

    it("serializes a page config into a URL without the query string and anchor that are empty", () => {
      remoteURLSerializer.serialize.mockReturnValue(
        "https://xwiki.org/xwiki/bin/view/Some/Page",
      );

      expect(
        targetType.toURL({ ref: documentRef, queryString: "", anchor: " " }),
      ).toBe("https://xwiki.org/xwiki/bin/view/Some/Page");
    });
  });

  describe("reference", () => {
    it("parses a document reference", () => {
      modelReferenceParser.parse.mockReturnValue(documentRef);

      const config = targetType.fromReference?.({
        type: "doc",
        typed: true,
        reference: "Some.Page",
        parameters: {},
      });

      expect(config).toEqual({ ref: documentRef });
    });

    it("parses the query string and the anchor of a document reference", () => {
      modelReferenceParser.parse.mockReturnValue(documentRef);

      const config = targetType.fromReference?.({
        type: "doc",
        typed: true,
        reference: "Some.Page",
        parameters: { queryString: "foo=bar", anchor: "HSection" },
      });

      expect(config).toEqual({
        ref: documentRef,
        queryString: "foo=bar",
        anchor: "HSection",
      });
    });

    it("does not match a non-document reference", () => {
      expect(
        targetType.fromReference?.({
          type: "attach",
          typed: true,
          reference: "Some.Page@file.png",
          parameters: {},
        }),
      ).toBeNull();
    });

    it("converts a page config back into a reference, dropping the type prefix added by the serializer", () => {
      modelReferenceSerializer.serialize.mockReturnValue("doc:Some.Page");

      const reference = targetType.toReference?.({ ref: documentRef });

      expect(reference).toEqual({
        type: "doc",
        typed: true,
        reference: "Some.Page",
        parameters: {},
      });
    });

    it("keeps the query string and the anchor in the parameters of the reference, without their URL separators", () => {
      modelReferenceSerializer.serialize.mockReturnValue("doc:Some.Page");

      const reference = targetType.toReference?.({
        ref: documentRef,
        queryString: " ?foo=bar",
        anchor: "#HSection",
      });

      expect(reference).toEqual({
        type: "doc",
        typed: true,
        reference: "Some.Page",
        parameters: { queryString: "foo=bar", anchor: "HSection" },
      });
    });

    it("leaves the empty query string and anchor out of the parameters of the reference", () => {
      modelReferenceSerializer.serialize.mockReturnValue("doc:Some.Page");

      const reference = targetType.toReference?.({
        ref: documentRef,
        queryString: "",
        anchor: "#",
      });

      expect(reference?.parameters).toEqual({});
    });

    it("returns undefined when converting to a reference with no page selected", () => {
      expect(targetType.toReference?.({ ref: null })).toBeUndefined();
    });

    it("leaves the title of the page out of the reference", () => {
      modelReferenceSerializer.serialize.mockReturnValue("doc:Some.Page");

      expect(
        targetType.toReference?.({ ref: documentRef, title: "Some title" }),
      ).toEqual({
        type: "doc",
        typed: true,
        reference: "Some.Page",
        parameters: {},
      });
    });
  });

  describe("default label", () => {
    it("is the title of the page when it's known, e.g. from the picked suggestion", () => {
      expect(
        targetType.getDefaultLabel({ ref: documentRef, title: " Some title " }),
      ).toBe("Some title");
      expect(modelReferenceHandler.getTitle).not.toHaveBeenCalled();
    });

    it("is the name of the page otherwise, as the backend displays it (e.g. not WebHome for a nested page)", () => {
      modelReferenceHandler.getTitle.mockReturnValue("Page");

      expect(targetType.getDefaultLabel({ ref: documentRef })).toBe("Page");
      expect(targetType.getDefaultLabel({ ref: documentRef, title: "" })).toBe(
        "Page",
      );
      expect(modelReferenceHandler.getTitle).toHaveBeenCalledWith(documentRef);
    });

    it("is empty when no page is selected", () => {
      expect(targetType.getDefaultLabel({ ref: null })).toBe("");
    });
  });
});

describe("PageLinkTargetType without the services of the current wiki", () => {
  it("matches nothing when the current wiki has no model or remote URL service", () => {
    const noServices = { get: () => undefined };
    const targetTypeWithoutServices = new PageLinkTargetType(
      noServices,
      noServices,
      noServices,
      noServices,
      noServices,
    );

    expect(
      targetTypeWithoutServices.fromURL("https://example.org/Some/Page"),
    ).toBeNull();
    expect(
      targetTypeWithoutServices.fromReference({
        type: "doc",
        typed: true,
        reference: "Some.Page",
        parameters: {},
      }),
    ).toBeNull();
    expect(
      targetTypeWithoutServices.toReference({ ref: documentRef }),
    ).toBeUndefined();
    expect(
      targetTypeWithoutServices.getDefaultLabel({ ref: documentRef }),
    ).toBe("");
  });

  describe("relative to the source document of the link", () => {
    const sourceDocument = {
      type: EntityType.DOCUMENT,
      name: "Source",
    } as unknown as DocumentReference;
    const context: LinkContext = { sourceDocument };

    it("resolves the reference against the source document", () => {
      modelReferenceParser.parse.mockReturnValue(documentRef);

      const config = targetType.fromReference?.(
        { type: "doc", typed: true, reference: "Other", parameters: {} },
        context,
      );

      expect(config?.ref).toBe(documentRef);
      expect(modelReferenceParser.parse).toHaveBeenCalledWith("Other", {
        type: EntityType.DOCUMENT,
        base: sourceDocument,
      });
    });

    it("serializes the reference relative to the source document", () => {
      modelReferenceSerializer.serialize.mockReturnValue("doc:Other");

      const reference = targetType.toReference?.({ ref: documentRef }, context);

      expect(reference?.reference).toBe("Other");
      expect(modelReferenceSerializer.serialize).toHaveBeenCalledWith(
        documentRef,
        sourceDocument,
      );
    });
  });
});
