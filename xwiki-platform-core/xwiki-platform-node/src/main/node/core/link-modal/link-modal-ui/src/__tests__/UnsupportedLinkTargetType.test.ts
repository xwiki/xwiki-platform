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
import { UnsupportedLinkTargetType } from "../UnsupportedLinkTargetType";
import UnsupportedLinkTargetForm from "../vue/UnsupportedLinkTargetForm.vue";
import { describe, expect, it } from "vitest";
import type { UnsupportedLinkTargetConfig } from "@xwiki/platform-link-type-api";

function interwikiConfig(): UnsupportedLinkTargetConfig {
  return {
    reference: {
      type: "interwiki",
      typed: true,
      reference: "wikipedia:XWiki",
      parameters: { anchor: "History" },
    },
  };
}

describe("UnsupportedLinkTargetType", () => {
  it("is labelled with the type of the linked resource, as is, and edited with a plain text form", async () => {
    const linkTargetType = new UnsupportedLinkTargetType(interwikiConfig());

    await expect(linkTargetType.getLabel()).resolves.toBe("interwiki");
    expect(linkTargetType.component()).toBe(UnsupportedLinkTargetForm);
  });

  it("restores the original configuration, even after it or a previous default configuration was modified", () => {
    const config = interwikiConfig();
    const linkTargetType = new UnsupportedLinkTargetType(config);

    config.reference.reference = "wikipedia:Wiki";
    const defaultConfig = linkTargetType.createDefaultConfig();
    expect(defaultConfig).toEqual(interwikiConfig());

    defaultConfig.reference.parameters.anchor = "Features";
    expect(linkTargetType.createDefaultConfig()).toEqual(interwikiConfig());
  });
});
