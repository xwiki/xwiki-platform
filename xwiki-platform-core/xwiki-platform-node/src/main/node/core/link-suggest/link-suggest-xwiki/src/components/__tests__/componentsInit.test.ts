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

import { ComponentInit } from "../componentsInit";
import { LinkType, name } from "@xwiki/platform-link-suggest-api";
import { EntityType } from "@xwiki/platform-model-api";
import { Container } from "inversify";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type {
  Link,
  LinkSuggestService,
} from "@xwiki/platform-link-suggest-api";

/**
 * Builds the container the link suggest service is resolved from, with just enough of its
 * dependencies for the service to run: the entity reference parser and the URL serializer are
 * identity-like so that the tests can assert on the raw reference the service produces, and on the
 * type of entity it designates (the parser prefixes the reference with it).
 */
function buildContainer(): Container {
  const container = new Container();
  container.bind("CristalApp").toConstantValue({
    getWikiConfig: () => ({ baseURL: "http://localhost/xwiki" }),
  });
  container.bind("ModelReferenceParserProvider").toConstantValue({
    get: () => ({
      parse: (reference: string, options?: { type?: EntityType }) =>
        `${EntityType[options?.type ?? -1] ?? "untyped"}/${reference}`,
    }),
  });
  container.bind("RemoteURLSerializerProvider").toConstantValue({
    get: () => ({
      serialize: (reference: string) => `http://localhost/xwiki/${reference}`,
    }),
  });
  container.bind("AuthenticationManagerProvider").toConstantValue({
    get: () => undefined,
  });
  new ComponentInit(container);
  return container;
}

function mockSolrResults(results: unknown[]): void {
  vi.stubGlobal(
    "fetch",
    vi.fn().mockResolvedValue({ json: async () => results }),
  );
}

function getLinks(
  query: string,
  type?: LinkType,
  mimetype?: string,
): Promise<Link[]> {
  return buildContainer()
    .get<LinkSuggestService>(name, { name: "XWiki" })
    .getLinks(query, type, mimetype);
}

/**
 * @returns the parameters of the request made to the search suggest service
 */
function getRequestParameters(): URLSearchParams {
  const [url] = vi.mocked(fetch).mock.calls[0];
  return new URL(url as string).searchParams;
}

describe("XWikiLinkSuggestService", () => {
  beforeEach(() => {
    vi.unstubAllGlobals();
  });

  it("drops the entity type prefix of the Solr reference of a page", async () => {
    // The Solr "reference" field prefixes the serialized entity reference with the lower case entity
    // type. Keeping that prefix would make the reference resolver (which parses from the right) take
    // "document:wiki" for the wiki name.
    mockSolrResults([
      {
        id: "wiki:Space.Page_en",
        type: "DOCUMENT",
        reference: "document:wiki:Space.Page",
        title_: "Some Page",
        location: "Space",
      },
    ]);

    expect(await getLinks("Some")).toEqual([
      {
        type: LinkType.PAGE,
        id: "wiki:Space.Page_en",
        url: "http://localhost/xwiki/DOCUMENT/wiki:Space.Page",
        reference: "wiki:Space.Page",
        label: "Some Page",
        hint: "Space",
      },
    ]);
  });

  it("drops the entity type prefix of the Solr reference of an attachment", async () => {
    mockSolrResults([
      {
        id: "wiki:Space.Page@image.gif",
        type: "ATTACHMENT",
        reference: "attachment:wiki:Space.Page@image.gif",
        filename: ["image.gif"],
        location: "Space / Page",
      },
    ]);

    expect(await getLinks("image")).toEqual([
      {
        type: LinkType.ATTACHMENT,
        id: "wiki:Space.Page@image.gif",
        url: "http://localhost/xwiki/ATTACHMENT/wiki:Space.Page@image.gif",
        reference: "wiki:Space.Page@image.gif",
        label: "image.gif",
        hint: "Space / Page",
      },
    ]);
  });

  it("leaves a reference that has no entity type prefix untouched", async () => {
    mockSolrResults([
      {
        id: "wiki:Space.Page_en",
        type: "DOCUMENT",
        reference: "wiki:Space.Page",
        title_: "Some Page",
      },
    ]);

    expect((await getLinks("Some"))[0].reference).toBe("wiki:Space.Page");
  });

  it("uses the location of the suggested entity, trimmed and unescaped, as its hint", async () => {
    mockSolrResults([
      {
        id: "wiki:Space.Page_en",
        type: "DOCUMENT",
        reference: "document:wiki:Space.Page",
        title_: "Some Page",
        // The location is XML escaped and surrounded by the white space of the Velocity code producing it.
        location:
          "\n      R&#38;D / Cats &amp; dogs &#x7B;1&#125; &#60;new&#62;\n  ",
      },
    ]);

    expect((await getLinks("Some"))[0].hint).toBe(
      "R&D / Cats & dogs {1} <new>",
    );
  });

  it("keeps the invalid character references of the location as they are", async () => {
    mockSolrResults([
      {
        id: "wiki:Space.Page_en",
        type: "DOCUMENT",
        reference: "document:wiki:Space.Page",
        title_: "Some Page",
        location: "Space &#x110000; &unknown;",
      },
    ]);

    expect((await getLinks("Some"))[0].hint).toBe("Space &#x110000; &unknown;");
  });

  it("leaves the hint empty when the suggested entity has no location", async () => {
    mockSolrResults([
      {
        id: "wiki:Main.WebHome_en",
        type: "DOCUMENT",
        reference: "document:wiki:Main.WebHome",
        title_: "Home",
      },
    ]);

    expect((await getLinks("Home"))[0].hint).toBe("");
  });
});

describe("XWikiLinkSuggestService search request", () => {
  beforeEach(() => {
    vi.unstubAllGlobals();
  });

  it("searches for each word of the query", async () => {
    mockSolrResults([]);

    await getLinks(" cat.png ", LinkType.ATTACHMENT, "image/*");

    const parameters = getRequestParameters();
    expect(parameters.get("input")).toBe("*cat* *png*");
    expect(parameters.get("query")).toBe(
      "fq=locale:*\nfq=wiki:xwiki\nfq=type:ATTACHMENT\nfq=mimetype:((image/*))",
    );
  });

  it("suggests the most recent attachments for an empty query", async () => {
    mockSolrResults([]);

    await getLinks("", LinkType.ATTACHMENT, "image/*");

    const parameters = getRequestParameters();
    expect(parameters.get("input")).toBe("*");
    expect(parameters.get("query")).toBe(
      "fq=locale:*\nfq=wiki:xwiki\nfq=type:ATTACHMENT\nfq=mimetype:((image/*))\n" +
        "sort=attdate_sort desc",
    );
  });

  it("suggests the most recent pages for a query without words", async () => {
    mockSolrResults([]);

    await getLinks(" . ", LinkType.PAGE);

    const parameters = getRequestParameters();
    expect(parameters.get("input")).toBe("*");
    expect(parameters.get("query")).toBe(
      "fq=locale:*\nfq=wiki:xwiki\nfq=type:DOCUMENT\nsort=date desc",
    );
  });

  it("searches all the entity types when no type is specified", async () => {
    mockSolrResults([]);

    await getLinks("cat");

    expect(getRequestParameters().get("query")).toBe(
      "fq=locale:*\nfq=wiki:xwiki",
    );
  });
});
