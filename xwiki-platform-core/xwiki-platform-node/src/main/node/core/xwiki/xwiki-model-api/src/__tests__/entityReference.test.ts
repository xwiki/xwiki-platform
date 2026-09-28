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

type SerializerCase = [
  input: string | null,
  output: string,
  entityType: number,
  defaultValueProvider?: XWiki.DefaultValueProvider,
];

function expectSerialization(cases: SerializerCase[]): void {
  for (const [input, output, entityType, defaultValueProvider] of cases) {
    const reference = XWiki.Model.resolve(
      input,
      entityType,
      defaultValueProvider,
    );
    expect(XWiki.Model.serialize(reference), `input: [${input}]`).toEqual(
      output,
    );
  }
}

describe("Serializer", () => {
  it("Wiki reference", () => {
    expectSerialization([["some:wiki", "some:wiki", XWiki.EntityType.WIKI]]);
  });

  it("Space reference", () => {
    expectSerialization([
      [
        "wiki:Al\\.ice.B\\\\ob.Ca\\:rol",
        "wiki:Al\\.ice.B\\\\ob.Ca\\:rol",
        XWiki.EntityType.SPACE,
      ],
    ]);
  });

  it("Document reference", () => {
    const type = XWiki.EntityType.DOCUMENT;
    expectSerialization([
      ["wiki:path.to.page", "wiki:path.to.page", type],
      ["wiki:path.to.", "wiki:path.to.Success", type, [null, null, "Success"]],
      ["path.to.", "the:path.to.Failure", type, ["the", null, "Failure"]],
      ["page", "page", type],
      ["", "", type],
      [".", "A.B", type, [null, "A", "B"]],
      [null, "", type],
      [
        "wiki1.wiki2:wiki3:space1.sp\\.ace2.sp\\:ace3.page",
        "wiki1.wiki2:wiki3:space1.sp\\.ace2.sp\\:ace3.page",
        type,
      ],
      ["some\\.space.page", "some\\.space.page", type],
      ["wiki:page", "wiki:page", type],
      ["\\.:@\\.", "\\.:@\\.", type],
      ["\\\\:\\\\.\\\\", "\\\\:\\\\.\\\\", type],
      // The escaping here is not necessary but we want to test that it works.
      ["\\wiki:\\space.\\page", "wiki:space.page", type],
    ]);
  });

  it("Attachment reference", () => {
    const type = XWiki.EntityType.ATTACHMENT;
    expectSerialization([
      ["wiki:space.page@filename", "wiki:space.page@filename", type],
      ["", "", type],
      ["wiki:space.page@my.png", "wiki:space.page@my.png", type],
      ["some:file.name", "some:file.name", type],
      [":.\\@", ":.\\@", type],
    ]);
  });

  it("Reference with child", () => {
    const reference = XWiki.Model.resolve(
      "wiki:Space.Page",
      XWiki.EntityType.DOCUMENT,
    )!;
    expect(XWiki.Model.serialize(reference.parent)).toEqual("wiki:Space");
    expect(XWiki.Model.serialize(reference.parent!.parent)).toEqual("wiki");
  });

  it("Object reference", () => {
    const type = XWiki.EntityType.OBJECT;
    expectSerialization([
      ["wiki:space.page^Object", "wiki:space.page^Object", type],
      ["", "", type],
      // Property reference with no object.
      ["wiki:space.page.property", "wiki:space.page.property", type],
      // Test escaping character.
      ["wiki:space.page^Obje\\^ct", "wiki:space.page^Obje\\^ct", type],
      ["wiki:spa^ce.page^Obje\\^ct", "wiki:spa^ce.page^Obje\\^ct", type],
      [":.\\^@", ":.\\^@", type],
    ]);
  });

  it("Object property reference", () => {
    const type = XWiki.EntityType.OBJECT_PROPERTY;
    expectSerialization([
      [
        "wiki:space.page^xwiki.class[0].prop",
        "wiki:space.page^xwiki.class[0].prop",
        type,
      ],
      ["", "", type],
      // Using separators.
      ["space^page@attachment", "space^page@attachment", type],
      ["wiki:space^object", "wiki:space^object", type],
      // Test escaping character.
      [
        "wiki:space.page^xwiki.class[0].prop\\.erty",
        "wiki:space.page^xwiki.class[0].prop\\.erty",
        type,
      ],
      [":\\.^@", ":\\.^@", type],
    ]);
  });

  it("Class property reference", () => {
    const type = XWiki.EntityType.CLASS_PROPERTY;
    expectSerialization([
      ["wiki:space.page^ClassProperty", "wiki:space.page^ClassProperty", type],
      ["", "", type],
      // Property reference with no object.
      ["wiki:space.page.property", "wiki:space\\.page\\.property", type],
      // Test escaping character.
      ["wiki:space.page^Obje\\^ct", "wiki:space.page^Obje\\^ct", type],
      ["wiki:spa^ce.page^Obje\\^ct", "wiki:spa^ce.page^Obje\\^ct", type],
      [":.\\^@", ":\\.\\^@", type],
    ]);
  });

  it("Relative reference", () => {
    let reference = new XWiki.EntityReference(
      "page",
      XWiki.EntityType.DOCUMENT,
    );
    expect(XWiki.Model.serialize(reference)).toEqual("page");

    reference = new XWiki.EntityReference(
      "page",
      XWiki.EntityType.DOCUMENT,
      new XWiki.EntityReference("space", XWiki.EntityType.SPACE),
    );
    expect(XWiki.Model.serialize(reference)).toEqual("space.page");
  });

  it("escapes every escaping character of a wiki name", () => {
    const reference = new XWiki.WikiReference("a\\b\\c");
    expect(XWiki.Model.serialize(reference)).toEqual("a\\\\b\\\\c");
    // The serialization has to round-trip through the resolver.
    const resolved = XWiki.Model.resolve(
      XWiki.Model.serialize(reference),
      XWiki.EntityType.WIKI,
    );
    expect(resolved!.name).toEqual("a\\b\\c");
  });
});

