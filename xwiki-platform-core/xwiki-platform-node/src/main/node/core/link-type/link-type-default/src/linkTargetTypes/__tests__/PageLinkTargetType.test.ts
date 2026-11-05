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
import type { DocumentReference } from "@xwiki/platform-model-api";
import type {
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
const targetType = new PageLinkTargetType(
  { get: () => remoteURLParser },
  { get: () => remoteURLSerializer },
  { get: () => modelReferenceParser },
  { get: () => modelReferenceSerializer },
);

describe("PageLinkTargetType", () => {
  beforeEach(() => {
    mockReset(remoteURLParser);
    mockReset(remoteURLSerializer);
    mockReset(modelReferenceParser);
    mockReset(modelReferenceSerializer);
  });

  it("has the expected identity", async () => {
    expect(targetType.type).toBe("page");
    await expect(targetType.getLabel("en")).resolves.toBe("Page");
  });

  it("parses an empty URL as a page link with no reference selected yet", () => {
    expect(targetType.fromURL("")).toEqual({ ref: null });
  });

  it("parses a document URL, including its query string and anchor", () => {
    remoteURLParser.parse.mockReturnValue(documentRef);

    const config = targetType.fromURL(
      "https://xwiki.org/xwiki/bin/view/Some/Page?foo=bar#Anchor",
    );

    expect(config).toEqual({
      ref: documentRef,
      queryString: "?foo=bar",
      anchor: "#Anchor",
    });
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
      queryString: "?foo=bar",
      anchor: "#Anchor",
    });

    expect(url).toBe(
      "https://xwiki.org/xwiki/bin/view/Some/Page?foo=bar#Anchor",
    );
  });

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

  it("returns undefined when converting to a reference with no page selected", () => {
    expect(targetType.toReference?.({ ref: null })).toBeUndefined();
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
  });
});
