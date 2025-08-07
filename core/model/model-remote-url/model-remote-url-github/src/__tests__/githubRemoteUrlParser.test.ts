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
import { GitHubRemoteURLParser } from "../gitHubRemoteURLParser";
import {
  AttachmentReference,
  DocumentReference,
  EntityType,
} from "@xwiki/cristal-model-api";
import { describe, expect, it } from "vitest";
import type { CristalApp } from "@xwiki/cristal-api";

describe("GithubRemoteURLParser", () => {
  const gitHubRemoteURLParser = new GitHubRemoteURLParser({
    getWikiConfig() {
      return {
        baseRestURL: "https://api.github.com/repos/account/project",
        baseURL: "https://raw.githubusercontent.com/account/project/main",
      };
    },
  } as CristalApp);
  it("parse url with query parameters", () => {
    const entityReference = gitHubRemoteURLParser.parse(
      "https://api.github.com/repos/account/project/contents/README.md?ref=main",
      EntityType.DOCUMENT,
    );
    expect(entityReference).toEqual(new DocumentReference("README"));
  });
  it("parse attachment url with query parameters", () => {
    const entityReference = gitHubRemoteURLParser.parse(
      "https://raw.githubusercontent.com/account/project/main/.DOC/attachments/file.png?token=AACQBMF4YMSSADGD3DU77A3IKLOEG",
      EntityType.DOCUMENT,
    );
    expect(entityReference).toEqual(
      new AttachmentReference("file.png", new DocumentReference("DOC")),
    );
  });
});