describe("Resolver", () => {
  function serializeChain(reference: XWiki.EntityReference): string {
    return reference
      .getReversedReferenceChain()
      .map(
        (component) =>
          XWiki.EntityType.getName(component.type)!.toUpperCase() +
          ":" +
          component.name,
      )
      .join(" ");
  }

  const baseReference = XWiki.Model.resolve(
    "wiki:path.to.page",
    XWiki.EntityType.DOCUMENT,
  )!;

  it("Document reference", () => {
    const type = XWiki.EntityType.DOCUMENT;
    const cases: [
      string,
      number | null | undefined,
      XWiki.DefaultValueProvider,
      string,
    ][] = [
      [
        "wiki:path.to.page",
        type,
        undefined,
        "WIKI:wiki SPACE:path SPACE:to DOCUMENT:page",
      ],
      [
        "wiki:space.",
        type,
        baseReference,
        "WIKI:wiki SPACE:space DOCUMENT:page",
      ],
      ["", type, baseReference, "WIKI:wiki SPACE:path SPACE:to DOCUMENT:page"],
      [
        ".one.Two",
        type,
        baseReference,
        "WIKI:wiki SPACE:path SPACE:to SPACE:one DOCUMENT:Two",
      ],
      ["one.Two", type, baseReference, "WIKI:wiki SPACE:one DOCUMENT:Two"],
      ["space.", type, [null, null, "Test"], "SPACE:space DOCUMENT:Test"],
      ["page", type, undefined, "DOCUMENT:page"],
      ["", type, () => "X", "WIKI:X SPACE:X DOCUMENT:X"],
      [
        "wiki1.wiki2:wiki3:space1.sp\\.ace2.sp\\:ace3.page",
        type,
        undefined,
        "WIKI:wiki1.wiki2:wiki3 SPACE:space1 SPACE:sp.ace2 SPACE:sp:ace3 DOCUMENT:page",
      ],
      ["one.two.page", type, undefined, "SPACE:one SPACE:two DOCUMENT:page"],
      ["wiki:page", type, undefined, "DOCUMENT:wiki:page"],
      // Resolve without entity type.
      [
        "document:wiki:path.to.page",
        undefined,
        undefined,
        "WIKI:wiki SPACE:path SPACE:to DOCUMENT:page",
      ],
      [
        "document:",
        null,
        baseReference,
        "WIKI:wiki SPACE:path SPACE:to DOCUMENT:page",
      ],
    ];
    for (const [input, entityType, provider, expected] of cases) {
      const reference = XWiki.Model.resolve(input, entityType, provider)!;
      expect(serializeChain(reference), `input: [${input}]`).toEqual(expected);
    }
  });

  it("returns undefined when there is nothing to resolve", () => {
    for (const input of [".", null, ""]) {
      const reference = XWiki.Model.resolve(input, XWiki.EntityType.DOCUMENT);
      expect(reference, `input: [${input}]`).toBeUndefined();
    }
  });

  it("unescapes the escaping characters", () => {
    const reference = XWiki.Model.resolve(
      "\\\\\\.:@\\.",
      XWiki.EntityType.DOCUMENT,
    )!;
    expect(reference.extractReference(XWiki.EntityType.WIKI)).toBeUndefined();
    expect(reference.extractReference(XWiki.EntityType.SPACE)).toBeUndefined();
    expect(reference.name).toEqual("\\.:@.");
  });

  it("keeps the escaped space separator", () => {
    const reference = XWiki.Model.resolve(
      "some\\.space.page",
      XWiki.EntityType.DOCUMENT,
    )!;
    expect(reference.extractReference(XWiki.EntityType.WIKI)).toBeUndefined();
    expect(reference.extractReference(XWiki.EntityType.SPACE)!.name).toEqual(
      "some.space",
    );
    expect(reference.name).toEqual("page");
  });

  it("unescapes the doubled escaping characters", () => {
    const reference = XWiki.Model.resolve(
      "\\\\:\\\\.\\\\",
      XWiki.EntityType.DOCUMENT,
    )!;
    expect(reference.extractReference(XWiki.EntityType.WIKI)!.name).toEqual(
      "\\",
    );
    expect(reference.extractReference(XWiki.EntityType.SPACE)!.name).toEqual(
      "\\",
    );
    expect(reference.name).toEqual("\\");
  });

  it("drops the unnecessary escaping characters", () => {
    const reference = XWiki.Model.resolve(
      "\\wiki:\\space.\\page",
      XWiki.EntityType.DOCUMENT,
    )!;
    expect(reference.extractReference(XWiki.EntityType.WIKI)!.name).toEqual(
      "wiki",
    );
    expect(reference.extractReference(XWiki.EntityType.SPACE)!.name).toEqual(
      "space",
    );
    expect(reference.name).toEqual("page");
  });

  it("rejects unsupported entity types", () => {
    expect(() => XWiki.Model.resolve("x", 99)).toThrow(
      "No parsing definition found for Entity Type [99]",
    );
    expect(() => XWiki.Model.resolve("x", "bogus")).toThrow(
      "No parsing definition found for Entity Type [NaN]",
    );
  });
});

