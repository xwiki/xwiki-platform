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
import { parseLinkTarget, serializeLinkTarget } from "../convert";
import { linkTargetTypeExtensionRole } from "../linkTargetType";
import { Container, injectable } from "inversify";
import { describe, expect, it } from "vitest";
import { mock } from "vitest-mock-extended";
import type { LinkTargetTypeExtension } from "../linkTargetType";
import type {
  RemoteURLParser,
  RemoteURLSerializer,
} from "@xwiki/platform-model-remote-url-api";

const ctx = {
  remoteURLParser: mock<RemoteURLParser>(),
  remoteURLSerializer: mock<RemoteURLSerializer>(),
};

@injectable()
class CatchAllExtension implements LinkTargetTypeExtension<{ url: string }> {
  readonly type = "url";
  readonly order = Number.MAX_SAFE_INTEGER;
  getLabel = () => "URL";
  createDefaultConfig = () => ({ url: "" });
  component = () => {
    throw new Error("not used in this test");
  };
  tryParseUrl = (url: string) => ({ url });
  serializeUrl = (config: { url: string }) => config.url;
}

@injectable()
class PageExtension implements LinkTargetTypeExtension<{ ref: string }> {
  readonly type = "page";
  readonly order = 0;
  getLabel = () => "Page";
  createDefaultConfig = () => ({ ref: "" });
  component = () => {
    throw new Error("not used in this test");
  };
  tryParseUrl = (url: string) =>
    url.startsWith("page:") ? { ref: url.slice("page:".length) } : null;
  serializeUrl = (config: { ref: string }) => `page:${config.ref}`;
}

@injectable()
class DisabledExtension implements LinkTargetTypeExtension<unknown> {
  readonly type = "disabled";
  getLabel = () => "Disabled";
  createDefaultConfig = () => undefined;
  component = () => {
    throw new Error("not used in this test");
  };
  tryParseUrl = () => null;
  serializeUrl = () => "";
}

function buildContainer(
  extensions: (new () => LinkTargetTypeExtension)[],
): Container {
  const container = new Container();

  extensions.forEach((extension) => {
    container.bind(linkTargetTypeExtensionRole).to(extension);
  });

  return container;
}

describe("parseLinkTarget", () => {
  it("returns the type contributed by the first matching extension, in order", () => {
    const container = buildContainer([CatchAllExtension, PageExtension]);

    const target = parseLinkTarget("page:Some.Page", container, ctx);

    expect(target).toEqual({ type: "page", config: { ref: "Some.Page" } });
  });

  it("falls back to the catch-all (lowest order) when nothing else matches", () => {
    const container = buildContainer([CatchAllExtension, PageExtension]);

    const target = parseLinkTarget("https://example.org", container, ctx);

    expect(target).toEqual({
      type: "url",
      config: { url: "https://example.org" },
    });
  });

  it("throws when no registered extension matches", () => {
    const container = buildContainer([PageExtension]);

    expect(() =>
      parseLinkTarget("https://example.org", container, ctx),
    ).toThrow();
  });
});

describe("serializeLinkTarget", () => {
  it("serializes using the extension matching the target's type", () => {
    const container = buildContainer([CatchAllExtension, PageExtension]);

    const url = serializeLinkTarget(
      { type: "page", config: { ref: "Some.Page" } },
      container,
      ctx,
    );

    expect(url).toBe("page:Some.Page");
  });

  it("throws when no registered extension matches the target's type", () => {
    const container = buildContainer([PageExtension]);

    expect(() =>
      serializeLinkTarget({ type: "email", config: {} }, container, ctx),
    ).toThrow();
  });
});
