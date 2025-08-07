/**
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
import { GitHubModelReferenceParser } from "../gitHubModelReferenceParser";
import { DocumentReference, SpaceReference } from "@xwiki/cristal-model-api";
import { describe, expect, it } from "vitest";

describe("GitHubModelReferenceParser", () => {
  const gitHubModelReferenceParser = new GitHubModelReferenceParser();
  it("parse single segment reference", () => {
    const entityReference = gitHubModelReferenceParser.parse("a");
    expect(entityReference).toEqual(
      new DocumentReference("a", new SpaceReference(undefined)),
    );
  });

  it("parse many segments reference", () => {
    const entityReference = gitHubModelReferenceParser.parse("a.x/b/c");
    expect(entityReference).toEqual(
      new DocumentReference("c", new SpaceReference(undefined, "a.x", "b")),
    );
  });
});
