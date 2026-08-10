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
import AttachmentConfig from "../../vue/AttachmentConfig.vue";
import { AttachmentLinkTargetType } from "../AttachmentLinkTargetType";
import { EntityType } from "@xwiki/platform-model-api";
import { describe, expect, it } from "vitest";
import { mock } from "vitest-mock-extended";
import type { AttachmentReference } from "@xwiki/platform-model-api";
import type {
  RemoteURLParser,
  RemoteURLSerializer,
} from "@xwiki/platform-model-remote-url-api";

const attachmentRef = {
  type: EntityType.ATTACHMENT,
} as unknown as AttachmentReference;

describe("AttachmentLinkTargetType", () => {
  const extension = new AttachmentLinkTargetType();

  it("has the expected identity", () => {
    expect(extension.type).toBe("attachment");
    expect(extension.getLabel("en")).toBe("Attachment");
    expect(extension.component()).toBe(AttachmentConfig);
  });

  it("parses an attachment URL, including its query string", () => {
    const remoteURLParser = mock<RemoteURLParser>();
    remoteURLParser.parse.mockReturnValue(attachmentRef);
    const remoteURLSerializer = mock<RemoteURLSerializer>();

    const config = extension.tryParseUrl(
      "https://xwiki.org/xwiki/bin/download/Some/Page/file.png?foo=bar",
      { remoteURLParser, remoteURLSerializer },
    );

    expect(config).toEqual({ ref: attachmentRef, queryString: "?foo=bar" });
  });

  it("does not match a non-attachment URL", () => {
    const remoteURLParser = mock<RemoteURLParser>();
    remoteURLParser.parse.mockReturnValue(undefined);
    const remoteURLSerializer = mock<RemoteURLSerializer>();

    expect(
      extension.tryParseUrl("https://example.org", {
        remoteURLParser,
        remoteURLSerializer,
      }),
    ).toBeNull();
  });

  it("serializes an attachment config back into a URL, including its query string", () => {
    const remoteURLParser = mock<RemoteURLParser>();
    const remoteURLSerializer = mock<RemoteURLSerializer>();
    remoteURLSerializer.serialize.mockReturnValue(
      "https://xwiki.org/xwiki/bin/download/Some/Page/file.png",
    );

    const url = extension.serializeUrl(
      { ref: attachmentRef, queryString: "?foo=bar" },
      { remoteURLParser, remoteURLSerializer },
    );

    expect(url).toBe(
      "https://xwiki.org/xwiki/bin/download/Some/Page/file.png?foo=bar",
    );
  });
});
