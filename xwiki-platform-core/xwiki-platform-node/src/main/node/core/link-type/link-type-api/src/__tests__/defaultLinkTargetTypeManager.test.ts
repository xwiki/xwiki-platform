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
import { ComponentInit } from "../componentInit";
import { linkTargetTypeRole } from "../linkTargetType";
import { linkTargetTypeManagerRole } from "../linkTargetTypeManager";
import { unsupportedLinkTargetType } from "../unsupportedLinkTarget";
import {
  DocumentReference,
  SpaceReference,
  WikiReference,
} from "@xwiki/platform-model-api";
import { Container, injectable } from "inversify";
import { describe, expect, it } from "vitest";
import type { LinkContext } from "../linkContext";
import type { LinkTargetType } from "../linkTargetType";
import type { LinkTargetTypeManager } from "../linkTargetTypeManager";
import type { ResourceReference } from "@xwiki/platform-rendering-api";

@injectable()
class CatchAllTargetType implements LinkTargetType<{ url: string }> {
  readonly type = "url";
  readonly order = Number.MAX_SAFE_INTEGER;
  getLabel = async () => "URL";
  createDefaultConfig = () => ({ url: "" });
  fromURL = (url: string) => ({ url });
  toURL = (config: { url: string }) => config.url;
  // Unlike fromURL, this is deliberately NOT a catch-all — see URLLinkTargetType.
  fromReference = (reference: ResourceReference) =>
    reference.type === "url" ? { url: reference.reference } : null;
  toReference = (config: { url: string }): ResourceReference => ({
    type: "url",
    typed: false,
    reference: config.url,
    parameters: {},
  });
}

@injectable()
class PageTargetType implements LinkTargetType<{ ref: string }> {
  readonly type = "page";
  readonly order = 0;
  getLabel = async () => "Page";
  createDefaultConfig = () => ({ ref: "" });
  fromURL = (url: string) =>
    url.startsWith("page:") ? { ref: url.slice("page:".length) } : null;
  toURL = (config: { ref: string }) => `page:${config.ref}`;
  fromReference = (reference: ResourceReference) =>
    reference.type === "doc" ? { ref: reference.reference } : null;
  toReference = (config: { ref: string }): ResourceReference => ({
    type: "doc",
    typed: true,
    reference: config.ref,
    parameters: {},
  });
}

// Prefixes the page references with the name of the source document of the link, to check that the link context
// reaches the link target types.
@injectable()
class ContextAwarePageTargetType extends PageTargetType {
  fromReference = (reference: ResourceReference, context?: LinkContext) =>
    reference.type === "doc"
      ? { ref: `${context?.sourceDocument?.name}/${reference.reference}` }
      : null;
  toReference = (
    config: { ref: string },
    context?: LinkContext,
  ): ResourceReference => ({
    type: "doc",
    typed: true,
    reference: config.ref.replace(`${context?.sourceDocument?.name}/`, ""),
    parameters: {},
  });
}

const linkContext: LinkContext = {
  sourceDocument: new DocumentReference(
    "Source",
    new SpaceReference(new WikiReference("xwiki"), "Space"),
    true,
  ),
};

// Deliberately doesn't implement fromReference/toReference, to exercise the "link target type doesn't support
// reference conversion" skip path.
@injectable()
class NoReferenceSupportTargetType implements LinkTargetType<{ ref: string }> {
  readonly type = "no-reference-support";
  readonly order = 50;
  getLabel = async () => "No reference support";
  createDefaultConfig = () => ({ ref: "" });
  fromURL = () => null;
  toURL = (config: { ref: string }) => config.ref;
}

// Doesn't specify an order, so it comes after the link target types with an order below the default one.
@injectable()
class UnorderedTargetType implements LinkTargetType<{ value: string }> {
  readonly type = "unordered";
  getLabel = async () => "Unordered";
  createDefaultConfig = () => ({ value: "" });
  fromURL = () => null;
  toURL = (config: { value: string }) => config.value;
}

function buildContainer(
  linkTargetTypes: (new () => LinkTargetType)[],
): Container {
  const container = new Container();
  new ComponentInit(container);
  linkTargetTypes.forEach((linkTargetType) => {
    container.bind(linkTargetTypeRole).to(linkTargetType);
  });
  return container;
}

function getManager(container: Container): LinkTargetTypeManager {
  return container.get<LinkTargetTypeManager>(linkTargetTypeManagerRole);
}

describe("list", () => {
  it("lists the registered link target types by ascending order", () => {
    const manager = getManager(
      buildContainer([
        CatchAllTargetType,
        UnorderedTargetType,
        PageTargetType,
        NoReferenceSupportTargetType,
      ]),
    );

    expect(manager.list().map((linkTargetType) => linkTargetType.type)).toEqual(
      ["page", "no-reference-support", "unordered", "url"],
    );
  });

  it("lists nothing when no link target type is registered", () => {
    expect(getManager(buildContainer([])).list()).toEqual([]);
  });

  it("lists the link target types registered after a previous resolution of the manager", () => {
    const container = buildContainer([PageTargetType]);
    getManager(container);

    container.bind(linkTargetTypeRole).to(CatchAllTargetType);

    expect(
      getManager(container)
        .list()
        .map((linkTargetType) => linkTargetType.type),
    ).toEqual(["page", "url"]);
  });

  it("returns a copy of the list", () => {
    const manager = getManager(buildContainer([PageTargetType]));

    manager.list().pop();

    expect(manager.list()).toHaveLength(1);
  });
});

