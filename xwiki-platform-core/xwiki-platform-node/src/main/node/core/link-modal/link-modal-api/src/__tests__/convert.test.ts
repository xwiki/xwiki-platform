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
  listEnabledLinkTargetTypeExtensions,
  parseLinkTarget,
  resourceReferenceToLinkTarget,
  serializeLinkTarget,
} from "../convert";
import { linkTargetTypeExtensionRole } from "../linkTargetType";
import { Container, injectable } from "inversify";
import { describe, expect, it } from "vitest";
import { mock } from "vitest-mock-extended";
import type { LinkTargetTypeExtension } from "../linkTargetType";
import type {
  ModelReferenceParser,
  ModelReferenceSerializer,
} from "@xwiki/platform-model-reference-api";
import type {
  RemoteURLParser,
  RemoteURLSerializer,
} from "@xwiki/platform-model-remote-url-api";
import type { ResourceReference } from "@xwiki/platform-rendering-api";

const ctx = {
  remoteURLParser: mock<RemoteURLParser>(),
  remoteURLSerializer: mock<RemoteURLSerializer>(),
};

const referenceCtx = {
  modelReferenceParser: mock<ModelReferenceParser>(),
  modelReferenceSerializer: mock<ModelReferenceSerializer>(),
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
  // Unlike tryParseUrl, this is deliberately NOT a catch-all — see UrlLinkTargetType.
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
  getLabel = () => "Page";
  createDefaultConfig = () => ({ ref: "" });
  component = () => {
    throw new Error("not used in this test");
  };
  tryParseUrl = (url: string) =>
    url.startsWith("page:") ? { ref: url.slice("page:".length) } : null;
  serializeUrl = (config: { ref: string }) => `page:${config.ref}`;
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
  getLabel = () => "No reference support";
  createDefaultConfig = () => ({ ref: "" });
  component = () => {
    throw new Error("not used in this test");
  };
  tryParseUrl = () => null;
  serializeUrl = (config: { ref: string }) => config.ref;
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
  isEnabled = () => false;
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

describe("resourceReferenceToLinkTarget", () => {
  it("returns the type contributed by the first matching extension, in order", () => {
    const container = buildContainer([CatchAllExtension, PageExtension]);

    const target = resourceReferenceToLinkTarget(
      { type: "doc", typed: true, reference: "Some.Page", parameters: {} },
      container,
      referenceCtx,
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
      referenceCtx,
    );

    expect(target).toEqual({ type: "page", config: { ref: "Some.Page" } });
  });

  it("returns undefined, not throwing, when no registered extension claims the resource type", () => {
    const container = buildContainer([PageExtension]);

    const target = resourceReferenceToLinkTarget(
      { type: "interwiki", typed: true, reference: "wiki:Space.Page", parameters: {} },
      container,
      referenceCtx,
    );

    expect(target).toBeUndefined();
  });
});

describe("linkTargetToResourceReference", () => {
  it("converts using the extension matching the target's type", () => {
    const container = buildContainer([CatchAllExtension, PageExtension]);

    const reference = linkTargetToResourceReference(
      { type: "page", config: { ref: "Some.Page" } },
      container,
      referenceCtx,
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
      referenceCtx,
    );

    expect(reference).toBeUndefined();
  });

  it("returns undefined when no registered extension matches the target's type", () => {
    const container = buildContainer([PageExtension]);

    const reference = linkTargetToResourceReference(
      { type: "email", config: {} },
      container,
      referenceCtx,
    );

    expect(reference).toBeUndefined();
  });
});

describe("listEnabledLinkTargetTypeExtensions", () => {
  it("excludes extensions whose isEnabled() resolves to false", async () => {
    const container = buildContainer([
      CatchAllExtension,
      PageExtension,
      DisabledExtension,
    ]);

    const enabled = await listEnabledLinkTargetTypeExtensions(container);

    expect(enabled.map((e) => e.type).sort()).toEqual(["page", "url"]);
  });
});