describe("Misc", () => {
  function attach(serializedReference: string): XWiki.EntityReference {
    return XWiki.Model.resolve(
      serializedReference,
      XWiki.EntityType.ATTACHMENT,
    )!;
  }
  function doc(serializedReference: string): XWiki.EntityReference {
    return XWiki.Model.resolve(serializedReference, XWiki.EntityType.DOCUMENT)!;
  }
  function space(serializedReference: string): XWiki.EntityReference {
    return XWiki.Model.resolve(serializedReference, XWiki.EntityType.SPACE)!;
  }
  function wiki(wikiName: string): XWiki.EntityReference {
    return new XWiki.WikiReference(wikiName);
  }

  it("relativeTo", () => {
    const cases: [XWiki.EntityReference, XWiki.EntityReference, string][] = [
      // Absolute base reference.
      [doc("wiki:Path.To.Page"), attach("wiki:Path.To.Page@file"), ""],
      [doc("wiki:Path.To.Page"), doc("wiki:Path.To.Page"), ""],
      [doc("wiki:Path.To.Page"), space("wiki:Path.To"), "Page"],
      [doc("wiki:Path.To.Page"), space("wiki:Path"), "To.Page"],
      [doc("wiki:Path.To.Page"), wiki("wiki"), "Path.To.Page"],

      [doc("wiki:Path.To.Page"), wiki("xwiki"), "wiki:Path.To.Page"],
      [doc("wiki:Path.To.Page"), doc("wiki:Some.Other.Page"), "Path.To.Page"],
      [doc("wiki:Path.To.Page"), doc("wiki:Path.Of.Page"), "Path.To.Page"],
      [doc("wiki:Path.To.Page"), doc("wiki:Path.Page"), "To.Page"],
      [doc("wiki:Path.To.Page"), doc("wiki:Path.To.OtherPage"), "Page"],
      [doc("wiki:Path.To.Page"), space("wiki:Path.Space"), "Path.To.Page"],
      [doc("wiki:Path.To.Page"), space("wiki:Path.From.Page"), "Path.To.Page"],

      [space("wiki:Path.To.Space"), doc("wiki:Path.To.Space.Home"), ""],
      [space("wiki:Path.To.Space"), doc("wiki:Path.To.Page"), "Space"],
      [space("wiki:Path.To.Space"), doc("wiki:Path.Page"), "To.Space"],
      [space("wiki:Path.To.Space"), doc("wiki:Path.Of.Page"), "Path.To.Space"],

      [wiki("wiki"), doc("wiki:Path.To.Page"), ""],
      [wiki("wiki"), space("xwiki:Home"), "wiki"],

      // Relative base reference.
      [doc("wiki:Path.To.Page"), attach("file"), ""],
      [doc("wiki:Path.To.Page"), attach("OtherPage@file"), "Page"],
      [doc("wiki:Path.To.Page"), attach("Path.OtherPage@file"), "To.Page"],
      [doc("wiki:Path.To.Page"), attach("Path.Of.Page@file"), "Path.To.Page"],
      [doc("wiki:Path.To.Page"), doc("Page"), ""],
      [doc("wiki:Path.To.Page"), space("Path.To"), "Page"],
      [doc("wiki:Path.To.Page"), space("Path"), "To.Page"],
    ];
    for (const [reference, baseReference, expected] of cases) {
      const actual = XWiki.Model.serialize(reference.relativeTo(baseReference));
      expect(actual, `[${reference}] relative to [${baseReference}]`).toEqual(
        expected,
      );
    }
  });

  it("constructor", () => {
    // Construct a Nested Space reference
    let reference: XWiki.EntityReference = new XWiki.SpaceReference("wiki", [
      "space1",
      "space2",
    ]);
    expect(XWiki.Model.serialize(reference)).toEqual("wiki:space1.space2");
    reference = new XWiki.DocumentReference(
      "wiki",
      ["space1", "space2"],
      "page",
    );
    expect(XWiki.Model.serialize(reference)).toEqual("wiki:space1.space2.page");
    // Construct a non-Nested Space reference
    reference = new XWiki.SpaceReference("wiki", "space");
    expect(XWiki.Model.serialize(reference)).toEqual("wiki:space");
    // Try passing non-valid space parameters
    expect(() => new XWiki.SpaceReference("wiki", [])).toThrow(
      "Missing mandatory space name or invalid type for: []",
    );
    expect(
      () => new XWiki.SpaceReference("wiki", 12 as unknown as string),
    ).toThrow("Missing mandatory space name or invalid type for: [12]");
  });

  it("throws the error message as a plain string", () => {
    let thrown: unknown;
    try {
      new XWiki.SpaceReference("wiki", []);
    } catch (error) {
      thrown = error;
    }
    expect(thrown).toBe("Missing mandatory space name or invalid type for: []");
  });

  it("equals", () => {
    const reference1 = new XWiki.DocumentReference(
      "wiki",
      ["space1", "space2"],
      "page",
    );
    const reference2 = new XWiki.DocumentReference(
      "wiki",
      ["space1", "space2"],
      "page",
    );
    const reference3 = new XWiki.DocumentReference(
      "wiki2",
      ["space1", "space2"],
      "page",
    );
    expect(reference1.equals(reference2)).toBe(true);
    expect(reference1.equals(reference3)).toBe(false);
  });

  it("hasParent", () => {
    const documentReference = new XWiki.DocumentReference(
      "wiki",
      ["Path", "To"],
      "Page",
    );

    expect(documentReference.hasParent(new XWiki.WikiReference("wiki"))).toBe(
      true,
    );
    expect(
      documentReference.hasParent(new XWiki.SpaceReference("wiki", "Path")),
    ).toBe(true);
    expect(documentReference.hasParent(documentReference.parent)).toBe(true);

    expect(new XWiki.WikiReference("wiki").hasParent(null)).toBe(true);

    expect(documentReference.hasParent(new XWiki.WikiReference("xwiki"))).toBe(
      false,
    );
    expect(
      documentReference.hasParent(new XWiki.SpaceReference("wiki", "To")),
    ).toBe(false);
    expect(documentReference.hasParent(null)).toBe(false);
  });
});
