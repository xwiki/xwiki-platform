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

describe("EntityType", () => {
  it("declares the entity types in their historical order", () => {
    expect(XWiki.EntityType.WIKI).toBe(0);
    expect(XWiki.EntityType.SPACE).toBe(1);
    expect(XWiki.EntityType.DOCUMENT).toBe(2);
    expect(XWiki.EntityType.ATTACHMENT).toBe(3);
    expect(XWiki.EntityType.OBJECT).toBe(4);
    expect(XWiki.EntityType.OBJECT_PROPERTY).toBe(5);
    expect(XWiki.EntityType.CLASS_PROPERTY).toBe(6);
  });

  it("exposes the ordinals before the utility methods", () => {
    expect(Object.keys(XWiki.EntityType)).toEqual([
      "WIKI",
      "SPACE",
      "DOCUMENT",
      "ATTACHMENT",
      "OBJECT",
      "OBJECT_PROPERTY",
      "CLASS_PROPERTY",
      "getName",
      "byName",
    ]);
  });

  it("names the entity types in camel case", () => {
    const names = [
      "wiki",
      "space",
      "document",
      "attachment",
      "object",
      "objectProperty",
      "classProperty",
    ];
    names.forEach((name, ordinal) => {
      expect(XWiki.EntityType.getName(ordinal)).toEqual(name);
    });
    expect(XWiki.EntityType.getName(99)).toBeUndefined();
  });

  it("looks up entity types by name, ignoring the case", () => {
    expect(XWiki.EntityType.byName("document")).toBe(2);
    expect(XWiki.EntityType.byName("DOCUMENT")).toBe(2);
    expect(XWiki.EntityType.byName("objectProperty")).toBe(5);
    expect(XWiki.EntityType.byName("OBJECTPROPERTY")).toBe(5);
    // The underscore separated name is not a valid entity type name.
    expect(XWiki.EntityType.byName("OBJECT_PROPERTY")).toBe(-1);
    expect(XWiki.EntityType.byName("notAnEntityType")).toBe(-1);
  });
});

describe("EntityReference", () => {
  it("always declares the name, type, parent and locale properties", () => {
    const reference = new XWiki.EntityReference("page", 2);
    expect(Object.keys(reference)).toEqual([
      "name",
      "type",
      "parent",
      "locale",
    ]);
    expect(reference.parent).toBeUndefined();
    expect(reference.locale).toBeUndefined();
    expect(JSON.stringify(reference)).toEqual('{"name":"page","type":2}');
  });

  it("serializes itself through the Model", () => {
    const reference = new XWiki.DocumentReference("wiki", ["a", "b"], "page");
    expect(`${reference}`).toEqual("wiki:a.b.page");
  });
});
