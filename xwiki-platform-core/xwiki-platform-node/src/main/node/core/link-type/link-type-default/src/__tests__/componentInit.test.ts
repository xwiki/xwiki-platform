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
import { linkTargetTypeFormRole } from "@xwiki/platform-link-modal-ui";
import { linkTargetTypeExtensionRole } from "@xwiki/platform-link-type-api";
import { Container, injectable } from "inversify";
import { describe, expect, it } from "vitest";
import type { LinkTargetTypeForm } from "@xwiki/platform-link-modal-ui";
import type { LinkTargetTypeExtension } from "@xwiki/platform-link-type-api";

/**
 * The built-in page and attachment link target types inject the providers of the model reference and remote URL
 * services, which must then be registered in the container they are resolved from.
 */
function containerWithModelServices(): Container {
  const container = new Container();
  for (const provider of [
    "ModelReferenceParserProvider",
    "ModelReferenceSerializerProvider",
    "RemoteURLParserProvider",
    "RemoteURLSerializerProvider",
  ]) {
    container.bind(provider).toConstantValue({ get: () => undefined });
  }
  return container;
}

describe("ComponentInit", () => {
  it("registers exactly the 6 built-in link target types", () => {
    const container = containerWithModelServices();
    new ComponentInit(container);

    const extensions = container.getAll<LinkTargetTypeExtension>(
      linkTargetTypeExtensionRole,
    );

    expect(extensions.map((e) => e.type).sort()).toEqual([
      "attachment",
      "email",
      "page",
      "path",
      "unc",
      "url",
    ]);
  });

  it("lets a 3rd party add its own link target type into the same container", () => {
    @injectable()
    class CustomLinkTargetType implements LinkTargetTypeExtension {
      readonly type = "custom";
      getLabel = async () => "Custom";
      createDefaultConfig = () => undefined;
      tryParseURL = () => null;
      serializeURL = () => "";
    }

    const container = containerWithModelServices();
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
      "path",
      "unc",
      "url",
    ]);
  });

  it("registers the forms of the built-in link target types, except the path and UNC path ones", () => {
    const container = new Container();
    new ComponentInit(container);

    const forms = container.getAll<LinkTargetTypeForm>(linkTargetTypeFormRole);

    // The path and UNC path link target types are edited with the plain text form of the link modal.
    expect(forms.map((form) => form.type).sort()).toEqual([
      "attachment",
      "email",
      "page",
      "url",
    ]);
  });
});
