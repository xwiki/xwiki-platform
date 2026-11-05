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
import {
  linkTargetToResourceReference,
  parseLinkTarget,
  resourceReferenceToLinkTarget,
  serializeLinkTarget,
} from "../convert";
import { linkTargetTypeExtensionRole } from "../linkTargetType";
import { unsupportedLinkTargetType } from "../unsupportedLinkTarget";
import { Container, injectable } from "inversify";
import { describe, expect, it } from "vitest";
import type { LinkTargetTypeExtension } from "../linkTargetType";
import type { ResourceReference } from "@xwiki/platform-rendering-api";

@injectable()
class CatchAllExtension implements LinkTargetTypeExtension<{ url: string }> {
  readonly type = "url";
  readonly order = Number.MAX_SAFE_INTEGER;
  getLabel = async () => "URL";
  createDefaultConfig = () => ({ url: "" });
  tryParseURL = (url: string) => ({ url });
  serializeURL = (config: { url: string }) => config.url;
  // Unlike tryParseURL, this is deliberately NOT a catch-all — see URLLinkTargetType.
  tryParseReference = (reference: ResourceReference) =>
    reference.type === "url" ? { url: reference.reference } : null;
  configToReference = (config: { url: string }): ResourceReference => ({
    type: "url",
    typed: false,
    reference: config.url,
    parameters: {},
  });
}

@injectable()
class PageExtension implements LinkTargetTypeExtension<{ ref: string }> {
  readonly type = "page";
  readonly order = 0;
  getLabel = async () => "Page";
  createDefaultConfig = () => ({ ref: "" });
  tryParseURL = (url: string) =>
    url.startsWith("page:") ? { ref: url.slice("page:".length) } : null;
  serializeURL = (config: { ref: string }) => `page:${config.ref}`;
  tryParseReference = (reference: ResourceReference) =>
    reference.type === "doc" ? { ref: reference.reference } : null;
  configToReference = (config: { ref: string }): ResourceReference => ({
    type: "doc",
    typed: true,
    reference: config.ref,
    parameters: {},
  });
}

// Deliberately doesn't implement tryParseReference/configToReference, to exercise the "extension
// doesn't support reference conversion" skip path.
@injectable()
class NoReferenceSupportExtension
  implements LinkTargetTypeExtension<{ ref: string }>
{
  readonly type = "no-reference-support";
  readonly order = 50;
  getLabel = async () => "No reference support";
  createDefaultConfig = () => ({ ref: "" });
  tryParseURL = () => null;
  serializeURL = (config: { ref: string }) => config.ref;
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

    const target = parseLinkTarget("page:Some.Page", container);

    expect(target).toEqual({ type: "page", config: { ref: "Some.Page" } });
  });

  it("falls back to the catch-all (lowest order) when nothing else matches", () => {
    const container = buildContainer([CatchAllExtension, PageExtension]);

    const target = parseLinkTarget("https://example.org", container);

    expect(target).toEqual({
      type: "url",
      config: { url: "https://example.org" },
    });
  });

  it("throws when no registered extension matches", () => {
    const container = buildContainer([PageExtension]);

    expect(() => parseLinkTarget("https://example.org", container)).toThrow();
  });
});

describe("serializeLinkTarget", () => {
  it("serializes using the extension matching the target's type", () => {
    const container = buildContainer([CatchAllExtension, PageExtension]);

    const url = serializeLinkTarget(
      { type: "page", config: { ref: "Some.Page" } },
      container,
    );

    expect(url).toBe("page:Some.Page");
  });

  it("throws when no registered extension matches the target's type", () => {
    const container = buildContainer([PageExtension]);

    expect(() =>
      serializeLinkTarget({ type: "email", config: {} }, container),
    ).toThrow();
  });
});

describe("resourceReferenceToLinkTarget", () => {
  it("returns the type contributed by the first matching extension, in order", () => {
    const container = buildContainer([CatchAllExtension, PageExtension]);

    const target = resourceReferenceToLinkTarget(
      { type: "doc", typed: true, reference: "Some.Page", parameters: {} },
      container,
    );

    expect(target).toEqual({ type: "page", config: { ref: "Some.Page" } });
  });

  it("skips extensions that don't implement tryParseReference", () => {
    const container = buildContainer([
      NoReferenceSupportExtension,
      PageExtension,
    ]);

    const target = resourceReferenceToLinkTarget(
      { type: "doc", typed: true, reference: "Some.Page", parameters: {} },
      container,
    );

    expect(target).toEqual({ type: "page", config: { ref: "Some.Page" } });
  });

  it("returns an unsupported target holding a copy of the reference when no extension claims its type", () => {
    const container = buildContainer([PageExtension]);
    const reference = {
      type: "interwiki",
      typed: true,
      reference: "wikipedia:XWiki",
      parameters: { anchor: "History" },
    };

    const target = resourceReferenceToLinkTarget(reference, container);

    expect(target).toEqual({
      type: unsupportedLinkTargetType,
      config: { reference },
    });

    // Editing the target must not modify the reference of the edited link.
    const { reference: copy } = target.config as {
      reference: typeof reference;
    };
    copy.reference = "wikipedia:Wiki";
    copy.parameters.anchor = "Features";
    expect(reference).toEqual({
      type: "interwiki",
      typed: true,
      reference: "wikipedia:XWiki",
      parameters: { anchor: "History" },
    });
  });
});

describe("linkTargetToResourceReference", () => {
  it("converts using the extension matching the target's type", () => {
    const container = buildContainer([CatchAllExtension, PageExtension]);

    const reference = linkTargetToResourceReference(
      { type: "page", config: { ref: "Some.Page" } },
      container,
    );

    expect(reference).toEqual({
      type: "doc",
      typed: true,
      reference: "Some.Page",
      parameters: {},
    });
  });

  it("returns undefined when the matching extension doesn't implement configToReference", () => {
    const container = buildContainer([NoReferenceSupportExtension]);

    const reference = linkTargetToResourceReference(
      { type: "no-reference-support", config: { ref: "x" } },
      container,
    );

    expect(reference).toBeUndefined();
  });

  it("returns the reference held by an unsupported target, keeping its type, typed flag and parameters", () => {
    const container = buildContainer([PageExtension]);
    const reference = {
      type: "interwiki",
      typed: false,
      reference: "wikipedia:Wiki",
      parameters: { anchor: "History" },
    };

    expect(
      linkTargetToResourceReference(
        { type: unsupportedLinkTargetType, config: { reference } },
        container,
      ),
    ).toBe(reference);
  });

  it("returns undefined for an unsupported target with an empty reference", () => {
    const container = buildContainer([PageExtension]);

    expect(
      linkTargetToResourceReference(
        {
          type: unsupportedLinkTargetType,
          config: {
            reference: {
              type: "interwiki",
              typed: true,
              reference: " ",
              parameters: {},
            },
          },
        },
        container,
      ),
    ).toBeUndefined();
  });

  it("returns undefined when no registered extension matches the target's type", () => {
    const container = buildContainer([PageExtension]);

    const reference = linkTargetToResourceReference(
      { type: "email", config: {} },
      container,
    );

    expect(reference).toBeUndefined();
  });
});
