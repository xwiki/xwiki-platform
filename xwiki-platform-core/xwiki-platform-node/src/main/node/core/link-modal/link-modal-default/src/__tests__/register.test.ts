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
import { registerLinkModalDefaults } from "../register";
import { createManager } from "@xwiki/platform-component-manager-default";
import { linkTargetTypeExtensionRole } from "@xwiki/platform-link-modal-api";
import { injectable } from "inversify";
import { describe, expect, it } from "vitest";
import type { LinkTargetTypeExtension } from "@xwiki/platform-link-modal-api";

describe("registerLinkModalDefaults", () => {
  it("registers exactly the 4 built-in link target types", async () => {
    const manager = createManager();
    registerLinkModalDefaults(manager);

    const resolver = await manager.build();
    const extensions = await resolver.getAllAsync<LinkTargetTypeExtension>(
      linkTargetTypeExtensionRole,
    );

    expect(extensions.map((e) => e.type).sort()).toEqual([
      "attachment",
      "email",
      "page",
      "url",
    ]);
  });

  it("lets a 3rd-party registration override a built-in type by using the same name at a lower priority", async () => {
    @injectable()
    class OverridePage implements LinkTargetTypeExtension {
      readonly type = "page";
      getLabel = () => "Custom page";
      createDefaultConfig = () => undefined;
      component = (): never => {
        throw new Error("not used in this test");
      };
      tryParseUrl = () => null;
      serializeUrl = () => "";
    }

    const manager = createManager();
    registerLinkModalDefaults(manager);
    manager.registerComponent(
      linkTargetTypeExtensionRole,
      async () => OverridePage,
      {
        name: "page",
        priority: 1,
      },
    );

    const resolver = await manager.build();
    const page = await resolver.getAsync<LinkTargetTypeExtension>(
      linkTargetTypeExtensionRole,
      "page",
    );

    expect(page).toBeInstanceOf(OverridePage);
  });
});
