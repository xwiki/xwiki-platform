/*
 * See the LICENSE file distributed with this work for additional
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

import "reflect-metadata";
import { XWikiModelReferenceSerializer } from "../xWikiModelReferenceSerializer";
import {
  DocumentReference,
  SpaceReference,
  WikiReference,
} from "@xwiki/cristal-model-api";
import { describe, expect, it } from "vitest";

describe("defaultLocalURLSerializer", () => {
  const serializer = new XWikiModelReferenceSerializer();
  it("serialize a wiki", () => {
    expect(serializer.serialize(new WikiReference("wiki"))).to.eq("wiki");
  });
  it("serialize a space", () => {
    expect(
      serializer.serialize(
        new SpaceReference(new WikiReference("wiki"), "S1", "S2"),
      ),
    ).to.eq("wiki:S1.S2");
  });
  it("serialize a space with no wiki", () => {
    expect(
      serializer.serialize(new SpaceReference(undefined, "S1", "S2")),
    ).to.eq("S1.S2");
  });
  it("serialize a document reference", () => {
    expect(
      serializer.serialize(
        new DocumentReference("Page", new SpaceReference(undefined, "Space")),
      ),
    ).to.eq("Space.Page");
  });
  it("serialize a document reference without a space", () => {
    expect(serializer.serialize(new DocumentReference("Page"))).to.eq("Page");
  });
  it("serialize a document reference without a space", () => {
    expect(serializer.serialize(new DocumentReference("Page"))).to.eq("Page");
  });
});
