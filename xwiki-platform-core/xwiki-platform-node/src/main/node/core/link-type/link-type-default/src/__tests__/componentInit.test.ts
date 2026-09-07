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
import { linkTargetTypeExtensionRole } from "@xwiki/platform-link-type-api";
import { Container, injectable } from "inversify";
import { describe, expect, it } from "vitest";
import type { LinkTargetTypeExtension } from "@xwiki/platform-link-type-api";

describe("ComponentInit", () => {
  it("registers exactly the 4 built-in link target types", () => {
    const container = new Container();
    new ComponentInit(container);

    const extensions = container.getAll<LinkTargetTypeExtension>(
      linkTargetTypeExtensionRole,
    );

    expect(extensions.map((e) => e.type).sort()).toEqual([
      "attachment",
      "email",
      "page",
      "url",
    ]);
  });

  it("lets a 3rd party add its own link target type into the same container", () => {
    @injectable()
    class CustomLinkTargetType implements LinkTargetTypeExtension {
      readonly type = "custom";
      getLabel = () => "Custom";
      createDefaultConfig = () => undefined;
      component = (): never => {
        throw new Error("not used in this test");
      };
      tryParseUrl = () => null;
      serializeUrl = () => "";
    }

    const container = new Container();
    new ComponentInit(container);
    container.bind(linkTargetTypeExtensionRole).to(CustomLinkTargetType);

    const extensions = container.getAll<LinkTargetTypeExtension>(
      linkTargetTypeExtensionRole,
    );

    expect(extensions.map((e) => e.type).sort()).toEqual([
      "attachment",
      "custom",
      "email",
      "page",
      "url",
    ]);
  });
});
