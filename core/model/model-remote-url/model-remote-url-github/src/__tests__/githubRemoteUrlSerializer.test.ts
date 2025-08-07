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
import { GitHubRemoteURLSerializer } from "../gitHubRemoteURLSerializer";
import { CristalApp, WikiConfig } from "@xwiki/cristal-api";
import {
  AttachmentReference,
  DocumentReference,
} from "@xwiki/cristal-model-api";
import { describe, expect, it } from "vitest";
import { mock } from "vitest-mock-extended";

describe("GithubRemoteUrlSerializer", () => {
  const cristalAppMock = mock<CristalApp>();
  const wikiConfig = mock<WikiConfig>();
  wikiConfig.baseURL =
    "https://raw.githubusercontent.com/USERNAME/REPOSITORY/BRANCH";
  cristalAppMock.getWikiConfig.mockReturnValue(wikiConfig);
  const githubRemoteUrlSerializer = new GitHubRemoteURLSerializer(
    cristalAppMock,
  );

  it("serialize remote attachment", () => {
    const serialize = githubRemoteUrlSerializer.serialize(
      new AttachmentReference("file.ext", new DocumentReference("name")),
    );
    expect(serialize).toEqual(
      "https://raw.githubusercontent.com/USERNAME/REPOSITORY/BRANCH/.name/attachments/file.ext",
    );
  });

  it("serialize remote attachment with space", () => {
    const serialize = githubRemoteUrlSerializer.serialize(
      new AttachmentReference("file .ext", new DocumentReference("na me")),
    );
    expect(serialize).toEqual(
      "https://raw.githubusercontent.com/USERNAME/REPOSITORY/BRANCH/.na%20me/attachments/file%20.ext",
    );
  });
});
