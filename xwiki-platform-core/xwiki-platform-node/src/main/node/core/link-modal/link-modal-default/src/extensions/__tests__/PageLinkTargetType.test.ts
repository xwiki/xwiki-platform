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
import { describe, expect, it } from "vitest";
import { mock } from "vitest-mock-extended";
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

describe("PageLinkTargetType", () => {
  const extension = new PageLinkTargetType();

  it("has the expected identity", () => {
    expect(extension.type).toBe("page");
    expect(extension.getLabel("en")).toBe("Page");
  });

  it("parses an empty URL as a page link with no reference selected yet", () => {
    const remoteURLParser = mock<RemoteURLParser>();
    const remoteURLSerializer = mock<RemoteURLSerializer>();

    expect(
      extension.tryParseUrl("", { remoteURLParser, remoteURLSerializer }),
    ).toEqual({ ref: null });
  });

  it("parses a document URL, including its query string and anchor", () => {
    const remoteURLParser = mock<RemoteURLParser>();
    remoteURLParser.parse.mockReturnValue(documentRef);
    const remoteURLSerializer = mock<RemoteURLSerializer>();

    const config = extension.tryParseUrl(
      "https://xwiki.org/xwiki/bin/view/Some/Page?foo=bar#Anchor",
      { remoteURLParser, remoteURLSerializer },
    );

    expect(config).toEqual({
      ref: documentRef,
      queryString: "?foo=bar",
      anchor: "#Anchor",
    });
  });

  it("does not match a non-document URL", () => {
    const remoteURLParser = mock<RemoteURLParser>();
    remoteURLParser.parse.mockReturnValue(undefined);
    const remoteURLSerializer = mock<RemoteURLSerializer>();

    expect(
      extension.tryParseUrl("https://example.org", {
        remoteURLParser,
        remoteURLSerializer,
      }),
    ).toBeNull();
  });

  it("serializes a page config back into a URL, including query string and anchor", () => {
    const remoteURLParser = mock<RemoteURLParser>();
    const remoteURLSerializer = mock<RemoteURLSerializer>();
    remoteURLSerializer.serialize.mockReturnValue(
      "https://xwiki.org/xwiki/bin/view/Some/Page",
    );

    const url = extension.serializeUrl(
      { ref: documentRef, queryString: "?foo=bar", anchor: "#Anchor" },
      { remoteURLParser, remoteURLSerializer },
    );

    expect(url).toBe(
      "https://xwiki.org/xwiki/bin/view/Some/Page?foo=bar#Anchor",
    );
  });

  it("parses a document reference", () => {
    const modelReferenceParser = mock<ModelReferenceParser>();
    modelReferenceParser.parse.mockReturnValue(documentRef);
    const modelReferenceSerializer = mock<ModelReferenceSerializer>();

    const config = extension.tryParseReference?.(
      { type: "doc", typed: true, reference: "Some.Page", parameters: {} },
      { modelReferenceParser, modelReferenceSerializer },
    );

    expect(config).toEqual({ ref: documentRef });
  });

  it("does not match a non-document reference", () => {
    const modelReferenceParser = mock<ModelReferenceParser>();
    const modelReferenceSerializer = mock<ModelReferenceSerializer>();

    expect(
      extension.tryParseReference?.(
        { type: "attach", typed: true, reference: "Some.Page@file.png", parameters: {} },
        { modelReferenceParser, modelReferenceSerializer },
      ),
    ).toBeNull();
  });

  it("converts a page config back into a reference, dropping the type prefix added by the serializer", () => {
    const modelReferenceParser = mock<ModelReferenceParser>();
    const modelReferenceSerializer = mock<ModelReferenceSerializer>();
    modelReferenceSerializer.serialize.mockReturnValue("doc:Some.Page");

    const reference = extension.configToReference?.(
      { ref: documentRef },
      { modelReferenceParser, modelReferenceSerializer },
    );

    expect(reference).toEqual({
      type: "doc",
      typed: true,
      reference: "Some.Page",
      parameters: {},
    });
  });

  it("returns undefined when converting to a reference with no page selected", () => {
    const modelReferenceParser = mock<ModelReferenceParser>();
    const modelReferenceSerializer = mock<ModelReferenceSerializer>();

    expect(
      extension.configToReference?.(
        { ref: null },
        { modelReferenceParser, modelReferenceSerializer },
      ),
    ).toBeUndefined();
  });
});