describe("fromURL", () => {
  it("returns the target of the first link target type accepting the URL, in order", () => {
    const manager = getManager(
      buildContainer([CatchAllTargetType, PageTargetType]),
    );

    expect(manager.fromURL("page:Some.Page")).toEqual({
      type: "page",
      config: { ref: "Some.Page" },
    });
  });

  it("falls back to the catch-all (highest order) when nothing else accepts the URL", () => {
    const manager = getManager(
      buildContainer([CatchAllTargetType, PageTargetType]),
    );

    expect(manager.fromURL("https://example.org")).toEqual({
      type: "url",
      config: { url: "https://example.org" },
    });
  });

  it("throws when no registered link target type accepts the URL", () => {
    const manager = getManager(buildContainer([PageTargetType]));

    expect(() => manager.fromURL("https://example.org")).toThrow(
      "No link target type accepts the URL [https://example.org]",
    );
  });
});

describe("toURL", () => {
  it("converts using the link target type matching the target's type", () => {
    const manager = getManager(
      buildContainer([CatchAllTargetType, PageTargetType]),
    );

    expect(manager.toURL({ type: "page", config: { ref: "Some.Page" } })).toBe(
      "page:Some.Page",
    );
  });

  it("throws when no registered link target type matches the target's type", () => {
    const manager = getManager(buildContainer([PageTargetType]));

    expect(() => manager.toURL({ type: "email", config: {} })).toThrow(
      "No link target type of type [email]",
    );
  });
});

describe("fromReference", () => {
  it("returns the target of the first link target type accepting the reference, in order", () => {
    const manager = getManager(
      buildContainer([CatchAllTargetType, PageTargetType]),
    );

    expect(
      manager.fromReference({
        type: "doc",
        typed: true,
        reference: "Some.Page",
        parameters: {},
      }),
    ).toEqual({ type: "page", config: { ref: "Some.Page" } });
  });

  it("skips the link target types that don't implement fromReference", () => {
    const manager = getManager(
      buildContainer([NoReferenceSupportTargetType, PageTargetType]),
    );

    expect(
      manager.fromReference({
        type: "doc",
        typed: true,
        reference: "Some.Page",
        parameters: {},
      }),
    ).toEqual({ type: "page", config: { ref: "Some.Page" } });
  });

  it("passes the link context to the link target types", () => {
    const manager = getManager(buildContainer([ContextAwarePageTargetType]));

    expect(
      manager.fromReference(
        { type: "doc", typed: true, reference: "Some.Page", parameters: {} },
        linkContext,
      ),
    ).toEqual({ type: "page", config: { ref: "Source/Some.Page" } });
  });

  it("returns an unsupported target holding a copy of the reference when no link target type accepts it", () => {
    const manager = getManager(buildContainer([PageTargetType]));
    const reference = {
      type: "interwiki",
      typed: true,
      reference: "wikipedia:XWiki",
      parameters: { anchor: "History" },
    };

    const target = manager.fromReference(reference);

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

describe("toReference", () => {
  it("converts using the link target type matching the target's type", () => {
    const manager = getManager(
      buildContainer([CatchAllTargetType, PageTargetType]),
    );

    expect(
      manager.toReference({ type: "page", config: { ref: "Some.Page" } }),
    ).toEqual({
      type: "doc",
      typed: true,
      reference: "Some.Page",
      parameters: {},
    });
  });

  it("passes the link context to the link target type", () => {
    const manager = getManager(buildContainer([ContextAwarePageTargetType]));

    expect(
      manager.toReference(
        { type: "page", config: { ref: "Source/Some.Page" } },
        linkContext,
      )?.reference,
    ).toBe("Some.Page");
  });

  it("returns undefined when the matching link target type doesn't implement toReference", () => {
    const manager = getManager(buildContainer([NoReferenceSupportTargetType]));

    expect(
      manager.toReference({
        type: "no-reference-support",
        config: { ref: "x" },
      }),
    ).toBeUndefined();
  });

  it("returns the reference held by an unsupported target, keeping its type, typed flag and parameters", () => {
    const manager = getManager(buildContainer([PageTargetType]));
    const reference = {
      type: "interwiki",
      typed: false,
      reference: "wikipedia:Wiki",
      parameters: { anchor: "History" },
    };

    expect(
      manager.toReference({
        type: unsupportedLinkTargetType,
        config: { reference },
      }),
    ).toBe(reference);
  });

  it("returns undefined for an unsupported target with an empty reference", () => {
    const manager = getManager(buildContainer([PageTargetType]));

    expect(
      manager.toReference({
        type: unsupportedLinkTargetType,
        config: {
          reference: {
            type: "interwiki",
            typed: true,
            reference: " ",
            parameters: {},
          },
        },
      }),
    ).toBeUndefined();
  });

  it("returns undefined when no registered link target type matches the target's type", () => {
    const manager = getManager(buildContainer([PageTargetType]));

    expect(manager.toReference({ type: "email", config: {} })).toBeUndefined();
  });
});
