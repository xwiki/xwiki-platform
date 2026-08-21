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
import { Container } from "inversify";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type {
  Link,
  LinkSuggestService,
} from "@xwiki/platform-link-suggest-api";

/**
 * Builds the container the link suggest service is resolved from, with just enough of its
 * dependencies for the service to run: the entity reference parser and the URL serializer are
 * identity-like so that the tests can assert on the raw reference the service produces.
 */
function buildContainer(): Container {
  const container = new Container();
  container.bind("CristalApp").toConstantValue({
    getWikiConfig: () => ({ baseURL: "http://localhost/xwiki" }),
  });
  container.bind("ModelReferenceParserProvider").toConstantValue({
    get: () => ({ parse: (reference: string) => reference }),
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

function getLinks(query: string): Promise<Link[]> {
  return buildContainer()
    .get<LinkSuggestService>(name, { name: "XWiki" })
    .getLinks(query);
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
      },
    ]);

    expect(await getLinks("Some")).toEqual([
      {
        type: LinkType.PAGE,
        id: "wiki:Space.Page_en",
        url: "http://localhost/xwiki/wiki:Space.Page",
        reference: "wiki:Space.Page",
        label: "Some Page",
        hint: "Some Page",
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
      },
    ]);

    expect(await getLinks("image")).toEqual([
      {
        type: LinkType.ATTACHMENT,
        id: "wiki:Space.Page@image.gif",
        url: "http://localhost/xwiki/wiki:Space.Page@image.gif",
        reference: "wiki:Space.Page@image.gif",
        label: "image.gif",
        hint: "image.gif",
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
});
