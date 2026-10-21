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

import * as XWiki from "..";
import { describe, expect, it } from "vitest";

const wikiJSON = { name: "xwiki", parent: null, type: "WIKI" };
const mainSpaceJSON = { name: "Main", parent: wikiJSON, type: "SPACE" };
const pageSpaceJSON = { name: "Page", parent: mainSpaceJSON, type: "SPACE" };
const pageDocumentJSON = {
  name: "Page",
  parent: mainSpaceJSON,
  type: "DOCUMENT",
  locale: "",
};
const webHomeDocumentJSON = {
  name: "WebHome",
  parent: pageSpaceJSON,
  type: "DOCUMENT",
  locale: "",
};

// Input JSON data.
const jsonTree: XWiki.EntityReferenceTreeNodeJSON = {
  reference: wikiJSON,
  children: [
    {
      reference: mainSpaceJSON,
      children: [
        {
          reference: pageSpaceJSON,
          children: [
            {
              reference: webHomeDocumentJSON,
              children: [],
              locales: [webHomeDocumentJSON],
            },
          ],
          locales: [],
        },
        {
          reference: pageDocumentJSON,
          children: [],
          locales: [pageDocumentJSON],
        },
      ],
      locales: [],
    },
  ],
  locales: [],
};

describe("EntityReferenceTree", () => {
  const tree = XWiki.EntityReferenceTree.fromJSONObject(jsonTree);
  const mainSpaceNode = tree.children["Main"][XWiki.EntityType.SPACE];
  const pageSpaceNode = mainSpaceNode.children["Page"][XWiki.EntityType.SPACE];

  it("fromJSONObject reads the root reference", () => {
    expect(tree.reference!.name).toEqual("xwiki");
    expect(tree.reference!.type).toEqual(XWiki.EntityType.WIKI);
  });

  it("fromJSONObject reads the Main space", () => {
    expect(tree.children["Main"]).not.toBeUndefined();
    expect(tree.children["Main"][XWiki.EntityType.SPACE]).not.toBeUndefined();
    expect(tree.children["Main"][XWiki.EntityType.WIKI]).toBeUndefined();
    expect(tree.children["Main"][XWiki.EntityType.DOCUMENT]).toBeUndefined();
    expect(mainSpaceNode.reference!.name).toEqual("Main");
    expect(mainSpaceNode.reference!.type).toEqual(XWiki.EntityType.SPACE);
  });

  it("fromJSONObject supports a space and a document with the same name", () => {
    const children = mainSpaceNode.children["Page"];
    expect(children).not.toBeUndefined();
    expect(children[XWiki.EntityType.DOCUMENT]).not.toBeUndefined();
    expect(children[XWiki.EntityType.SPACE]).not.toBeUndefined();
    expect(children[XWiki.EntityType.WIKI]).toBeUndefined();

    const pageDocumentNode = children[XWiki.EntityType.DOCUMENT];
    expect(pageDocumentNode.reference!.name).toEqual("Page");
    expect(pageDocumentNode.reference!.type).toEqual(XWiki.EntityType.DOCUMENT);
    expect(pageSpaceNode.reference!.name).toEqual("Page");
    expect(pageSpaceNode.reference!.type).toEqual(XWiki.EntityType.SPACE);
  });

  it("fromJSONObject reads the nested WebHome document", () => {
    const children = pageSpaceNode.children["WebHome"];
    expect(children).not.toBeUndefined();
    expect(children[XWiki.EntityType.DOCUMENT]).not.toBeUndefined();
    expect(children[XWiki.EntityType.SPACE]).toBeUndefined();
    expect(children[XWiki.EntityType.WIKI]).toBeUndefined();

    const webHomeDocumentNode = children[XWiki.EntityType.DOCUMENT];
    expect(webHomeDocumentNode.reference!.name).toEqual("WebHome");
    expect(webHomeDocumentNode.reference!.type).toEqual(
      XWiki.EntityType.DOCUMENT,
    );
  });

  function expectChild(
    treeNode: XWiki.EntityReferenceTreeNode,
    stringReferencePath: string,
    referenceType: number,
    expectedNodeStringReference: string | null,
  ): void {
    const referencePath = XWiki.Model.resolve(
      stringReferencePath,
      referenceType,
    )!;
    const node = treeNode.getChildByReference(referencePath);
    if (expectedNodeStringReference === null) {
      expect(node).toBeNull();
      return;
    }
    const expected = XWiki.Model.resolve(
      expectedNodeStringReference,
      referenceType,
    )!;
    if (expected.type === XWiki.EntityType.DOCUMENT) {
      // The resolver does not set any locale but the JSON specifies '' locales for documents.
      expected.locale = "";
    }
    expect(
      node,
      `no node for the path [${stringReferencePath}]`,
    ).not.toBeNull();
    expect(node!.reference).toEqual(expected);
  }

  it("getChildByReference", () => {
    // From the tree root.
    expectChild(tree, "Main", XWiki.EntityType.SPACE, "xwiki:Main");
    expectChild(
      tree,
      "Main.Page",
      XWiki.EntityType.DOCUMENT,
      "xwiki:Main.Page",
    );
    expectChild(tree, "Main.Page", XWiki.EntityType.SPACE, "xwiki:Main.Page");
    expectChild(
      tree,
      "Main.Page.WebHome",
      XWiki.EntityType.DOCUMENT,
      "xwiki:Main.Page.WebHome",
    );

    // From the Main Space node.
    expectChild(
      mainSpaceNode,
      "Page",
      XWiki.EntityType.DOCUMENT,
      "xwiki:Main.Page",
    );

    // Node not found.
    expectChild(tree, "NotFound", XWiki.EntityType.SPACE, null);
  });

  it("getChildByReference returns null when no path is given", () => {
    expect(tree.getChildByReference(undefined)).toBeNull();
  });
});
