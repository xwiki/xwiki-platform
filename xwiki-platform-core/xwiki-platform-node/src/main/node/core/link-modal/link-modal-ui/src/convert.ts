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

import { tryFallible } from "@xwiki/platform-fn-utils";
import { EntityType } from "@xwiki/platform-model-api";
import type { LinkTarget } from "./data/linkType";
import type { RemoteURLParser } from "@xwiki/platform-model-remote-url-api";

/**
 * Parse a link target from an URL
 *
 * @param url - URL to parse
 * @param remoteURLParser - Remote URL parser
 *
 * @returns The parsed link target
 *
 * @since 18.4.0RC-1
 * @beta
 */
export function parseLinkTarget(
  url: string,
  remoteURLParser: RemoteURLParser,
): LinkTarget {
  // When no URL is input, show the "page" link selector by default
  if (url.trim() === "") {
    return { type: "page", config: { ref: null } };
  }

  const ref = tryFallible(() => remoteURLParser.parse(url));

  switch (ref?.type) {
    case EntityType.DOCUMENT:
      return {
        type: "page",
        config: {
          ref,
          // TODO
          anchor: undefined,
          // TODO
          queryString: undefined,
        },
      };

    case EntityType.ATTACHMENT:
      return {
        type: "attachment",
        config: {
          ref,
          // TODO
          queryString: undefined,
        },
      };
  }

  const parsedUrl = tryFallible(() => new URL(url));

  if (parsedUrl?.protocol === "mailto:") {
    const params = new URLSearchParams(parsedUrl.searchParams);

    return {
      type: "email",
      config: {
        address: parsedUrl.pathname,
        messageSubject: params.get("subject") ?? undefined,
        messageBody: params.get("body") ?? undefined,
      },
    };
  }

  return { type: "url", config: { url } };
}
