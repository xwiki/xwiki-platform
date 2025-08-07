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
import { NextcloudRemoteURLParser } from "../nextcloudRemoteURLParser";
import {
  AttachmentReference,
  DocumentReference,
  SpaceReference,
  WikiReference,
} from "@xwiki/cristal-model-api";
import { describe, expect, it } from "vitest";
import type { CristalApp, WikiConfig } from "@xwiki/cristal-api";

describe("NextcloudRemoteURLParser", () => {
  it("parse default root url", () => {
    const nextcloudRemoteUrlParser = new NextcloudRemoteURLParser({
      getWikiConfig() {
        return { baseRestURL: "http://localhost/remote.php/dav" } as WikiConfig;
      },
    } as CristalApp);
    const entityReference = nextcloudRemoteUrlParser.parse(
      "http://localhost/remote.php/dav/files/testuser/.cristal/home",
    );
    expect(entityReference).toEqual(
      new SpaceReference(new WikiReference("testuser"), "home"),
    );
  });
  it("parse default root url with spaces", () => {
    const nextcloudRemoteUrlParser = new NextcloudRemoteURLParser({
      getWikiConfig() {
        return { baseRestURL: "http://localhost/remote.php/dav" } as WikiConfig;
      },
    } as CristalApp);
    const entityReference = nextcloudRemoteUrlParser.parse(
      "http://localhost/remote.php/dav/files/testuser/.cristal/a/b/c.md",
    );
    expect(entityReference).toEqual(
      new DocumentReference(
        "c",
        new SpaceReference(new WikiReference("testuser"), "a", "b"),
      ),
    );
  });
  it("parse public root url", () => {
    const nextcloudRemoteUrlParser = new NextcloudRemoteURLParser({
      getWikiConfig() {
        return {
          baseRestURL: "http://localhost/public.php/webdav",
          storageRoot: "/.cristal",
        } as WikiConfig;
      },
    } as CristalApp);
    const entityReference = nextcloudRemoteUrlParser.parse(
      "http://localhost/public.php/webdav/.cristal/a/b/c.md",
    );
    expect(entityReference).toEqual(
      new DocumentReference("c", new SpaceReference(undefined, "a", "b")),
    );
  });
  it("parse attachment", () => {
    const nextcloudRemoteUrlParser = new NextcloudRemoteURLParser({
      getWikiConfig() {
        return {
          baseRestURL: "http://localhost/remote.php/dav",
          storageRoot: "/files/${username}/.cristal",
        } as WikiConfig;
      },
    } as CristalApp);
    const entityReference = nextcloudRemoteUrlParser.parse(
      "http://localhost/remote.php/dav/files/testuser/.cristal/a/b/.c/attachments/image.jpg",
    );
    expect(entityReference).toEqual(
      new AttachmentReference(
        "image.jpg",
        new DocumentReference(
          "c",
          new SpaceReference(new WikiReference("testuser"), "a", "b"),
        ),
      ),
    );
  });
});
